
define([
    'service/ontology'
], function(OntologyService) {
    'use strict';

    // TODO: proper handling of dataType
    // var ontologyService = new OntologyService();

    return {

        prop: function(vertex, name, defaultValue) {
            var p = vertex && vertex.properties;
            return (p && p[name] && !_.isUndefined(p[name].value)) ? 
                p[name].value :
                (defaultValue || ('No ' + name + ' available'));
        }
    
    };
});
