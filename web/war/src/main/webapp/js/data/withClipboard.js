define([
    'util/clipboardManager',
    'util/promise'
], function(ClipboardManager, Promise) {
    'use strict';

    return withClipboard;

    function getVerticesFromClipboardData(data) {
        return new Promise(function(fulfill) {
            var vertexIds = [];

            if (data) {
                require(['util/vertex/urlFormatters'], function(F) {
                    var p = F.vertexUrl.parametersInUrl(data);
                    if (p && p.vertexIds) {
                        fulfill(p.vertexIds);
                    } else {
                        fulfill(vertexIds);
                    }
                })
            } else {
                fulfill(vertexIds);
            }
        });
    }

    function formatVertexAction(action, vertices) {
        var len = vertices.length;
        return i18n('vertex.clipboard.action.' + (
            len === 1 ? 'one' : 'some'
        ), i18n('vertex.clipboard.action.' + action.toLowerCase()), len);
    }

    function withClipboard() {

        this.after('initialize', function() {
            ClipboardManager.attachTo(this.$node);

            this.on('clipboardPaste', this.onClipboardPaste);
            this.on('clipboardCut', this.onClipboardCut);
        });

        this.onClipboardCut = function(evt, data) {
            var self = this;

            getVerticesFromClipboardData(data.data)
                .done(function(vertexIds) {
                    var len = vertexIds.length;

                    if (len) {
                        self.trigger('updateWorkspace', {
                            entityDeletes: vertexIds
                        });
                        self.trigger('displayInformation', {
                            message: formatVertexAction('Cut', vertexIds)
                        });
                    }
                });
        };

        this.onClipboardPaste = function(evt, data) {
            var self = this;

            getVerticesFromClipboardData(data.data)
                .done(function(vertexIds) {
                    var len = vertexIds.length,
                        plural = len === 1 ? 'vertex' : 'vertices';

                    if (len) {
                        self.trigger('displayInformation', {
                            message: formatVertexAction('Paste', vertexIds) + '...'
                        });

                        self.dataRequestPromise.done(function(dataRequest) {
                            dataRequest('workspace', 'store')
                                .then(function(workspaceVertices) {
                                    vertexIds = _.reject(vertexIds, function(v) {
                                        return v in workspaceVertices;
                                    });
                                    if (vertexIds.length === 0) {
                                        return Promise.resolve([]);
                                    }
                                    return dataRequest('vertex', 'store', {
                                        vertexIds: vertexIds
                                    });
                                })
                                .done(function(vertices) {
                                    if (vertices.length !== vertexIds.length) {
                                        self.trigger('displayInformation', {
                                            message: i18n('vertex.clipboard.private.vertices.' + (
                                                (vertexIds.length - vertices.length) === 1 ? 'one' : 'some'
                                            ), (vertexIds.length - vertices.length))
                                        });
                                    } else {
                                        self.trigger('displayInformation', {
                                            message: formatVertexAction('Paste', vertexIds)
                                        });
                                    }
                                    if (vertices.length) {
                                        self.trigger('updateWorkspace', {
                                            entityUpdates: vertices.map(function(v) {
                                                return {
                                                    vertexId: v.id,
                                                    graphLayoutJson: {}
                                                }
                                            })
                                        });
                                    }
                                });
                        })
                    }
                });
        };
    }
});
