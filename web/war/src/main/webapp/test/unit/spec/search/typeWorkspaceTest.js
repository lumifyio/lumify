
describeComponent('search/types/typeWorkspace', function(TypeWorkspace) {

    describe('SearchTypeWorkspace', function() {

        beforeEach(function() {
            this.component = null;
            setupComponent(this)
        })

        describe('on initialize', function(){

            it('should initialize', function() {
                var c = this.component;
            })

        })


        describe('on queryupdated events', function(){

            it('should trigger filter', function() {
                var c = this.component,
                    searchBegan = $.Deferred(),
                    filter = $.Deferred()

                c.on('searchRequestBegan', function(event) {
                    event.stopPropagation()
                    searchBegan.resolve()
                })
                c.on('filterWorkspace', function(event) {
                    event.stopPropagation()
                    filter.resolve()
                })
                c.trigger('queryupdated')
                return $.when(searchBegan, filter).promise()
            })

        })

        describe('on workspaceFiltered events', function(){

            it('should trigger request completed', function(done) {
                var c = this.component

                c.on('searchRequestCompleted', function() {
                    done();
                })
                c.trigger('workspaceFiltered', {
                    hits: 0,
                    total: 0
                })
            })

        })

        describe('on clearWorkspaceFilter events', function(){

            it('should trigger clearSearch', function(done) {
                var c = this.component

                c.on('clearSearch', function() {
                    done();
                })
                c.trigger('clearWorkspaceFilter');
            })

        })

    })
})
