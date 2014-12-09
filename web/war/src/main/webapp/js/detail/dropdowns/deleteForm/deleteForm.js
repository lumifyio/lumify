define([
    'flight/lib/component',
    '../withDropdown',
    'tpl!./deleteForm',
    'tpl!util/alert',
    'util/withDataRequest'
], function(
    defineComponent,
    withDropdown,
    template,
    alertTemplate,
    withDataRequest) {
    'use strict';

    return defineComponent(DeleteForm, withDropdown, withDataRequest);

    function DeleteForm() {

        this.defaultAttrs({
            primarySelector: '.btn-danger'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                primarySelector: this.onDelete
            })
            this.$node.html(template(this.attr.data));
        });

        this.onDelete = function(event) {
            var self = this;

            this.buttonLoading('.btn-danger');

            this.dataRequest(this.attr.data.type, 'delete', this.attr.data.id)
                .then(function() {
                    self.teardown();
                })
                .catch(function(error) {
                    self.markFieldErrors(error && error.statusText || error);
                    self.clearLoading();
                })
        };
    }
});
