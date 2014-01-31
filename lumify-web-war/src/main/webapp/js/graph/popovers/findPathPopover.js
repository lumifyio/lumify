
define([
    'flight/lib/component',
    './withVertexPopover',
    'util/formatters',
    'service/vertex',
    'data'
], function(
    defineComponent,
    withVertexPopover,
    formatters,
    VertexService,
    appData) {
    'use strict';

    return defineComponent(FindPathPopover, withVertexPopover);

    function FindPathPopover() {

        this.vertexService = new VertexService();

        this.defaultAttrs({
            buttonSelector: 'button',
            findPathButtonSelector: '.find-path-form button',
            findPathHopsButtonSelector: '.popover-title .dropdown-menu a'
        });

        this.after('teardown', function() {
            if (this.findPathRequest && this.findPathRequest.abort) {
                this.findPathRequest.abort();
            }
        });

        this.before('initialize', function(node, config) {
            config.hops = config.connectionData && config.connectionData.hops || 1;
            config.template = 'findPathPopover';
        });

        this.after('initialize', function() {
            this.on('click', {
                findPathButtonSelector: this.onFindPathButton
            });

            this.on('popoverInitialize', this.onFindPath);
        });

        this.onFindPath = function() {
            this.trigger('defocusPaths');

            var self = this,
                cy = this.attr.cy,
                title = this.popover.find('.popover-title').hide().find('.title').text(''),
                text = this.popover.find('span.path').text('Finding paths...'),
                button = this.popover.find('button').hide().text('Add Vertices').attr('disabled', true).focus();

            this.select('findPathHopsButtonSelector').off('click').on('click', this.onFindPathHopsButton.bind(this));

            this.positionDialog();

            var src = this.attr.edge.data('source'),
                dest = this.attr.edge.data('target');

            this.findPathRequest = this.findPath(src, dest)
                .done(function(result) {

                    var paths = result.paths,
                        vertices = result.uniqueVertices,
                        verticesNotSourceDest = vertices.filter(function(v) {
                            return v.id !== src && v.id !== dest;
                        }),
                        notInWorkspace = vertices.filter(function(v) { 
                            return !appData.workspaceVertices[v.id]; 
                        }),
                        pathsFoundText = formatters.string.plural(paths.length, 'path') + ' found';

                    if (paths.length) {
                        if (notInWorkspace.length) {
                            var vertexText = formatters.string.plural(notInWorkspace.length, 'vertex', 'vertices'),
                                suffix = notInWorkspace.length === 1 ? ' isn\'t' : ' aren\'t';
                            text.text(vertexText + suffix + ' already in workspace');
                            button.text('Add ' + vertexText).removeAttr('disabled').show();

                            var index, map = {};
                            for (var i = 0; i < notInWorkspace.length; i++) {
                                path_loop: for (var j = 0; j < paths.length; j++) {
                                    for (var x = 0; x < paths[j].length; x++) {
                                        if (paths[j][x].id === notInWorkspace[i].id) {
                                            map[notInWorkspace[i].id] = {
                                                sourceId: paths[j][x-1].id,
                                                targetId: paths[j][x+1].id
                                            };
                                            break path_loop;
                                        }
                                    }
                                }
                            }

                            self.verticesToAdd = notInWorkspace;
                            self.verticesToAddLayoutMap = map;
                        } else {
                            text.text('all vertices are already added to workspace');
                        }

                        cy.$('.temp').remove();
                        self.trigger('focusPaths', { paths:paths, sourceId:src, targetId:dest });
                    } else text.text('Searching up to ' + formatters.string.plural(self.attr.hops, 'hop'));


                    title.text(pathsFoundText).closest('.popover-title').show();
                    self.positionDialog();
                });
        }

        this.findPath = function(source, dest) {
            var parameters = {
                sourceGraphVertexId: source,
                destGraphVertexId: dest,
                depth: 5,
                hops: this.attr.hops
            };

            return this.vertexService.findPath(parameters)
                        .then(function (data) {
                            var vertices = [], added = {};
                            data.paths.forEach(function (path) {
                                path.forEach(function (vertex) {
                                    if (!added[vertex.id]) {
                                        vertices.push(vertex);
                                        added[vertex.id] = true;
                                    }
                                });
                            });

                            return {
                                paths: data.paths,
                                uniqueVertices: vertices
                            };
                        });
        };

        this.onFindPathButton = function(e) {
            var vertices = this.verticesToAdd;

            this.trigger('finishedVertexConnection');
            this.trigger('addVertices', { 
                vertices: vertices,
                options: {
                    layout: {
                        type: 'path',
                        map: this.verticesToAddLayoutMap
                    }
                }
            });
            this.trigger('selectObjects', { vertices:vertices });
        };

        this.onFindPathHopsButton = function(e) {
            var $target = $(e.target),
                newHops = $target.data('hops');

            if ($target.closest('.disabled').length) return;

            var list = $target.closest('ul')
            
            list.siblings('.dropdown-toggle').html($target.data('displayText') + ' <span class="caret"/>');
            list.find('.disabled').removeClass('disabled');
            $target.closest('li').addClass('disabled');

            this.attr.hops = newHops;
            this.onFindPath();
        };
    }
});
