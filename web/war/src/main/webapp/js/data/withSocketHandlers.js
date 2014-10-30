define(['underscore'], function(_) {
    'use strict';

    // Define this.[type]SocketHandler for each of these
    var TYPES = _.indexBy([
        'chatMessage',
        'edgeDeletion',
        'entityImageUpdated',
        'longRunningProcessChange',
        'propertiesChange',
        'sync',
        'textUpdated',
        'userStatusChange',
        'userWorkspaceChange',
        'verticesDeleted',
        'workspaceChange',
        'workspaceDelete',
    ]);

    return withSocketHandlers;

    function typeToHandlerName(type) {
        return type + 'SocketHandler';
    }

    function withSocketHandlers() {

        this.after('initialize', function() {
            var self = this,
                types = _.partition(TYPES, function(type) {
                    var handler = typeToHandlerName(type);
                    return (handler in self) && _.isFunction(self[handler]);
                });

            if (types[1].length) {
                console.warn(
                    types[1].length + ' undefined socket handler(s): ',
                    types[1].map(typeToHandlerName).join(', ')
                );
            }

            this.on('socketMessage', this.onSocketMessage);
        });

        this.onSocketMessage = function(evt, message) {
            if (message.type in TYPES) {
                var handler = typeToHandlerName(message.type);
                if (handler in this) {
                    this[handler](message);
                }
            } else {
                console.warn('Unhandled socket message', message.type, message.data);
            }
        };

        this.propertiesChangeSocketHandler = function(message) {
            var self = this,
                updated = null;

            if (message.data && message.data.vertex && !message.data.vertex.sourceVertexId) {
                if (self.cachedVertices[message.data.vertex.id]) {
                    updated = self.updateCacheWithVertex(message.data.vertex, { returnNullIfNotChanged: true });
                    if (updated) {
                        self.trigger('verticesUpdated', {
                            vertices: [updated],
                            options: {
                                originalEvent: message.type,
                                originalData: message.data
                            }
                        });
                    }
                }
            } else if (message.data && message.data.edge) {
                var label = message.data.edge.label,
                    vertices = _.compact([
                        self.cachedVertices[message.data.edge.sourceVertexId],
                        self.cachedVertices[message.data.edge.destVertexId]
                    ]);

                vertices.forEach(function(vertex) {
                    if (!vertex.edgeLabels) {
                        vertex.edgeLabels = [];
                    }

                    if (vertex.edgeLabels.indexOf(label) === -1) {
                        vertex.edgeLabels.push(label);
                    }
                });

                if (vertices.length) {
                    self.trigger('verticesUpdated', {
                        vertices: vertices,
                        options: {
                            originalEvent: message.type
                        }
                    });
                    if (vertices.length === 2) {
                        self.trigger('refreshRelationships');
                    }
                }

                self.trigger('edgesUpdated', {
                    edges: [message.data.edge],
                    options: {
                        originalData: message.data
                    }
                })
            }
        };

        this.entityImageUpdatedSocketHandler = function(message) {
            var updated = null;

            if (message.data && message.data.graphVertexId) {
                updated = this.updateCacheWithVertex(message.data.vertex, { returnNullIfNotChanged: true });
                if (updated) {
                    this.trigger('verticesUpdated', { vertices: [updated] });
                    this.trigger('iconUpdated', { src: null });
                }
            } else console.warn('entityImageUpdated event received with no graphVertexId', message);
        };

        this.textUpdatedSocketHandler = function(message) {
            if (message.data && message.data.graphVertexId) {
                this.trigger('textUpdated', { vertexId: message.data.graphVertexId })
            } else console.warn('textUpdated event received with no graphVertexId', message);
        };

        this.edgeDeletionSocketHandler = function(message) {
            if (_.findWhere(this.selectedEdges, { id: message.data.edgeId })) {
                this.trigger('selectObjects');
            }
            this.trigger('edgesDeleted', { edgeId: message.data.edgeId});
        };

        this.verticesDeletedSocketHandler = function(message) {
            if (_.some(this.selectedVertices, function(vertex) {
                    return ~message.data.vertexIds.indexOf(vertex.id);
                })) {
                this.trigger('selectObjects');
            }
            this.trigger('verticesDeleted', {
                vertices: message.data.vertexIds.map(function(vId) {
                    return { id: vId };
                })
            });
        };

        this.workspaceChangeSocketHandler = function(message) {
            var workspace = $.extend({}, message.data),
                user = _.findWhere(message.data.users, { userId: currentUser.id });

            workspace.editable = /WRITE/i.test(user && user.access);
            workspace.isSharedToUser = workspace.createdBy !== currentUser.id;

            this.trigger('workspaceUpdated', {
                workspace: workspace
            });
        };

        this.workspaceDeleteSocketHandler = function(message) {
            this.trigger('workspaceDeleted', {
                workspaceId: message.workspaceId
            });
        };

        this.userStatusChangeSocketHandler = function(message) {
            // FIXME: disable since this will logout a user if they open
            if (message.data &&
                message.data.status &&
                message.data.status === 'OFFLINE' &&
                message.data.id &&
                message.data.id === currentUser.id) {
                $(document).trigger('logout', {  message: i18n('lumify.session.expired') });
            }
        };

        // TODO: move external handled socket messages here, then trigger local
        // events
        this.userWorkspaceChangeSocketHandler = _.identity;
        this.longRunningProcessChangeSocketHandler = _.identity;
        this.chatMessageSocketHandler = _.identity;
        this.syncSocketHandler = _.identity;
    }
});
