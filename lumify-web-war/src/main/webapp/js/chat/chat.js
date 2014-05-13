define([
    'require',
    'flight/lib/component',
    'hbs!./chatTpl',
], function(
    require,
    defineComponent,
    template) {
    'use strict';

    return defineComponent(Chat);

    function Chat() {

        this.defaultAttrs({
            usersListSelector: '.users-list',
            activeChatSelector: '.active-chat'
        });

        this.after('initialize', function() {
            this.$node.html(template({}));

            this.on(document, 'menubarToggleDisplay', this.onMenubarToggleDisplay);
            this.on(document, 'socketMessage', this.onSocketMessage);

        });

        this.onSocketMessage = function(evt, data) {
            var self = this;

            switch (data.type) {
                case 'chatMessage':
                    self.attachComponents(function() {
                        self.trigger(document, 'chatMessage', data.data);
                    });
                    break;
            }
        };

        this.attachComponents = function(callback) {
            var self = this,
                userListPane = this.select('usersListSelector'),
                activeChatPane = this.select('activeChatSelector');

            require([
                './window',
                './userList'
            ], function(Window, UserList) {
                Window.attachTo(activeChatPane);
                UserList.attachTo(userListPane);
                if (callback) {
                    callback();
                }
            });

            // Set function to noop for future calls
            this.attachComponents = function(callback) {
                if (callback) {
                    callback();
                }
            };
        };

        this.onMenubarToggleDisplay = function(event, data) {
            if (data.name === 'chat') {
                this.attachComponents();
            }
        };

    }
});
