define(['dataRequestHandler'], function(DataRequestHandler) {
    describeComponent('search/search', function() {

        beforeEach(function(done) {
            setupComponent(this);
            var c = this.component;

            switchToSearchType(c, 'lumify').done(function() {
                querySetValue(c, '');
                done();
            });
        })

        describe('search', function() {

            describe('on initialize', function(){

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
            })

            describe('click events', function(){

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

                it('should hide clear search button when type changed', function(done) {
                    var c = this.component,
                        $q = c.select('querySelector'),
                        $clear = c.select('clearSearchSelector');

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
                        _.defer(function() {
                            $q.val().should.be.empty
                            done()
                        })
                    })
                    querySetValue(c, 'Some text')
                    _.defer(function() {
                        $clear.css('display').should.equal('inline')
                        $clear.click()
                    })
                })
            })

            describe('change, keydown, keyup, and paste events', function(){

                it('should show clear search button when text typed', function() {
                    var c = this.component,
                        $q = c.select('querySelector'),
                        $clear = c.select('clearSearchSelector');

                    querySetValue(c, 'Some text');
                    $clear.css('display').should.equal('inline')
                    querySetValue(c, '');
                    $clear.css('display').should.equal('none')
                })

                it('should blur search field on escape', function() {
                    var $q = this.component.select('querySelector'),
                        event = $.Event('keyup');

                    $q.focus()
                    document.activeElement.should.equal($q[0])
                    event.which = event.keyCode = $.ui.keyCode.ESCAPE;
                    $q.trigger(event);
                    document.activeElement.should.not.equal($q[0])
                })

                it('should clear search field on escape when value', function(done) {
                    var c = this.component,
                        $q = c.select('querySelector'),
                        event = $.Event('keyup');

                    c.$node.find('.search-type-lumify').on('clearSearch', function() {
                        _.defer(function() {
                            $q.val().should.be.empty
                            done()
                        })
                    })

                    $q.focus()
                    querySetValue(c, 'something')
                    document.activeElement.should.equal($q[0])
                    event.which = event.keyCode = $.ui.keyCode.ESCAPE;
                    $q.trigger(event);
                })

                it('should trigger submit event on enter', function(done) {
                    var c = this.component;

                    this.$node.on('querysubmit', function() {
                        done()
                    })
                    querySetValue(c, 'test')
                    querySubmit(c);
                })

                it('should select search text after enter', function(done) {
                    var c = this.component,
                        $q = c.select('querySelector'),
                        event = $.Event('keyup');

                    $q.focus()
                    querySetValue(c, 'something')
                    document.activeElement.should.equal($q[0])
                    event.which = event.keyCode = $.ui.keyCode.ENTER
                    $q.trigger(event)

                    _.defer(function() {
                        var selection = window.getSelection()
                        selection.rangeCount.should.equal(1)
                        selection.toString().should.equal('something')
                        done()
                    })
                })

            })

            describe('focus events', function(){

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

            })

            describe('on filterschange events', function(){

                describe('when there is no query and wildcard search is enabled', function() {

                    it('should set query to "*" and trigger querysubmit', function(done) {
                        var c = this.component,
                            query = c.select('querySelector'),
                            propertiesData = {
                                    search: {
                                        disableWildcardSearch: 'true'
                                     }
                            },
                            data = {
                                    setAsteriskSearchOnEmpty: true,
                                    conceptFilter: ['filter']
                            };

                        DataRequestHandler.setResponse('config', 'properties', true, propertiesData);
                        DataRequestHandler.listen(c);

                        c.trigger('filterschange', data);

                        $(document).on('querysubmit', function() {
                            $(document).off('querysubmit');
                            DataRequestHandler.stop();
                            expect(query.val()).to.equal('*');
                            done();
                        });
                    })

                })

                describe('when there is a query', function() {

                    it('should not change the query and should trigger querysubmit', function(done) {
                        var c = this.component,
                            query = c.select('querySelector'),
                            queryString = 'Some Query',
                            propertiesData = {
                                    search: {
                                        disableWildcardSearch: 'true'
                                     }
                            },
                            data = {
                                    setAsteriskSearchOnEmpty: true,
                                    conceptFilter: ['filter']
                            };

                        DataRequestHandler.setResponse('config', 'properties', true, propertiesData);
                        DataRequestHandler.listen(c);

                        query.val(queryString)
                        c.trigger('filterschange', data);

                        $(document).on('querysubmit', function() {
                            $(document).off('querysubmit');
                            DataRequestHandler.stop();
                            expect(query.val()).to.equal(queryString);
                            done();
                        });
                    })

                })

            })

            describe('on clearSearch events', function(){

                describe('when search type is the target', function() {

                    beforeEach(function(done){
                        var c = this.component,
                            target = c.$node.find('.search-type-lumify'),
                            $error = c.select('queryValidationSelector');

                        target.on('clearSearch', function() {
                            _.defer(function() {
                                done()
                            })
                        })

                        querySetValue(c, 'Some text');
                        c.filters = {};
                        $error.html('Error');
                        c.savedQueries['Workspace'].query = 'Some Query';
                        c.savedQueries['Workspace'].filters = ['filter'];

                        _.defer(function() {
                            target.trigger('clearSearch');
                        })
                    })

                    it('should clear search query', function() {
                        var c = this.component,
                            $q = c.select('querySelector');

                        $q.val().should.be.empty;
                    })

                    it('should nullify search filters', function() {
                        var c = this.component;

                        expect(c.filters).to.be.null;
                    })

                    it('should clear query error', function() {
                        var c = this.component,
                            $error = c.select('queryValidationSelector');

                         $error.html().should.be.empty;
                    })

                    it('should trigger queryupdated', function(done) {
                        $(document).on('queryupdated', function() {
                            done();
                        })
                    })

                    it('should not clear the saved query', function() {
                        var c = this.component;

                        expect(c.savedQueries['Workspace'].query).to.not.equal('');
                        expect(c.savedQueries['Workspace'].filters).to.not.eql([]);
                    })

                })


                describe('when search type is not the target', function() {

                    beforeEach(function(done){
                        var c = this.component,
                            target = c.$node.find('.search-type-workspace'),
                            $error = c.select('queryValidationSelector');

                        target.on('clearSearch', function() {
                            _.defer(function() {
                                done()
                            })
                        })

                        querySetValue(c, 'Some text');
                        c.filters = {};
                        $error.html('Error');

                        c.savedQueries['Workspace'].query = 'Some Query';
                        c.savedQueries['Workspace'].filters = ['filter'];

                        _.defer(function() {
                            target.trigger('clearSearch');
                        })
                    })

                    it('should not clear search query', function() {
                        var c = this.component,
                            $q = c.select('querySelector');

                        $q.val().should.not.be.empty;
                    })

                    it('should not nullify search filters', function() {
                        var c = this.component;

                        expect(c.filters).to.not.be.null;
                    })

                    it('should not clear query error', function() {
                        var c = this.component,
                            $error = c.select('queryValidationSelector');

                         $error.html().should.not.be.empty;
                    })

                    it('should clear the saved query', function() {
                        var c = this.component;

                        expect(c.savedQueries['Workspace'].query).to.equal('');
                        expect(c.savedQueries['Workspace'].filters).to.eql([]);
                    })

                });

            })

            describe('on searchRequestBegan events', function(){

                it('should add the loading class to the query container', function() {
                    var c = this.component,
                        $queryContainer = c.select('queryContainerSelector');

                    expect($queryContainer.hasClass('loading')).to.be.false;

                    c.trigger('searchRequestBegan');
                    expect($queryContainer.hasClass('loading')).to.be.true;
                })

            })

            describe('on searchRequestCompleted events', function(){

                it('should show search errors', function() {
                    var c = this.component,
                        $error = c.$node.find('.search-query-validation')

                    $error.html().should.be.empty
                    c.trigger('searchRequestCompleted', { success: false, error: 'An error message' })
                    $error.html().should.not.be.empty
                    $error.find('button').remove()
                    $.trim($error.text()).should.equal('An error message')
                })

                it('should show search error when no message is passed', function() {
                    var c = this.component,
                        $error = c.$node.find('.search-query-validation')

                    c.trigger('searchRequestCompleted', { success: false })
                    $error.html().should.not.be.empty
                    $error.find('button').remove()
                    $.trim($error.text()).should.equal('search.query.error')
                })

                it('should show search error when no message is passed', function() {
                    var c = this.component,
                        $error = c.$node.find('.search-query-validation')

                    c.trigger('searchRequestCompleted', { success: false })
                    $error.html().should.not.be.empty
                    $error.find('button').remove()
                    $.trim($error.text()).should.equal('search.query.error')
                })

                it('should hide errors on successful query', function() {
                    var c = this.component,
                        $error = c.$node.find('.search-query-validation')

                    c.trigger('searchRequestCompleted', { success: false })
                    $error.html().should.not.be.empty
                    this.component.trigger('searchRequestCompleted', { success: true })
                    $error.html().should.be.empty
                })

                it('should remove the loading class from the query container', function() {
                    var c = this.component,
                        $queryContainer = c.select('queryContainerSelector');

                    $queryContainer.addClass('loading');
                    expect($queryContainer.hasClass('loading')).to.be.true;

                    c.trigger('searchRequestCompleted');
                    expect($queryContainer.hasClass('loading')).to.be.false;
                })
            })

            describe('on searchForPhrase events', function(){

                it('should open Lumify search type', function(done) {
                    var c = this.component,
                        data = {
                            query: 'Query String'
                        };


                    switchToSearchType(c, 'workspace').done(function() {

                        $(document).on('menubarToggleDisplay', function(){
                            _.defer(function() {
                                expect(c.searchType).to.equal('Lumify');
                                done();
                            });

                            $(document).off('menubarToggleDisplay');
                            $(document).trigger('searchPaneVisible');

                        })

                        c.trigger('searchForPhrase', data);

                    })
                })

                it('should trigger clearSearch on typeLumify', function(done) {
                    var c = this.component,
                        typeLumify = c.$node.find('.search-type-lumify'),
                        data = {
                            query: 'Query String'
                        };

                    $(document).on('menubarToggleDisplay', function(){
                        $(document).off('menubarToggleDisplay');
                        $(document).trigger('searchPaneVisible');
                    })

                    typeLumify.on('clearSearch', function() {
                        typeLumify.off('clearSearch');
                        done();
                    })

                    c.trigger('searchForPhrase', data);

                })

                it('should set the query value', function(done) {
                    var c = this.component,
                        queryContainer = c.select('querySelector'),
                        query = 'Query String',
                        data = {
                            query: query
                        };

                    $(document).on('menubarToggleDisplay', function(){
                        $(document).off('menubarToggleDisplay');
                        $(document).trigger('searchPaneVisible');
                    })

                    $(document).on('querysubmit', function() {
                        $(document).off('querysubmit');
                        expect(queryContainer.val()).to.equal('"' + query + '"');
                        done();
                    })

                    c.trigger('searchForPhrase', data);

                })

                it('should trigger querysubmit', function(done) {
                    var c = this.component,
                        query = 'Query String',
                        data = {
                            query: query
                        };

                    $(document).on('menubarToggleDisplay', function(){
                        $(document).off('menubarToggleDisplay');
                        $(document).trigger('searchPaneVisible');
                    })

                    $(document).on('querysubmit', function() {
                        $(document).off('querysubmit');
                        done();
                    })

                    c.trigger('searchForPhrase', data);

                })

            })

            describe('on searchByRelatedEntity events', function(){

                it('should open Lumify search type', function(done) {
                    var c = this.component,
                        data = {
                            query: 'Query String'
                        };

                    switchToSearchType(c, 'workspace').done(function() {

                        $(document).on('menubarToggleDisplay', function(){
                            _.defer(function() {
                                expect(c.searchType).to.equal('Lumify');
                                done();
                            });

                            $(document).off('menubarToggleDisplay');
                            $(document).trigger('searchPaneVisible');

                        })

                        c.trigger('searchByRelatedEntity', data);

                    })
                })


                it('should clear the query value', function(done) {
                    var c = this.component,
                        queryContainer = c.select('querySelector'),
                        query = 'Query String',
                        data = {
                            query: query
                        };

                    queryContainer.val('Something');

                    $(document).on('menubarToggleDisplay', function(){
                        _.defer(function() {
                            expect(queryContainer.val()).to.equal('');
                            done();
                        })

                        $(document).off('menubarToggleDisplay');
                        $(document).trigger('searchPaneVisible');
                    })

                    c.trigger('searchByRelatedEntity', data);

                })

                it('should trigger searchByRelatedEntity on Lumify search type', function(done) {
                    var c = this.component,
                        searchLumify = c.$node.find('.search-type-lumify').find('.search-filters .content'),
                        query = 'Query String',
                        data = {
                            query: query
                        };

                    $(document).on('menubarToggleDisplay', function(){
                        $(document).off('menubarToggleDisplay');
                        $(document).trigger('searchPaneVisible');
                    })

                    searchLumify.on('searchByRelatedEntity', function() {
                        $(document).off('searchByRelatedEntity');
                        done();
                    })

                    c.trigger('searchByRelatedEntity', data);

                })

            })

            describe('on searchPaneVisible events', function(){

                it('should focus the query field', function(done){
                    var c = this.component,
                        query = c.select('querySelector');

                    query.on('focus', function() {
                        done();
                    });

                    c.trigger('searchPaneVisible');
                })

            })

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
                .val(string)
                .click()
                .focus()
                .change();
        }

        function switchToSearchType(component, type) {
            var d = $.Deferred();
            component.on('searchtypeloaded', d.resolve);
            component.$node.find('.find-' + type).click();
            return d.promise();
        }

    });
});