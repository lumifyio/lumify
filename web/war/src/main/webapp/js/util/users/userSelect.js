define([
    'flight/lib/component',
    'hbs!./userSelectTpl',
    'tpl!./user',
    'service/user'
], function(
    defineComponent,
    template,
    userTemplate,
    UserService) {
    'use strict';

    var userService = new UserService();

    return defineComponent(UserSelect);

    function UserSelect() {

        this.defaultAttrs({
            inputSelector: 'input',
            filterUserIds: []
        });

        this.after('initialize', function() {
            this.on('clearUser', this.onClearUser);
            this.on(document, 'socketMessage', this.onSocketMessage);

            this.$node.html(template({
                placeholder: this.attr.placeholder || i18n('user.selection.field.placeholder'),
            }));

            this.setupTypeahead();
        });

        this.onSocketMessage = function(event, message) {
            if (message && ~'userStatusChange'.indexOf(message.type)) {
                var user = message.data;
                this.$node.find('.user-row').each(function() {
                    var $this = $(this);
                    if ($this.data('user').id === user.id) {
                        $this.find('.user-status')
                            .removeClass('online offline unknown')
                            .addClass((user.status && user.status.toLowerCase()) || 'unknown');
                    }
                })
            }
        };

        this.onClearUser = function() {
            var self = this;

            _.defer(function() {
                self.select('inputSelector').val('');
            });
        };

        this.setupTypeahead = function() {
            var self = this,
                userMap = {};

            this.select('inputSelector').typeahead({
                source: function(query, callback) {
                    userService.search(query)
                        .done(function(response) {
                            var users = response.users.filter(function(user) {
                                    return self.attr.filterUserIds.indexOf(user.id) === -1;
                                }),
                                ids = _.pluck(users, 'id');

                            userMap = _.indexBy(users, 'id');

                            if (users.length > 0) {
                                users[0].email = 'jharwig@gmail.com';
                                users[0].userName = 'firstusername';
                            }

                            if (users.length > 1)
                            users[1].userName = 'awesomesauce';

                            callback(ids);
                        });
                },
                matcher: function() {
                    return true;
                },
                sorter: function(userIds) {
                    return _.sortBy(userIds, function(userId) {
                        return userMap[userId].displayName;
                    });
                },
                updater: function(userId) {
                    self.trigger('userSelected', {
                        user: userMap[userId]
                    });
                    return userMap[userId].displayName;
                },
                highlighter: function(userId) {
                    return userTemplate({ user: userMap[userId] });
                }
            });
        };

    }
});
