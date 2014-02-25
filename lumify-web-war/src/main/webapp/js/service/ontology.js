define([
    'service/serviceBase'
], function (ServiceBase) {
    'use strict';


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
        return this._ajaxGet({ url:'ontology' })
                    .then(function(ontology) {
                        return $.extend({}, ontology, {
                            conceptsById: _.indexBy(ontology.concepts, 'id'),
                            propertiesById: _.indexBy(ontology.properties, 'id')
                        });
                    });
    };

    OntologyService.prototype.concepts = function () {
        console.log('BUILDING concepts')
        return this.ontology()
                    .then(function(ontology) {
                        return {
                            entityConcept: buildTree(ontology.concepts, _.findWhere(ontology.concepts, {title:'entity'})),
                            byId: ontology.conceptsById, 
                            byTitle: _.chain(ontology.concepts)
                                .map(addFlattenedTitles.bind(null, ontology.conceptsById))
                                .sortBy('flattenedDisplayName')
                                .value()
                        };
                    });

        function buildTree(concepts, root) {
            var groupedByParent = _.groupBy(concepts, 'parentConcept'),
                findChildrenForNode = function(node) {
                    node.children = groupedByParent[node.id] || [];
                    node.children.forEach(findChildrenForNode);
                }

            findChildrenForNode(root);

            return root;
        }

        function addFlattenedTitles(conceptsById, concept) {
            var parentConceptId = concept.parentConcept,
                currentParentConcept = null,
                parents = [];

            while (parentConceptId) {
                currentParentConcept = conceptsById[parentConceptId];
                if (_.contains(['rootConcept', 'entity'], currentParentConcept.title)) break;
                parents.push(currentParentConcept);
                parentConceptId = currentParentConcept.parentConcept;
            }

            parents.reverse();
            var leadingSlashIfNeeded = parents.length ? '/' : '';

            return $.extend({}, concept, {
                flattenedTitle: _.pluck(parents, 'title').join('/') + 
                                leadingSlashIfNeeded + concept.title,
                flattenedDisplayName: _.pluck(parents, 'displayName').join('/') + 
                                leadingSlashIfNeeded + concept.displayName
            });
        }
    };

    OntologyService.prototype.relationships = function () {
        return this.ontology()
                    .then(function(ontology) {
                        return {
                            list: _.sortBy(ontology.relationships, 'displayName'),
                            byId: _.indexBy(ontology.relationships, 'id'),
                            byTitle: _.indexBy(ontology.relationships, 'title')
                        };
                    });
    };

    OntologyService.prototype.conceptToConceptRelationships = function(sourceConceptTypeId, destConceptTypeId) {
        return this.relationships()
                    .then(function(relationships) {
                        var groupedBySourceDest = _.groupBy(relationships.list, function(r) {
                            return r.source + '>' + r.dest;
                        })

                        return _.sortBy(groupedBySourceDest[sourceConceptTypeId + '>' + destConceptTypeId] || [], 'displayName');
                    });
    };
    OntologyService.prototype.conceptToConceptRelationships.memoizeHashFunction = function(s,d) {
        return s+d;
    };

    OntologyService.prototype.properties = function () {
        return this.ontology()
                    .then(function(ontology) {
                        return {
                            list: _.sortBy(ontology.properties, 'displayName'),
                            byTitle: _.indexBy(ontology.properties, 'title')
                        };
                    });
    };

    OntologyService.prototype.propertiesByConceptId = function (conceptId) {
        return this.ontology()
                    .then(function(ontology) {
                        var propertyIds = ontology.conceptsById[conceptId].properties,
                            properties = _.map(propertyIds, function(pId) {
                                return ontology.propertiesById[pId];
                            });

                        return {
                            list: _.sortBy(properties, 'displayName'),
                            byTitle: _.indexBy(properties, 'title')
                        };
                    });
    };

    OntologyService.prototype.propertiesByRelationshipLabel = function (relationshipLabel) {
        return this.ontology()
                    .then(function(ontology) {
                        var relationship = _.findWhere(ontology.relationships, { displayName:relationshipLabel }),
                            propertyIds = ontology.conceptsById[relationship.id].properties,
                            properties = _.map(propertyIds, function(pId) {
                                return ontology.propertiesById[pId];
                            });

                        return {
                            list: _.sortBy(properties, 'displayName'),
                            byTitle: _.indexBy(properties, 'title')
                        };
                    });
    };

    var memoizedMap = {};
    OntologyService.prototype.memoizeFunctions = function(toMemoize) {
        var self = this;
        toMemoize.forEach(function(f) {
            var cachedFunction = self[f],
            hashFunction = self[f].memoizeHashFunction;
            self[f] = function() {
                var key = hashFunction && hashFunction.apply(self, arguments);
                if (!key && arguments.length) key = arguments[0];
                if (!key) key = '(noargs)';
                key = f + key;

                var result = memoizedMap[key];

                if (result && result.statusText != 'abort') {
                    return result;
                }
                memoizedMap[key] = result = cachedFunction.apply(self, arguments);
                if (result.fail) {
                    result.fail(function() {
                        delete memoizedMap[key];
                    })
                }
                return result;
            }
        });
    }

    function buildPropertiesByTitle(properties) {
        var result = {};
        properties.forEach(function (property) {
            result[property.title] = property;
        });
        return result;
    }

    return OntologyService;
});

