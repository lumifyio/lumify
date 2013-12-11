define([
    'flight/lib/component',
    'tpl!./dashboard',
], function(defineComponent,
    template) {
    'use strict';

    return defineComponent(DashboardView);

    function DashboardView() {
        this.defaultAttrs({
            dashboardSelector: '#dashboard'
        });

        this.after('initialize', function() {
            var self = this;
            this.$node.html(template({}));
            this.$node.on('click', this.onDashboardClicked.bind(this));
            this.on('select-all', function(e) { e.stopPropagation(); });
        });

        this.onDashboardClicked = function(event) {
            this.trigger('selectObjects');
        };
    }
});
