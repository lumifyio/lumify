
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
        if (parts.length == 2) {
            return [ $.trim(parts[0]), $.trim(parts[1]) ];
        }
        return null;
    }

    function GeoLocationField() {

        this.after('initialize', function() {
            var _this = this;
            this.$node.html(template(this.attr));

            this.on('change keyup', {
                inputSelector: function(event) {
                    var latLon = splitLatLon(this.getValues()[0]);
                    if (latLon) {
                        var latInput = _this.$node.find('input.lat');
                        var lonInput = _this.$node.find('input.lon');
                        latInput.val(latLon[0]);
                        lonInput.val(latLon[1]);
                        lonInput.focus();
                    }
                    this.filterUpdated(this.getValues().map(function(v) {
                        return makeNumber(v);
                    }));
                }
            });
        });

        this.isValid = function() {
            return _.every(this.getValues(), function(v) {
                return v.length && _.isNumber(makeNumber(v)) && !isNaN(v);
            });
        };
    }
});
