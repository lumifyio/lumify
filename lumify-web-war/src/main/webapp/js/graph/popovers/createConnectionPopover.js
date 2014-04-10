
define([
    'flight/lib/component',
    './withVertexPopover',
    'service/vertex',
    'service/ontology',
    'service/relationship',
    'util/withFormFieldErrors'
], function(
    defineComponent,
    withVertexPopover,
    VertexService,
    OntologyService,
    RelationshipService,
    withFormFieldErrors) {
    'use strict';

    return defineComponent(CreateConnectionPopover, withVertexPopover, withFormFieldErrors);

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

                this.on('visibilitychange', this.onVisibilityChange);

                this.getRelationshipLabels(
                    cy.getElementById(this.attr.edge.data('source')),
                    cy.getElementById(this.attr.edge.data('target'))
                ).fail(function() {
                    select.html('<option>Unknown Server Error</option>');
                }).done(function(relationships) {

                    if (relationships.length) {
                        select.html(
                            relationships.map(function(d) {
                                return '<option value="' + d.title + '">' + d.displayName + '</option>';
                            }).join('')
                        ).siblings('button').removeAttr('disabled');

                        require(['configuration/plugins/visibility/visibilityEditor'], function(Visibility) {
                            Visibility.attachTo(self.$node.find('.visibility'), {
                                value: ''
                            });
                            self.positionDialog();
                        });
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
            var self = this,
                $target = $(e.target)
                    .text('Connecting...')
                    .attr('disabled', true),
                parameters = {
                    sourceGraphVertexId: this.attr.sourceVertexId,
                    destGraphVertexId: this.attr.targetVertexId,
                    predicateLabel: $target.siblings('select').val(),
                    visibilitySource: this.visibilitySource
                },
                inputs = this.$node.find('select, input')
                    .attr('disabled', true);

            this.attr.teardownOnTap = false;

            this.relationshipService.createRelationship(parameters)
                .always(function() {
                    self.attr.teardownOnTap = true;
                })
                .fail(function(req, reason, statusText) {
                    $target.text('Connect')
                        .add(inputs)
                        .removeAttr('disabled');
                    self.markFieldErrors(statusText);
                })
                .done(function(data) {
                    self.on(document, 'relationshipsLoaded', function loaded() {
                        self.trigger('finishedVertexConnection');
                        self.off(document, 'relationshipsLoaded', loaded);
                    });
                    self.trigger('refreshRelationships');
                });
        };

        this.getRelationshipLabels = function(source, dest) {
            var self = this,
                sourceConceptTypeId = source.data('http://lumify.io#conceptType').value,
                destConceptTypeId = dest.data('http://lumify.io#conceptType').value;

            return $.when(
                this.ontologyService.conceptToConceptRelationships(sourceConceptTypeId, destConceptTypeId),
                this.ontologyService.relationships()
            ).then(function(relationships, ontologyRelationships) {
                var relationshipsTpl = [];

                relationships.forEach(function(relationship) {
                    var ontologyRelationship = ontologyRelationships.byTitle[relationship.title],
                        displayName;
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
