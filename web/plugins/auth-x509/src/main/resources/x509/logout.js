require([
    'configuration/plugins/logout/plugin',
    'service/user',
], function(logoutHandlers, UserService) {
    var userService = new UserService();
    logoutHandlers.registerLogoutHandler(function() {
        userService.logout()
            .fail(function () {
                console.log("logout failed!");
            })
            .done(function() {
                window.location = "logout.html";
            });

        return false;
    });
});