define(['require', 'underscore'], function(require, _) {
    'use strict';

    var F = {
        date: {
            local: function(str) {
                if (_.isUndefined(str)) return '';
                var millis = _.isString(str) && !isNaN(Number(str)) ? Number(str) : str,
                    dateInLocale = _.isDate(millis) ? millis : new Date(millis);

                return dateInLocale;
            },
            utc: function(str) {
                if (_.isUndefined(str)) return '';
                var dateInLocale = F.date.local(str),
                    millisInMinutes = 1000 * 60,
                    millisFromLocaleToUTC = dateInLocale.getTimezoneOffset() * millisInMinutes,
                    dateInUTC = new Date(dateInLocale.getTime() + millisFromLocaleToUTC);
                return dateInUTC;
            }
        }
    };

    return function(message) {
        require([
            '../util/store',
            '../services/ontology'
        ], function(store, ontology) {
            var workspace = store.getObject(publicData.currentWorkspaceId, 'workspace'),
                vertexIds = _.keys(workspace.vertices),
                data = message.data,
                filters = data && data.filters,
                query = data && data.value,
                conceptFilter = filters && filters.conceptFilter,
                propertyFilters = filters && filters.propertyFilters,
                vertices = store.getObjects(workspace.workspaceId, 'vertex', vertexIds);

            ontology.ontology().done(function(ontology) {
                var result = partitionVertices(ontology, vertices, query, conceptFilter, propertyFilters),
                    matchingVertices = result.shift(),
                    otherVertices = result.shift();

                dispatchMain('workspaceUpdated', {
                    workspace: workspace,
                    newVertices: matchingVertices,
                    entityUpdates: _.values(_.pick(workspace.vertices, _.pluck(matchingVertices, 'id'))),
                    entityDeletes: _.pluck(otherVertices, 'id'),
                    userUpdates: [],
                    userDeletes: []
                });
                dispatchMain('rebroadcastEvent', {
                    eventName: 'workspaceFiltered',
                    data: {
                        hits: matchingVertices.length,
                        total: vertexIds.length
                    }
                })
            });
        })
    }

    function isKindOfConcept(conceptsById, vertex, conceptTypeFilter) {
        var conceptTypeProperty = _.findWhere(vertex.properties, { name: 'http://lumify.io#conceptType' }),
            conceptType = conceptTypeProperty && conceptTypeProperty.value;

        if (conceptType) {
            do {
                if (conceptType === conceptTypeFilter) {
                    return true;
                }

                conceptType = conceptsById[conceptType] && conceptsById[conceptType].parentConcept;
            } while (conceptType)
        }

        return false;
    }

    function partitionVertices(ontology, vertices, query, conceptFilter, propertyFilters) {
        var propertiesByTitle = ontology.properties.byTitle,
            conceptsById = ontology.concepts.byId,
            hasGeoFilter = _.any(propertyFilters, function(filter) {
                var ontologyProperty = propertiesByTitle[filter.propertyId];
                return ontologyProperty && ontologyProperty.dataType === 'geoLocation';
            });

        if (hasGeoFilter) {
            debugger;
        }

        return _.partition(vertices, function(v) {
            var queryMatch = query && query !== '*' ?
                    _.chain(v.properties)
                        .map(function(p) {
                            var ontologyProperty = propertiesByTitle[p.name];
                            if (p.value &&
                                ontologyProperty &&
                                ontologyProperty.possibleValues &&
                                ontologyProperty.possibleValues[p.value]) {

                                return _.extend({}, p, {
                                    value: ontologyProperty.possibleValues[p.value]
                                });
                            }
                            return p;
                        })
                        .pluck('value')
                        .compact()
                        .value()
                        .join(' ')
                        .toLowerCase()
                        .indexOf(query) >= 0 : true,
                filterConceptMatch = conceptFilter ?
                    isKindOfConcept(conceptsById, v, conceptFilter) : true,
                filterPropertyMatch = propertyFilters && propertyFilters.length ?
                    matchesPropertyFilters(propertiesByTitle, v, propertyFilters) : true;

            return queryMatch && filterConceptMatch && filterPropertyMatch;
        });
    }

    function matchesPropertyFilters(propertiesByTitle, vertex, filters) {
        return _.every(filters, function(filter) {
            var predicate = filter.predicate,
                property = propertiesByTitle[filter.propertyId],
                vertexProperties = _.where(vertex.properties, { name: filter.propertyId });

            if (vertexProperties.length === 0) {
                return false;
            }

            return _.any(vertexProperties, function(vertexProperty) {
                var propertyValue = vertexProperty.value,
                    values = filter.values,
                    predicateCompare = function(values, actual) {
                        switch (predicate) {
                            case '<': return actual <= values[0];
                            case '>': return actual >= values[0];
                            case 'range': return actual >= values[0] && actual <= values[1];
                            case 'equal':
                                return _.isEqual(values[0], actual);
                            case 'contains': return actual.indexOf(values[0]) >= 0;

                            default: console.warn('Unknown predicate:', predicate);
                        }

                        return false;
                    },
                    compareFunction,
                    transformFunction = _.identity;

                if (_.isUndefined(propertyValue)) {
                    return false;
                }

                switch (property.dataType) {
                    case 'date':
                        if (property.displayType !== 'dateOnly') {
                            propertyValue = F.date.utc(propertyValue).getTime();
                            transformFunction = F.date.local;
                        } else {
                            transformFunction = function(v, i) {
                                if (_.isUndefined(i)) {
                                    return new Date(v);
                                }
                                return F.date.utc(v);
                            }
                        }
                        compareFunction = predicateCompare;
                        break;

                    case 'geoLocation':
                        transformFunction = function(v) {
                            return v.latitude ? v : parseFloat(v);
                        };
                        compareFunction = function(values, actual) {
                            var km = haversineDistanceBetween(values[0], values[1], actual.latitude, actual.longitude);
                            //var from = new OpenLayers.Geometry.Point(values[1], values[0]),
                                //to = new OpenLayers.Geometry.Point(actual.longitude, actual.latitude),
                                //line = new OpenLayers.Geometry.LineString([from, to]),
                                //km = line.getGeodesicLength(new OpenLayers.Projection('EPSG:4326')) / 1000;

                            return km <= values[2];
                        };
                        break;

                    case 'double':
                    case 'integer':
                    case 'heading':
                    case 'currency':
                        compareFunction = predicateCompare;
                        break;

                    case 'boolean':
                        compareFunction = predicateCompare;
                        transformFunction = function(v) {
                            return v === 'true' || v === true;
                        };
                        predicate = 'equal';
                        break;

                    default:
                        transformFunction = function(v) {
                            return v.toLowerCase();
                        };
                        predicate = 'contains';
                        compareFunction = predicateCompare;
                }

                return compareFunction(values.map(transformFunction), transformFunction(propertyValue));
            })
        });
    }

    function toRadians(number) {
        return number * Math.PI / 180;
    }

    function haversineDistanceBetween(lat1, lon1, lat2, lon2) {
        var EARTH_RADIUS_KM = 6371,
            x1 = toRadians(lat2 - lat1),
            x2 = toRadians(lon2 - lon1),
            a = Math.sin(x1 / 2) * Math.sin(x1 / 2) +
                Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
                Math.sin(x2 / 2) * Math.sin(x2 / 2),
            c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            distanceKm = EARTH_RADIUS_KM * c;

        return distanceKm;
    }
});
