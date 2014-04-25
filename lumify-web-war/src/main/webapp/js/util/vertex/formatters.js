
define([
    'service/ontology'
], function(OntologyService) {
    'use strict';

    var ontologyService = new OntologyService(),
        F,
        properties;

    ontologyService.properties().done(function(p) {
        properties = p;
    });

    return vertexFormatters;

    function vertexFormatters(F) {
        var V = {

            propName: function(name) {
                var autoExpandedName = (/^http:\/\/lumify.io/).test(name) ?
                        name : ('http://lumify.io#' + name),
                    ontologyProperty = properties && (

                        properties.byTitle[name] ||
                        properties.byTitle[autoExpandedName]
                    ),

                    resolvedName = ontologyProperty && (
                        ontologyProperty.title === name ? name : autoExpandedName
                    ) || name;

                return resolvedName;
            },

            displayProp: function(property, name) {
                var autoExpandedName = V.propName(name),
                    value = V.prop(property, autoExpandedName),
                    ontologyProperty = properties && properties.byTitle[autoExpandedName];

                if (!ontologyProperty) {
                    return value;
                }

                switch (ontologyProperty.dataType) {
                    case 'date': return F.date.dateString(value);
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

            // TODO: support looking for underscore properties like _source?
            prop: function(vertexOrProperty, name, defaultValue) {
                var autoExpandedName = V.propName(name),

                    ontologyProperty = properties && properties.byTitle[autoExpandedName],

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
                    (defaultValue || ('No ' + displayName.toLowerCase() + ' available'));
            },

            isEdge: function(vertex) {
                return V.prop(vertex, 'conceptType') === 'relationship';
            }

        };

        return V;
    }
});
