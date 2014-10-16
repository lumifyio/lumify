define([
    'flight/lib/component',
    'data',
    '../withTypeContent',
    '../withHighlighting',
    '../toolbar/toolbar',
    'tpl!./relationship',
    'service/edge',
    'service/vertex',
    'service/ontology',
    'detail/properties/properties',
    'util/vertex/formatters',
    'sf',
    'd3'
], function(
    defineComponent,
    appData,
    withTypeContent,
    withHighlighting,
    Toolbar,
    template,
    EdgeService,
    VertexService,
    OntologyService,
    Properties,
    F,
    sf,
    d3) {
    'use strict';

    var predicate = { name: 'http://lumify.io#conceptType' },
        edgeService = new EdgeService(),
        vertexService = new VertexService(),
        ontologyService = new OntologyService();

    return defineComponent(Edge, withTypeContent, withHighlighting);

    function Edge() {

        this.defaultAttrs({
            vertexToVertexRelationshipSelector: '.vertex-to-vertex-relationship',
            propertiesSelector: '.properties',
            toolbarSelector: '.comp-toolbar'
        });

        this.after('teardown', function() {
            this.$node.off('click.paneClick');
        });

        this.after('initialize', function() {
            this.$node.on('click.paneClick', this.onPaneClicked.bind(this));
            this.on('click', {
                vertexToVertexRelationshipSelector: this.onVertexToVertexRelationshipClicked
            });

            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.loadRelationship();
        });

        this.onVerticesUpdated = function(event, data) {
            var source = _.findWhere(data.vertices, { id: this.relationship.source.id }),
                target = _.findWhere(data.vertices, { id: this.relationship.target.id });

            if (source) {
                this.relationship.source = source;
            }
            if (target) {
                this.relationship.target = target;
            }

            this.update();
        };

        this.update = function() {
            var relationship = this.relationship,
                source = relationship.source,
                target = relationship.target;

            d3.select(this.node).selectAll('.vertex-to-vertex-relationship')
                .data([source, target])
                .call(function() {
                    this
                        .text(function(d) {
                            return F.vertex.title(d);
                        })
                        .attr('data-vertex-id', function(d) {
                            return d.id;
                        })
                        .append('div')
                        .attr('class', 'subtitle')
                        .text(function(d) {
                            return d.concept.displayName;
                        });
                })
        };

        this.loadRelationship = function() {
            var self = this,
                data = this.attr.data;
            $.when(
                self.handleCancelling(self.ontologyService.ontology()),
                self.handleCancelling(self.ontologyService.relationships()),
                self.handleCancelling(edgeService.properties(data.id))
            ).done(function(ontology, ontologyRelationships, relationshipData) {
                var relationship = relationshipData[0];

                self.ontology = ontology;
                self.ontologyRelationships = ontologyRelationships;
                self.relationship = relationship;
                $.extend(relationship.source, {
                    concept: self.ontology.conceptsById[
                        _.findWhere(relationship.source.properties, predicate).value
                    ]
                });

                $.extend(relationship.target, {
                    concept: self.ontology.conceptsById[
                        _.findWhere(relationship.target.properties, predicate).value
                    ]
                });
                self.$node.html(template({}));
                self.update();

                Properties.attachTo(self.select('propertiesSelector'), {
                    data: relationship
                });

                Toolbar.attachTo(self.select('toolbarSelector'), {
                    toolbar: [
                        Toolbar.ITEMS.AUDIT
                    ]
                });

                self.updateEntityAndArtifactDraggables();
            });
        };

        this.onVertexToVertexRelationshipClicked = function(evt) {
            var $target = $(evt.target),
                id = $target.data('vertexId');
            this.trigger(document, 'selectObjects', { vertices: [appData.vertex(id)] });
        };

        this.onPaneClicked = function(evt) {
            var $target = $(evt.target);

            if ($target.is('.entity, .artifact, span.relationship')) {
                var id = $target.data('vertexId');
                this.trigger(document, 'selectObjects', { vertices: [appData.vertex(id)] });
                evt.stopPropagation();
            }
        };
    }
});
