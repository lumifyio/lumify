
describeComponent('search/types/typeWorkspace', function(TypeWorkspace) {

    describe('SearchTypeWorkspace', function() {

        beforeEach(function() {
            setupComponent(this)
        })

        it('should trigger filter on query updated', function() {
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

        it('should trigger request completed on filter finish', function(done) {
            var c = this.component

            c.on('searchRequestCompleted', function() {
                done()
            })
            c.trigger('workspaceFiltered')
        })
    })
})
