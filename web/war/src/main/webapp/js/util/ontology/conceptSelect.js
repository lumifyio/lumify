define([
    'flight/lib/component',
    'hbs!./concept-options',
    'service/ontology'
], function(
    defineComponent,
    conceptsTemplate,
    OntologyService) {
    'use strict';

    var ontologyService = new OntologyService();

    return defineComponent(ConceptSelector);

    function ConceptSelector() {

        this.defaultAttrs({
            conceptSelector: 'select',
            showAdminConcepts: false,
            onlySearchable: false,
            restrictConcept: ''
        });

        this.onConceptSelected = function(event) {
            var index = event.target.selectedIndex;

            this.trigger('conceptSelected', {
                concept: index > 0 ? this.allConcepts[index - 1].rawConcept : null
            });
        };

        this.onClearConcept = function(event) {
            this.select('conceptSelector').val('');
        };

        this.onSelectConcept = function(event, data) {
            this.select('conceptSelector').val(data && data.conceptId || '').change();
        };

        this.onEnableConcept = function(event, data) {
            if (data.disable || !data.enable) {
                this.select('conceptSelector').attr('disabled', true);
            } else {
                this.select('conceptSelector').removeAttr('disabled');
            }
        };

        this.after('initialize', function() {
            this.$node.html('<select><option>' + i18n('ontology.concept.loading') + '</option></select>');

            var self = this,
                select = this.select('conceptSelector');

            this.on('change', {
                conceptSelector: this.onConceptSelected
            });

            this.on('clearSelectedConcept', this.onClearConcept);
            this.on('selectConcept', this.onSelectConcept);
            this.on('enableConcept', this.onEnableConcept);

            ontologyService.concepts()
                .done(function(concepts) {
                    select.html(
                        conceptsTemplate({
                            defaultText: self.attr.defaultText,
                            concepts: self.allConcepts = _.chain(
                                concepts[self.attr.showAdminConcepts ? 'forAdmin' : 'byTitle']
                            )
                                .filter(function(c) {
                                    if (c.userVisible === false) {
                                        return false;
                                    }

                                    if (self.attr.restrictConcept) {
                                        // Walk up tree to see if any match
                                        var parentConceptId = c.id,
                                            shouldRestrictConcept = true;
                                        do {
                                            if (self.attr.restrictConcept === parentConceptId) {
                                                shouldRestrictConcept = false;
                                                break;
                                            }
                                        } while (
                                            parentConceptId &&
                                            (parentConceptId = concepts.byId[parentConceptId].parentConcept)
                                        );

                                        if (shouldRestrictConcept) {
                                            return false;
                                        }
                                    }

                                    if (self.attr.onlySearchable && c.searchable === false) {
                                        return false;
                                    }

                                    return true;
                                })
                                .map(function(c) {
                                    return {
                                        id: c.id,
                                        displayName: c.displayName,
                                        indent: c.flattenedDisplayName
                                                 .replace(/[^\/]/g, '')
                                                 .replace(/\//g, '&nbsp;&nbsp;&nbsp;&nbsp;'),
                                        selected: self.attr.selected === c.id,
                                        rawConcept: c
                                    }
                                })
                                .value()
                        })
                    ).change();
                })

        });

    }
});
