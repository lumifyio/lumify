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
            buttonSelector: '.btn-primary',
            inputSelector: 'input,textarea'
        })

        this.after('initialize', function() {
            this.on('change keyup paste', {
                inputSelector: this.checkValid
            });

            this.on('click', {
                buttonSelector: this.onCreate
            });

            this.$node
                .html(template({
                    severity: 'INFORMATIONAL WARNING CRITICAL'.split(' ')
                }))
                .find('*[name=severity]').eq(0).prop('checked', true)

            this.checkValid();
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
            return {
                title: $.trim(this.$node.find('.title').val()),
                message: $.trim(this.$node.find('.message').val()),
                severity: this.$node.find('*[name=severity]:checked').val(),
                startDate: $.trim(this.$node.find('.startDate').val()),
                endDate: this.$node.find('.endDate').val()
            };
        }

        this.onCreate = function(event) {
            var self = this;
            require(['util/formatters'], function(F) {
                self.dataRequest('admin', 'systemNotificationCreate', self.getNotification())
                    .then(function() {
                        self.$node.find('*[name=severity]:checked').removeAttr('checked');
                        self.$node.find('*[name=severity]').eq(0).prop('checked', true)
                        self.select('inputSelector').val('');
                        self.showSuccess('Saved Notification');
                    })
                    .catch(function() {
                        self.showError();
                    })
            })
        }
    }
});
