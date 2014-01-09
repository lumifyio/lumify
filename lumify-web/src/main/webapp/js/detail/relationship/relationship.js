define([
    'flight/lib/component',
    'data',
    '../withTypeContent',
    '../withHighlighting',
    'tpl!./relationship',
    'service/relationship',
    'service/vertex',
    'detail/properties',
    'sf'
], function(
    defineComponent,
    appData,
    withTypeContent,
    withHighlighting,
    template,
    RelationshipService,
    VertexService,
    Properties,
    sf) {
    'use strict';

    var relationshipService = new RelationshipService();
    var vertexService = new VertexService();

    return defineComponent(Relationship, withTypeContent, withHighlighting);

    function Relationship() {

        this.defaultAttrs({
            vertexToVertexRelationshipSelector: '.vertex-to-vertex-relationship',
            propertiesSelector: '.properties'
        });

        this.after('teardown', function() {
            this.$node.off('click.paneClick');
        });

        this.after('initialize', function() {
            this.$node.on('click.paneClick', this.onPaneClicked.bind(this));
            this.on('click', {
                vertexToVertexRelationshipSelector: this.onVertexToVertexRelationshipClicked
            });

            this.loadRelationship();
        });


        this.loadRelationship = function() {
            var self = this,
                data = this.attr.data;

            this.vertexService.getVertexToVertexRelationshipDetails(
                    data.properties.source,
                    data.properties.target,
                    data.properties.relationshipType
            ).done(function(relationshipData) {
                self.$node.html(template({
                    auditsButton: self.auditsButton(),
                    relationshipData: relationshipData
                }));

                var properties = $.extend({}, data.properties);
                properties.relationshipLabel = data.properties.relationshipType;
                relationshipData.properties.forEach(function(prop) {
                    properties[prop.key] = prop.value;
                });

                Properties.attachTo(self.select('propertiesSelector'), {
                    data: {
                        id: encodeURIComponent(data.id),
                        properties: properties
                    }
                });

                self.updateEntityAndArtifactDraggables();
            });
        };

        this.onVertexToVertexRelationshipClicked = function(evt) {
            var $target = $(evt.target);
            var id = $target.data('vertexId');
            this.trigger(document, 'selectObjects', { vertices:[appData.vertex(id)] });
        };

        this.onPaneClicked = function(evt) {
            var $target = $(evt.target);

            if ($target.is('.entity, .artifact, span.relationship')) {
                var id = $target.data('vertexId');
                this.trigger(document, 'selectObjects', { vertices:[appData.vertex(id)] });
                evt.stopPropagation();
            }
        };
    }
});











