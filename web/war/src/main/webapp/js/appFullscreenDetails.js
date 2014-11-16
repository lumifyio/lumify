
define([
    'flight/lib/component',
    'flight/lib/registry',
    'tpl!./appFullscreenDetails',
    'tpl!./appFullscreenDetailsError',
    'detail/detail',
    'util/vertex/formatters',
    'util/withDataRequest',
    'util/jquery.removePrefixedClasses'
], function(defineComponent, registry, template, errorTemplate, Detail, F, withDataRequest) {
    'use strict';

    return defineComponent(FullscreenDetails, withDataRequest);

    function filterEntity(v) {
        return !filterArtifacts(v);
    }

    function filterArtifacts(v) {
        var concept = F.vertex.concept(v);
        return concept && (/^(document|image|video)$/).test(concept.displayType) || false;
    }

    function FullscreenDetails() {

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
            var requiredFallback = this.attr.workspaceId !== lumifyData.currentWorkspaceId;

            document.title = requiredFallback ?
                i18n('fullscreen.unauthorized') :
                i18n('fullscreen.no_vertices');

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

        this.handleVerticesLoaded = function(vertices) {
            var fallbackToPublic = this.attr.workspaceId !== lumifyData.currentWorkspaceId;

            Detail.teardownAll();
            this.$node.find('.detail-pane').remove();

            if (vertices.length === 0) {
                return this.handleNoVertices();
            }

            this.vertices = _.sortBy(vertices, function(v) {
                var descriptors = [],
                    concept = F.vertex.concept(v);

                // Image/Video/Audio before documents
                descriptors.push(
                    /document/.test(concept.displayType) ? '1' : '0'
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
                    subType = F.vertex.concept(v).displayType,
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

            this.dataRequest('workspace', 'all')
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

                self.dataRequest('vertex', 'store', { vertexIds: self.attr.graphVertexIds })
                    .then(self.handleVerticesLoaded.bind(self))
                    .catch(self.handleVerticesFailed.bind(self))
            });
            this.trigger(document, 'switchWorkspace', { workspaceId: workspaceId });
        };

        this.onVertexUrlChange = function(event, data) {
            var self = this,
                deferred = $.Deferred();

            if (data.workspaceId) {
                this.attr.workspaceId = data.workspaceId;
                if (lumifyData.currentWorkspaceId !== this.attr.workspaceId) {
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
                return this.dataRequest('vertex', 'store', {
                    vertexIds: _.uniq(data.add.concat(_.pluck(this.vertices, 'id')))
                })
                    .then(function(vertices) {
                        self.handleVerticesLoaded(vertices);
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

            this.dataRequest('vertex', 'store', { vertexIds: existingVertexIds.concat(newVertices) })
                .done(function(vertices) {
                    self.handleVerticesLoaded(vertices);
                    self.flashTitle(newVertices);
                })
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
                            document.title = i18n(
                                'fullscreen.title.added.one',
                                F.vertex.title(newVerticesById[newVertexIds[0]])
                            );
                        } else {
                            document.title = i18n('fullscreen.title.added.some', newVertexIds.length);
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
                return i18n('fullscreen.loading');
            }

            var sorted = _.sortBy(this.vertices, function(v) {
                return v.notFound ? 1 : -1;
            });

            if (sorted.length === 1) {
                return F.vertex.title(sorted[0]);
            } else {
                var first = '"' + F.vertex.title(sorted[0]) + '"',
                    l = sorted.length - 1;

                if (l > 1) {
                    return i18n('fullscreen.title.some', first, l)
                }

                return i18n('fullscreen.title.one', first)
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
