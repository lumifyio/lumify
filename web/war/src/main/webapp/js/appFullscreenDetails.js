
define([
    'data',
    'flight/lib/component',
    'flight/lib/registry',
    'tpl!./appFullscreenDetails',
    'tpl!./appFullscreenDetailsError',
    'service/vertex',
    'service/workspace',
    'detail/detail',
    'util/vertex/formatters',
    'util/jquery.removePrefixedClasses'
], function(appData, defineComponent, registry, template, errorTemplate, VertexService, WorkspaceService, Detail, F) {
    'use strict';

    return defineComponent(FullscreenDetails);

    function filterEntity(v) {
        return v.concept && !filterArtifacts(v);
    }

    function filterArtifacts(v) {
        return v.concept && (/^(document|image|video)$/).test(v.concept.displayType);
    }

    function FullscreenDetails() {
        this.vertexService = new VertexService();

        this.defaultAttrs({
            detailSelector: '.detail-pane .content',
            closeSelector: '.close-detail-pane',
            noResultsSelector: '.no-results',
            changeWorkspaceSelector: '.no-workspace-access li a'
        });

        this.after('initialize', function() {
            var self = this;

            this.$node.html(template({}));
            this.updateTitle();

            this.on('selectObjects', function(e, d) {
                e.stopPropagation();
                // TODO: update location hash
                // need some way to remove from self.vertices with d.vertices
            });
            this._windowIsHidden = false;
            this.on(document, 'window-visibility-change', this.onVisibilityChange);
            this.on(document, 'vertexUrlChanged', this.onVertexUrlChange);
            this.on('click', {
                closeSelector: this.onClose,
                changeWorkspaceSelector: this.onChangeWorkspace
            });
            this.on('click', this.clearFlashing.bind(this));
            $(window).focus(this.clearFlashing.bind(this));

            this.vertices = [];
            this.fullscreenIdentifier = Math.floor((1 + Math.random()) * 0xFFFFFF).toString(16).substring(1);
            this.$node.addClass('fullscreen-details');

            this.trigger(document, 'applicationReady');
            this.switchWorkspace(this.attr.workspaceId);
        });

        this.clearFlashing = function() {
            clearTimeout(this.timer);
            this._windowIsHidden = false;
        };

        this.onClose = function(event) {
            event.preventDefault();

            var self = this,
                pane = $(event.target).closest('.detail-pane'),
                node = pane.find('.content'),
                instanceInfos = registry.findInstanceInfoByNode(node[0]);

            if (instanceInfos.length) {
                var ids = [];
                instanceInfos.forEach(function(info) {
                    ids.push(info.instance.attr.loadGraphVertexData.id);
                });

                this.updateVertices({
                    remove: ids
                });
            }
        };

        this.updateLocationHash = function() {
            location.hash = F.vertexUrl.fragmentUrl(this.vertices, this.attr.workspaceId);
        };

        this.updateLayout = function() {
            var entities = _.filter(this.vertices, filterEntity).length,
                artifacts = _.filter(this.vertices, filterArtifacts).length,
                verts = entities + artifacts;

            this.$node
                .removePrefixedClasses('vertices- artifacts- entities- has- entity-cols- onlyone')
                .toggleClass('onlyone', verts === 1)
                .addClass([
                    verts <= 4 ? 'vertices-' + verts : 'vertices-many',
                    'entities-' + entities,
                    'entity-cols-' + _.find([4,3,2,1], function(i) {
                        return entities % i === 0;
                    }),
                    entities ? 'has-entities' : '',
                    'artifacts-' + artifacts,
                    artifacts ? 'has-artifacts' : ''
                ].join(' '));
        };

        this.updateTitle = function() {
            document.title = this.titleForVertices();
        };

        this.handleNoVertices = function() {
            var requiredFallback = this.attr.workspaceId !== appData.workspaceId;

            document.title = requiredFallback ? 'Unauthorized' : 'No vertices found';
            this.select('noResultsSelector')
                .html(errorTemplate({
                    vertices: this.attr.graphVertexIds,
                    somePublished: false,
                    requiredFallback: requiredFallback
                }))
                .addClass('visible');
        };

        this.handleVerticesFailed = function(data) {
            this.handleNoVertices();
        };

        this.handleVerticesLoaded = function(data) {
            var vertices = data.vertices,
                fallbackToPublic = this.attr.workspaceId !== appData.workspaceId;

            Detail.teardownAll();
            this.$node.find('.detail-pane').remove();

            if (vertices.length === 0) {
                return this.handleNoVertices();
            }

            this.vertices = _.sortBy(vertices, function(v) {
                var descriptors = [];

                // Image/Video/Audio before documents
                descriptors.push(
                    /document/.test(v.concept.displayType) ? '1' : '0'
                );

                // Sort by title
                descriptors.push(F.vertex.title(v).toLowerCase());
                return descriptors.join('');
            });

            // Find vertices not found and insert at beginning
            var notFoundIds = _.difference(this.attr.graphVertexIds, _.pluck(this.vertices, 'id')),
                notFound = _.map(notFoundIds, function(nId) {
                    return {
                        id: nId,
                        notFound: true,
                        properties: {
                            title: '?'
                        }
                    };
                });

            this.vertices.splice.apply(this.vertices, [0,0].concat(notFound));
            if (notFound.length || fallbackToPublic) {
                this.select('noResultsSelector')
                    .html(errorTemplate({
                        vertices: notFoundIds,
                        requiredFallback: fallbackToPublic,
                        somePublished: true,
                        workspaceTitle: this.workspaceTitle,
                        noWorkspaceGiven: !this.attr.workspaceId
                    }))
                    .addClass('visible someVerticesFound');
                this.loadWorkspaces();
            }

            this.vertices.forEach(function(v) {
                if (v.notFound) return;

                var node = filterEntity(v) ?
                        this.$node.find('.entities-container') :
                        this.$node.find('.artifacts-container'),
                    type = filterArtifacts(v) ? 'artifact' : 'entity',
                    subType = v.concept.displayType,
                    $newPane = $('<div class="detail-pane visible highlight-none"><div class="content"/></div>')
                        .addClass('type-' + type +
                                  (subType ? (' subType-' + subType) : '') +
                                  ' ' + F.className.to(v.id))
                        .appendTo(node)
                        .find('.content');

                Detail.attachTo($newPane, {
                    loadGraphVertexData: v,
                    highlightStyle: 2
                });
            }.bind(this));

            this.updateLocationHash();
            this.updateLayout();
            this.updateTitle();

            if (!this._commSetup) {
                this.setupTabCommunications();
                this._commSetup = true;
            }
        };

        this.loadWorkspaces = function() {
            var self = this;

            new WorkspaceService().list()
                .done(function(data) {
                    if (data.workspaces.length > 1) {
                        var template = _.template(
                            '<li data-id="{workspaceId}" ' +
                            '<% if (disabled) { %>class="disabled"<% } %>>' +
                            '<a>{title}</a>' +
                            '</li>'
                        );
                        self.$node.find('.no-workspace-access')
                            .find('.caret').show()
                            .end()
                            .find('.dropdown-menu')
                            .html(_.chain(data.workspaces)
                                    .sortBy(function(w) {
                                        return w.title.toLowerCase();
                                    })
                                    .map(function(workspace) {
                                        workspace.disabled = workspace.workspaceId === self.actualWorkspaceId;
                                        return template(workspace);
                                    })
                                    .value()
                                    .join(''))
                            .prev('.dropdown-toggle').removeClass('disabled')
                    }
                });
        };

        this.onChangeWorkspace = function(event) {
            var workspaceId = $(event.target).closest('li').data('id').toString();
            this.switchWorkspace(workspaceId);
        };

        this.switchWorkspace = function(workspaceId) {
            var self = this;

            this.on(document, 'workspaceLoaded', function loaded(event, workspace) {
                self.workspaceTitle = workspace.title;
                self.actualWorkspaceId = workspace.workspaceId;
                self.off(document, 'workspaceLoaded', loaded);
                appData.cachedConceptsDeferred.done(function() {
                    self.vertexService
                        .getMultiple(self.attr.graphVertexIds, true)
                        .fail(self.handleVerticesFailed.bind(self))
                        .done(self.handleVerticesLoaded.bind(self));
                });
            });
            this.trigger(document, 'switchWorkspace', { workspaceId: workspaceId });
        };

        this.onVertexUrlChange = function(event, data) {
            var self = this,
                deferred = $.Deferred();

            if (data.workspaceId) {
                this.attr.workspaceId = data.workspaceId;
                if (appData.workspaceId !== this.attr.workspaceId) {
                    this.on(document, 'workspaceLoaded', function loaded() {
                        self.off(document, 'workspaceLoaded', loaded);
                        deferred.resolve();
                    });
                    this.trigger(document, 'switchWorkspace', { workspaceId: this.attr.workspaceId });
                } else deferred.resolve();
            } else deferred.resolve();

            var toRemove = _.difference(this.attr.graphVertexIds, data.graphVertexIds),
                toAdd = _.difference(data.graphVertexIds, this.attr.graphVertexIds);

            if (data.graphVertexIds) {
                this.attr.graphVertexIds = data.graphVertexIds;
            }

            deferred.done(function() {
                self.updateVertices({
                    remove: toRemove,
                    add: toAdd,
                    preventRecursiveUrlChange: true
                });
            })
        };

        this.onVisibilityChange = function(event, data) {
            this._windowIsHidden = data.hidden;
            if (data.visible) {
                clearTimeout(this.timer);
                this.updateTitle();
            }
        };

        this.updateVertices = function(data) {
            var self = this,
                willRemove = !_.isEmpty(data.remove),
                willAdd = !_.isEmpty(data.add);

            if (!willRemove && !willAdd) {
                return;
            }

            if (willAdd) {
                return appData.refresh(_.uniq(data.add.concat(_.pluck(this.vertices, 'id'))))
                    .done(function(vertices) {
                        self.handleVerticesLoaded({ vertices: vertices });
                    });
            }

            if (willRemove) {
                data.remove.forEach(function(vertexId) {
                    var $pane = self.$node.find('.detail-pane.' + F.className.to(vertexId));
                    if ($pane.length) {
                        $pane
                            .find('.content').teardownAllComponents()
                            .end()
                            .remove();
                    }
                });

                this.vertices = _.reject(this.vertices, function(v) {
                    return _.contains(data.remove, v.id);
                });
            }

            if (data.preventRecursiveUrlChange !== true) {
                self.updateLocationHash();
            }
            self.updateLayout();
            self.updateTitle();
        };

        this.onAddGraphVertices = function(data) {
            var self = this,
                vertices = data.vertices,
                targetIdentifier = data.targetIdentifier;

            if (targetIdentifier !== this.fullscreenIdentifier) {
                return;
            }

            var existingVertexIds = _.pluck(this.vertices, 'id'),
                newVertices = _.reject(vertices, function(v) {
                    return existingVertexIds.indexOf(v) >= 0;
                });

            if (newVertices.length === 0) {
                return;
            }

            if (this._windowIsHidden) {
                this.flashTitle(vertices);
            }

            this.vertexService
                .getMultiple(existingVertexIds.concat(newVertices))
                .done(this.handleVerticesLoaded.bind(this))
                .done(this.flashTitle.bind(this, newVertices));
        };

        this.flashTitle = function(newVertexIds, newVertices) {
            var self = this,
                i = 0;

            clearTimeout(this.timer);

            if (!newVertices || newVertices.length === 0) return;

            var newVerticesById = _.indexBy(newVertices, 'id');

            if (this._windowIsHidden) {
                this.timer = setTimeout(function f() {
                    if (self._windowIsHidden && i++ % 2 === 0) {
                        if (newVertexIds.length === 1) {
                            document.title = '"' +
                                F.vertex.title(newVerticesById[newVertexIds[0]]) +
                                '" added';
                        } else {
                            document.title = newVertexIds.length + ' items added';
                        }
                    } else {
                        self.updateTitle();
                    }

                    if (self._windowIsHidden) {
                        self.timer = setTimeout(f, 500);
                    }
                }, 500);
            }
        };

        this.titleForVertices = function() {
            if (!this.vertices || this.vertices.length === 0) {
                return 'Loading...';
            }

            var sorted = _.sortBy(this.vertices, function(v) {
                return v.notFound ? 1 : -1;
            });

            if (sorted.length === 1) {
                return F.vertex.title(sorted[0]);
            } else {
                var first = '"' + F.vertex.title(sorted[0]) + '"',
                    l = sorted.length - 1;

                return first + ' and ' + l + ' other' + (l > 1 ? 's' : '');
            }
        };

        this.setupTabCommunications = function() {
            var self = this;

            require(['intercom'], function(Intercom) {
                var intercom = Intercom.getInstance(),
                    broadcast = function() {
                        intercom.emit('fullscreenDetailsWithVertices', {
                            message: JSON.stringify({
                                vertices: self.vertices,
                                title: self.titleForVertices(),
                                identifier: self.fullscreenIdentifier
                            })
                        });
                    };

                intercom.on('addVertices', function(data) {
                    self.onAddGraphVertices(JSON.parse(data.message));
                });
                intercom.on('ping', broadcast);
                broadcast();
            });
        };
    }
});
