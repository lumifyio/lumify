require([
    'configuration/plugins/logout/plugin',
    'util/messages'
], function(logoutHandlers, messages) {
    logoutHandlers.registerLogoutHandler(function() {
        require(['util/withDataRequest'], function(withDataRequest) {
            withDataRequest.dataRequest('user', 'logout')
                .finally(function() {
                    window.location = 'logout.html?msg=' + encodeURIComponent(messages('lumify.session.expired'));
                })
        });

        return false;
    });
});
