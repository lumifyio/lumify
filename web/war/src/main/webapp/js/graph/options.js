define([
    'flight/lib/component'
], function(
    defineComponent) {
    'use strict';

    return defineComponent(GraphOptions);

    function GraphOptions() {

        this.after('initialize', function() {
            var self = this;

            this.$node
                .html('<label>Edge Labels <input type="checkbox" checked></label>')
                .css({
                    'white-space': 'nowrap'
                })
                .find('input').on('change', function() {
                    var checked = $(this).is(':checked');
                    console.log(checked)
                    self.attr.cy.done(function(cy) {
                        cy.style().selector('edge')
                            .css({
                                content: checked ? 'data(label)' : ''
                            })
                            .update();
                    })
                });

        });

    }
});
