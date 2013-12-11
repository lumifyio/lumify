define([
    'flight/lib/component',
    'service/chat',
    'tpl!./chatWindow',
    'tpl!./chatMessage',
    'sf'
], function (defineComponent, ChatService, chatWindowTemplate, chatMessageTemplate, sf) {
    'use strict';

    return defineComponent(Chat);

    function Chat() {
        this.chatService = new ChatService();
        this.openChats = {};
        this.currentUser = null;

        this.defaultAttrs({
            newMessageFormSelector: 'form.new-message',
            chatWindowSelector: '.chat-window'
        });

        this.after('initialize', function () {
            this.on(document, 'onlineStatusChanged', this.onOnlineStatusChanged);
            this.on(document, 'userSelected', this.onUserSelected);
            this.on(document, 'chatMessage', this.onChatMessage);
            this.on(document, 'socketMessage', this.onSocketMessage);
            this.on(document, 'chatCreated', this.onChatCreated);
            this.on('submit', {
                newMessageFormSelector: this.onNewMessageFormSubmit
            });
        });

        this.onOnlineStatusChanged = function (evt, data) {
            this.currentUser = data.user;
        };

        this.findChatByUserRowKey = function (userRowKey) {
            var self = this;
            var chatRowKey = Object.keys(this.openChats).filter(function (chatRowKey) {
                var chat = self.openChats[chatRowKey];
                return chat.users.some(function (u) {
                    return u.rowKey == userRowKey;
                });
            });
            if (chatRowKey.length > 0) {
                return self.openChats[chatRowKey[0]];
            }
            return null;
        };

        this.onUserSelected = function (evt, userData) {
            var chat = this.findChatByUserRowKey(userData.rowKey);
            if (chat) {
                this.select('chatWindowSelector').hide();
                return $('#chat-window-' + chat.rowKey).show().find('.message').focus();
            }

            var chat = {
                rowKey: userData.rowKey,
                users: [userData]
            };
            this.openChats[chat.rowKey] = chat;
            console.log('onUserSelected chatCreated', chat);
            this.trigger(document, 'chatCreated', chat);
        };

        this.onChatCreated = function (evt, chat) {
            console.log('onChatCreated', chat);
            this.createChatWindowAndFocus(chat);
        };

        this.createChatWindowAndFocus = function (chat) {
            if (!chat.windowCreated) {
                var html = $(chatWindowTemplate({ chat: chat }));
                html.hide().appendTo(this.$node);
                chat.windowCreated = true;
            }

            this.select('chatWindowSelector').hide();
            $('#chat-window-' + chat.rowKey).show().find('.message').focus();
        };

        this.addMessage = function (messageData) {
            console.log('addMessage', messageData);
            if (messageData.tempId) {
                $('#' + messageData.tempId).remove();
            }
            this.checkChatWindow(messageData);
            var $chatWindow = $('#chat-window-' + messageData.chatRowKey);
            var data = {
                messageData: messageData
            };
            if (messageData.postDate) {
                data.prettyDate = sf('{0:hh:mm:ss tt}', new Date(messageData.postDate));
            } else {
                data.prettyDate = sf('{0:hh:mm:ss tt}', new Date());
            }
            $chatWindow.find('.chat-messages').append(chatMessageTemplate(data));

            this.scrollWindowToBottom($chatWindow);
        };

        this.checkChatWindow = function (messageData) {
            var $chatWindow = $('#chat-window-' + messageData.chatRowKey);
            console.log('checkChatWindow', messageData, $chatWindow);
            if ($chatWindow.length === 0) {
                var chat = this.openChats[messageData.chatRowKey];
                if (!chat) {
                    chat = {
                        rowKey: messageData.chatRowKey,
                        users: [messageData.from]
                    };
                    this.openChats[messageData.chatRowKey] = chat;
                    this.trigger(document, 'chatCreated', chat);
                } else {
                    this.createChatWindowAndFocus(chat);
                }
                return;
            }

            this.scrollWindowToBottom($chatWindow);
        };

        this.scrollWindowToBottom = function (chatWindow) {
            console.log('scrollWindowToBottom', chatWindow);
            clearTimeout(this.scrollTimeout);
            this.scrollTimeout = setTimeout(function () {
                var bottom = chatWindow.get(0).scrollHeight - chatWindow.height();
                chatWindow.clearQueue().animate({scrollTop: bottom}, 'fast');
            }, 100);
        };

        this.onSocketMessage = function (evt, data) {
            var self = this;

            switch (data.type) {
                case 'chatMessage':
                    console.log('onSocketMessage: chatMessage:', data);
                    self.trigger(document, 'chatMessage', data.data);
                    break;
            }
        };

        this.onChatMessage = function (evt, message) {
            console.log('onChatMessage', message);
            this.addMessage(message);
        };

        this.onNewMessageFormSubmit = function (evt) {
            evt.preventDefault();

            var self = this;
            var $target = $(evt.target);
            var $chatWindow = $target.parents('.chat-window');
            var $messageInput = $('.message', $target);

            var chatRowKey = $chatWindow.data('chatrowkey');
            var chat = this.openChats[chatRowKey];

            var tempId = 'chat-message-temp-' + Date.now();

            // add a temporary message to create the feel of responsiveness
            var messageData = {
                chatRowKey: chatRowKey,
                from: this.currentUser,
                message: $messageInput.val(),
                postDate: null,
                tempId: tempId
            };
            this.addMessage(messageData);

            var userRowKeys = chat.users.map(function (u) {
                return u.rowKey;
            });

            this.chatService.sendChatMessage(userRowKeys, messageData);

            $messageInput.val('');
            $messageInput.focus();
        };

    }
});
