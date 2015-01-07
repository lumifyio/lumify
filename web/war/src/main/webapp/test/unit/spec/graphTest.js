
describeComponent('graph/graph', function(Graph) {

    var conceptDeferred,
        defaultConcepts = {
            entityConcept: {
                id: 4, title: 'entityConcept', children: []
            }
        }

    beforeEach(function() {
        setupComponent(this);
        this.component.ontologyService.concepts = function() {
            conceptDeferred = $.Deferred();
            conceptDeferred.resolve(defaultConcepts);
            return conceptDeferred;
        }
    });

    describe('initialization', function() {

        it('should attach', function() {
            expect(this.component.attr).to.have.property('emptyGraphSelector');
            expect(this.component.ontologyService).to.exist;
        });

        it('should request concepts', function(done) {
            expect(conceptDeferred.state()).to.equal('resolved');
            done();
        });

    });

});
