
define([
    'flight/lib/component',
    'tpl!./string',
    './withPropertyField'
], function(defineComponent, template, withPropertyField) {
    'use strict';

    return defineComponent(StringField, withPropertyField);

    function StringField() {

        this.after('initialize', function() {
            var self = this;

            this.$node.html(template(this.attr));

            this.on('change keyup', {
                inputSelector: function(event) {
                    var val = $.trim($(event.target).val());

                    this.filterUpdated(val);
                }
            });
        });

        this.isValid = function() {
            return _.every(this.getValues(), function(v) {
                return $.trim(v).length > 0;
            });
        };
    }
});
