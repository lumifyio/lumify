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

            this.initialWorkspaceLoad = $.Deferred();

            this.on(document, 'menubarToggleDisplay', this.onMenubarToggleDisplay);

            //_.delay(function() {
            //    this.trigger(document, 'menubarToggleDisplay', { name: 'chat' })
            //}.bind(this), 1000);
        });

        this.attachComponents = function() {
            var self = this,
                userListPane = this.select('usersListSelector'),
                activeChatPane = this.select('activeChatSelector');

            require([
                './window',
                './userList'
            ], function(Window, UserList) {
                Window.attachTo(activeChatPane);
                UserList.attachTo(userListPane)
            });

            // Set function to noop for future calls
            this.attachComponents = function() {};
        };

        this.onMenubarToggleDisplay = function(event, data) {
            if (data.name === 'chat') {
                this.attachComponents();
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
