require([
    'configuration/plugins/logout/plugin',
    'util/messages'
], function(logoutHandlers, messages) {
    logoutHandlers.registerLogoutHandler(function() {
        userService.logout()
            .always(function () {
                window.location = "logout.html?msg=" + encodeURIComponent(messages("lumify.session.expired"));
            });

        return false;
    });
});
