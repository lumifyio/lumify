
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
                return V.sandboxStatus.apply(null, arguments) === undefined;
            },

            sandboxStatus: function(vertexOrProperty) {
                if (arguments.length === 3) {
                    var props = V.props.apply(null, arguments);
                    if (props.length) {
                        return _.any(props, function(p) {
                            return V.sandboxStatus(p) === undefined;
                        }) ? undefined : i18n('vertex.status.unpublished');
                    }
                    return;
                }

                return (/^(private|public_changed)$/i).test(vertexOrProperty.sandboxStatus) ?
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
                            return withDataRequest.dataRequest('user', 'getUserNames', [userId])
                        })
                        .then(function(users) {
                            el.textContent = users && users[0] || i18n('user.unknown.displayName');
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
                    $(el).html(_.escape(property.value || '').replace(/\r?\n+/g, '<br><br>'));
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
                    justification = property['http://lumify.io#justification'],
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
                    var posterFrame =  _.any(vertex.properties, function(p) {
                        return p.name === 'http://lumify.io#rawPosterFrame';
                    });
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
                var videoPreview =  _.any(vertex.properties, function(p) {
                    return p.name === 'http://lumify.io#videoPreviewImage';
                });
                if (videoPreview) {
                    return 'vertex/video-preview?' + $.param({
                        workspaceId: optionalWorkspaceId || lumifyData.currentWorkspaceId,
                        graphVertexId: vertex.id
                    });
                }
            },

            propName: function(name) {
                var autoExpandedName = (/^http:\/\/lumify.io/).test(name) ?
                        name : ('http://lumify.io#' + name),
                    ontologyProperty = propertiesByTitle[name] || propertiesByTitle[autoExpandedName],

                    resolvedName = ontologyProperty && (
                        ontologyProperty.title === name ? name : autoExpandedName
                    ) || name;

                return resolvedName;
            },

            longestProp: function(vertex, optionalName) {
                var properties = vertex.properties
                    .filter(function(a) {
                        var ontologyProperty = propertiesByTitle[a.name];
                        if (optionalName && optionalName !== a.name) {
                            return false;
                        }
                        return ontologyProperty && ontologyProperty.userVisible;
                    })
                    .sort(function(a, b) {
                        return V.prop(vertex, b.name, b.key).length - V.prop(vertex, a.name, a.key).length;
                    });
                if (properties.length > 0) {
                    return V.prop(vertex, properties[0].name, properties[0].key);
                }
            },

            propFromAudit: function(vertex, propertyAudit) {
                //propertyName, newValue, previousValue
                return V.propDisplay(propertyAudit.propertyName, propertyAudit.newValue || propertyAudit.previousValue);
            },

            propDisplay: function(name, value) {
                name = V.propName(name);
                var ontologyProperty = propertiesByTitle[name];

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
                        case 'byte': return F.bytes.pretty(value);
                        case 'heading': return F.number.heading(value);
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

                    case 'double':
                    case 'integer':
                    case 'currency':
                    case 'number': return F.number.pretty(value);
                    case 'geoLocation': return F.geoLocation.pretty(value);

                    default: return value;
                }
            },

            prop: function(vertex, name, optionalKey, optionalOpts) {
                checkVertexAndPropertyNameArguments(vertex, name);

                if (_.isObject(optionalKey)) {
                    optionalOpts = optionalKey;
                    optionalKey = null;
                }

                name = V.propName(name);

                var value = V.propRaw(vertex, name, optionalKey, optionalOpts),
                    ontologyProperty = propertiesByTitle[name];

                if (!ontologyProperty) {
                    return value;
                }

                if (_.isArray(value)) {
                    if (!optionalKey) {
                        var firstMatchingProperty = _.find(vertex.properties, function(p) {
                            return ~ontologyProperty.dependentPropertyIris.indexOf(p.name);
                        });
                        optionalKey = (firstMatchingProperty && firstMatchingProperty.key);
                    }
                    if (ontologyProperty.displayFormula) {
                        return formula(ontologyProperty.displayFormula, vertex, V, optionalKey);
                    } else {
                        return value.join(' ');
                    }
                }

                return V.propDisplay(name, value);
            },

            props: function(vertex, name, optionalKey) {
                checkVertexAndPropertyNameArguments(vertex, name);

                var hasKey = !_.isUndefined(optionalKey);

                if (arguments.length === 3 && !hasKey) {
                    throw new Error('Undefined key is not allowed. Remove parameter to return all named properties');
                }

                name = V.propName(name);

                var ontologyProperty = ontology.properties.byTitle[name],
                    dependentIris = ontologyProperty && ontologyProperty.dependentPropertyIris,
                    foundProperties = _.filter(vertex.properties, function(p) {
                        if (dependentIris) {
                            return ~dependentIris.indexOf(p.name) && (
                                hasKey ? optionalKey === p.key : true
                            );
                        }

                        return name === p.name && (
                            hasKey ? optionalKey === p.key : true
                        );
                    });

                return foundProperties;
            },

            propValid: function(vertex, values, propertyName, propertyKey) {
                checkVertexAndPropertyNameArguments(vertex, propertyName);
                if (!_.isArray(values)) {
                    throw new Error('Unable to validate without values array')
                }

                var ontologyProperty = ontology.properties.byTitle[propertyName],
                    dependentIris = ontologyProperty.dependentPropertyIris,
                    formulaString = ontologyProperty.validationFormula,
                    result = true;

                if (formulaString) {
                    if (values.length) {
                        var properties = [];
                        if (dependentIris) {
                            dependentIris.forEach(function(iri, i) {
                                var property = _.findWhere(vertex.properties, {
                                        name: iri,
                                        key: propertyKey
                                    }),
                                    value = _.isArray(values[i]) && values[i].length === 1 ? values[i][0] : values[i]

                                if (property) {
                                    property = _.extend({}, property, { value: value });
                                    if (_.isUndefined(values[i])) {
                                        property.value = undefined;
                                    }
                                } else {
                                    property = {
                                        name: iri,
                                        key: propertyKey,
                                        value: value
                                    };
                                }
                                properties.push(property);
                            })
                        }
                        vertex = _.extend({}, vertex, { properties: properties });
                    }
                    result = formula(formulaString, vertex, V, propertyKey);
                }

                return Boolean(result);
            },

            title: function(vertex) {
                var title = formulaResultForVertex(vertex, 'titleFormula')

                if (!title) {
                    title = V.prop(vertex, 'title', undefined, {
                        ignoreErrorIfTitle: true
                    });
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

            propRaw: function(vertex, name, optionalKey, optionalOpts) {
                checkVertexAndPropertyNameArguments(vertex, name);

                if (_.isObject(optionalKey)) {
                    optionalOpts = optionalKey;
                    optionalKey = null;
                }

                var hasKey = !_.isUndefined(optionalKey),
                    options = _.extend({
                        defaultValue: undefined,
                        ignoreErrorIfTitle: false
                    }, optionalOpts || {});

                if (options.ignoreErrorIfTitle !== true && name === 'title') {
                    throw new Error('Use title function, not generic prop');
                }

                name = V.propName(name);

                var ontologyProperty = propertiesByTitle[name],
                    dependentIris = ontologyProperty && ontologyProperty.dependentPropertyIris || [],
                    iris = dependentIris.length ? dependentIris : [name],
                    properties = transformMatchingVertexProperties(vertex, iris);

                if (dependentIris.length) {
                    if (options.throwErrorIfCompoundProperty) {
                        throw new Error('Compound properties that depend on compound properties are not allowed');
                    }

                    if (!hasKey && properties.length) {
                        optionalKey = properties[0].key;
                    }

                    options.throwErrorIfCompoundProperty = true;

                    return _.map(dependentIris, _.partial(V.propRaw, vertex, _, optionalKey, options));
                } else {
                    var foundProperties = hasKey ?
                            _.where(properties, { key: optionalKey }) :
                            properties,

                        hasValue = foundProperties &&
                            foundProperties.length &&
                            !_.isUndefined(foundProperties[0].value);

                    if (!hasValue &&
                        name !== 'http://lumify.io#title' &&
                        _.isUndefined(options.defaultValue)) {
                        return undefined;
                    }

                    return hasValue ? foundProperties[0].value :
                        (
                            options.defaultValue ||
                            i18n('vertex.property.not_available',
                                (ontologyProperty && ontologyProperty.displayName || '').toLowerCase() || name)
                        )
                }
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

    function transformMatchingVertexProperties(vertex, propertyNames) {
        var CONFIDENCE = 'http://lumify.io#confidence',
            CREATED = 'http://lumify.io#createDate',
            sorted = _.chain(vertex.properties)
                .filter(function(p) {
                    return ~propertyNames.indexOf(p.name);
                })
                .sortBy(function(p) {
                    var created = p.metadata && p.metadata[CREATED]
                    if (created) {
                        return created * -1;
                    }
                    return 0;
                })
                .sortBy(function(p) {
                    var confidence = p.metadata && p.metadata[CONFIDENCE]
                    if (confidence) {
                        return confidence * -1;
                    }
                    return 0;
                })
                .value()

        return sorted;
    }

    function checkVertexAndPropertyNameArguments(vertex, propertyName) {
        if (!vertex || !vertex.id || !_.isArray(vertex.properties)) {
            throw new Error('Vertex is invalid', vertex);
        }
        if (!propertyName || !_.isString(propertyName)) {
            throw new Error('Property name is invalid', propertyName);
        }
    }
});
