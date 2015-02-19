define([
    'flight/lib/component',
    'hbs!./relationships',
    'hbs!./relationship',
    'util/withDataRequest',
    './withSelect'
], function(
    defineComponent,
    template,
    relationshipTemplate,
    withDataRequest,
    withSelect) {
    'use strict';

    return defineComponent(RelationshipSelector, withDataRequest, withSelect);

    function RelationshipSelector() {

        this.defaultAttrs({
            defaultText: i18n('relationship.field.placeholder'),
            fieldSelector: 'input',
            limitParentConceptId: ''
        });

        this.after('initialize', function() {
            this.$node.html(template(this.attr));

            this.on('click', {
                fieldSelector: this.showTypeahead
            });
            this.on('limitParentConceptId', this.onLimitParentConceptId);

            this.setupTypeahead();
        });

        this.showTypeahead = function() {
            this.select('fieldSelector').typeahead('lookup').select();
        };

        this.onLimitParentConceptId = function(event, data) {
            this.attr.limitParentConceptId = data.conceptId;
            this.transformRelationships();
        };

        this.setupTypeahead = function() {
            var self = this;

            this.dataRequest('ontology', 'ontology')
                .done(function(ontology) {
                    self.ontology = ontology;

                    var ontologyConcepts = ontology.concepts,
                        relationshipOntology = ontology.relationships,
                        field = self.select('fieldSelector').attr('placeholder', self.attr.defaultText);

                    field.typeahead({
                        minLength: 0,
                        items: Number.MAX_VALUE,
                        source: function(query) {
                            var relationships = self.transformRelationships();

                            relationships.splice(0, 0, self.attr.defaultText);

                            return relationships;
                        },
                        matcher: function(relationship) {
                            if ($.trim(this.query) === '') {
                                return true;
                            }
                            if (relationship === self.attr.defaultText) {
                                return false;
                            }

                            return Object.getPrototypeOf(this).matcher.call(this, relationship.displayName);
                        },
                        sorter: _.identity,
                        updater: function(relationshipTitle) {
                            var $element = this.$element,
                                relationship = relationshipOntology.byTitle[relationshipTitle];

                            self.currentRelationshipTitle = relationship && relationship.title;
                            self.trigger('relationshipSelected', {
                                relationship: relationship
                            });
                            return relationship && relationship.displayName || '';
                        },
                        highlighter: function(relationship) {
                            return relationshipTemplate(relationship === self.attr.defaultText ?
                            {
                                relationship: {
                                    displayName: relationship,
                                }
                            } : {
                                relationship: relationship,
                            });
                        }
                    })

                    if (self.attr.focus) {
                        _.defer(function() {
                            field.focus();
                        })
                    }

                    self.allowEmptyLookup(field);
                });
        }

        this.transformRelationships = function() {
            var self = this,
                list = this.ontology.relationships.list;

            if (this.attr.limitParentConceptId) {
                list = _.chain(this.ontology.relationships.groupedBySourceDestConcepts)
                    .map(function(r, key) {
                        return ~key.indexOf(self.attr.limitParentConceptId) ? r : undefined;
                    })
                    .compact()
                    .flatten(true)
                    .unique(_.property('title'))
                    .value();
            }

            var previousSelectionFound = false,
                transformed = _.chain(list)
                    .sortBy('displayName')
                    .reject(function(r) {
                        return r.userVisible === false;
                    })
                    .map(function(r) {
                        if (r.title === self.currentRelationshipTitle) {
                            previousSelectionFound = true;
                        }
                        return _.extend({}, r, {
                            toString: function() {
                                return r.title;
                            }
                        })
                    })
                    .value();

            if (this.currentRelationshipTitle && !previousSelectionFound) {
                this.currentRelationshipTitle = null;
                this.select('fieldSelector').val('');
                this.trigger('relationshipSelected', {
                    relationship: null
                });
            }

            return transformed;
        }
    }
});
