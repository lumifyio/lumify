define(['dataRequestHandler'], function(DataRequestHandler) {

    describeComponent('map/map', function(MapViewOpenLayers) {

        beforeEach(function() {
            setupComponent(this);
        })

        describe('MapViewOpenLayers', function() {

            describe('on initialize', function(){

                it('should initialize', function() {
                    var c = this.component;
                })

            })

            describe('on mapShow events', function() {

                it('should initialize map', function(done) {
                    var c = this.component;

                    expect(c.$node.find('#map').hasClass('olMap')).to.be.false;
                    initMap(c, function() {
                        expect(c.$node.find('#map').hasClass('olMap')).to.be.true;
                        done();
                    })

                })

            })

            describe('on mapCenter events', function() {

                it('should set the latitude and longitude of the map', function(done) {
                    var c = this.component,
                        latitude = 34,
                        longitude = 56,
                        latlongstring = 'https://maps.google.com/maps?ll=' +
                                                latitude + ',' + longitude,
                        data = {
                            latitude: latitude,
                            longitude: longitude
                        };


                    initMap(c, function(){
                        expect(c.$node.html().indexOf(latlongstring)).to.eql(-1);
                        c.trigger('mapCenter', data);
                        _.delay(function() {
                            expect(c.$node.html().indexOf(latlongstring)).to.not.eql(-1);
                            done();
                        }, 300);
                    });


                })

            })

            describe('on workspaceLoaded events', function() {

            })

            describe('on workspaceUpdated events', function() {

            })

            describe('on updateWorkspace events', function() {

                it('should remove any deleted vertices', function(done) {
                    var c = this.component,
                        vertexIds = [
                            1,
                            7,
                            45
                        ],
                        data = {
                            entityDeletes: vertexIds.slice(0)
                        };

                    c.removeVertexIds = function(ids) {
                        expect(ids.length).to.eql(vertexIds.length);
                        ids.forEach(function(item, index) {
                            expect(item).to.eql(vertexIds[index]);
                        })
                        done();
                    }

                    c.trigger('updateWorkspace', data);
                })

            })

            describe('on verticesAdded events', function() {

                it('should call updateOrAddVertices', function(done) {
                    var c = this.component,
                        data = {};

                    c.updateOrAddVertices = function() {
                        done();
                    }

                    c.trigger('verticesAdded', data);
                })

            })

            describe('on verticesDropped events', function() {

                describe('when the map is visible', function() {

                    beforeEach(function() {
                        this.component.$node.show();
                    })

                    it('should trigger an addVertices event', function(done) {
                        var c = this.component;

                        c.on(document, 'addVertices', function() {
                            done();
                        })

                        c.trigger('verticesDropped');
                    })

                })

                describe('when the map is not visible', function() {

                    beforeEach(function() {
                        this.component.$node.hide();
                    })

                    it('should not trigger an addVertices event', function(done) {
                        var c = this.component;

                        c.on(document, 'addVertices', function() {
                            // This should not be triggered, so force an error
                            expect(true).to.be.false;
                        })

                        c.trigger('verticesDropped');

                        _.delay(function() {
                            done();
                        }, 300);
                    })

                })

            })

            describe('on verticesUpdated events', function() {

                it('should call updateOrAddVertices', function(done) {
                    var c = this.component,
                        data = {};

                    c.updateOrAddVertices = function() {
                        done();
                    }

                    c.trigger('verticesUpdated', data);
                })

            })

            describe('on verticesDeleted events', function() {

                it('should remove the given vertices', function(done) {
                    var c = this.component,
                        vertexIds = [
                            3,
                            6,
                            13
                        ],
                        data = {
                            vertexIds: vertexIds.slice(0)
                        };

                    c.removeVertexIds = function(ids) {
                        expect(ids.length).to.eql(vertexIds.length);
                        ids.forEach(function(item, index) {
                            expect(item).to.eql(vertexIds[index]);
                        })
                        done();
                    }

                    c.trigger('verticesDeleted', data);
                })

            })

            describe('on objectsSelected events', function() {

            })

            describe('on graphPaddingUpdated events', function() {

                it('should set the padding', function() {
                    var c = this.component,
                        padding = {
                            l: 10,
                            r: 3,
                            b: 55,
                            t: 7
                        },
                        data = {
                            padding: padding
                        };

                    expect(paddingEqual(padding, c.padding)).to.be.false;
                    c.trigger('graphPaddingUpdated', data);
                    expect(paddingEqual(padding, c.padding)).to.be.true;

                })

                function paddingEqual(p1, p2) {
                    return  p1.l === p2.l &&
                            p1.r === p2.r &&
                            p1.b === p2.b &&
                            p1.t === p2.t;
                }

            })

            describe('.updateOrAddVertices', function() {

            })

            describe('.removeVertexIds', function() {

            })

        })

        function initMap(c, callback) {
            var propertiesData = {
                'map.provider': 'google'
            };

            DataRequestHandler.setResponse('config', 'properties', true, propertiesData)
            DataRequestHandler.listen(c);

            c.trigger('mapShow');

            _.delay(callback, 500);
        }

    })

})