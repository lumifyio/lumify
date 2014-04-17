
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
                disableTooltipFlash: true,
                descriptions: ['Workspace saving', 'Workspace saved']
            },
            {
                eventStarting: 'workspaceDeleting',
                eventFinished: 'workspaceDeleted',
                disableTooltipFlash: true,
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

            this.on(document, 'ajaxSend', function(event, request, config) {
                var messages = config && config.activityMessages;

                if (messages && messages.length === 2) {
                    var activity = {
                            eventStarting: config.activityMessages[0],
                            eventFinished: config.activityMessages[1]
                        },
                        promise = config.wrappedPromise || request;

                    this.start(activity);

                    promise
                        .progress(function(complete) {
                            this.progress(activity, complete);
                        }.bind(this))
                        .always(function() {
                            this.finish(activity);
                        }.bind(this));
                }
            });

            this.$node.tooltip({ 
                placement: 'right',
                html: true,
                title: 'No&nbsp;activity' 
            });
            this.on('shown', function() {
                clearTimeout(this.hideTooltipTimeout);
            })
        });

        this.start = function(activity, event, data) {
            var desc = activity.descriptions || [];

            this.activitiesCount++;
            clearTimeout(this.finishDelay);

            activity.startedAt = Date.now();

            this.activities[activity.eventStarting] = activity;
            this.updateActivity(true, 
                (desc[0] || activity.eventStarting) + '...');
            
            if (activity.disableTooltipFlash !== true) {
                this.$node.tooltip('show');
                this.hideTooltipTimeout = _.delay(function() {
                    this.$node.tooltip('hide');
                }.bind(this), 1500)
            }
        };

        this.progress = function(activity, complete) {
            var desc = activity.descriptions || [];

            if (this.activities[activity.eventStarting]) {
                var percent = Math.round(complete * 100) + '%';
                this.updateActivity(
                    true, 
                    (desc[0] || activity.eventStarting) + '... ( ' + percent + ' )'
                );
            }
        };

        var minDurationMillis = MINIMUM_ANIMATION_DURATION_SECONDS * 1000;
        this.finish = function(activity, event, data) {

            this.activitiesCount--;

            var desc = activity.descriptions || [],
                duration = Date.now() - activity.startedAt,
                updateActivity = function() {
                    delete this.activities[activity.eventStarting];
                    this.updateActivity(false, desc[1] || activity.eventFinished);
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

            var activityIcon = this.$node.tooltip();
            
            message = message.replace(/\s+/g, '&nbsp;');

            activityIcon
                .attr('data-original-title', message)
                .tooltip('fixTitle');
            activityIcon.toggleClass('animating active', animating);

            var tooltip = activityIcon.data('tooltip'),
                tipEl = tooltip.tip();

            if (tipEl.is(':visible')) {
                tipEl.find('.tooltip-inner').html(message);
            }
        };
    }

});
