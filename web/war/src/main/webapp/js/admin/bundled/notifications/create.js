require([
    'configuration/admin/plugin',
    'util/withDataRequest',
    'hbs!admin/bundled/notifications/template'
], function(
    defineLumifyAdminPlugin,
    withDataRequest,
    template) {
    'use strict';

    return defineLumifyAdminPlugin(PluginList, {
        mixins: [withDataRequest],
        section: 'System Notifications',
        name: 'Create',
        subtitle: 'Create a New Notification'
    });

    function PluginList() {

        this.defaultAttrs({
            buttonSelector: '.btn-primary'
        })

        this.after('initialize', function() {
            this.on('click', {
                buttonSelector: this.onCreate
            });

            this.$node.html(template({}));
        });

        this.onCreate = function(event) {
            var self = this;
            require(['util/formatters'], function(F) {
                self.dataRequest('admin', 'systemNotificationCreate', {
                    severity: 'INFORMATIONAL',
                    title: 'System is all good',
                    message: 'This is a message about the system',
                    startDate: '2014-12-12 09:23'
                })
            })
        }
    }
});
