
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

    function GeoLocationField() {

        this.after('initialize', function() {
            this.$node.html(template(this.attr));

            this.on('change keyup', {
                inputSelector: function(event) {
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
