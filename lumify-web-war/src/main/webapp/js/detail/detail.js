define([
    'flight/lib/component',
    'flight/lib/registry',
    'tpl!./detail'
], function (defineComponent, registry, template) {
    'use strict';

    return defineComponent(Detail);

    function Detail() {


        this.defaultAttrs({
            mapCoordinatesSelector: '.map-coordinates',
            detailTypeContentSelector: '.type-content'
        });

        this.after('initialize', function () {
            this.on('click', {
                mapCoordinatesSelector: this.onMapCoordinatesClicked
            });

            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.preventDropEventsFromPropagating();

            this.before('teardown', this.teardownComponents);

            this.$node.html(template({}));

            if (this.attr.loadGraphVertexData) {
                this.onObjectsSelected(null, { vertices: [this.attr.loadGraphVertexData] });
            }
        });

        // Ignore drop events so they don't propagate to the graph/map
        this.preventDropEventsFromPropagating = function () {
            this.$node.droppable({ tolerance: 'pointer', accept: '*' });
        };

        this.onMapCoordinatesClicked = function (evt, data) {
            evt.preventDefault();
            var $target = $(evt.target);
            data = {
                latitude: $target.data('latitude'),
                longitude: $target.data('longitude')
            };
            this.trigger('mapCenter', data);
        };

        this.onObjectsSelected = function (evt, data) {
            var self = this,
                vertices = data.vertices,
                edges = data.edges,
                moduleName, moduleData;

            this.teardownComponents();

            if (!vertices.length && !edges.length) {
                return;
            }

            if (vertices.length > 1) {
                moduleName = 'multiple';
                moduleData = vertices;
            } else if (vertices.length === 1) {
                var vertex = vertices[0],
                    type = vertices[0].concept && vertices[0].concept.displayType ||
                        (vertices[0].properties._conceptType ? 'relationship' : 'entity');
                if (type === 'relationship') {
                    moduleName = type;
                } else {
                    moduleName = (((type != 'document' && type != 'image' && type != 'video') ? 'entity' : 'artifact') || 'entity').toLowerCase();
                }
                moduleData = vertex;
            } else {
                moduleName = 'relationship';
                moduleData = edges[0];
            }

            moduleName = moduleName.toLowerCase();

            require([
                'detail/' + moduleName + '/' + moduleName,
            ], function (Module) {
                Module.attachTo(self.select('detailTypeContentSelector'), {
                    data: moduleData,
                    highlightStyle: self.attr.highlightStyle
                });
            });
        };

        this.teardownComponents = function () {
            var typeContentNode = this.select('detailTypeContentSelector'),
                instanceInfos = registry.findInstanceInfoByNode(typeContentNode[0]);
            if (instanceInfos.length) {
                instanceInfos.forEach(function (info) {
                    info.instance.teardown();
                });
            }
        }
    }
});
