define([
    'flight/lib/component',
    'flight/lib/registry',
    'tpl!./detail'
], function(defineComponent, registry, template) {
    'use strict';

    return defineComponent(Detail);

    function Detail() {

        this.defaultAttrs({
            mapCoordinatesSelector: '.map-coordinates',
            detailTypeContentSelector: '.type-content'
        });

        this.after('initialize', function() {
            this.on('click', {
                mapCoordinatesSelector: this.onMapCoordinatesClicked
            });
            this.on('finishedLoadingTypeContent', this.onFinishedTypeContent);

            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.preventDropEventsFromPropagating();

            this.before('teardown', this.teardownComponents);

            this.$node.html(template({}));

            if (this.attr.loadGraphVertexData) {
                this.onObjectsSelected(null, { vertices: [this.attr.loadGraphVertexData] });
            }
        });

        this.onFinishedTypeContent = function() {
            this.$node.removeClass('loading');
        };

        // Ignore drop events so they don't propagate to the graph/map
        this.preventDropEventsFromPropagating = function() {
            this.$node.droppable({ tolerance: 'pointer', accept: '*' });
        };

        this.onMapCoordinatesClicked = function(evt, data) {
            evt.preventDefault();
            var $target = $(evt.target);
            data = {
                latitude: $target.data('latitude'),
                longitude: $target.data('longitude')
            };
            this.trigger('mapCenter', data);
        };

        this.onObjectsSelected = function(evt, data) {
            var self = this,
                vertices = data.vertices,
                edges = data.edges,
                moduleName, moduleData;

            if (!vertices.length && !edges.length) {
                var pane = this.$node.closest('.detail-pane');

                return pane.on(TRANSITION_END, function(e) {
                    if (/transform/.test(e.originalEvent && e.originalEvent.propertyName)) {
                        self.teardownComponents();
                        pane.off(TRANSITION_END);
                    }
                });
            }

            this.teardownComponents();
            this.$node.addClass('loading');

            if (vertices.length > 1) {
                moduleName = 'multiple';
                moduleData = vertices;
            } else if (vertices.length === 1) {
                var vertex = vertices[0],
                    type = vertices[0].concept && vertices[0].concept.displayType ||
                        (vertices[0].properties['http://lumify.io#conceptType'].value != 'relationship' ?
                         'entity' : 'relationship');
                if (type === 'relationship') {
                    moduleName = type;
                } else {
                    moduleName = (((type != 'document' &&
                                    type != 'image' &&
                                    type != 'video' &&
                                    type != 'audio') ? 'entity' : 'artifact'
                    ) || 'entity').toLowerCase();
                }
                moduleData = vertex;
            } else {
                moduleName = 'relationship';
                moduleData = edges[0];
            }

            moduleName = moduleName.toLowerCase();

            require([
                'detail/' + moduleName + '/' + moduleName,
            ], function(Module) {
                Module.attachTo(self.select('detailTypeContentSelector'), {
                    data: moduleData,
                    highlightStyle: self.attr.highlightStyle,
                    focusOffsets: data.focus && data.focus.offsets
                });
            });
        };

        this.teardownComponents = function() {
            this.select('detailTypeContentSelector').teardownAllComponents();
        }
    }
});
