define([
    'flight/lib/component',
    'tpl!./dashboard',
], function(defineComponent,
    template) {
    'use strict';

    return defineComponent(DashboardView);

    function DashboardView() {
        this.defaultAttrs({
            helpSelector: '.help'
        });

        this.after('initialize', function() {
            var self = this;
            this.$node.html(template({}));

            this.$node.on('click', this.onDashboardClicked.bind(this));
            this.on('click', {
                helpSelector: this.onHelp
            })

            this.on('select-all', function(e) { e.stopPropagation(); });
            this.on(document, 'graphPaddingUpdated', this.onPaddingUpdated);
        });

        this.onDashboardClicked = function(event) {
            this.trigger('selectObjects');
        };

        this.onHelp = function(event) {
            this.trigger('toggleHelp');
        };

        this.onPaddingUpdated = function(event, data) {
            this.select('helpSelector').css({
                right: data.padding.r
            });
        };
    }
});
