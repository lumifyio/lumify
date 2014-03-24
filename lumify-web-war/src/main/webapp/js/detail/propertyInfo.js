
define([
    'flight/lib/component',
    'tpl!./propertyInfo',
], function(
    defineComponent,
    template) {
    'use strict';

    return defineComponent(PropertyInfo);

    function PropertyInfo() {

        this.defaultAttrs({
            deleteButtonSelector: '.btn-danger',
            editButtonSelector: '.btn-default'
        });

        this.after('initialize', function() {
            this.$node.html(template({
                property: this.attr.property
            }));

            this.on('click', {
                deleteButtonSelector: this.onDelete,
                editButtonSelector: this.onEdit
            })

            this.on('addPropertyError', this.onAddPropertyError);
        })

        this.onAddPropertyError = function(event, data) {
            var button = this.select('deleteButtonSelector').removeClass('loading'),
                text = button.text();

            button.text(data.error || 'Unknown Error')
            _.delay(function() {
                button.text(text).removeAttr('disabled');
            }, 3000)
        };

        this.onEdit = function() {
            this.trigger('editProperty', {
                property: this.attr.property
            });
        };

        this.onDelete = function(e) {
            var button = this.select('deleteButtonSelector').addClass('loading').attr('disabled', true);
            this.trigger('deleteProperty', {
                property: this.attr.property.key
            });
        };
    }

});
