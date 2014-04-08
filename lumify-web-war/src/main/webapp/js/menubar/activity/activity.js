
define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    var MINIMUM_ANIMATION_DURATION_SECONDS = 2, 

        // Add activity start/end events that are triggered on document
        // * start/end events are required, 
        // * descriptions optional (event name used)
        ACTIVITIES = [

            {
                eventStarting: 'workspaceSaving',
                eventFinished: 'workspaceSaved',
                descriptions: ['Workspace saving', 'Workspace saved']
            },
            {
                eventStarting: 'workspaceDeleting',
                eventFinished: 'workspaceDeleted',
                descriptions: ['Workspace deleting', 'Workspace deleted']
            }

        ];

    return defineComponent(Activity);
    
    function Activity() {
        this.activitiesCount = 0;
        this.activities = { };

        this.after('initialize', function() {

            ACTIVITIES.forEach(function(a) {
                if (a.eventStarting && a.eventFinished) {
                    this.on(document, a.eventStarting, function(e, data) {
                        this.start(a, e, data);
                    });
                    this.on(document, a.eventFinished, function(e, data) {
                        this.finish(a, e, data);
                    });
                } else {
                    throw new Error('Activity description requires start/finish events');
                }
            }.bind(this));

            this.$node.tooltip({ 
                placement: 'right',
                html: true,
                title: 'No&nbsp;activity' 
            });

        });

        this.start = function(activity, event, data) {

            this.activitiesCount++;
            clearTimeout(this.finishDelay);

            activity.startedAt = Date.now();

            this.activities[activity.eventStarting] = activity;
            this.updateActivity(true, activity.descriptions[0] || activity.eventStarting);
        };

        var minDurationMillis = MINIMUM_ANIMATION_DURATION_SECONDS * 1000;
        this.finish = function(activity, event, data) {

            this.activitiesCount--;

            var duration = Date.now() - activity.startedAt,
                updateActivity = function() {
                    this.updateActivity(false, activity.descriptions[1] || activity.eventFinished);
                }.bind(this);

            if (duration < minDurationMillis) {
                this.finishDelay = setTimeout(function() {
                    updateActivity();
                }, minDurationMillis - duration);
            } else {
                updateActivity();
            }
        };

        this.updateActivity = function(animating, message) {
            var lastActivity = this.activitiesCount === 0;

            if (!lastActivity) {
                animating = true;
            }

            var activityIcon = this.$node.tooltip().show();
            activityIcon.attr('data-original-title', message).tooltip('fixTitle');
            activityIcon.toggleClass('animating active', animating);
        };
    }

});
