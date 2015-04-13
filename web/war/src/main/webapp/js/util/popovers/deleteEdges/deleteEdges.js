
define([
    'flight/lib/component',
    '../withPopover',
    'util/edge/list',
    'util/withFormFieldErrors',
    'util/withDataRequest'
], function(
    defineComponent,
    withPopover,
    EdgeList,
    withFormFieldErrors,
    withDataRequest) {
    'use strict';

    return defineComponent(DeleteEdges, withPopover, withFormFieldErrors, withDataRequest);

    function DeleteEdges() {

        this.defaultAttrs({
            cancelSelector: '.btn-default',
            deleteSelector: '.btn-danger'
        });

        this.after('teardown', function() {
            if (this.request && this.request.cancel) {
                this.request.cancel();
            }
        });

        this.before('initialize', function(node, config) {
            config.template = 'deleteEdges/template';
            config.teardownOnTap = true;

            this.after('setupWithTemplate', function() {
                var self = this;

                EdgeList.attachTo(this.popover.find('.edge-list'), {
                    edges: this.attr.edges
                });

                this.on(this.popover, 'selectObjects', this.onSelectObjects);

                this.on(this.popover, 'click', {
                    cancelSelector: this.onCancel,
                    deleteSelector: this.onDelete
                });

                this.positionDialog();

                window.focus();
            })
        });

        this.onSelectObjects = function(event, data) {
            event.stopPropagation();

            this.edgeId = data.edgeIds && data.edgeIds.length && data.edgeIds[0];
            if (this.edgeId) {
                this.popover.find('.btn-danger').removeAttr('disabled');
            } else {
                this.popover.find('.btn-danger').attr('disabled', true);
            }
        };

        this.onCancel = function() {
            this.teardown();
        };

        this.onDelete = function(event) {
            var self = this,
                button = $(event.target)
                    .text(i18n('popovers.delete_edges.button.deleting'))
                    .addClass('loading')
                    .attr('disabled', true);

            this.request = this.dataRequest('edge', 'delete', this.edgeId);

            this.request
                .then(function(result) {
                    self.attr.edges = _.reject(self.attr.edges, function(e) {
                        return e.id === self.edgeId;
                    });
                    if (self.attr.edges.length === 0) {
                        self.teardown();
                    } else {
                        self.positionDialog();
                    }
                })
                .finally(function() {
                    button
                        .text(i18n('popovers.delete_edges.button.delete'))
                        .removeClass('loading')
                })
                .catch(function() {
                    button.removeAttr('disabled');
                })
        }
    }
});
