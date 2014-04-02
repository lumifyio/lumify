
define([
    'flight/lib/component',
    '../withPopover'
], function(
    defineComponent,
    withPopover) {
    'use strict';

    return defineComponent(LoadRelatedPopover, withPopover);

    function LoadRelatedPopover() {

        this.defaultAttrs({
            searchButtonSelector: '.dialog-popover button.search',
            addButtonSelector: '.dialog-popover button.add'
        });

        this.before('initialize', function(node, config) {
            if (!config.title) {
                console.warn('title attribute required');
                config.title = 'Unknown';
            }
            config.template = config.forceSearch ? 
                'loadRelated/loadRelatedForceSearch' :
                'loadRelated/loadRelatedPrompt';
        });

        this.after('initialize', function() {
            this.on(document, 'click', {
                searchButtonSelector: this.onSearch,
                addButtonSelector: this.onAdd
            });
        });

        this.onSearch = function(event) {
            this.trigger(document, 'searchByRelatedEntity', { vertexId : this.attr.relatedToVertexId });
            this.teardown();
        };

        this.onAdd = function(event) {
            this.trigger(document, 'addVertices', { vertices:this.attr.vertices });
            this.teardown();
        };
    }
});
