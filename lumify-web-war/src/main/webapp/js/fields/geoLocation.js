
define([
    'flight/lib/component',
    'tpl!./geoLocation',
    './withPropertyField'
], function(defineComponent, template, withPropertyField) {
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

        this.after('initialize', function() {
            var self = this;
            this.$node.html(template(this.attr));

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
    }
});
