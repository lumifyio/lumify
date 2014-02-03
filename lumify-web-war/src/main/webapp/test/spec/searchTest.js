
describeComponent('search/search', function(Search) {

    beforeEach(function() {
        setupComponent();
        this.component.ontologyService.clearCaches();
    });


    describe('search', function() {

        it("should initialize", function() {
            var c = this.component;

            expect(c.select('querySelector').length).to.equal(1);
            expect(c.select('formSelector').length).to.equal(1);
        })
       
    });

});
