define(['dataRequestHandler'], function(DataRequestHandler) {

    describeComponent('search/types/typeLumify', function(TypeLumify) {

        beforeEach(function() {
            setupComponent(this);
        })

        describe('SearchTypeLumify', function() {

            describe('on initialize', function(){

                it('should initialize', function() {
                    var c = this.component
                })

            })

            describe('on clearSearch events', function(){

                it('should cancel and nullify current request', function() {
                    var $c = this.component,
                        $request = {
                            cancelled: false,
                            cancel: function() {
                                this.cancelled = true;
                            }
                        };

                    $c.currentRequest = $request;
                    expect($request.cancelled).to.be.false;
                    expect($c.currentRequest).to.not.be.null;

                    $c.trigger('clearSearch');

                    expect($request.cancelled).to.be.true;
                    expect($c.currentRequest).to.be.null;
                })

            })


            describe('on querysubmit events', function(){

                it('should make a request and trigger searchRequestBegan', function() {
                    var $c = this.component,
                        searchBegan = $.Deferred(),
                        data = {
                            filters: [],
                            value: 'querystring'
                        }

                    $c.on('searchRequestBegan', function(event) {
                         searchBegan.resolve();
                     });

                    expect($c.currentRequest).to.be.undefined;
                    $c.trigger('querysubmit', data);
                    _.defer(function(){expect($c.currentRequest).to.not.be.undefined});
                    return $.when(searchBegan).promise();
                })

                it('should trigger a successful searchRequestCompleted event for a successful query', function(){
                    var $c = this.component,
                        searchCompleted = $.Deferred(),
                        data = {
                            filters: [],
                            value: 'querystring'
                        },
                        success = true,
                        result = {
                            vertices: []
                        }

                    DataRequestHandler.setResponse('vertex', 'search', success, result);
                    DataRequestHandler.listen();

                    $c.on('searchRequestCompleted', function(event, data) {
                         DataRequestHandler.stop();
                         expect(data.success).to.be.true;
                         searchCompleted.resolve();
                     });


                    $c.trigger('querysubmit', data);
                    return $.when(searchCompleted).promise();
                })

                it('should trigger a failed searchRequestCompleted event for a failed query', function(){
                    var $c = this.component,
                        searchCompleted = $.Deferred(),
                        data = {
                            filters: [],
                            value: 'querystring'
                        },
                        success = false,
                        result = {
                            vertices: []
                        }

                    DataRequestHandler.setResponse('vertex', 'search', success, result);
                    DataRequestHandler.listen();

                    $c.on('searchRequestCompleted', function(event, data) {
                         DataRequestHandler.stop();
                         expect(data.success).to.be.false;
                         searchCompleted.resolve();
                     });


                    $c.trigger('querysubmit', data);
                    return $.when(searchCompleted).promise();
                })

            })


            describe('on filterschange events', function(){

                it('should set data.setAsteriskSearchOnEmpty to true', function() {
                    var $c = this.component;

                    $c.on('filterschange', function(event, data) {
                        expect(data.setAsteriskSearchOnEmpty).to.be.true;
                    });

                    $c.trigger('filterschange', {});
                })

            })


            describe('on infiniteScrollRequest events', function(){

                it('should trigger a successful addInfiniteVertices event for a successful query', function() {
                    var $c = this.component,
                        addInfinite = $.Deferred();

                    DataRequestHandler.setResponse('vertex', 'search', true, {});
                    DataRequestHandler.listen();

                    $c.on('addInfiniteVertices', function(event, data){
                        DataRequestHandler.stop();
                        expect(data.success).to.be.true;
                        addInfinite.resolve();
                    })

                    $c.currentFilters = {};
                    $c.trigger('infiniteScrollRequest', {});
                    return $.when(addInfinite).promise();
                })

                it('should trigger a failed addInfiniteVertices event for a failed query', function() {
                    var $c = this.component,
                        addInfinite = $.Deferred();

                    DataRequestHandler.setResponse('vertex', 'search', false, {});
                    DataRequestHandler.listen();

                    $c.on('addInfiniteVertices', function(event, data){
                        DataRequestHandler.stop();
                        expect(data.success).to.be.false;
                        addInfinite.resolve();
                    })

                    $c.currentFilters = {};
                    $c.trigger('infiniteScrollRequest', {});
                    return $.when(addInfinite).promise();
                })

            })


        })
    })
});
