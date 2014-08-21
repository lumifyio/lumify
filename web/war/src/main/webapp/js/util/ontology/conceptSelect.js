define([
    'flight/lib/component',
    'hbs!./concept-options',
    'service/ontology'
], function(
    defineComponent,
    conceptsTemplate,
    OntologyService) {
    'use strict';

    var ontologyService = new OntologyService();

    return defineComponent(ConceptSelector);

    function ConceptSelector() {

        this.defaultAttrs({
            conceptSelector: 'select'
        });

        this.after('initialize', function() {
            var select = this.select('conceptSelector');

            ontologyService.concepts()
                .done(function(concepts) {
                    select.html(
                        conceptsTemplate({
                            concepts: _.chain(concepts.byTitle)
                                .filter(function(c) {
                                    return c.userVisible !== false;
                                })
                                .map(function(c) {
                                    return {
                                        id: c.id,
                                        displayName: c.displayName,
                                        indent: c.flattenedDisplayName
                                                 .replace(/[^\/]/g, '')
                                                 .replace(/\//g, '&nbsp;&nbsp;&nbsp;&nbsp;'),
                                        selected: false
                                    }
                                })
                                .value()
                        })
                    );
                })

        });

    }
});
