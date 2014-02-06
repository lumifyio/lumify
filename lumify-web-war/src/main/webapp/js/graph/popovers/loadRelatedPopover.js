
define([
    'flight/lib/component',
    './withVertexPopover'
], function(
    defineComponent,
    withVertexPopover) {
    'use strict';

    return defineComponent(LoadRelatedPopover, withVertexPopover);

    function LoadRelatedPopover() {

        this.defaultAttrs({
            searchButtonSelector: 'button.search',
            addButtonSelector: 'button.add'
        });

        this.before('initialize', function(node, config) {
            config.template = config.forceSearch ? 
                'loadRelatedPopoverForceSearch' :
                'loadRelatedPopoverPrompt';
        });

        this.after('initialize', function() {
            this.on('click', {
                searchButtonSelector: this.onSearch,
                addButtonSelector: this.onAdd
            });
        });

        this.onSearch = function(event) {
            this.trigger(document, 'searchByRelatedEntity', { vertexId : this.attr.relatedToVertexId });
            this.teardown();
        };

        this.onAdd = function(event) {
            this.trigger(this.attr.addToWorkspaceEvent, { vertices:this.attr.vertices });
            this.teardown();
        };
    }
});
