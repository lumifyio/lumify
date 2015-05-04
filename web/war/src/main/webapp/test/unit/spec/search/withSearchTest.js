
describeMixin('search/types/withSearch', function() {

    beforeEach(function() {
        setupComponent(this);
    })

    describe('on initialize', function(){

        it('should initialize', function() {
            var c = this.component;

            expect(c.select('resultsSelector').length).to.equal(1);
            expect(c.select('filtersSelector').length).to.equal(1);
        })

    })

    describe('on clearSearch events', function(){

        it('should clear filters', function() {
            var $c = this.component,
                $filters = $c.select('filtersSelector').find('.entity-filters');
            $filters.html('<li>not empty</li>');
            $filters.html().should.not.be.empty;
            $c.on('clearSearch', function() {
                _.defer(function() {
                    $filters.html().should.be.empty;
                })
            })

            _.defer(function() {
                $c.trigger('clearSearch');
            });
        })

        it('should clear results', function() {
            var $c = this.component,
                $results = $c.select('resultsContainerSelector');
            $results.html('<li>not empty</li>');
            $results.html().should.not.be.empty;
            $c.on('clearSearch', function() {
                _.defer(function() {
                    $results.html().should.be.empty;
                })
            })

            _.defer(function() {
                $c.trigger('clearSearch');
            });
        })

    })

    describe('on menubarToggleDisplay events', function(){

        it('should hide results', function() {
            var $c = this.component,
                $results = $c.select('resultsSelector');
            $results.show();
            $results.is(":visible").should.be.true;
            $c.on('menubarToggleDisplay', function() {
                _.defer(function() {
                    $results.is(":visible").should.be.false;
                })
            })

            _.defer(function() {
                $c.trigger('menubarToggleDisplay', {
                    name: 'search'
                });
            });
        })

    })

    describe('on searchRequestCompleted events', function(){

        it('should hide results when there are zero results', function() {
            var $c = this.component,
                $results = $c.select('resultsSelector');
            $results.show();
            $results[0].style.display.should.equal('');
            $c.on('searchRequestCompleted', function() {
                $results[0].style.display.should.equal('none');
            })

            _.defer(function() {
                $c.trigger('searchRequestCompleted', {
                    success: true,
                    result: {
                        totalHits: 0,
                        vertices: []
                    }
                });
            });
        })

        it('should show results when there are non-zero hits', function() {
            var $c = this.component,
                $results = $c.select('resultsSelector');
            $results.hide();
            $results[0].style.display.should.equal('none');
            $c.on('searchRequestCompleted', function() {
                $results[0].style.display.should.equal('block');
            })

            _.defer(function() {
                $c.trigger('searchRequestCompleted', {
                    success: true,
                    result: {
                        totalHits: 1,
                        vertices: [],
                    }
                });
            });
        })

        it('should trigger a paneResized event on the results', function(){
            var $c = this.component,
                paneResized = $.Deferred(),
                $results = $c.select('resultsSelector');

            $results.on('paneResized', function(event) {
                 paneResized.resolve();
             });

             _.defer(function() {
                $c.trigger('searchRequestCompleted', {
                    success: true,
                    result: {
                        totalHits: 0,
                        vertices: [],
                        nextOffset: 10
                    }
                });
            });
            return $.when(paneResized).promise();
        })

    })



})
