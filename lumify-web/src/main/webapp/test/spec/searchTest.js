
describeComponent('search/search', function(Search) {

    beforeEach(function() {
        setupComponent();
        this.component.ontologyService.clearCaches();

        this.overrideTrigger = function(expectedQuery, callback) {
            this.component.trigger = function(event, options) {
                expect(event).to.equal('search');
                expect(options.query).to.equal(expectedQuery);
                callback();
            };
        }.bind(this);
    });


    describe('#onFormSearch', function() {

        it("should trigger 'search'", function(done) {
            var query = 'query';

            this.overrideTrigger(query, done);

            var evt = new sinon.Event();
            this.component.select('querySelector').val(query);
            this.component.onFormSearch(evt);

            expect(evt.defaultPrevented).to.equal(true);
        });

        it("search query must not be blank", function() {
            var query = ' ';

            var evt = new sinon.Event();
            this.component.select('querySelector').val(query);
            this.component.onFormSearch(evt);

            expect(this.component.select('resultsSummarySelector').html()).to.equal('');
            expect(evt.defaultPrevented).to.equal(true);
        });

    });

});
