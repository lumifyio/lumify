define([
    'flight/lib/component',
    'service/chat',
    'tpl!./chatWindow',
    'tpl!./chatMessage',
    'sf'
], function(defineComponent, ChatService, chatWindowTemplate, chatMessageTemplate, sf) {
    'use strict';

    return defineComponent(Chat);

    function Chat() {
        this.chatService = new ChatService();
        this.openChats = {};

        this.defaultAttrs({
            newMessageFormSelector: 'form.new-message',
            chatWindowSelector: '.chat-window'
        });

        this.after('initialize', function() {
            this.focusMessage = _.debounce(this.focusMessage.bind(this), 100);
            this.on(document, 'userSelected', this.onUserSelected);
            this.on(document, 'chatMessage', this.onChatMessage);
            this.on(document, 'socketMessage', this.onSocketMessage);
            this.on(document, 'chatCreated', this.onChatCreated);
            this.on('submit', {
                newMessageFormSelector: this.onNewMessageFormSubmit
            });
        });

        this.findChatByUserId = function(userId) {
            var self = this,
                chatRowKey = Object.keys(this.openChats).filter(function(chatRowKey) {
                    var chat = self.openChats[chatRowKey];
                    return chat.users.some(function(u) {
                        return u.id == userId;
                    });
                });

            if (chatRowKey.length > 0) {
                return self.openChats[chatRowKey[0]];
            }
            return null;
        };

        this.focusMessage = function(rowKey) {
            $('#chat-window-' + rowKey).show().find('.message').focus();
        };

        this.onUserSelected = function(evt, userData) {
            var chat = this.findChatByUserId(userData.id);
            if (chat) {
                return this.createChatWindowAndFocus(chat);
            }

            chat = {
                rowKey: userData.id,
                users: [userData]
            };
            this.openChats[chat.rowKey] = chat;
            this.trigger(document, 'chatCreated', chat);
        };

        this.onChatCreated = function(evt, chat) {
            this.createChatWindowAndFocus(chat);
        };

        this.createChatWindowAndFocus = function(chat) {
            if (!chat.windowCreated) {
                var html = $(chatWindowTemplate({ chat: chat }));
                html.hide().appendTo(this.$node);
                chat.windowCreated = true;
            }

            this.select('chatWindowSelector').hide();
            this.focusMessage(chat.rowKey);
        };

        this.addMessage = function(messageData) {
            if (messageData.tempId) {
                $('#' + messageData.tempId).remove();
            }
            if (messageData.chatRowKey === currentUser.rowKey) {
                messageData.chatRowKey = messageData.from.rowKey;
            }
            this.checkChatWindow(messageData);

            var $chatWindow = $('#chat-window-' + messageData.chatRowKey),
                data = {
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

        this.checkChatWindow = function(messageData) {
            var $chatWindow = $('#chat-window-' + messageData.chatRowKey);
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

        this.scrollWindowToBottom = function(chatWindow) {
            clearTimeout(this.scrollTimeout);
            this.scrollTimeout = setTimeout(function() {
                var bottom = chatWindow.get(0).scrollHeight - chatWindow.height();
                chatWindow.clearQueue().animate({scrollTop: bottom}, 'fast');
            }, 100);
        };

        this.onSocketMessage = function(evt, data) {
            var self = this;

            switch (data.type) {
                case 'chatMessage':
                    self.trigger(document, 'chatMessage', data.data);
                    break;
            }
        };

        this.onChatMessage = function(evt, message) {
            this.addMessage(message);
        };

        this.onNewMessageFormSubmit = function(evt) {
            evt.preventDefault();

            var self = this,
                $target = $(evt.target),
                $chatWindow = $target.parents('.chat-window'),
                $messageInput = $('.message', $target),
                chatRowKey = $chatWindow.data('chatrowkey'),
                chat = this.openChats[chatRowKey],
                tempId = 'chat-message-temp-' + Date.now(),

                // add a temporary message to create the feel of responsiveness
                messageData = {
                    chatRowKey: chatRowKey,
                    from: currentUser,
                    message: $messageInput.val(),
                    postDate: null,
                    tempId: tempId
                };

            this.addMessage(messageData);

            var userIds = chat.users.map(function(u) {
                return u.id;
            });
            userIds.push(currentUser.id);

            this.chatService.sendChatMessage(userIds, messageData);

            $messageInput.val('');
            $messageInput.focus();
        };

    }
});
