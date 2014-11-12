define([
    'flight/lib/component',
    'hbs!./userSelectTpl',
    'tpl!./user'
], function(
    defineComponent,
    template,
    userTemplate) {
    'use strict';

    return defineComponent(UserSelect);

    function UserSelect() {

        this.defaultAttrs({
            inputSelector: 'input',
            filterUserIds: []
        });

        this.after('initialize', function() {
            this.on('clearUser', this.onClearUser);
            this.on('updateFilterUserIds', this.onUpdateFilterUserIds);
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
                            .removeClass('active idle offline unknown')
                            .addClass((user.status && user.status.toLowerCase()) || 'unknown');
                    }
                })
            }
        };

        this.onUpdateFilterUserIds = function(event, data) {
            this.attr.filterUserIds = data.userIds;
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
