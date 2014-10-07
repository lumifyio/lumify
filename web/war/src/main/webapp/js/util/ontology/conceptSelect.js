define([
    'flight/lib/component',
    'hbs!./template',
    'hbs!./concept',
    'service/ontology'
], function(
    defineComponent,
    template,
    conceptTemplate,
    OntologyService) {
    'use strict';

    var ontologyService = new OntologyService();

    return defineComponent(ConceptSelector);

    function ConceptSelector() {

        this.defaultAttrs({
            defaultText: i18n('concept.field.placeholder'),
            fieldSelector: 'input',
            showAdminConcepts: false,
            onlySearchable: false,
            restrictConcept: ''
        });

        this.after('initialize', function() {
            this.$node.html(template(this.attr));

            this.on('click', {
                fieldSelector: this.showTypeahead
            });

            this.on('clearSelectedConcept', this.onClearConcept);
            this.on('enableConcept', this.onEnableConcept);

            this.setupTypeahead();

        });

        this.showTypeahead = function() {
            this.select('fieldSelector').typeahead('lookup').select();
        }

        this.onConceptSelected = function(event) {
            var index = event.target.selectedIndex;

            this.trigger('conceptSelected', {
                concept: index > 0 ? this.allConcepts[index - 1].rawConcept : null
            });
        };

        this.onClearConcept = function(event) {
            this.select('fieldSelector').val('');
        };

        this.onEnableConcept = function(event, data) {
            if (data.disable || !data.enable) {
                this.select('conceptSelector').attr('disabled', true);
            } else {
                this.select('conceptSelector').removeAttr('disabled');
            }
        };

        this.setupTypeahead = function() {
            var self = this;

            ontologyService.concepts().done(function(concepts) {
                concepts = self.transformConcepts(concepts);

                concepts.splice(0, 0, self.attr.defaultText);

                self.select('fieldSelector')
                    .attr('placeholder', self.attr.defaultText)
                    .typeahead({
                        minLength: 0,
                        items: Number.MAX_VALUE,
                        source: concepts,
                        matcher: function(concept) {
                            if ($.trim(this.query) === '') {
                                return true;
                            }
                            if (concept === self.attr.defaultText) {
                                return false;
                            }

                            return Object.getPrototypeOf(this).matcher.call(this, concept.flattenedDisplayName);
                        },
                        sorter: _.identity,
                        updater: function(conceptId) {
                            var $element = this.$element,
                                concept = self.conceptsById[conceptId];

                            self.trigger('conceptSelected', { concept: concept && concept.rawConcept });
                            _.defer(function() {
                                $element.blur();
                            });
                            return concept && concept.displayName || '';
                        },
                        highlighter: function(concept) {
                            return conceptTemplate(concept === self.attr.defaultText ?
                            {
                                concept: {
                                    displayName: concept,
                                    rawConcept: { }
                                },
                                path: null,
                                marginLeft: 0
                            } : {
                                concept: concept,
                                path: concept.flattenedDisplayName.replace(/\/?[^\/]+$/, ''),
                                marginLeft: concept.depth
                            });
                        }
                    })
                    .data('typeahead').lookup = allowEmptyLookup;
            });
        }

        this.transformConcepts = function(concepts) {
            var self = this;

            this.allConcepts = _.chain(
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
                        toString: function() {
                            return this.id;
                        },
                        displayName: c.displayName,
                        flattenedDisplayName: c.flattenedDisplayName,
                        depth: c.flattenedDisplayName
                                 .replace(/[^\/]/g, '').length,
                        selected: self.attr.selected === c.id,
                        rawConcept: c
                    }
                })
                .value();

            this.conceptsById = _.indexBy(this.allConcepts, 'id');

            return this.allConcepts;
        }
    }

    function allowEmptyLookup() {
        var items;

        this.query = this.$element.val();

        // Remove !this.query check to allow empty values to open dropdown
        if (this.query.length < this.options.minLength) {
            return this.shown ? this.hide() : this;
        }

        items = $.isFunction(this.source) ? this.source(this.query, $.proxy(this.process, this)) : this.source;

        return items ? this.process(items) : this;
    }
});
