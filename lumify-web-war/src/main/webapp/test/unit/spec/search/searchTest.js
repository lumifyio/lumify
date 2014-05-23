
describeComponent('search/search', function(Search) {

    beforeEach(function() {
        setupComponent()
    })

    describe('search', function() {

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
                originalQuery = 'My query';

            querySetValue(c, originalQuery);
            c.$node.find('.find-workspace').hasClass('active').should.be.false

            switchToSearchType(c, 'workspace').done(function() {
                $query.val().should.equal('')

                switchToSearchType(c, 'lumify').done(function() {
                    $query.val().should.equal(originalQuery)
                    done()
                })
            })
        })

        it('should trigger pane resized on type change', function(done) {
            var c = this.component,
                $query = c.select('querySelector'),
                originalQuery = 'My query';

            querySetValue(c, originalQuery);
            c.$node.find('.find-workspace').hasClass('active').should.be.false

            c.$node.on('paneResized', _.after(2, function() {
                done();
            }))

            switchToSearchType(c, 'workspace')
                .done(function() {
                    switchToSearchType(c, 'lumify')
                })
        })

        it('should show clear search button when text typed', function() {
            var c = this.component,
                $q = c.select('querySelector'),
                $clear = c.select('clearSearchSelector');

            $clear.css('display').should.equal('none')
            querySetValue(c, 'Some text');
            $clear.css('display').should.equal('inline')
        })

        it('should hide clear search button when type changed', function(done) {
            var c = this.component,
                $q = c.select('querySelector'),
                $clear = c.select('clearSearchSelector');

            $clear.css('display').should.equal('none')
            querySetValue(c, 'Some text');
            $clear.css('display').should.equal('inline')

            switchToSearchType(c, 'workspace')
                .done(function() {
                    $clear.css('display').should.equal('none')
                    done()
                })
        })

        it('should clear search on button', function(done) {
            var c = this.component,
                $q = c.select('querySelector'),
                $clear = c.select('clearSearchSelector')

            c.$node.find('.search-type-lumify').on('clearSearch', function() {
                $q.val().should.be.empty
                done()
            })
            querySetValue(c, 'Some text')
            $clear.click()
        })

        it('should trigger submit event on enter', function(done) {
            var c = this.component;

            this.$node.on('querysubmit', function() {
                done()
            })
            querySetValue(c, 'test')
            querySubmit(c);
        })

        it('should select search text after enter')

    })

    function querySubmit(component) {
        var $q = component.select('querySelector'),
            event = $.Event('keyup');

        $q.focus();
        event.which = event.keyCode = $.ui.keyCode.ENTER;
        $q.trigger(event);
    }

    function querySetValue(component, string) {
        component.select('querySelector')
            .focus()
            .val(string)
            .click()
            .change();
    }

    function switchToSearchType(component, type) {
        var d = $.Deferred();
        component.on('searchtypeloaded', d.resolve);
        component.$node.find('.find-' + type).click();
        return d.promise();
    }

})
