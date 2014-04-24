
define([
    'service/ontology'
], function(OntologyService) {
    'use strict';

    var ontologyService = new OntologyService(),
        properties;

    ontologyService.properties().done(function(p) {
        properties = p;
    });

    return {

        prop: function(vertex, name, defaultValue) {
            var fullName = (/^http:\/\/lumify.io/).test(name) ?
                    name : ('http://lumify.io#' + name),
                p = vertex && vertex.properties,
                property = properties && properties.byTitle[fullName],
                displayName = (property && property.displayName) || fullName;

            return (p && p[fullName] && !_.isUndefined(p[fullName].value)) ?
                p[fullName].value :
                (defaultValue || ('No ' + displayName.toLowerCase() + ' available'));
        }

    };
});
