define([
    'flight/lib/component',
    'service/longRunningProcess',
    'util/formatters',
    'data'
], function(
    defineComponent,
    LongRunningProcessService,
    F,
    appData) {
    'use strict';

    var longRunningProcessService = new LongRunningProcessService();

    return defineComponent(FindPath);

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
        });

        this.loadDefaultContent = function() {
            var count = this.attr.process.resultsCount || 0,
                $button = $('<button>').addClass('found-paths btn btn-mini')
                    .text(
                        i18n('popovers.find_path.paths.' + (
                             count === 0 ? 'none' : count === 1 ? 'one' : 'some'
                        ), F.number.pretty(count))
                    );

            if (count === 0) {
                $button.attr('disabled', true);
            }

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

            longRunningProcessService.get(this.attr.process.id)
                .done(function(process) {
                    var paths = process.results && process.results.paths || [],
                        allVertices = _.flatten(paths),
                        verticesById = _.chain(allVertices)
                            .indexBy('id')
                            .value(),
                        vertices = _.chain(allVertices)
                            .map(_.property('id'))
                            .unique()
                            .reject(function(vertexId) {
                                return appData.inWorkspace(vertexId);
                            })
                            .value(),
                        map = {};

                    for (var i = 0; i < vertices.length; i++) {
                        pathLoop: for (var j = 0; j < paths.length; j++) {
                            for (var x = 0; x < paths[j].length; x++) {
                                if (paths[j][x].id === vertices[i]) {
                                    map[vertices[i]] = {
                                        sourceId: paths[j][x - 1].id,
                                        targetId: paths[j][x + 1].id
                                    };
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
