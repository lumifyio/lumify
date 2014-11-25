
define(['../util/store'], function(store) {

    return {
        createStoreAccessorOrDownloader: function(kind, idsParam, responseObject, downloadPromiss) {
            return function(opts) {
                var options = _.extend({
                        workspaceId: publicData.currentWorkspaceId
                    }, opts),
                    returnSingular = false,
                    objIds = options[idsParam];

                if (!_.isArray(objIds)) {
                    if (objIds) {
                        returnSingular = true;
                        objIds = [objIds];
                    } else {
                        throw new Error(idsParam + ' must contain an object');
                    }
                }

                if (!returnSingular && objIds.length === 0) {
                    return Promise.resolve([]);
                }

                var objects = store.getObjects(options.workspaceId, kind, objIds),
                    toRequest = [];

                objects.forEach(function(vertex, i) {
                    if (!vertex) {
                        toRequest.push(objIds[i]);
                    }
                });

                if (toRequest.length === 0) {
                    return Promise.resolve(returnSingular ? objects[0] : objects);
                } else {
                    return downloadPromiss(toRequest)
                        .then(function(requested) {
                            var results = objects.map(function(obj) {
                                if (obj) {
                                    return obj;
                                }

                                if (responseObject) {
                                    return requested[responseObject].shift();
                                }

                                if (_.isArray(requested)) {
                                    return requested.shift();
                                }

                                return requested;
                            });
                            return returnSingular ? results[0] : results;
                        })
                }
            };
        }
    };
});
