
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
            var self = this;
            this.$node.html(template(this.attr));

            this.setupDescriptionTypeahead();
            this.on('change keyup', {
                inputSelector: function(event) {
                    var latLon = splitLatLon(this.getValues()[1]);
                    if (latLon) {
                        var latInput = self.$node.find('input.lat'),
                            lonInput = self.$node.find('input.lon');
                        latInput.val(latLon[0]);
                        lonInput.val(latLon[1]);
                        lonInput.focus();
                    }

                    var values = this.getValues();
                    this.filterUpdated(values.map(function(v, i) {
                        if (values.length === 3 && i === 0) {
                            return v;
                        }
                        return makeNumber(v);
                    }));
                }
            });
        });

        this.isValid = function() {
            var values = this.getValues();

            return _.every(values, function(v, i) {
                if (values.length === 3 && i === 0) {
                    return true;
                }
                return v.length && _.isNumber(makeNumber(v)) && !isNaN(v);
            });
        };

        this.setupDescriptionTypeahead = function() {
            var self = this;

            (new ConfigService()).getProperties()
                .done(function(config) {
                    if (config['geocoder.enabled'] === 'true') {
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
