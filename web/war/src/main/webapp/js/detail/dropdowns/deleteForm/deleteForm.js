define([
    'flight/lib/component',
    '../withDropdown',
    'tpl!./deleteForm',
    'tpl!util/alert',
    'util/withDataRequest',
    'util/vertex/formatters'
], function(
    defineComponent,
    withDropdown,
    template,
    alertTemplate,
    withDataRequest,
    F) {
    'use strict';

    return defineComponent(DeleteForm, withDropdown, withDataRequest);

    function DeleteForm() {

        this.defaultAttrs({
            primarySelector: '.btn-danger'
        });

        this.after('initialize', function() {
            var self = this,
                shouldConfirm = !F.vertex.isPublished(this.attr.data);

            if (shouldConfirm) {
                this.render();
            } else {
                this.onDelete();
            }
        });

        this.render = function() {
            this.on('click', {
                primarySelector: this.onDelete
            })
            this.$node.html(template(this.attr.data));
        }

        this.onDelete = function(event) {
            var self = this;

            if (event) {
                this.buttonLoading('.btn-danger');
            }

            this.trigger('maskWithOverlay', {
                loading: true,
                text: 'Deleting...'
            });

            this.dataRequest(this.attr.data.type, 'delete', this.attr.data.id)
                .finally(function() {
                    self.trigger('maskWithOverlay', { done: true });
                })
                .then(function() {
                    self.teardown();
                })
                .catch(function(error) {
                    if (self.$node.is(':empty')) {
                        self.render();
                    }
                    self.markFieldErrors(error && error.statusText || error);
                    self.clearLoading();
                })
        };
    }
});
