define([
    'flight/lib/component',
    'service/user',
    'chat/chat',
    'tpl!./users',
    'tpl!./userListItem'
], function (defineComponent, UsersService, Chat, usersTemplate, userListItemTemplate) {
    'use strict';

    return defineComponent(Users);

    function Users() {
        this.usersService = new UsersService();
        this.currentUserRowKey = null;

        this.defaultAttrs({
            usersListSelector: '.users-list',
            userListItemSelector: '.users-list .user',
            chatSelector: '.active-chat'
        });

        this.after('initialize', function () {
            this.$node.html(usersTemplate({}));

            this.$node.addClass('popover');


            this.on(document, 'newUserOnline', this.onNewUserOnline);
            this.on(document, 'userOnlineStatusChanged', this.onUserOnlineStatusChanged);
            this.on(document, 'chatMessage', this.onChatMessage);
            this.on(document, 'socketMessage', this.onSocketMessage);
            this.on(document, 'chatCreated', this.onChatCreated);
            this.on(document, 'startChat', this.onStartChat);
            this.on(document, 'userSelected', this.onUserSelected);
            this.on('click', {
                userListItemSelector: this.onUserListItemClicked
            });

            this.on(document, 'subscribeSocketOpened', this.doGetOnline.bind(this));
        });

        this.onStartChat = function(event, data) {

            if (data.userId !== this.currentUserRowKey &&
                this.$node.find('.user-' + data.userId).is('.online')) 
            {

                this.trigger(document, 'userSelected', {
                    rowKey: data.userId
                });
            }
        };

        this.onUserListItemClicked = function (evt) {
            evt.preventDefault();

            var $target = $(evt.target).closest('li');
            var userData = $target.data('userdata');

            $target.find('.badge').text('');

            if ($target.hasClass('offline')) {
                return;
            }

            this.$node.find('.active').removeClass('active');
            $target.addClass('active');

            this.trigger(document, 'userSelected', userData);
        };

        this.onUserSelected = function(event, data) {
            this.$node.find('.active').removeClass('active');
            this.$node.find('.conversation-' + data.rowKey).addClass('active')
        };

        this.onNewUserOnline = function (evt, userData) {
            var $usersList = this.select('usersListSelector');
            var html = userListItemTemplate({ user: userData });
            $usersList.find('li.status-' + userData.status).after(html);
        };

        this.onChatCreated = function (evt, chat) {
            this.createOrActivateConversation(chat);
        };

        this.createOrActivateConversation = function (chat) {
            var $usersList = this.select('usersListSelector');
            var to = chat.rowKey === currentUser.rowKey ? chat.users[0].rowKey : chat.rowKey;
            var activeChat = $usersList.find('li.conversation-' + to);

            if (!activeChat.length && chat.users) {
                activeChat = $usersList.find('li.online.user-' + chat.users[0].rowKey);

                if (!activeChat.length) {
                    return;
                }
                activeChat = activeChat.clone();
                activeChat.removePrefixedClasses('user-');
                activeChat.addClass('conversation-' + to);
                $usersList.find('li.conversations').after(activeChat);
            }

            $usersList.find('.active').removeClass('active');
            activeChat.addClass('active');
            this.trigger(document, 'userSelected', {rowKey:to});
        };

        this.onChatMessage = function (evt, message) {
            var chat = {
                rowKey: message.chatRowKey,
                users: [message.from]
            };
            this.createOrActivateConversation(chat);

            var badge = this.select('usersListSelector').find('li.conversation-' + message.from.rowKey + ':not(.active) .badge');
            badge.text(+badge.text() + 1);
        };


        this.onUserOnlineStatusChanged = function (evt, userData) {
            var $usersList = this.select('usersListSelector');
            var $user = $('.user-' + userData.rowKey, $usersList);
            if ($user.length) {
                $user.remove();
                var html = userListItemTemplate({ user: userData });
                $usersList.find('li.status-' + userData.status).after(html);
            } else {
                this.onNewUserOnline(evt, userData);
            }
        };

        this.onSocketMessage = function (evt, data) {
            var self = this;

            switch (data.type) {
                case 'userStatusChange':
                    self.updateUser(data.data);
                    break;
            }
        };

        this.updateUsers = function (users) {
            var self = this;
            users.forEach(self.updateUser.bind(this));
        };

        this.updateUser = function (user) {
            if (!this.currentUserRowKey || user.rowKey == this.currentUserRowKey) {
                return;
            }

            var $usersList = this.select('usersListSelector'),
                $user = $('.user-' + user.rowKey, $usersList);

            if ($user.length === 0) {
                this.trigger(document, 'newUserOnline', user);
            } else if (!$user.hasClass(user.status)) {
                this.trigger(document, 'userOnlineStatusChanged', user);
            }
        };

        this.doGetOnline = function () {
            var self = this;
            self.usersService.getOnline()
                .fail(function(err) {
                    console.error('getOnline', err);
                    var $usersList = self.select('usersListSelector');
                    $usersList.html('Could not get online: ' + err);
                })
                .done(function(data) {

                    window.currentUser = data.user;
                    self.currentUserRowKey = data.user.rowKey;

                    if (data.messages && data.messages.length > 0) {
                        data.messages.forEach(function (message) {
                            self.trigger(document, 'chatMessage', message);
                        });
                    }
                    Chat.attachTo(self.select('chatSelector'));

                    self.updateUsers(data.users);
                });
        };
    }
});
