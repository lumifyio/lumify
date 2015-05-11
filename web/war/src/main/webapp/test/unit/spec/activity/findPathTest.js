define(['dataRequestHandler'], function(DataRequestHandler) {

    describeComponent('activity/builtin/findPath', function(FindPath) {

        beforeEach(function() {
            var attr = {
                    process: {}
                };
            window.lumifyData = {};
            setupComponent(this, attr);
        })

        describe('FindPath', function() {

            describe('on initialize', function(){

                it('should initialize', function() {
                    var c = this.component
                })

                it('should create a found-paths button', function() {
                    var c = this.component,
                        buttonSelector = 'button.found-paths';

                    expect(c.$node.find(buttonSelector).length).to.eql(1);
                })

            })

            describe('on click events', function() {

                describe('when path is clicked', function() {

                    it('should create the add-vertices button', function(done) {
                        var c = this.component,
                            pathsButtonSelector = 'button.found-paths',
                            vertsButtonsSelector = 'button.add-vertices';

                        DataRequestHandler.listen(c);

                        c.on(document, 'dataRequestCompleted', function() {
                            _.defer(function() {
                                expect(c.$node.find(vertsButtonsSelector).length).to.eql(1);
                                done();
                            })
                        })

                        expect(c.$node.find(vertsButtonsSelector).length).to.eql(0);
                        c.$node.find(pathsButtonSelector).click();
                    })

                    it('should trigger a focusPaths event', function(done) {
                        var c = this.component,
                            pathsButtonSelector = 'button.found-paths';

                        DataRequestHandler.listen(c);

                        c.on('focusPaths', function() {
                            done();
                        })

                        c.$node.find(pathsButtonSelector).click();
                    })

                })

                describe('when add-vertices is clicked', function() {

                    beforeEach(function(done) {
                        var c = this.component,
                            pathsButtonSelector = 'button.found-paths';

                        DataRequestHandler.listen(c);

                        c.on(document, 'dataRequestCompleted', function() {
                            _.defer(function() {
                                done();
                            })
                        })

                        // Click the find paths button which will create the add vertices button
                        c.$node.find(pathsButtonSelector).click();
                    })

                    it('should trigger an updateWorkspace event with the toAdd vertex ids', function(done) {
                        var c = this.component,
                            vertsButtonsSelector = 'button.add-vertices',
                            toAdd = {
                                1: {},
                                2: {}
                            },
                            toAddLength = 2;

                        c.toAdd = toAdd;

                        c.on('updateWorkspace', function(event, data) {
                            expect(data.entityUpdates.length).to.eql(toAddLength);
                            for(var i = 0; i < toAddLength; i++) {
                                expect(toAdd[data.entityUpdates[i].vertexId]).to.not.be.undefined;
                            }
                            done();
                        })

                        c.$node.find(vertsButtonsSelector).click();
                    })

                    it('should trigger a selectObjects event with the toAdd vertex ids', function(done) {
                        var c = this.component,
                            vertsButtonsSelector = 'button.add-vertices',
                            toAdd = {
                                1: {},
                                2: {}
                            },
                            toAddLength = 2;

                        c.toAdd = toAdd;

                        c.on('selectObjects', function(event, data) {
                            expect(data.vertexIds.length).to.eql(toAddLength);
                            for(var i = 0; i < toAddLength; i++) {
                                expect(toAdd[data.vertexIds[i]]).to.not.be.undefined;
                            }
                            done();
                        })

                        c.$node.find(vertsButtonsSelector).click();
                    })

                })

            })

            describe('on focusPaths events', function() {

                it('should not update the button when the vertex ids match', function() {
                    var c = this.component,
                        buttonSelector = 'button.found-paths',
                        noneText = 'popovers.find_path.paths.none',
                        data = {
                            sourceId: 1,
                            targetId: 2
                        },
                        process = {
                            sourceVertexId: 1,
                            destVertexId: 2,
                            resultsCount: 5
                        };

                    c.attr.process = process;

                    expect(c.$node.find(buttonSelector).text()).to.eql(noneText);

                    c.trigger('focusPaths', data);

                    _.defer(function() {
                        expect(c.$node.find(buttonSelector).text()).to.eql(noneText);
                    })
                })

                it('should update the button when the vertex ids do not match', function() {
                    var c = this.component,
                        buttonSelector = 'button.found-paths',
                        noneText = 'popovers.find_path.paths.none',
                        someText = 'popovers.find_path.paths.some',
                        data = {
                            sourceId: 3,
                            targetId: 4
                        },
                        process = {
                            sourceVertexId: 1,
                            destVertexId: 2,
                            resultsCount: 5
                        };

                    c.attr.process = process;

                    expect(c.$node.find(buttonSelector).text()).to.eql(noneText);

                    c.trigger('focusPaths', data);

                    _.defer(function() {
                        expect(c.$node.find(buttonSelector).text()).to.eql(someText);
                    })
                })

            })

            describe('on defocusPaths events', function() {

                it('should update the button when the vertex ids do not match', function() {
                    var c = this.component,
                        buttonSelector = 'button.found-paths',
                        noneText = 'popovers.find_path.paths.none',
                        someText = 'popovers.find_path.paths.some',
                        process = {
                            sourceVertexId: 1,
                            destVertexId: 2,
                            resultsCount: 5
                        };

                    c.attr.process = process;

                    expect(c.$node.find(buttonSelector).text()).to.eql(noneText);

                    c.trigger('defocusPaths');

                    _.defer(function() {
                        expect(c.$node.find(buttonSelector).text()).to.eql(someText);
                    })
                })

            })

            describe('on workspaceLoaded events', function() {

                it('should set button title to wrong workspace when workspace ids do not match', function(done) {
                    var c = this.component,
                        buttonSelector = 'button.found-paths',
                        expectedTitle = 'popovers.find_path.wrong_workspace',
                        data = {
                            workspaceId: 2
                        },
                        process = {
                            workspaceId: 3,
                            sourceVertexId: 7,
                            destVertexId: 8
                        },
                        workspaceVertices = {
                            7: {},
                            8: {}
                        };

                    c.attr.process = process;

                    DataRequestHandler.setResponse('workspace', 'store', true, workspaceVertices);
                    DataRequestHandler.listen(c);

                    c.on(document, 'dataRequestCompleted', function() {
                        _.defer(function() {
                            expect(c.$node.find(buttonSelector).attr('title')).to.eql(expectedTitle);
                            done();
                        });
                    })

                    c.trigger('workspaceLoaded', data);
                })

                it('should set button title to error when source and/or destination vertices are missing', function(done) {
                    var c = this.component,
                        buttonSelector = 'button.found-paths',
                        expectedTitle = 'popovers.find_path.source_dest_missing',
                        data = {
                            workspaceId: 3
                        },
                        process = {
                            workspaceId: 3,
                            sourceVertexId: 6,
                            destVertexId: 9
                        },
                        workspaceVertices = {
                            7: {},
                            8: {}
                        };

                    c.attr.process = process;

                    DataRequestHandler.setResponse('workspace', 'store', true, workspaceVertices);
                    DataRequestHandler.listen(c);

                    c.on(document, 'dataRequestCompleted', function() {
                        _.defer(function() {
                            expect(c.$node.find(buttonSelector).attr('title')).to.eql(expectedTitle);
                            done();
                        });
                    })

                    c.trigger('workspaceLoaded', data);
                })

                it('should set button title correctly when vertices are not missing', function(done) {
                    var c = this.component,
                        buttonSelector = 'button.found-paths',
                        expectedTitle = 'popovers.find_path.show_path',
                        data = {
                            workspaceId: 3
                        },
                        process = {
                            workspaceId: 3,
                            sourceVertexId: 7,
                            destVertexId: 8
                        },
                        workspaceVertices = {
                            7: {},
                            8: {}
                        };

                    c.attr.process = process;

                    DataRequestHandler.setResponse('workspace', 'store', true, workspaceVertices);
                    DataRequestHandler.listen(c);

                    c.on(document, 'dataRequestCompleted', function() {
                        _.defer(function() {
                            expect(c.$node.find(buttonSelector).attr('title')).to.eql(expectedTitle);
                            done();
                        });
                    })

                    c.trigger('workspaceLoaded', data);
                })

            })

        })

    })

})