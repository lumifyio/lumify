define([
    'flight/lib/component',
    'service/user',
    'chat/chat',
    'tpl!./users',
    'tpl!./userListItem'
], function(
    defineComponent,
    UsersService, Chat,
    usersTemplate,
    userListItemTemplate) {
    'use strict';

    return defineComponent(Users);

    function Users() {
        this.usersService = new UsersService();
        this.currentUserId = null;

        this.defaultAttrs({
            usersListSelector: '.users-list',
            userListItemSelector: '.users-list .user',
            chatSelector: '.active-chat'
        });

        this.after('initialize', function() {
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

            if (data.userId !== this.currentUserId &&
                this.$node.find('.user-' + data.userId).is('.online')) 
            {

                this.trigger(document, 'userSelected', {
                    id: data.userId
                });
            }
        };

        this.onUserListItemClicked = function(evt) {
            evt.preventDefault();

            var $target = $(evt.target).closest('li'),
                userData = $target.data('userdata');

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
            this.$node.find('.conversation-' + data.id).addClass('active')
        };

        this.onNewUserOnline = function(evt, userData) {
            var $usersList = this.select('usersListSelector'),
                html = userListItemTemplate({ user: userData });
            $usersList.find('li.status-' + userData.status).after(html);
        };

        this.onChatCreated = function(evt, chat) {
            this.createOrActivateConversation(chat);
        };

        this.createOrActivateConversation = function(chat) {
            var $usersList = this.select('usersListSelector'),
                to = chat.rowKey === currentUser.id ? chat.users[0].id : chat.rowKey,
                activeChat = $usersList.find('li.conversation-' + to);

            if (!activeChat.length && chat.users) {
                activeChat = $usersList.find('li.online.user-' + chat.users[0].id);

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
            this.trigger(document, 'userSelected', {id: to});
        };

        this.onChatMessage = function(evt, message) {
            var chat = {
                rowKey: message.chatRowKey,
                users: [message.from]
            };
            this.createOrActivateConversation(chat);

            var badge = this.select('usersListSelector')
                .find('li.conversation-' + message.from.id + ':not(.active) .badge');
            badge.text(parseInt(badge.text(), 10) + 1);
        };

        this.onUserOnlineStatusChanged = function(evt, userData) {
            var $usersList = this.select('usersListSelector'),
                $user = $('.user-' + userData.id, $usersList);

            if ($user.length) {
                $user.remove();
                var html = userListItemTemplate({ user: userData });
                $usersList.find('li.status-' + userData.status).after(html);
            } else {
                this.onNewUserOnline(evt, userData);
            }
        };

        this.onSocketMessage = function(evt, data) {
            var self = this;

            switch (data.type) {
                case 'userStatusChange':
                    self.updateUser(data.data);
                    break;
            }
        };

        this.updateUsers = function(users) {
            var self = this;
            users.forEach(self.updateUser.bind(this));
        };

        this.updateUser = function(user) {
            if (!this.currentUserId || user.id == this.currentUserId) {
                return;
            }

            var $usersList = this.select('usersListSelector'),
                $user = $('.user-' + user.id, $usersList);

            if ($user.length === 0) {
                this.trigger(document, 'newUserOnline', user);
            } else if (!$user.hasClass(user.status)) {
                this.trigger(document, 'userOnlineStatusChanged', user);
            }
        };

        this.doGetOnline = function() {
            var self = this;
            self.usersService.getOnline()
                .fail(function(err) {
                    console.error('getOnline', err);
                    var $usersList = self.select('usersListSelector');
                    $usersList.html('Could not get online: ' + err);
                })
                .done(function(data) {

                    window.currentUser = data.user;
                    self.trigger('currentUserChanged', { user: data.user });
                    self.currentUserId = data.user.id;

                    if (data.messages && data.messages.length > 0) {
                        data.messages.forEach(function(message) {
                            self.trigger(document, 'chatMessage', message);
                        });
                    }
                    Chat.attachTo(self.select('chatSelector'));

                    self.updateUsers(data.users);
                });
        };
    }
});
