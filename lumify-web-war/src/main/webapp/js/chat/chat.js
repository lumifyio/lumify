define([
    'flight/lib/component',
    'hbs!./chatTpl'
], function(
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

            this.initialWorkspaceLoad = $.Deferred();

            this.on(document, 'menubarToggleDisplay', this.onMenubarToggleDisplay);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'switchWorkspace', this.onSwitchWorkspace);

        });

        this.attachComponents = function(workspaceId) {
            var self = this,
                userListPane = this.select('usersListSelector'),
                activeChatPane = this.select('activeChatSelector');

            require([
                './chat',
                './userList'
            ], function(Chat, UserList) {
                Chat.attachTo(chatPane);
                UserList.attachTo(userListPane, {
                    workspaceId: workspaceId
                });
            });

            // Set function to noop for future calls
            this.attachComponents = function() {};
        };

        this.onMenubarToggleDisplay = function(event, data) {
            if (data.name === 'chat') {
                this.initialWorkspaceLoad.done(this.attachComponents.bind(this));
            }
        };

        this.onWorkspaceLoaded = function(event, data) {
            if (this.initialWorkspaceLoad.state() !== 'resolved') {
                this.initialWorkspaceLoad.resolve(data.workspaceId);
            }
        };

        this.onSwitchWorkspace = function(event, data) {
            console.log(data)
        };

    }
});
