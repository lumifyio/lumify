define(['dataRequestHandler'], function(DataRequestHandler) {

    describeComponent('notifications/notifications', function(Notifications) {

        beforeEach(function() {
            setupComponent(this);
            DataRequestHandler.listen(this.component);
        })

        describe('Notifications', function() {

            describe('on initialize', function(){

                it('should initialize', function() {
                    var c = this.component;
                })

            })

            describe('on notificationActive events', function() {

                it('should display the notification', function(done) {
                    var c = this.component,
                        data = {
                            notification: {
                                type: 'whatever'
                            }
                        };

                    c.displayNotifications = function(notifications) {
                        expect(notifications[0]).to.eql(data.notification);
                        done();
                    }

                    c.trigger('notificationActive', data);
                })

            })

            describe('on notificationDeleted events', function() {

                it('should trigger a notificationCountUpdated event with the updated count', function(done) {
                    var c = this.component,
                        data = {
                            notificationId: 3
                        },
                        stack = [
                            {
                                id: 1
                            },
                            {
                                id: 2
                            },
                            {
                                id: 3
                            }
                        ];

                    c.stack = stack;

                    c.on(document, 'notificationCountUpdated', function(event, data) {
                        expect(data.count).to.eql(2);
                        done();
                    })

                    c.trigger('notificationDeleted', data);
                })

                it('should update', function(done) {
                    var c = this.component,
                        data = {
                            notificationId: 3
                        };

                    c.update = done;

                    c.trigger('notificationDeleted', data);
                })

            })

            describe('.displayNotifications', function() {

                beforeEach(function() {
                    var c = this.component,
                            stack = [
                                    {
                                        id: 1
                                    },
                                    {
                                        id: 2
                                    },
                                    {
                                        id: 3
                                    }
                                ];

                        c.stack = stack;
                })

                describe('when given an empty notification list', function() {

                    it('should trigger a notificationCountUpdated event with the size of the stack', function(done) {
                        var c = this.component,
                            notifications = [];

                        c.on('notificationCountUpdated', function(event, data) {
                            expect(data.count).to.eql(3);
                            done();
                        })

                        c.displayNotifications(notifications);
                    })

                })

                describe('when given a non-empty notification list', function() {

                    it('should trigger a notificationCountUpdated event with the size of the stack', function(done) {
                        var c = this.component,
                            notifications = [
                                {
                                    id: 2
                                }
                            ];

                        c.on('notificationCountUpdated', function(event, data) {
                            expect(data.count).to.eql(3);
                            done();
                        })

                        c.displayNotifications(notifications);
                    })

                    it('should update', function(done) {
                        var c = this.component,
                            notifications = [
                                {
                                    id: 2
                                }
                            ];

                        c.update = done;

                        c.displayNotifications(notifications);
                    })

                    it('should add new notifications to the stack', function(done) {
                        var c = this.component,
                            notifications = [
                                {
                                    id: 4
                                }
                            ];

                        c.on('notificationCountUpdated', function(event, data) {
                            expect(data.count).to.eql(4);
                            expect(c.stack.length).to.eql(4);
                            done();
                        })

                        c.displayNotifications(notifications);
                    })

                    it('should replace existing notifications with updated ones on the stack', function(done) {
                        var c = this.component,
                            id = 2,
                            title = 'updated!',
                            notifications = [
                                {
                                    id: id,
                                    title: title
                                }
                            ];

                        c.on('notificationCountUpdated', function(event, data) {
                            c.stack.forEach(function(n) {
                                if(n.id === id) {
                                    expect(n.title).to.eql(title);
                                    done();
                                }
                            })
                        })

                        c.displayNotifications(notifications);
                    })

                })

            })

            describe('.dismissNotification', function() {

                beforeEach(function() {
                    var c = this.component,
                            stack = [
                                    {
                                        id: 1
                                    },
                                    {
                                        id: 2
                                    },
                                    {
                                        id: 3
                                    }
                                ];

                        c.stack = stack;
                })

                describe('when immediate option is true', function() {

                    it('should call immediateUpdate', function(done) {
                        var c = this.component,
                            notification = {
                                id: 2
                            },
                            options = {
                                immediate: true
                            };

                        c.immediateUpdate = done;

                        c.dismissNotification(notification, options);
                    })

                    it('should remove the notification from the stack', function(done) {
                        var c = this.component,
                            notification = {
                                id: 2
                            },
                            options = {
                                immediate: true
                            };

                        expect(c.stack.length).to.eql(3);

                        c.dismissNotification(notification, options);

                        _.defer(function() {
                            expect(c.stack.length).to.eql(2);
                            c.stack.forEach(function(n) {
                                // should not find the dismissed notification
                                if (n.id === notification.id) {
                                    expect(true).to.be.false;
                                }
                            })
                            done();
                        })
                    })

                })

                describe('when immediate option is false', function() {

                    it('should call update', function(done) {
                        var c = this.component,
                            notification = {
                                id: 2
                            },
                            options = {
                                immediate: false
                            };

                        c.update = done;

                        c.dismissNotification(notification, options);
                    })

                    it('should remove the notification from the stack', function(done) {
                        var c = this.component,
                            notification = {
                                id: 2
                            },
                            options = {
                                immediate: false
                            };

                        expect(c.stack.length).to.eql(3);

                        c.dismissNotification(notification, options);

                        _.defer(function() {
                            expect(c.stack.length).to.eql(2);
                            c.stack.forEach(function(n) {
                                // should not find the dismissed notification
                                if (n.id === notification.id) {
                                    expect(true).to.be.false;
                                }
                            })
                            done();
                        })
                    })

                })

            })

            describe('.sendMarkRead', function() {

                it('should send a data request with the read notifications', function(done) {
                    // This function is debounced 3 seconds,
                    // so the default 2 second wait will not suffice
                    this.timeout(6000);

                    var c = this.component,
                        markRead = [
                            {
                                id: 2
                            }
                        ];

                    c.markRead = markRead;

                    c.on(document, 'dataRequest', function(event, data) {
                        expect(data.parameters[0][0].id).to.eql(2);
                        expect(data['service']).to.eql('notification');
                        expect(data['method']).to.eql('markRead');

                        done();
                    })

                    c.sendMarkRead();
                })

            })

            describe('.update', function() {

                describe('when the notification stack is empty', function() {

                    it('should not add to the node when the stack is empty', function() {
                        var c = this.component,
                            stack = [];

                        c.stack = stack;

                        expect(getNotifications(c).length).to.eql(0);

                        c.update();

                        _.delay(function() {
                            expect(getNotifications(c).length).to.eql(0);
                        }, 400);
                    })

                })

                describe('when the notification stack is not empty', function() {

                    it('should add the notifications to the node', function() {
                        var c = this.component,
                        stack = [
                                {
                                    id: 1,
                                    title: 'Title1',
                                    message: 'Message1'
                                },
                                {
                                    id: 2,
                                    title: 'Title2',
                                    message: 'Message2'
                                },
                                {
                                    id: 3,
                                    title: 'Title3',
                                    message: 'Message3'
                                }
                            ];

                        c.stack = stack;

                        expect(getNotifications(c).length).to.eql(0);

                        c.update();

                        _.delay(function() {
                            var n = getNotifications(c);
                            expect(n.length).to.eql(stack.length);
                            for(var i = 0; i < stack.length; i++) {
                                expect($(n[i]).find('h1').html()).to.eql(stack[i].title)
                                expect($(n[i]).find('h2').html()).to.eql(stack[i].message)
                            }

                        }, 400);
                    })

                })


            })

        })

        function getNotifications(c) {
            return c.$node.find('div.notifications').find('li');
        }

    })

})