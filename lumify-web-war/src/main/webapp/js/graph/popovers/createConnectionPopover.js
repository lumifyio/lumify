
define([
    'flight/lib/component',
    './withVertexPopover',
    'service/vertex',
    'service/ontology',
    'service/relationship'
], function(
    defineComponent,
    withVertexPopover,
    VertexService,
    OntologyService,
    RelationshipService) {
    'use strict';

    return defineComponent(CreateConnectionPopover, withVertexPopover);

    function CreateConnectionPopover() {

        this.vertexService = new VertexService();
        this.ontologyService = new OntologyService();
        this.relationshipService = new RelationshipService();

        this.defaultAttrs({
            buttonSelector: 'button'
        });

        this.before('initialize', function(node, config) {
            config.template = 'createConnectionPopover';
        });


        this.after('initialize', function() {
            this.on('click', {
                buttonSelector: this.onCreateConnection
            });

            this.on('popoverInitialize', function() {

                var self = this,
                    cy = this.attr.cy,
                    select = this.popover.find('select'),
                    button = this.popover.find('button');
                
                select.html('<option>Loading...</option>');
                button.text('Connect').attr('disabled', true).focus();

                require(['configuration/plugins/visibility/visibilityEditor'], function(Visibility) {
                    Visibility.attachTo(self.$node.find('.visibility'), {
                        value: ''
                    });
                    self.positionDialog();
                });
                this.on('visibilitychange', this.onVisibilityChange);

                this.getRelationshipLabels(
                    cy.getElementById(this.attr.edge.data('source')),
                    cy.getElementById(this.attr.edge.data('target'))
                ).fail(function() {
                    select.html('<option>Unknown Server Error</option>');
                }).done(function(relationships) {

                    if (relationships.length) {
                        select.html(
                            relationships.map(function(d){
                                return '<option value="' + d.title + '">' + d.displayName + '</option>';
                            }).join('')
                        ).siblings('button').removeAttr('disabled');
                    } else {
                        select.html('<option>No valid relationships</option>');
                    }

                    self.positionDialog();
                });
            });
        });

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data.value;
        };


        this.onCreateConnection = function(e) {
            var $target = $(e.target);

            $target.text('Connecting...').attr('disabled', true);

            var self = this,
                parameters = {
                    sourceGraphVertexId: this.attr.sourceVertexId,
                    destGraphVertexId: this.attr.targetVertexId,
                    predicateLabel: $target.siblings('select').val(),
                    visibilitySource: this.visibilitySource
                };

            this.relationshipService.createRelationship(parameters)
                .done(function(data) {
                    self.on(document, 'relationshipsLoaded', function loaded() {
                        self.trigger('finishedVertexConnection');
                        self.off(document, 'relationshipsLoaded', loaded);
                    });
                    // TODO: should we send an expected relationship so
                    // data.js will continue checking until it's eventually
                    // consistent?
                    self.trigger('refreshRelationships');
                });
        };

        this.getRelationshipLabels = function (source, dest) {
            var self = this,
                sourceConceptTypeId = source.data('_conceptType').value,
                destConceptTypeId = dest.data('_conceptType').value;

            return $.when(
                this.ontologyService.conceptToConceptRelationships(sourceConceptTypeId, destConceptTypeId),
                this.ontologyService.relationships()
            ).then(function(relationships, ontologyRelationships) {
                var relationshipsTpl = [];

                relationships.forEach(function (relationship) {
                    var ontologyRelationship = ontologyRelationships.byTitle[relationship.title];
                    var displayName;
                    if (ontologyRelationship) {
                        displayName = ontologyRelationship.displayName;
                    } else {
                        displayName = relationship.title;
                    }

                    var data = {
                        title: relationship.title,
                        displayName: displayName
                    };

                    relationshipsTpl.push(data);
                });

                return relationshipsTpl;

            });
        };
    }
});
