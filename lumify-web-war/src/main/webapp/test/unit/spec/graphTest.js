
describeComponent('graph/graph', function(Graph) {

    var conceptDeferred,
        defaultConcepts = {"id":"4","title":"rootConcept"};

    beforeEach(function() {
        setupComponent();
        this.component.ontologyService.clearCaches();
        this.component.ontologyService.concepts = function() { 
            conceptDeferred = $.Deferred();
            conceptDeferred.resolve(defaultConcepts);
            return conceptDeferred;
        }
    });


    describe('initialization', function() {

        it("should attach", function() {
            expect(this.component.attr).to.have.property('emptyGraphSelector');
            expect(this.component.ontologyService).to.exist;
        });

        it("should request concepts", function(done) {
            expect(conceptDeferred.state()).to.equal("resolved");
            done();
        });

    });

});
