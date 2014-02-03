
define(['data'], function(data) {
    var conceptDeferred,
        defaultConcepts = {
            "id":"4",
            "title":"rootConcept",
            "children":[
                {"id":"5","title":"entity","children":[]}
            ],
            "pluralDisplayName":"rootConcepts",
            "displayName":"rootConcept"
        };

    describe('data', function() {

        it('should bind to document node', function() {
            expect(data.node).to.equal(document);
        });

        xit('should register keyboard shortcuts', function(wait) {
            data.on('registerKeyboardShortcuts', function(e, d) {
                expect(d.shortcuts['meta-a'].fire).to.equal('selectAll');
                expect(d.shortcuts['delete'].fire).to.equal('deleteSelected');
                wait();
            });
            
            data.ontologyService._ajaxRequests['ontology/concept'].resolve(defaultConcepts);
            data.cachedConceptsDeferred.resolve(defaultConcepts);
            data.trigger('applicationReady');
        });
    });
});
