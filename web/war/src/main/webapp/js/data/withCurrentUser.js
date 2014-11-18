define([], function() {
    'use strict';

    return withCurrentUser;

    function withCurrentUser() {

        this.around('dataRequestCompleted', function(dataRequestCompleted, request) {
            if (isUserMeRequest(request)) {
                var self = this,
                user = request.result;

                this.setPublicApi('currentUser', user, { onlyIfNull:true });

                if (user.currentWorkspaceId) {
                    this.setPublicApi('currentWorkspaceId', user.currentWorkspaceId, { onlyIfNull:true });
                } else {
                    return this.findOrCreateWorkspace(user.id, dataRequestCompleted, request);
                }
            }

            return dataRequestCompleted.call(this, request);
        });

        this.findOrCreateWorkspace = function(userId, dataRequestCompleted, request) {
            var self = this;

            this.dataRequest('workspace', 'all')
                .then(function(workspaces) {
                    if (workspaces.length) {
                        return Promise.resolve(workspaces[0]);
                    }

                    return self.dataRequest('workspace', 'create')
                })
                .done(function(workspace) {
                    self.pushSocket({
                        type: 'setActiveWorkspace',
                        data: {
                            workspaceId: workspace.workspaceId,
                            userId: userId
                        }
                    });
                    self.setPublicApi('currentWorkspaceId', workspace.workspaceId);
                    dataRequestCompleted.call(this, request);
                });
        };

    }

    function isUserMeRequest(request) {
        return request &&
               request.success &&
               request.originalRequest.service === 'user' &&
               request.originalRequest.method === 'me' &&
               request.result;
    }
});
