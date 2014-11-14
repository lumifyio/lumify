
define([
    './urlFormatters',
    './formula',
    'util/messages',
    'util/requirejs/promise!../service/ontologyPromise'
], function(
    F,
    formula,
    i18n,
    ontology) {
    'use strict';

    var propertiesByTitle = ontology.properties.byTitle,
        V = {

            isPublished: function(vertex) {
                return V.sandboxStatus(vertex) === undefined;
            },

            sandboxStatus: function(vertex) {
                return (/^(private|public_changed)$/i).test(vertex.sandboxStatus) ?
                        i18n('vertex.status.unpublished') :
                        undefined;
            },

            metadata: {
                // Define/override metadata dataType specific displayTransformers here
                //
                // All functions receive: function(el, value, property, vertexId)
                // set the value synchronously
                // - or -
                // append "Async" to function name and return a $.Deferred().promise()

                datetime: function(el, value) {
                    el.textContent = F.date.dateTimeString(value);
                },

                sandboxStatus: function(el, value) {
                    el.textContent = V.sandboxStatus({ sandboxStatus: value }) || '';
                },

                percent: function(el, value) {
                    el.textContent = F.number.percent(value);
                },

                userAsync: function(el, userId) {
                    return Promise.require('util/withDataRequest')
                        .then(function(withDataRequest) {
                            return withDataRequest.dataRequest('user', 'info', userId)
                        })
                        .then(function(user) {
                            el.textContent = user && user.displayName || i18n('user.unknown.displayName');
                        })
                }
            },

            properties: {
                // Define/override dataType specific displayTransformers here
                //
                // All functions receive: function(HtmlElement, property, vertexId)
                // Must populate the dom element with value
                //
                // for example: geoLocation: function(...) { el.textContent = 'coords'; }

                visibility: function(el, property) {
                    $('<i>').text((
                        property.value &&
                        property.value.source
                    ) || i18n('visibility.blank')).appendTo(el);
                },

                geoLocation: function(el, property) {
                    if ($('#app.fullscreen-details').length) {
                        $(el).append(
                            F.geoLocation.pretty(property.value)
                        );
                        return;
                    }

                    var anchor = $('<a>')
                        .addClass('map-coordinates')
                        .data({
                            latitude: property.value.latitude,
                            longitude: property.value.longitude
                        }),
                        displayValue = F.geoLocation.pretty(property.value, true);

                    if (property.value.description) {
                        anchor.append(property.value.description + ' ');
                    }

                    $('<small>')
                        .css('white-space', 'nowrap')
                        .text(F.geoLocation.pretty(property.value, true))
                        .appendTo(anchor);

                    anchor.appendTo(el);
                },

                byte: function(el, property) {
                    el.textContent = F.bytes.pretty(property.value);
                },

                link: function(el, property) {
                    var anchor = document.createElement('a'),
                        href = $.trim(property.value);

                    if (!(/^http/).test(href)) {
                        href = 'http://' + href;
                    }

                    anchor.setAttribute('href', href);
                    anchor.setAttribute('target', '_blank');
                    anchor.textContent = property.value;

                    el.appendChild(anchor);
                },

                textarea: function(el, property) {
                    $(el).html((property.value||'').replace(/\r?\n/g, '<br />'));
                },

                heading: function(el, property) {
                    var div = document.createElement('div'),
                        dim = 12,
                        half = dim / 2;

                    el.textContent = F.number.heading(property.value);
                    div.style.width = div.style.height = dim + 'px';
                    div.style.display = 'inline-block';
                    div.style.marginRight = '0.25em';
                    div = el.insertBefore(div, el.childNodes[0]);

                    require(['d3'], function(d3) {
                        d3.select(div)
                            .append('svg')
                                .style('vertical-align', 'middle')
                                .attr('width', dim)
                                .attr('height', dim)
                                .append('g')
                                    .attr('transform', 'rotate(' + property.value + ' ' + half + ' ' + half + ')')
                                    .call(function() {
                                        this.append('line')
                                            .attr('x1', half)
                                            .attr('y1', 0)
                                            .attr('x2', half)
                                            .attr('y2', dim)
                                            .call(styling)

                                        this.append('g')
                                            .attr('transform', 'rotate(30 ' + half + ' 0)')
                                            .call(createArrowLine)

                                        this.append('g')
                                            .attr('transform', 'rotate(-30 ' + half + ' 0)')
                                            .call(createArrowLine)
                                    });
                    });

                    function createArrowLine() {
                        this.append('line')
                            .attr('x1', half)
                            .attr('y1', 0)
                            .attr('x2', half)
                            .attr('y2', dim / 3)
                            .call(styling);
                    }
                    function styling() {
                        this.attr('stroke', '#555')
                            .attr('line-cap', 'round')
                            .attr('stroke-width', '1');
                    }
                }

            },

            hasMetadata: function(property) {
                var status = V.sandboxStatus(property),
                    modifiedBy = property['http://lumify.io#modifiedBy'],
                    modifiedDate = property['http://lumify.io#modifiedDate'],
                    sourceTimezone = property['http://lumify.io#sourceTimezone'],
                    confidence = property['http://lumify.io#confidence'],
                    justification = property._justificationMetadata,
                    source = property._sourceMetadata;

                return (
                    status ||
                    justification ||
                    source ||
                    modifiedBy ||
                    modifiedDate ||
                    sourceTimezone ||
                    confidence
                );
            },

            concept: function(vertex) {
                var conceptType = vertex && V.prop(vertex, 'conceptType');

                if (!conceptType || conceptType === 'Unknown') {
                    conceptType = 'http://www.w3.org/2002/07/owl#Thing';
                }

                return ontology.concepts.byId[conceptType];
            },

            isKindOfConcept: function(vertex, conceptTypeFilter) {
                var conceptType = V.prop(vertex, 'conceptType');

                do {
                    if (conceptType === conceptTypeFilter) {
                        return true;
                    }

                    conceptType = ontology.concepts.byId[conceptType].parentConcept;
                } while (conceptType)

                return false;
            },

            partitionVertices: function(vertices, query, conceptFilter, propertyFilters) {
                // TODO: move to webworker
                var deferred = $.Deferred(),
                    hasGeoFilter = _.any(propertyFilters, function(filter) {
                        var ontologyProperty = propertiesByTitle[filter.propertyId];
                        return ontologyProperty && ontologyProperty.dataType === 'geoLocation';
                    });

                if (hasGeoFilter) {
                    require(['openlayers'], function(OpenLayers) {
                        deferred.resolve(OpenLayers);
                    });
                } else {
                    deferred.resolve();
                }

                return deferred.then(function(OpenLayers) {
                    return _.partition(vertices, function(v) {
                        var queryMatch = query && query !== '*' ?
                                _.chain(v.properties)
                                    .map(function(p) {
                                        var ontologyProperty = propertiesByTitle[p.name];
                                        if (p.value &&
                                            ontologyProperty &&
                                            ontologyProperty.possibleValues &&
                                            ontologyProperty.possibleValues[p.value]) {

                                            return $.extend({}, p, {
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
                                V.isKindOfConcept(v, conceptFilter) : true,
                            filterPropertyMatch = propertyFilters && propertyFilters.length ?
                                V.matchesPropertyFilters(v, propertyFilters, OpenLayers) : true;

                        return queryMatch && filterConceptMatch && filterPropertyMatch;
                    });
                }).promise();
            },

            matchesPropertyFilters: function(vertex, filters, OpenLayers) {
                // TODO: move to webworder
                return _.every(filters, function(filter) {
                    var predicate = filter.predicate,
                        property = propertiesByTitle[filter.propertyId],
                        vertexProperty = V.prop(vertex, filter.propertyId),
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

                    if (_.isUndefined(vertexProperty)) {
                        return false;
                    }

                    switch (property.dataType) {
                        case 'date':
                            if (property.displayType !== 'dateOnly') {
                                vertexProperty = F.date.utc(vertexProperty).getTime();
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
                                var from = new OpenLayers.Geometry.Point(values[1], values[0]),
                                    to = new OpenLayers.Geometry.Point(actual.longitude, actual.latitude),
                                    line = new OpenLayers.Geometry.LineString([from, to]),
                                    km = line.getGeodesicLength(new OpenLayers.Projection('EPSG:4326')) / 1000;

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

                    return compareFunction(values.map(transformFunction), transformFunction(vertexProperty));
                });
            },

            image: function(vertex, optionalWorkspaceId, width) {
                var entityImageUrl = V.prop(vertex, 'entityImageUrl');
                if (entityImageUrl) {
                    return entityImageUrl;
                }

                var entityImageVertexId = V.prop(vertex, 'entityImageVertexId'),
                    concept = V.concept(vertex),
                    isImage = /image/i.test(concept.displayType),
                    isVideo = /video/i.test(concept.displayType);

                if (entityImageVertexId || isImage) {
                    return 'vertex/thumbnail?' + $.param({
                        workspaceId: optionalWorkspaceId || lumifyData.currentWorkspaceId,
                        graphVertexId: entityImageVertexId || vertex.id,
                        width: width || 150
                    });
                }

                if (isVideo) {
                    var posterFrame =  V.prop(vertex, 'http://lumify.io#rawPosterFrame');
                    if (posterFrame) {
                        return 'vertex/poster-frame?' + $.param({
                            workspaceId: optionalWorkspaceId || lumifyData.currentWorkspaceId,
                            graphVertexId: vertex.id
                        });
                    }
                }

                return concept.glyphIconHref;
            },

            imageIsFromConcept: function(vertex, optionalWorkspaceId) {
                return V.image(vertex, optionalWorkspaceId) === V.concept(vertex).glyphIconHref;
            },

            imageDetail: function(vertex, optionalWorkspaceId) {
                return V.image(vertex, optionalWorkspaceId, 800);
            },

            raw: function(vertex, optionalWorkspaceId) {
                return 'vertex/raw?' + $.param({
                    workspaceId: optionalWorkspaceId || lumifyData.currentWorkspaceId,
                    graphVertexId: vertex.id
                });
            },

            imageFrames: function(vertex, optionalWorkspaceId) {
                if (V.prop(vertex, 'http://lumify.io#videoPreviewImage')) {
                    return 'vertex/video-preview?' + $.param({
                        workspaceId: optionalWorkspaceId || lumifyData.currentWorkspaceId,
                        graphVertexId: vertex.id
                    });
                }
            },

            /*
            function setPreviewsForVertex(vertex, currentWorkspace) {
                var params = {
                        workspaceId: currentWorkspace,
                        graphVertexId: vertex.id
                    },
                    artifactUrl = function(type, p) {
                        return _.template('vertex/{ type }?' + $.param($.extend(params, p || {})), { type: type });
                    },
                    entityImageUrl = F.vertex.prop(vertex, 'entityImageUrl'),
                    entityImageVertexId = F.vertex.prop(vertex, 'entityImageVertexId');

                vertex.imageSrcIsFromConcept = false;

                if (entityImageUrl) {
                    vertex.imageSrc = entityImageUrl;
                    vertex.imageDetailSrc = entityImageUrl;
                } else if (entityImageVertexId) {
                    vertex.imageSrc = artifactUrl('thumbnail', { graphVertexId: entityImageVertexId, width: 150 });
                    vertex.imageDetailSrc =
                        artifactUrl('thumbnail', { graphVertexId: entityImageVertexId, width: 800 });
                } else {

                    // TODO: scale glyphs
                    vertex.imageSrc = vertex.concept.glyphIconHref;
                    vertex.imageDetailSrc = vertex.concept.glyphIconHref;
                    vertex.imageRawSrc = artifactUrl('raw');
                    vertex.imageSrcIsFromConcept = true;

                    switch (vertex.concept.displayType) {

                        case 'image':
                            vertex.imageSrc = artifactUrl('thumbnail', { width: 150 });
                            vertex.imageSrcIsFromConcept = false;
                            vertex.imageDetailSrc = artifactUrl('thumbnail', { width: 800 });
                            break;

                        case 'video':
                            vertex.properties.forEach(function(p) {
                                if (p.name === 'http://lumify.io#rawPosterFrame') {
                                    vertex.imageSrc = artifactUrl('poster-frame');
                                    vertex.imageDetailSrc = artifactUrl('poster-frame');
                                    vertex.imageSrcIsFromConcept = false;
                                } else if (p.name === 'http://lumify.io#videoPreviewImage') {
                                    vertex.imageFramesSrc = artifactUrl('video-preview');
                                }
                            });
                            break;
                    }
                }
            */

            propName: function(name) {
                var autoExpandedName = (/^http:\/\/lumify.io/).test(name) ?
                        name : ('http://lumify.io#' + name),
                    ontologyProperty = propertiesByTitle[name] || propertiesByTitle[autoExpandedName],

                    resolvedName = ontologyProperty && (
                        ontologyProperty.title === name ? name : autoExpandedName
                    ) || name;

                return resolvedName;
            },

            longestProp: function(vertex) {
                var properties = vertex.properties
                    .filter(function(a) {
                        var ontologyProperty = propertiesByTitle[a.name];
                        return ontologyProperty && ontologyProperty.userVisible;
                    })
                    .sort(function(a, b) {
                        return V.displayProp(b).length - V.displayProp(a).length;
                    });
                if (properties.length > 0) {
                    return V.displayProp(properties[0]);
                } else {
                    return null;
                }
            },

            displayProp: function(vertexOrProperty, optionalName) {
                var name = _.isUndefined(optionalName) ? vertexOrProperty.name : optionalName,
                    value = V.prop(vertexOrProperty, name),
                    ontologyProperty = propertiesByTitle[name];

                if (!ontologyProperty) {
                    return value;
                }

                if (ontologyProperty.possibleValues) {
                    var foundPossibleValue = ontologyProperty.possibleValues[value];
                    if (foundPossibleValue) {
                        return foundPossibleValue;
                    } else {
                        console.warn('Unknown ontology value for key', value, ontologyProperty);
                    }
                }

                if (ontologyProperty.displayType) {
                    switch (ontologyProperty.displayType) {
                        case 'byte': return F.bytes.pretty(value)
                    }
                }

                switch (ontologyProperty.dataType) {
                    case 'boolean': return F.boolean.pretty(value);

                    case 'date': {
                        if (ontologyProperty.displayType !== 'dateOnly') {
                            return F.date.dateTimeString(value);
                        }
                        return F.date.dateStringUtc(value);
                    }

                    case 'heading': return F.number.heading(value);

                    case 'double':
                    case 'integer':
                    case 'currency':
                    case 'number': return F.number.pretty(value);
                    case 'geoLocation': return F.geoLocation.pretty(value);

                    default: return value;
                }
            },

            props: function(vertex, name) {
                var autoExpandedName = V.propName(name),
                    foundProperties = _.where(vertex.properties, { name: autoExpandedName });

                return foundProperties;
            },

            propForNameAndKey: function(vertex, name, key) {
                return _.findWhere(vertex.properties, { name: name, key: key });
            },

            title: function(vertex) {
                var title = formulaResultForVertex(vertex, 'titleFormula')

                if (!title) {
                    title = V.prop(vertex, 'title', undefined, true);
                }

                return title;
            },

            subtitle: _.partial(formulaResultForVertex, _, 'subtitleFormula', ''),

            time: _.partial(formulaResultForVertex, _, 'timeFormula', ''),

            heading: function(vertex) {
                var headingProp = _.find(vertex.properties, function(p) {
                  return p.name.indexOf('heading') > 0;
                });
                if (headingProp) {
                    return headingProp.value;
                }
                return 0;
            },

            // TODO: support looking for underscore properties like _source?
            prop: function(vertexOrProperty, name, defaultValue, ignoreErrorIfTitle) {
                if (ignoreErrorIfTitle !== true && name === 'title') {
                    throw new Error('Use title function, not generic prop');
                }

                var autoExpandedName = V.propName(name),

                    ontologyProperty = propertiesByTitle[autoExpandedName],

                    displayName = (ontologyProperty && ontologyProperty.displayName) ||
                        autoExpandedName,

                    foundProperties = vertexOrProperty.properties ?
                        _.where(vertexOrProperty.properties, { name: autoExpandedName }) :
                        [vertexOrProperty],

                    hasValue = foundProperties &&
                        foundProperties.length &&
                        !_.isUndefined(foundProperties[0].value);

                if (!hasValue &&
                    autoExpandedName !== 'http://lumify.io#title' &&
                    _.isUndefined(defaultValue)) {
                    return undefined;
                }

                return hasValue ? foundProperties[0].value :
                    (defaultValue ||
                    i18n('vertex.property.not_available', displayName.toLowerCase()))
            },

            isEdge: function(vertex) {
                var propsIsObjectNotArray = _.isObject(vertex && vertex.properties) &&
                    vertex.properties['http://lumify.io#conceptType'] === 'relationship';
                return propsIsObjectNotArray ||
                    V.prop(vertex, 'conceptType') === 'relationship' ||
                    (_.has(vertex, 'sourceVertexId') && _.has(vertex, 'destVertexId'));
            }
        }

    return $.extend({}, F, { vertex: V });

    function treeLookupForConceptProperty(conceptId, propertyName) {
        var ontologyConcept = conceptId && ontology.concepts.byId[conceptId],
            formulaString = ontologyConcept && ontologyConcept[propertyName];

        if (formulaString) {
            return formulaString;
        }

        if (ontologyConcept && ontologyConcept.parentConcept) {
            return treeLookupForConceptProperty(ontologyConcept.parentConcept, propertyName);
        }
    }

    function formulaResultForVertex(vertex, formulaKey, defaultValue) {
        var conceptId = V.prop(vertex, 'conceptType'),
            formulaString = treeLookupForConceptProperty(conceptId, formulaKey),
            result = defaultValue;

        if (formulaString) {
            result = formula(formulaString, vertex, V);
        }

        return result;
    }
});
