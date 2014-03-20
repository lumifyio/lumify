
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
        })

        this.onEdit = function() {
            //this.trigger('editProperty')
        };

        this.onDelete = function() {
            //this.trigger('deleteProperty');
        };
    }

});
