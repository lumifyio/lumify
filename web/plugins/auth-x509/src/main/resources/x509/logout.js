require([
    'configuration/plugins/logout/plugin',
    'service/user',
    'util/messages'
], function(logoutHandlers, UserService, messages) {
    var userService = new UserService();
    logoutHandlers.registerLogoutHandler(function() {
        var logoutPath = "logout.html?msg=" + messages("lumify.session.expired");
        userService.logout()
            .fail(function () {
                window.location = logoutPath;
            })
            .done(function() {
                window.location = logoutPath;
            });

        return false;
    });
});