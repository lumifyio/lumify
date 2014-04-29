
define([
    'util/formatters',
    'promise!../service/ontologyPromise'
], function(
    F,
    ontology) {
    'use strict';

    var propertiesByTitle = ontology.propertiesByTitle,
        V = {
            sandboxStatus: function(vertex) {
                return (/^private$/i).test(vertex.sandboxStatus) ? 'unpublished' : undefined;
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

            displayProp: function(property) {
                var value = V.prop(property, property.name),
                    ontologyProperty = propertiesByTitle[property.name];

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

            propForKey: function(vertex, key) {
                return _.findWhere(vertex.properties, { key: key });
            },

            // TODO: support looking for underscore properties like _source?
            prop: function(vertexOrProperty, name, defaultValue) {
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
                    (defaultValue || ('No ' + displayName.toLowerCase() + ' available'));
            },

            isEdge: function(vertex) {
                return V.prop(vertex, 'conceptType') === 'relationship';
            }
        }

    return $.extend({}, F, { vertex: V });
});
