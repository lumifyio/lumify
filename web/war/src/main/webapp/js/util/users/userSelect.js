define([
    'flight/lib/component',
    'hbs!./userSelectTpl',
    'tpl!./user',
    'util/withDataRequest'
], function(
    defineComponent,
    template,
    userTemplate,
    withDataRequest) {
    'use strict';

    return defineComponent(UserSelect, withDataRequest);

    function UserSelect() {

        this.defaultAttrs({
            inputSelector: 'input',
            filterUserIds: []
        });

        this.after('initialize', function() {
            this.on('clearUser', this.onClearUser);
            this.on('updateFilterUserIds', this.onUpdateFilterUserIds);
            this.on(document, 'userStatusChange', this.onUserStatusChange);

            this.$node.html(template({
                placeholder: this.attr.placeholder || i18n('user.selection.field.placeholder'),
            }));

            this.setupTypeahead();
        });

        this.onUserStatusChange = function(event, user) {
            this.$node.find('.user-row').each(function() {
                var $this = $(this);
                if ($this.data('user').id === user.id) {
                    $this.find('.user-status')
                        .removeClass('active idle offline unknown')
                        .addClass((user.status && user.status.toLowerCase()) || 'unknown');
                }
            })
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
                    self.dataRequest('user', 'search', query)
                        .done(function(users) {
                            var otherUsers = users.filter(function(user) {
                                    return self.attr.filterUserIds.indexOf(user.id) === -1;
                                }),
                                ids = _.pluck(otherUsers, 'id');

                            userMap = _.indexBy(otherUsers, 'id');

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
