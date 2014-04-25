
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
                return (/^http:\/\/lumify.io/).test(name) ?
                    name :
                    ('http://lumify.io#' + name);
            },

            displayProp: function(property, name) {
                var fullName = V.propName(name),
                    value = V.prop(property, fullName),
                    ontologyProperty = properties && properties.byTitle[fullName];

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

            // TODO: support looking for underscore properties like _source?
            prop: function(vertexOrProperty, name, defaultValue) {
                var fullName = V.propName(name),

                    ontologyProperty = properties && properties.byTitle[fullName],

                    displayName = (ontologyProperty && ontologyProperty.displayName) ||
                        fullName,

                    foundProperties = vertexOrProperty.properties ?
                        _.where(vertexOrProperty.properties, { name: fullName }) :
                        [vertexOrProperty],

                    hasValue = foundProperties &&
                        foundProperties.length &&
                        !_.isUndefined(foundProperties[0].value);

                if (!hasValue && fullName !== 'http://lumify.io#title' && _.isUndefined(defaultValue)) {
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
