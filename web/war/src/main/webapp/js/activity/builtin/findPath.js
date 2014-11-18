define([
    'flight/lib/component',
    'util/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    F,
    withDataRequest) {
    'use strict';

    return defineComponent(FindPath, withDataRequest);

    function FindPath() {

        this.after('teardown', function() {
            this.$node.empty();
            this.trigger('defocusPaths');
        });

        this.defaultAttrs({
            pathsSelector: '.found-paths',
            addVerticesSelector: '.add-vertices'
        });

        this.after('initialize', function() {

            this.on('click', {
                pathsSelector: this.onPathClick,
                addVerticesSelector: this.onAddVertices
            });

            this.loadDefaultContent();

            this.on(document, 'focusPaths', this.onFocusPaths);
            this.on(document, 'defocusPaths', this.onDefocusPaths);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
        });

        this.updateButton = function($button, workspaceId) {
            var self = this,
                onDifferentWorkspace = workspaceId !== this.attr.process.workspaceId,
                noResults = (self.attr.process.resultsCount || 0) === 0;

            this.dataRequest('workspace', 'store', workspaceId)
                .done(function(workspaceVertices) {
                    var sourceDestInWorkspace = (self.attr.process.sourceVertexId in workspaceVertices) &&
                            (self.attr.process.destVertexId in workspaceVertices),
                        disabled = onDifferentWorkspace ||
                            noResults ||
                            !sourceDestInWorkspace;

                    if (disabled) {
                        $button.attr('disabled', true);
                    } else {
                        $button.removeAttr('disabled');
                    }

                    $button.attr('title', onDifferentWorkspace ?
                        i18n('popovers.find_path.wrong_workspace') :
                        !sourceDestInWorkspace ?
                        i18n('popovers.find_path.source_dest_missing') :
                        i18n('popovers.find_path.show_path'));
                });
        };

        this.onWorkspaceLoaded = function(event, data) {
            this.updateButton(this.select('pathsSelector'), data.workspaceId);
        };

        this.loadDefaultContent = function() {
            var count = this.attr.process.resultsCount || 0,
                $button = $('<button>').addClass('found-paths btn btn-mini')
                    .text(
                        i18n('popovers.find_path.paths.' + (
                             count === 0 ? 'none' : count === 1 ? 'one' : 'some'
                        ), F.number.pretty(count))
                    );

            this.updateButton($button, lumifyData.currentWorkspaceId);

            this.$node.empty().append($button);
        }

        this.onFocusPaths = function(event, data) {
            // FIXME: if multiple paths using same src, dest it doesn't reset
            // the others
            if (data.sourceId !== this.attr.process.sourceVertexId ||
               data.targetId !== this.attr.process.destVertexId) {
                this.loadDefaultContent();
            }
        };

        this.onDefocusPaths = function(event, data) {
            this.loadDefaultContent();
        };

        this.onAddVertices = function(event) {
            var vertices = _.values(this.toAdd);

            this.trigger('addVertices', {
                vertices: vertices,
                options: {
                    layout: {
                        type: 'path',
                        map: this.toAddLayout
                    }
                }
            });
            this.trigger('selectObjects', { vertices: vertices });
            this.loadDefaultContent();
        };

        this.onPathClick = function(event) {
            var self = this,
                $target = $(event.target).addClass('loading').attr('disabled', true);

            Promise.all([
                this.dataRequest('longRunningProcess', 'get', this.attr.process.id),
                this.dataRequest('workspace', 'store', this.attr.process.workspaceId)
            ])
                .done(function(results) {
                    var process = results.shift(),
                        workspaceVertices = results.shift(),
                        paths = process.results && process.results.paths || [],
                        allVertices = _.flatten(paths),
                        verticesById = _.chain(allVertices)
                            .indexBy('id')
                            .value(),
                        vertices = _.chain(allVertices)
                            .map(_.property('id'))
                            .unique()
                            .reject(function(vertexId) {
                                return vertexId in workspaceVertices;
                            })
                            .value(),
                        map = {};

                    for (var i = 0; i < vertices.length; i++) {
                        pathLoop: for (var j = 0; j < paths.length; j++) {
                            for (var x = 0; x < paths[j].length; x++) {
                                if (paths[j][x].id === vertices[i]) {
                                    // If first or last in path the source/dest
                                    // aren't in the graph
                                    if (x !== 0 || x !== (paths[j].length - 1)) {
                                        map[vertices[i]] = {
                                            sourceId: paths[j][x - 1].id,
                                            targetId: paths[j][x + 1].id
                                        };
                                    }
                                    break pathLoop;
                                }
                            }
                        }
                    }

                    self.toAdd = _.pick(verticesById, vertices);
                    self.toAddLayout = map;

                    self.trigger('focusPaths', {
                        paths: paths,
                        sourceId: self.attr.process.sourceVertexId,
                        targetId: self.attr.process.destVertexId
                    });

                    $target.hide();

                    var $addButton = $('<button>').addClass('btn btn-mini btn-primary add-vertices')

                    if (vertices.length === 0) {
                        $addButton.attr('disabled', true);
                    }

                    $addButton.text(i18n('popovers.find_path.add.' + (
                        vertices.length === 0 ? 'none' :
                        vertices.length === 1 ? 'one' : 'some'
                    ), vertices.length));

                    self.$node.append($addButton);
                })
        };
    }
});
