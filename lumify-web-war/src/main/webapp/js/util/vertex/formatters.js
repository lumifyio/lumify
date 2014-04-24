
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

    return (F = {

        // TODO: support looking for underscore properties like _source?
        prop: function(vertex, name, defaultValue) {
            var fullName = (/^http:\/\/lumify.io/).test(name) ?
                    name : ('http://lumify.io#' + name),
                p = vertex && vertex.properties,
                property = properties && properties.byTitle[fullName],
                displayName = (property && property.displayName) || fullName,
                foundProperties = p && _.where(p, { name: fullName }),
                hasValue = foundProperties && foundProperties.length && !_.isUndefined(foundProperties[0].value);

            if (!hasValue && fullName !== 'http://lumify.io#title' && _.isUndefined(defaultValue)) {
                return undefined;
            }

            return hasValue ? foundProperties[0].value :
                (defaultValue || ('No ' + displayName.toLowerCase() + ' available'));
        },

        isEdge: function(vertex) {
            return F.prop(vertex, 'conceptType') === 'relationship';
        }

    });
});
