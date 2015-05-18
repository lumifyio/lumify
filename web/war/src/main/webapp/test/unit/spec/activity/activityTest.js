define(['dataRequestHandler'], function(DataRequestHandler) {

    describeComponent('activity/activity', function(Activity) {

            beforeEach(function() {
                window.lumifyData = {};
                setupComponent(this);
            })

            describe('Activity', function() {

                describe('on initialize', function(){

                    it('should initialize', function() {
                        var c = this.component
                    })

                    describe('when setting up event watchers', function() {

                        it('should set up saveWorkspace', function(done) {
                            var c = this.component;


                            expect(c.tasks.length).to.eql(0);
                            $(document).trigger('workspaceSaving');
                            $(document).trigger('workspaceSaved');
                            _.defer(function(){
                                expect(c.tasks.length).to.eql(1);
                                expect(c.tasks[0].type).to.eql('saveWorkspace');
                                done();
                            })
                        })

                    })

                })

                describe('on menubarToggleDisplay events', function() {

                    describe('when the activity node is being toggled', function() {

                        it('should begin auto-updating when the activity node is visible', function() {
                            var c = this.component;

                            c.$node.addClass('visible');

                            expect(c.autoUpdateTimer).to.be.undefined;

                            c.trigger('menubarToggleDisplay', {name: 'activity'});

                            expect(c.autoUpdateTimer).to.not.be.undefined;
                        })

                        it('should not begin auto-updating when the activity node is not visible', function() {
                            var c = this.component;

                            c.$node.removeClass('visible');

                            expect(c.autoUpdateTimer).to.be.undefined;

                            c.trigger('menubarToggleDisplay', {name: 'activity'});

                            expect(c.autoUpdateTimer).to.be.undefined;
                        })

                    })

                    describe('when the activity node is not being toggled', function() {

                        it('should not begin auto-updating', function() {
                            var c = this.component;

                            c.$node.addClass('visible');

                            expect(c.autoUpdateTimer).to.be.undefined;

                            c.trigger('menubarToggleDisplay', {name: 'something else'});

                            expect(c.autoUpdateTimer).to.be.undefined;
                        })

                    })

                })

                describe('on longRunningProcessChanged events', function() {

                    it('should add the process to the task list', function() {
                        var c = this.component,
                            data = {
                                process: {}
                            };

                        expect(c.tasks.length).to.eql(0);

                        c.trigger('longRunningProcessChanged', data);

                        expect(c.tasks.length).to.eql(1);
                    })

                    it('should update', function(done) {
                        var c = this.component,
                            data = {
                                process: {}
                            };

                        c.update = function() {
                            done();
                        }

                        c.trigger('longRunningProcessChanged', data);

                    })
                })

                describe('on showActivityDisplay events', function() {

                    describe('when the activity node is visible', function() {

                        beforeEach(function() {
                            var c = this.component;
                            c.$node.addClass('visible');
                        })

                        it('should not trigger menubarToggleDisplay', function() {
                            var c = this.component;

                            c.on('menubarToggleDisplay', function() {
                                // break
                                expect(true).to.be.false;
                            })

                            c.trigger('showActivityDisplay');
                        })

                    })

                    describe('when the activity node is not visible', function() {

                        beforeEach(function() {
                            var c = this.component;
                            c.$node.removeClass('visible');
                        })

                        it('should trigger menubarToggleDisplay', function(done) {
                            var c = this.component;

                            c.on('menubarToggleDisplay', function() {
                                done();
                            })

                            c.trigger('showActivityDisplay');
                        })

                    })

                })

                describe('on activityHandlersUpdated events', function() {

                    it('should update', function(done) {
                        var c = this.component;

                        c.update = function() {
                            done();
                        }

                        c.trigger('activityHandlersUpdated');

                    })

                    it('should set up saveWorkspace', function(done) {
                        var c = this.component;

                        c.trigger('activityHandlersUpdated');

                        expect(c.tasks.length).to.eql(0);
                        $(document).trigger('workspaceSaving');
                        $(document).trigger('workspaceSaved');
                        _.defer(function(){
                            expect(c.tasks.length).to.eql(1);
                            expect(c.tasks[0].type).to.eql('saveWorkspace');
                            done();
                        })
                    })

                })

                describe('on verticesUpdated events', function() {

                    it('should update', function(done) {
                        var c = this.component;

                        c.update = function() {
                            done();
                        }

                        c.trigger('verticesUpdated');

                    })

                })

                describe('on click events', function() {

                    beforeEach(function(done) {
                        // setup activity to have buttons
                        var c = this.component;

                        $(document).trigger('workspaceSaving');
                        _.defer(function() {
                            c.$node.addClass('visible');
                            c.isOpen = true;
                            c.update();
                            done();
                        });
                    })

                    describe('when the delete button is clicked', function() {

                        it('should update', function(done) {
                            var c = this.component,
                                deleteButton = c.$node.find('button.delete');

                            DataRequestHandler.listen(c);

                            c.update = function() {
                                DataRequestHandler.stop();
                                done();
                                return Promise.resolve();
                            }

                            deleteButton.click();
                        })

                        it('should add the process to the removed tasks list', function(done) {
                            var c = this.component,
                                deleteButton = c.$node.find('button.delete'),
                                processId = 'saveWorkspace';

                            DataRequestHandler.listen(c);

                            c.update = function() {
                                DataRequestHandler.stop();
                                expect(c.removedTasks[processId]).to.be.true;
                                done();
                                return Promise.resolve();
                            }

                            expect(c.removedTasks[processId]).to.be.undefined;
                            deleteButton.click();
                        })

                        it('should remove the process from the tasks lists', function(done) {
                            var c = this.component,
                                deleteButton = c.$node.find('button.delete'),
                                processId = 'saveWorkspace';

                            DataRequestHandler.listen(c);

                            c.update = function() {
                                DataRequestHandler.stop();
                                expect(c.tasksById[processId]).to.be.undefined;
                                done();
                                return Promise.resolve();
                            }

                            expect(c.tasksById[processId]).to.not.be.undefined;
                            deleteButton.click();
                        })

                        it('should trigger a menubarToggleDisplay event', function(done) {
                            var c = this.component,
                                deleteButton = c.$node.find('button.delete');

                            DataRequestHandler.listen(c);

                            c.on(document, 'menubarToggleDisplay', function() {
                                DataRequestHandler.stop();
                                done();
                            });

                            deleteButton.click();
                        })

                    })

                    describe('when the cancel button is clicked', function() {

                        it('should update', function(done) {
                            var c = this.component,
                                cancelButton = c.$node.find('button.cancel');

                            DataRequestHandler.listen(c);

                            c.update = function() {
                                DataRequestHandler.stop();
                                done();
                                return Promise.resolve();
                            }

                            cancelButton.click();
                        })

                        it('should add the process to the removed tasks list', function(done) {
                            var c = this.component,
                                cancelButton = c.$node.find('button.cancel'),
                                processId = 'saveWorkspace';

                            DataRequestHandler.listen(c);

                            c.update = function() {
                                DataRequestHandler.stop();
                                expect(c.removedTasks[processId]).to.be.true;
                                done();
                                return Promise.resolve();
                            }

                            expect(c.removedTasks[processId]).to.be.undefined;
                            cancelButton.click();
                        })

                        it('should remove the process from the tasks lists', function(done) {
                            var c = this.component,
                                cancelButton = c.$node.find('button.cancel'),
                                processId = 'saveWorkspace';

                            DataRequestHandler.listen(c);

                            c.update = function() {
                                DataRequestHandler.stop();
                                expect(c.tasksById[processId]).to.be.undefined;
                                done();
                                return Promise.resolve();
                            }

                            expect(c.tasksById[processId]).to.not.be.undefined;
                            cancelButton.click();
                        })

                    })

                })

                describe('.update', function() {

                    describe('when the activity node is open', function() {

                        beforeEach(function() {
                            var c = this.component;
                            c.$node.addClass('visible');
                            c.isOpen = true;
                        })

                        it('should notify activity monitors', function(done) {
                            var c = this.component;

                            c.notifyActivityMonitors = function() {
                                done();
                            }

                            c.update();
                        })

                        describe('when there are no processes running', function() {

                            it('should append neither a delete nor a cancel button', function(done) {
                                var c = this.component;

                                expect(c.$node.find('button.delete').length).to.eql(0);
                                expect(c.$node.find('button.cancel').length).to.eql(0);

                                c.update();

                                _.defer(function() {
                                    expect(c.$node.find('button.delete').length).to.eql(0);
                                    expect(c.$node.find('button.cancel').length).to.eql(0);
                                    done();
                                })
                            })

                        })

                        describe('when there are processes running', function() {

                            beforeEach(function() {
                                $(document).trigger('workspaceSaving');
                            })

                            it('should append a delete button for the process', function(done) {
                                var c = this.component;

                                expect(c.$node.find('button.delete').length).to.eql(0);

                                c.update();

                                _.defer(function() {
                                    expect(c.$node.find('button.delete').length).to.eql(1);
                                    done();
                                })
                            })

                            it('should append a cancel button for the process', function(done) {
                                var c = this.component;

                                expect(c.$node.find('button.cancel').length).to.eql(0);

                                c.update();

                                _.defer(function() {
                                    expect(c.$node.find('button.cancel').length).to.eql(1);
                                    done();
                                })
                            })

                        })

                    })

                    describe('when the activity node is not open', function() {

                        beforeEach(function() {
                            var c = this.component;
                            c.$node.removeClass('visible');
                            c.isOpen = false;
                        })

                        it('should notify activity monitors', function(done) {
                            var c = this.component;

                            c.notifyActivityMonitors = function() {
                                done();
                            }

                            c.update();
                        })

                    })

                })

                describe('.notifyActivityMonitors', function() {

                    describe('when there are no processes running', function() {

                        it('should trigger activityUpdated when the previous activity count is not defined', function(done) {
                            var c = this.component;

                            c.previousCountForNotify = undefined;

                            c.on(document, 'activityUpdated', function() {
                                done();
                            })

                            c.notifyActivityMonitors([]);
                        })

                        it('should trigger activityUpdated when the previous activity count is defined', function(done) {
                            var c = this.component;

                            c.previousCountForNotify = 5;

                            c.on(document, 'activityUpdated', function() {
                                done();
                            })

                            c.notifyActivityMonitors([]);
                        })

                    })

                    describe('when there are processes running', function() {

                        it('should trigger activityUpdated when the previous activity count is not defined', function(done) {
                            var c = this.component;

                            c.previousCountForNotify = undefined;

                            c.on(document, 'activityUpdated', function() {
                                done();
                            })

                            c.notifyActivityMonitors([{}]);
                        })

                        it('should trigger activityUpdated when the previous activity count is different from the current count', function(done) {
                            var c = this.component;

                            c.previousCountForNotify = 5;

                            c.on(document, 'activityUpdated', function() {
                                done();
                            })

                            c.notifyActivityMonitors([{}]);
                        })

                        it('should not trigger activityUpdated when the previous activity count equals the current count', function(done) {
                            var c = this.component;

                            c.previousCountForNotify = 1;

                            c.on(document, 'activityUpdated', function() {
                                // shouldn't be triggered, so break
                                expect(true).to.be.false;
                            })

                            c.notifyActivityMonitors([{}]);

                            // prevent the function from firing a second time
                            c.notifyActivityMonitors = function(){};
                            _.delay(function() {
                                done();
                            }, 200);
                        })

                    })

                })

            })

    })
})