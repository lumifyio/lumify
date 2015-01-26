define([
    'flight/lib/component',
    'tpl!./dashboard',
    'util/withCollapsibleSections'
], function(defineComponent,
    template,
    withCollapsibleSections) {
    'use strict';

    return defineComponent(DashboardView, withCollapsibleSections);

    function DashboardView() {
        this.defaultAttrs({
            helpSelector: '.help',
            notificationsSelector: '.notifications-container',
            notificationsNodeSelector: '.collapsible-section'
        });

        this.after('initialize', function() {
            var self = this;
            this.$node.html(template({}));

            this.$node.on('click', this.onDashboardClicked.bind(this));
            this.on('click', {
                helpSelector: this.onHelp
            })

            this.on('select-all', function(e) {
                e.stopPropagation();
            });
            this.on(document, 'graphPaddingUpdated', this.onPaddingUpdated);
            this.on(document, 'didToggleDisplay', this.onDidToggleDisplay);
            this.on('notificationCountUpdated', this.onNotificationCountUpdated);
        });

        this.onNotificationCountUpdated = function(event, data) {
            this.$node.find('.badge')
                .removeClass('loading')
                .text(data.count);
        };

        this.onDashboardClicked = function(event) {
            this.trigger('selectObjects');
        };

        this.onHelp = function(event) {
            this.trigger('toggleHelp');
        };

        this.onDidToggleDisplay = function(event, data) {
            var $notifications = this.select('notificationsNodeSelector');

            if (data.name === 'dashboard' && data.visible) {
                require(['notifications/notifications'], function(Notifications) {
                    Notifications.attachTo($notifications, {
                        allowDismiss: false,
                        animated: false,
                        showUserDismissed: true,
                        showInformational: true
                    });
                });
            }
        };

        this.onPaddingUpdated = function(event, data) {
            this.select('helpSelector').css({
                right: data.padding.r
            });
        };
    }
});
