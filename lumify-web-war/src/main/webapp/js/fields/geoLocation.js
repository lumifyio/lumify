
define([
    'flight/lib/component',
    'tpl!./geoLocation',
    './withPropertyField',
    'service/map',
    'service/config'
], function(defineComponent, template, withPropertyField, MapService, ConfigService) {
    'use strict';

    return defineComponent(GeoLocationField, withPropertyField);

    function makeNumber(v) {
        return parseFloat(v, 10);
    }

    function splitLatLon(latLonStr) {
        var parts = latLonStr.split(',');
        if (parts.length === 2) {
            return [ $.trim(parts[0]), $.trim(parts[1]) ];
        }
        return null;
    }

    function GeoLocationField() {

        this.defaultAttrs({
            descriptionSelector: '.description'
        });

        this.after('initialize', function() {
            var self = this,
                d;

            if (this.attr.predicates) {
                d = this.hasGeocoder().done(function(enabled) {
                    self.attr.hasGeocoder = enabled;
                });
            } else {
                d = $.Deferred().resolve();
            }

            d.done(function() {
                self.$node.html(template(self.attr));
                self.setupDescriptionTypeahead();
                self.on(self.select('descriptionSelector'), 'focus blur', self.onFocusDescription);
                self.on('change keyup', {
                    inputSelector: function(event) {
                        var latLon = splitLatLon(self.getValues()[1]);
                        if (latLon) {
                            var latInput = self.$node.find('input.lat'),
                                lonInput = self.$node.find('input.lon');
                            latInput.val(latLon[0]);
                            lonInput.val(latLon[1]);
                            lonInput.focus();
                        }

                        self.triggerUpdate();
                    }
                });
            })
        });

        this.onFocusDescription = function(event) {
            if (!this.attr.predicates || !this.attr.hasGeocoder) {
                return;
            }

            this.$node.toggleClass('desc-focus', event.type === 'focus');
        }

        this.triggerUpdate = function() {
            var values = this.getValues();
            this.filterUpdated(_.compact(values.map(function(v, i) {
                if (values.length === 3 && i === 0) {
                    return v;
                }
                return makeNumber(v);
            })));
        }

        this.isValid = function() {
            var self = this,
                values = this.getValues();

            return _.every(values, function(v, i) {
                if ((self.attr.hasGeocoder || !self.attr.predicates) && i === 0) {
                    return true;
                }
                if (self.attr.predicates && i === (self.attr.hasGeocoder ? 3 : 2)) {
                    return makeNumber(v) > 0;
                }
                return v.length && _.isNumber(makeNumber(v)) && !isNaN(v);
            });
        };

        this.hasGeocoder = function() {
            return (new ConfigService()).getProperties()
                .then(function(config) {
                    return config['geocoder.enabled'] === 'true';
                });
        };

        this.setupDescriptionTypeahead = function() {
            var self = this;

            this.hasGeocoder().done(function(enabled) {
                if (enabled) {
                    var mapService = new MapService(),
                        savedResults,
                        request;

                    self.select('descriptionSelector')
                        .parent().css('position', 'relative').end()
                        .typeahead({
                            items: 15,
                            minLength: 3,
                            source: function(q, process) {
                                if (request && request.abort) {
                                    request.abort();
                                }

                                request = mapService.geocode(q)
                                    .fail(function() {
                                        process([]);
                                    })
                                    .done(function(data) {
                                        savedResults = _.indexBy(data.results, 'name');
                                        process(_.keys(savedResults));
                                    });
                            },
                            updater: function(item) {
                                var result = savedResults[item];
                                if (result) {
                                    var lat = self.$node.find('.lat').val(result.latitude)
                                            .parent().removePrefixedClasses('pop-'),
                                        lon = self.$node.find('.lon').val(result.longitude)
                                            .parent().removePrefixedClasses('pop-');

                                    requestAnimationFrame(function() {
                                        lat.addClass('pop-fast');
                                        _.delay(function() {
                                            lon.addClass('pop-fast');
                                        }, 250)
                                    })

                                    if (self.isValid()) {
                                        self.triggerUpdate();
                                    } else {
                                        self.$node.find('.radius').focus();
                                    }

                                    return result.name;
                                }
                                return item;
                            }
                        });
                    }
                });
        }
    }
});
