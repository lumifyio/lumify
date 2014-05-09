define([
    'flight/lib/component',
    'service/chat',
    'hbs!./windowTpl',
    'hbs!./message',
    'sf',
    'util/formatters'
], function(
    defineComponent,
    ChatService,
    chatWindowTemplate,
    chatMessageTemplate,
    sf,
    F) {
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
            this.$node
                .find('.' + F.className.to(rowKey)).show()
                .find('.message').focus();
        };

        this.onUserSelected = function(evt, userData) {

            if (userData) {
                var chat = this.findChatByUserId(userData.id);
                if (chat) {
                    return this.createChatWindowAndFocus(chat);
                }

                chat = {
                    rowKey: userData.id,
                    users: [userData]
                };

                this.openChats[chat.rowKey] = chat;
                this.createChatWindowAndFocus(chat);
            } else {
                this.select('chatWindowSelector').hide();
            }
        };

        this.createChatWindowAndFocus = function(chat) {
            var id = chat.rowKey === currentUser.id ?
                chat.users[0].id : chat.rowKey;

            if (!chat.windowCreated) {
                var html = $(chatWindowTemplate({
                    cls: F.className.to(id),
                    rowKey: id,
                    to: chat.users[0].userName
                }));
                html.hide().appendTo(this.$node);
                chat.windowCreated = true;
            }

            this.select('chatWindowSelector').hide();
            this.focusMessage(id);
        };

        this.addMessage = function(messageData) {
            if (messageData.tempId) {
                $('#' + messageData.tempId).remove();
            }
            if (messageData.chatRowKey === currentUser.id) {
                messageData.chatRowKey = messageData.from.id;
            }
            this.checkChatWindow(messageData);

            var $chatWindow = this.$node.find('.' + F.className.to(messageData.chatRowKey)),
                data = {
                    userName: messageData.from.userName,
                    tempId: messageData.tempId,
                    timestamp: sf('{0:hh:mm:ss tt}',
                        messageData.postDate ?
                            new Date(messageData.postDate) :
                            new Date()
                    ),
                    message: messageData.message
                };

            $chatWindow.find('.chat-messages').append(chatMessageTemplate(data));

            this.scrollWindowToBottom($chatWindow);
        };

        this.checkChatWindow = function(messageData) {
            var $chatWindow = this.$node.find('.' + F.className.to(messageData.chatRowKey));
            if ($chatWindow.length === 0) {
                var chat = this.openChats[messageData.chatRowKey];
                if (!chat) {
                    chat = {
                        rowKey: messageData.chatRowKey,
                        users: [messageData.from]
                    };
                    this.openChats[messageData.chatRowKey] = chat;
                    this.createChatWindowAndFocus(chat);
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

        this.onChatMessage = function(evt, message) {
            this.addMessage(message);
        };

        this.onNewMessageFormSubmit = function(evt) {
            evt.preventDefault();

            var self = this,
                $target = $(evt.target),
                $chatWindow = $target.parents('.chat-window'),
                $messageInput = $('.message', $target),
                chatRowKey = $chatWindow.data('rowKey'),
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
