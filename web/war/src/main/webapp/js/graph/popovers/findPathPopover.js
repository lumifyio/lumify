
define([
    'flight/lib/component',
    './withVertexPopover',
    'util/formatters',
    'service/vertex',
    'data'
], function(
    defineComponent,
    withVertexPopover,
    F,
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
            config.hops = config.connectionData && config.connectionData.hops || 2;
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
                text = this.popover.find('span.path').text(
                    i18n('popovers.find_path.loading')
                ),
                button = this.popover.find('button').hide()
                    .text(i18n('popovers.find_path.button.add')).attr('disabled', true).focus();

            this.select('findPathHopsButtonSelector').off('click').on('click', this.onFindPathHopsButton.bind(this));

            this.positionDialog();

            var src = this.attr.sourceVertexId,
                dest = this.attr.targetVertexId;

            this.findPathRequest = this.findPath(src, dest)
                .fail(function() {
                    self.popover.find('span.path').text(i18n('popovers.find_path.error'));
                    self.positionDialog();
                })
                .done(function(result) {

                    var paths = result.paths,
                        vertices = result.uniqueVertices,
                        verticesNotSourceDest = vertices.filter(function(v) {
                            return v.id !== src && v.id !== dest;
                        }),
                        notInWorkspace = vertices.filter(function(v) {
                            return !appData.workspaceVertices[v.id];
                        }),
                        pathsFoundText = i18n('popovers.find_path.' + (
                            paths.length === 0 ? 'none' :
                            paths.length === 1 ? 'one' : 'some'
                        ), paths.length);

                    if (paths.length) {
                        if (notInWorkspace.length) {
                            var vertexText = i18n('popovers.find_path.not_in_workspace.' + (
                                    notInWorkspace.length === 1 ? 'one' : 'some'
                                ), notInWorkspace.length);

                            text.text(vertexText);
                            button.text(i18n('popovers.find_path.button.add.' + (
                                notInWorkspace.length === 1 ? 'one' : 'some'
                            ), notInWorkspace.length)).removeAttr('disabled').show();

                            var index, map = {};
                            for (var i = 0; i < notInWorkspace.length; i++) {
                                pathLoop: for (var j = 0; j < paths.length; j++) {
                                    for (var x = 0; x < paths[j].length; x++) {
                                        if (paths[j][x].id === notInWorkspace[i].id) {
                                            map[notInWorkspace[i].id] = {
                                                sourceId: paths[j][x - 1].id,
                                                targetId: paths[j][x + 1].id
                                            };
                                            break pathLoop;
                                        }
                                    }
                                }
                            }

                            self.verticesToAdd = notInWorkspace;
                            self.verticesToAddLayoutMap = map;
                        } else {
                            text.text(i18n('popovers.find_path.not_in_workspace.none'));
                        }

                        cy.$('.temp').remove();
                        self.trigger('focusPaths', { paths: paths, sourceId: src, targetId: dest });
                    } else text.text(i18n('popovers.find_path.searching.hops', self.attr.hops));

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
                        .then(function(data) {
                            var vertices = [], added = {};
                            data.paths.forEach(function(path) {
                                path.forEach(function(vertex) {
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
            this.trigger('selectObjects', { vertices: vertices });
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
