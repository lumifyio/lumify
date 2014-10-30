
define([
    'flight/lib/component',
    'util/formatters'
], function(defineComponent, F) {
    'use strict';

    return defineComponent(Activity);

    function Activity() {
        this.activitiesCount = 0;
        this.activities = { };

        this.after('initialize', function() {
            this.on(document, 'activityUpdated', this.onActivityUpdated);
        });

        this.onActivityUpdated = function(event, data) {
            var count = data.count,
                $badge = this.$node.find('.activityCount');

            if ($badge.length === 0 && count === 0) {
                return;
            } else if ($badge.length === 0) {
                $badge = $('<div>')
                    .addClass('activityCount')
                    .appendTo(this.$node);
            }

            $badge
                .toggle(count > 0)
                .text(F.number.pretty(count));

            this.$node.toggleClass('animating', count > 0);
        };

    }

});
