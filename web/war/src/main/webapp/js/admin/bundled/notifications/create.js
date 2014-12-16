require([
    'configuration/admin/plugin',
    'util/withDataRequest',
    'hbs!admin/bundled/notifications/template'
], function(
    defineLumifyAdminPlugin,
    withDataRequest,
    template) {
    'use strict';

    var F;

    return defineLumifyAdminPlugin(PluginList, {
        mixins: [withDataRequest],
        section: 'System Notifications',
        name: 'Create',
        subtitle: 'Create a New Notification'
    });

    function PluginList() {

        this.defaultAttrs({
            buttonSelector: '.btn-primary',
            inputSelector: 'input,textarea'
        })

        this.after('initialize', function() {
            var self = this,
                notification = this.attr.notification;

            this.on('change keyup paste', {
                inputSelector: this.checkValid
            });

            this.on('click', {
                buttonSelector: this.onCreate
            });

            require(['util/formatters'], function(_F) {
                F = _F;
                if (notification) {
                    notification.startDate = F.date.dateTimeString(notification.startDate)
                    if (notification.endDate) {
                        notification.endDate = F.date.dateTimeString(notification.endDate)
                    }
                } else {
                    notification = {
                        startDate: F.date.dateTimeString(new Date())
                    }
                }

                self.$node
                    .html(template({
                        buttonText: notification.id ? 'Update' : 'Create',
                        severity: _.map('INFORMATIONAL WARNING CRITICAL'.split(' '), function(name, i) {
                            return {
                                name: name,
                                checked: notification.severity === name || i === 0
                            }
                        }),
                        notification: notification
                    }))

                self.checkValid();
            })
        });

        this.checkValid = function() {
            var n = this.getNotification(),
                valid = (
                    n.title &&
                    n.message &&
                    n.severity &&
                    n.startDate
                );

            if (valid) {
                this.select('buttonSelector').removeAttr('disabled');
            } else {
                this.select('buttonSelector').attr('disabled', true);
            }
        }

        this.getNotification = function() {
            var self = this;

            return _.tap({
                title: $.trim(this.$node.find('.title').val()),
                message: $.trim(this.$node.find('.message').val()),
                severity: this.$node.find('*[name=severity]:checked').val(),
                startDate: $.trim(this.$node.find('.startDate').val()),
                endDate: this.$node.find('.endDate').val()
            }, function(newNotification) {
                if (self.attr.notification) {
                    newNotification.notificationId = self.attr.notification.id;
                }
                if (newNotification.startDate) {
                    newNotification.startDate = F.date.dateTimeStringUtc(newNotification.startDate)
                }
                if (newNotification.endDate) {
                    newNotification.endDate = F.date.dateTimeStringUtc(newNotification.endDate)
                }
            });
        }

        this.onCreate = function(event) {
            var self = this;
            require(['util/formatters'], function(F) {
                self.dataRequest('admin', 'systemNotificationCreate', self.getNotification())
                    .then(function() {
                        if (self.attr.notification) {
                            self.trigger('showAdminPlugin', {
                                section: 'System Notifications',
                                name: 'List'
                            });
                        } else {
                            self.$node.find('*[name=severity]:checked').removeAttr('checked');
                            self.$node.find('*[name=severity]').eq(0).prop('checked', true)
                            self.$node.find('.title,.message,.startDate,.endDate').val('');
                            self.$node.find('.startDate').val(F.date.dateTimeString(new Date()));
                            self.checkValid();
                            self.showSuccess('Saved Notification');
                        }
                    })
                    .catch(function() {
                        self.showError();
                    })
            })
        }
    }
});
