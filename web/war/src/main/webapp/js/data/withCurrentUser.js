define([], function() {
    'use strict';

    return withCurrentUser;

    function withCurrentUser() {

        this.before('dataRequestCompleted', function(request) {
            if (request.success &&
                request.originalRequest.service === 'user' &&
                request.originalRequest.method === 'me') {
                this.setPublicApi('currentUser', request.result, { onlyIfNull:true });
                this.setPublicApi('currentWorkspaceId', request.result.currentWorkspaceId, { onlyIfNull:true });
            }
        });

    }
});
