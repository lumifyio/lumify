
describeComponent('search/search', function(Search) {

    beforeEach(function() {
        setupComponent()
    })

    describe.only('search', function() {

        it('should initialize', function() {
            var c = this.component

            expect(c.select('querySelector').length).to.equal(1)
            expect(c.select('formSelector').length).to.equal(1)
        })

        it('should set lumify type to active', function() {
            this.component.$node.find('.search-type-lumify').hasClass('active').should.be.true
            this.component.$node.find('.find-lumify').hasClass('active').should.be.true
        })

        it('should set workspace type to inactive', function() {
            this.component.$node.find('.search-type-workspace').hasClass('active').should.be.false
            this.component.$node.find('.find-workspace').hasClass('active').should.be.false
        })

        it('should attach search types based on segmented control after focus', function(done) {
            var c = this.component,
                workspaceLoaded = $.Deferred();

            expect(c.$node.find('.search-type-workspace').html().length).to.equal(0)

            c.on('searchtypeloaded', workspaceLoaded.resolve);
            c.$node.find('.find-workspace').click();

            workspaceLoaded.done(function() {
                var node = c.$node.find('.search-type-workspace')
                node.hasClass('active').should.be.true
                expect(node.html().length).to.be.above(0)
                done()
            })
        })

        it('should reset the search text on type change', function(done) {
            var c = this.component,
                $query = c.select('querySelector'),
                workspaceLoaded = $.Deferred(),
                lumifyLoaded = $.Deferred(),
                originalQuery = 'My query';

            $query.focus().val(originalQuery).click().change();
            c.$node.find('.find-workspace').hasClass('active').should.be.false

            c.on('searchtypeloaded', workspaceLoaded.resolve);
            c.$node.find('.find-workspace').click();

            workspaceLoaded.done(function() {
                $query.val().should.equal('')

                c.on('searchtypeloaded', lumifyLoaded.resolve);
                c.$node.find('.find-lumify').click();
                lumifyLoaded.done(function() {
                    $query.val().should.equal(originalQuery)
                    done()
                })
            })
        })

    })

})
