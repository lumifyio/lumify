define(['util/promise'], function() {
    return new Promise(function(resolve) {
        require([
            'text!../test/unit/mocks/ontology.json',
            'data/web-worker/services/ontology',
            'data/web-worker/util/ajax'
        ], function(json, ontology, ajax) {
            // Hack ontology for testing
            var ontologyJson = JSON.parse(json),
                person = _.findWhere(ontologyJson.concepts, { id: 'http://lumify.io/dev#person' });

            // Delete color for person
            if (person) {
                delete person.color;
            }

            // Add compound field that dependends on another compound
            ontologyJson.properties.push({
                title: 'http://lumify.io/testing#compound1',
                displayName: 'Testing Compound',
                userVisible: true,
                searchable: true,
                dataType: 'string',
                validationFormula:
                    'dependentProp("http://lumify.io/dev#title") && ' +
                    'dependentProp("http://lumify.io/dev#name")',
                displayFormula:
                    'dependentProp("http://lumify.io/dev#name") + ", "' +
                    'dependentProp("http://lumify.io/dev#title")',
                dependentPropertyIris: [
                    'http://lumify.io/dev#title',
                    'http://lumify.io/dev#name'
                ]
            })

            // Add heading
            ontologyJson.properties.push({
                title: 'http://lumify.io/testing#heading1',
                displayName: 'Testing Heading',
                userVisible: true,
                searchable: true,
                dataType: 'double',
                displayType: 'heading'
            })

            ontologyJson.properties.push({
                title: 'http://lumify.io/testing#integer1',
                displayName: 'Testing integer',
                userVisible: true,
                searchable: true,
                dataType: 'integer'
            })

            ontologyJson.properties.push({
                title: 'http://lumify.io/testing#number1',
                displayName: 'Testing number',
                userVisible: true,
                searchable: true,
                dataType: 'number'
            })

            // Add video sub concept to test displayType
            ontologyJson.concepts.push({
                id:'http://lumify.io/dev#videoSub',
                title:'http://lumify.io/dev#videoSub',
                displayName:'VideoSub',
                parentConcept:'http://lumify.io/dev#video',
                pluralDisplayName:'VideoSubs',
                searchable:true,
                properties:[]
            });

            ajax.setNextResponse(ontologyJson);
            resolve(ontology.ontology());
        });
    });
});
