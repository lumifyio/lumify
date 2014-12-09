
define([
    'flight/lib/component',
    '../withPopover',
    'util/vertex/formatters',
    'util/withDataRequest',
    'd3',
], function(
    defineComponent,
    withPopover,
    F,
    withDataRequest,
    d3) {
    'use strict';

    return defineComponent(AddToWorkspace, withPopover, withDataRequest);

    function AddToWorkspace() {

        this.defaultAttrs({
            addButtonSelector: '.btn-primary',
            cancelButtonSelector: '.btn-default'
        });

        this.after('teardown', function() {
            window.location.hash = '';
        });

        this.before('initialize', function(node, config) {
            config.template = 'addToWorkspace/template';
            var count = config.addVertexIds.vertexIds.length;
            config.vertexCount = i18n('popovers.add_to_workspace.vertex_count.' + (
                count === 0 ? 'none' : count === 1 ? 'one' : 'some'
            ), config.addVertexIds.vertexIds.length);

            this.after('setupWithTemplate', function() {
                this.on(this.popover, 'click', {
                    addButtonSelector: this.onAdd,
                    cancelButtonSelector: this.onCancel
                });

                Promise.all([
                    this.loadVertices(),
                    this.loadWorkspaces()
                ]).then(this.done.bind(this))
                  .catch(this.fail.bind(this));
            });
        });

        this.loadWorkspaces = function() {
            return this.dataRequest('workspace', 'all');
        };

        this.loadVertices = function() {
            return this.dataRequest('vertex', 'store', { vertexIds: this.attr.addVertexIds.vertexIds });
        };

        this.fail = function() {
            this.popover.find('.btn-primary')
                .text(i18n('popovers.add_to_workspace.error'))
        };

        this.done = function(results) {
            var vertices = results.shift(),
                workspaces = results.shift(),
                select = this.popover.find('select').empty();

            d3.select(select.get(0))
                .selectAll('option')
                .data(
                    _.sortBy(workspaces, function(workspace) {
                        return workspace.title.toLowerCase();
                    })
                )
                .call(function() {
                    this.enter().append('option');
                })
                .attr('selected', function(workspace) {
                    return lumifyData.currentWorkspaceId === workspace.workspaceId ?
                        'selected' : null;
                })
                .attr('disabled', function(workspace) {
                    return workspace.editable ? null : 'disabled';
                })
                .attr('value', function(workspace) {
                    return workspace.workspaceId;
                })
                .text(function(workspace) {
                    if (!workspace.editable) {
                        return workspace.title + ' (' + i18n('popovers.add_to_workspace.readonly') + ')';
                    }
                    return workspace.title;
                });

            this.verticesToAdd = vertices;
            this.popover.find('.btn-primary').removeAttr('disabled')
        };

        this.addToCurrentWorkspace = function() {
            this.trigger(document, 'addVertices', {
                vertices: this.verticesToAdd,
                options: {
                    fit: true
                }
            });
            this.teardown();
        }

        this.onAdd = function(event) {
            var self = this,
                workspaceId = this.popover.find('select').val();

            if (lumifyData.currentWorkspaceId !== workspaceId) {
                this.on(document, 'workspaceLoaded', function loaded(event, workspace) {
                    self.addToCurrentWorkspace();
                });
                this.trigger(document, 'switchWorkspace', {
                    workspaceId: workspaceId
                })
            } else {
                this.addToCurrentWorkspace();
            }
        };

        this.onCancel = function(event) {
            this.teardown();
        };
    }
});
