define([
    'flight/lib/component',
    '../withDropdown',
    'tpl!./statementForm',
    'tpl!./relationship-options'
], function(
    defineComponent,
    withDropdown,
    statementFormTemplate,
    relationshipTypeTemplate) {
    'use strict';

    return defineComponent(StatementForm, withDropdown);

    function StatementForm() {

        this.defaultAttrs({
            formSelector: '.form',
            sourceTermSelector: '.src-term',
            destTermSelector: '.dest-term',
            termLabelsSelector: '.src-term span, .dest-term span',
            createStatementButtonSelector: '.create-statement',
            statementLabelSelector: '.statement-label',
            invertAnchorSelector: 'a.invert',
            relationshipSelector: 'select',
            buttonDivSelector: '.buttons'
        });

        this.after('initialize', function() {
            var self = this;

            this.$node.html(statementFormTemplate({
                source: this.attr.sourceTerm.text(),
                dest: this.attr.destTerm.text()
            }));

            this.on('visibilitychange', this.onVisibilityChange);
            this.on('justificationchange', this.onJustificationChange);

            this.applyTermClasses(this.attr.sourceTerm, this.select('sourceTermSelector'));
            this.applyTermClasses(this.attr.destTerm, this.select('destTermSelector'));

            this.attr.sourceTerm.addClass('focused');
            this.attr.destTerm.addClass('focused');

            this.select('createStatementButtonSelector').attr('disabled', true);
            this.getRelationshipLabels();

            this.on('click', {
                createStatementButtonSelector: this.onCreateStatement,
                invertAnchorSelector: this.onInvert
            });
            this.on('opened', this.onOpened);
            this.on('keyup', {
                relationshipSelector: this.onInputKeyUp
            })
        });

        this.after('teardown', function() {
            this.attr.sourceTerm.removeClass('focused');
            this.attr.destTerm.removeClass('focused');
        });

        this.onInputKeyUp = function(event) {
            if (!this.select('createStatementButtonSelector').is(':disabled')) {
                switch (event.which) {
                    case $.ui.keyCode.ENTER:
                        this.onCreateStatement(event);
                }
            }
        }

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data;
            this.checkValid();
        };

        this.onJustificationChange = function(event, data) {
            this.justification = data;
            this.checkValid();
        };

        this.checkValid = function() {
            var button = this.select('createStatementButtonSelector');

            if (this.visibilitySource && this.visibilitySource.valid &&
                this.justification && this.justification.valid &&
                this.select('relationshipSelector').val().length) {
                button.removeAttr('disabled');
            } else {
                button.attr('disabled', true);
            }
        };

        this.applyTermClasses = function(el, applyToElement) {
            var classes = el.attr('class').split(/\s+/),
                ignored = [/^ui-*/, /^term$/, /^entity$/, /^label-info$/, /^detected-object$/];

            classes.forEach(function(cls) {
                var ignore = _.any(ignored, function(regex) {
                    return regex.test(cls);
                });
                if (!ignore) {
                    applyToElement.addClass(cls);
                }
            });

            this.ontologyService.concepts().done(function(concepts) {
                var concept = concepts.byId[el.data('info')['http://lumify.io#conceptType']];
                if (concept) {
                    applyToElement.addClass('concepticon-' + concept.className);
                }
            })
        };

        this.onSelection = function(e) {
            this.realtionshipTypeSelection = this.select('relationshipSelector').val();
            this.checkValid();
        };

        this.onOpened = function() {
            this.select('relationshipSelector')
                .on('change', this.onSelection.bind(this))
                .focus();
        };

        this.onInvert = function(e) {
            e.preventDefault();

            var sourceTerm = this.attr.sourceTerm;
            this.attr.sourceTerm = this.attr.destTerm;
            this.attr.destTerm = sourceTerm;

            this.select('formSelector').toggleClass('invert');
            this.getRelationshipLabels();
        };

        this.onCreateStatement = function(event) {
            var self = this,
                parameters = {
                    sourceGraphVertexId: this.attr.sourceTerm.data('info').graphVertexId ||
                        this.attr.sourceTerm.data('vertex-id'),
                    destGraphVertexId: this.attr.destTerm.data('info').graphVertexId ||
                        this.attr.destTerm.data('vertex-id'),
                    predicateLabel: this.select('relationshipSelector').val(),
                    visibilitySource: this.visibilitySource.value
                };

            if (this.select('formSelector').hasClass('invert')) {
                var swap = parameters.sourceGraphVertexId;
                parameters.sourceGraphVertexId = parameters.destGraphVertexId;
                parameters.destGraphVertexId = swap;
            }

            if (this.justification.sourceInfo) {
                parameters.sourceInfo = JSON.stringify(this.justification.sourceInfo);
            } else if (this.justification.justificationText) {
                parameters.justificationText = this.justification.justificationText;
            }

            _.defer(this.buttonLoading.bind(this));

            this.edgeService.create(parameters)
                .fail(function(req, reason, status) {
                    self.clearLoading();
                    self.markFieldErrors(status);
                })
                .done(function(data) {
                    _.defer(self.teardown.bind(self));
                    self.trigger(document, 'loadEdges');
                });
        };

        this.getRelationshipLabels = function() {
            var self = this,
                sourceConceptTypeId = this.attr.sourceTerm.data('info')['http://lumify.io#conceptType'],
                destConceptTypeId = this.attr.destTerm.data('info')['http://lumify.io#conceptType'];

            self.ontologyService.conceptToConceptRelationships(sourceConceptTypeId, destConceptTypeId)
                .done(function(relationships) {
                    self.displayRelationships(relationships);
                });
        };

        this.displayRelationships = function(relationships) {
            var self = this;

            this.visibilitySource = { source: '', valid: true };
            self.ontologyService.relationships().done(function(ontologyRelationships) {
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

                if (relationships.length) {
                    require([
                        'configuration/plugins/visibility/visibilityEditor',
                        'detail/dropdowns/propertyForm/justification',
                    ], function(Visibility, Justification) {

                        Visibility.attachTo(self.$node.find('.visibility'), {
                            value: ''
                        });

                        Justification.attachTo(self.$node.find('.justification'));
                    });
                } else self.$node.find('.visibility').teardownAllComponents().empty();

                self.select('relationshipSelector').html(relationshipTypeTemplate({ relationships: relationshipsTpl }));
            });
        };
    }

});
