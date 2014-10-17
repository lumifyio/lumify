require([
    'configuration/plugins/logout/plugin',
    'service/user',
    'util/messages'
], function(logoutHandlers, UserService, messages) {
    var userService = new UserService();
    logoutHandlers.registerLogoutHandler(function() {
        userService.logout()
            .always(function () {
                window.location = "logout.html?msg=" + encodeURIComponent(messages("lumify.session.expired"));
            });

        return false;
    });
});