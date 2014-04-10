define([
    'service/serviceBase'
], function(ServiceBase) {
    'use strict';

    var PARENT_CONCEPT = 'http://www.w3.org/2002/07/owl#Thing';

    function OntologyService() {
        ServiceBase.call(this);

        var toMemoize = [
            'ontology',
            'concepts',
            'relationships',
            'conceptToConceptRelationships',
            'properties',
            'propertiesByConceptId',
            'propertiesByRelationshipLabel'
        ];

        this.memoizeFunctions(toMemoize);
        return this;
    }

    OntologyService.prototype = Object.create(ServiceBase.prototype);

    OntologyService.prototype.ontology = function() {
        return this._ajaxGet({ url: 'ontology' })
                    .then(function(ontology) {
                        return $.extend({}, ontology, {
                            conceptsById: _.indexBy(ontology.concepts, 'id'),
                            propertiesByTitle: _.indexBy(ontology.properties, 'title')
                        });
                    });
    };

    OntologyService.prototype.concepts = function() {
        var clsIndex = 0, conceptToClassMap = {}, classToConceptMap = {};
        return this.ontology()
                    .then(function(ontology) {
                        return {
                            entityConcept: buildTree(
                                ontology.concepts,
                                _.findWhere(ontology.concepts, {id: PARENT_CONCEPT})
                            ),
                            byId: ontology.conceptsById, 
                            byClassName: _.indexBy(ontology.concepts, 'className'),
                            byTitle: _.chain(ontology.concepts)
                                .filter(onlyEntityConcepts.bind(null, ontology.conceptsById))
                                .map(addFlattenedTitles.bind(null, ontology.conceptsById))
                                .sortBy('flattenedDisplayName')
                                .value()
                        };
                    });

        function buildTree(concepts, root) {
            var groupedByParent = _.groupBy(concepts, 'parentConcept'),
                findChildrenForNode = function(node) {
                    node.className = 'conceptId-' + (clsIndex++);
                    node.children = groupedByParent[node.id] || [];
                    node.children.forEach(function(child) {
                        if (!child.glyphIconHref) {
                            child.glyphIconHref = node.glyphIconHref;
                        }
                        if (!child.displayType) {
                            child.displayType = node.displayType;
                        }
                        if (!child.color) {
                            if (node.color) {
                                child.color = node.color;
                            } else {
                                console.warn('No color specified in concept hierarchy for conceptType:', child.id);
                                child.color = 'rgb(0, 0, 0)';
                            }
                        }
                        findChildrenForNode(child);
                    });
                }

            findChildrenForNode(root);

            return root;
        }

        function onlyEntityConcepts(conceptsById, concept) {
            var parentConceptId = concept.parentConcept,
                currentParentConcept = null;

            while (parentConceptId) {
                currentParentConcept = conceptsById[parentConceptId];
                if (currentParentConcept.id === PARENT_CONCEPT) {
                    return true;
                }
                parentConceptId = currentParentConcept.parentConcept;
            }

            return false;
        }

        function addFlattenedTitles(conceptsById, concept) {
            var parentConceptId = concept.parentConcept,
                currentParentConcept = null,
                parents = [];

            while (parentConceptId) {
                currentParentConcept = conceptsById[parentConceptId];
                if (currentParentConcept.id === PARENT_CONCEPT) break;
                parents.push(currentParentConcept);
                parentConceptId = currentParentConcept.parentConcept;
            }

            parents.reverse();
            var leadingSlashIfNeeded = parents.length ? '/' : '';

            return $.extend({}, concept, {
                flattenedDisplayName: _.pluck(parents, 'displayName').join('/') + 
                                leadingSlashIfNeeded + concept.displayName
            });
        }
    };

    OntologyService.prototype.relationships = function() {
        return $.when(this.concepts(), this.ontology())
                    .then(function(concepts, ontology) {
                        var list = _.sortBy(ontology.relationships, 'displayName');
                        return {
                            list: list,
                            byId: _.indexBy(ontology.relationships, 'id'),
                            byTitle: _.indexBy(ontology.relationships, 'title'),
                            groupedBySourceDestConcepts: conceptGrouping(concepts, list),
                            groupedBySourceDestConceptsKeyGen: genKey
                        };
                    });

        function genKey(source, dest) {
            return [source, dest].join('>');
        }

        // Calculates cache with all possible mappings from source->dest
        // including all possible combinations of source->children and
        // dest->children
        function conceptGrouping(concepts, relationships) {
            var groups = {},
                addToAllDestChildrenGroups = function(r, source, dest) {
                    var key = genKey(source, dest);

                    if (!groups[key]) {
                        groups[key] = [];
                    }

                    groups[key].push(r);

                    var destConcept = concepts.byId[dest]
                    if (destConcept && destConcept.children) {
                        destConcept.children.forEach(function(c) { 
                            addToAllDestChildrenGroups(r, source, c.id); 
                        })
                    }
                };
                
            relationships.forEach(function(r) {
                addToAllDestChildrenGroups(r, r.source, r.dest); 

                var sourceConcept = concepts.byId[r.source]
                if (sourceConcept && sourceConcept.children) {
                    sourceConcept.children.forEach(function(c) { 
                        addToAllDestChildrenGroups(r, c.id, r.dest); 
                    })
                }
            });

            return groups;
        }
    };

    OntologyService.prototype.conceptToConceptRelationships = function(sourceConceptTypeId, destConceptTypeId) {
        return this.relationships()
                    .then(function(relationships) {
                        var key = relationships.groupedBySourceDestConceptsKeyGen(
                            sourceConceptTypeId,
                            destConceptTypeId
                        );

                        return _.chain(relationships.groupedBySourceDestConcepts[key] || [])
                            .uniq(function(r) {
                                return r.title 
                            })
                            .sortBy('displayName')
                            .value()
                    });
    };
    OntologyService.prototype.conceptToConceptRelationships.memoizeHashFunction = function(s,d) {
        return s + d;
    };

    OntologyService.prototype.properties = function() {
        return this.ontology()
                    .then(function(ontology) {
                        return {
                            list: _.sortBy(ontology.properties, 'displayName'),
                            byTitle: _.indexBy(ontology.properties, 'title'),
                            byDataType: _.groupBy(ontology.properties, 'dataType')
                        };
                    });
    };

    OntologyService.prototype.propertiesByConceptId = function(conceptId) {
        return this.ontology()
                    .then(function(ontology) {
                        var 
                            propertyIds = [],
                            collectPropertyIds = function(conceptId) {
                                var concept = ontology.conceptsById[conceptId],
                                    properties = concept.properties,
                                    parentConceptId = concept.parentConcept;

                                if (properties.length) {
                                    propertyIds.push.apply(propertyIds, properties);
                                }
                                if (parentConceptId) {
                                    collectPropertyIds(parentConceptId);
                                }
                            };

                        collectPropertyIds(conceptId);

                        var properties = _.chain(propertyIds)
                            .uniq()
                            .map(function(pId) {
                                return ontology.propertiesByTitle[pId];
                            })
                            .value();

                        return {
                            list: _.sortBy(properties, 'displayName'),
                            byTitle: _.indexBy(properties, 'title')
                        };
                    });
    };

    OntologyService.prototype.propertiesByRelationshipLabel = function(relationshipLabel) {
        return this.ontology()
                    .then(function(ontology) {
                        var relationship = _.findWhere(ontology.relationships, { 
                                displayName: relationshipLabel 
                            }),
                            propertyIds = relationship.properties || [],
                            properties = _.map(propertyIds, function(pId) {
                                return ontology.propertiesByTitle[pId];
                            });

                        return {
                            list: _.sortBy(properties, 'displayName'),
                            byTitle: _.indexBy(properties, 'title')
                        };
                    });
    };

    function buildPropertiesByTitle(properties) {
        var result = {};
        properties.forEach(function(property) {
            result[property.title] = property;
        });
        return result;
    }

    return OntologyService;
});
