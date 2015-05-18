define(['dataRequestHandler'], function(DataRequestHandler) {

describeComponent('dashboard/dashboard', function(DashboardView) {

    beforeEach(function() {
        setupComponent(this);
    })

    describe('DashboardView', function() {

        describe('on initialize', function(){

            it('should initialize', function() {
                var c = this.component
            })

        })

        describe('on click events', function() {

            describe('when the dashboard is clicked', function() {

                it('should trigger a selectObjects event', function(done) {
                    var c = this.component;

                    c.on(document, 'selectObjects', function() {
                        done();
                    })

                    c.$node.click();
                })

            })

            describe('when the help button is clicked', function() {

                it('should trigger a toggleHelp event', function(done) {
                    var c = this.component,
                        buttonSelector = '.help';

                    c.on(document, 'toggleHelp', function() {
                        done();
                    })

                    c.$node.find(buttonSelector).click();
                })

            })

        })

        describe('on select-all events', function() {

            it('should stop the event propagation', function(done) {
                var c = this.component;

                c.on(document, 'select-all', function() {
                    // this should not be triggered
                    expect(true).to.be.false;
                })

                c.trigger('select-all');
                _.delay(function() {
                    done();
                }, 300);
            })

        })

        describe('on graphPaddingUpdated events', function() {

            it('should set the right css according to the event data', function() {
                var c = this.component,
                    buttonSelector = '.help',
                    right = 12;
                    data = {
                        padding: {
                            r: right
                        }
                    };

                expect(c.$node.find(buttonSelector).css('right')).to.not.eq(right + 'px');

                c.trigger('graphPaddingUpdated', data);

                _.defer(function() {
                    expect(c.$node.find(buttonSelector).css('right')).to.eq(right + 'px');
                })
            })

        })

        describe('on didToggleDisplay events', function() {

            it('should attach a Notifications component when the dashboard is toggled to visible', function(done) {
                var c = this.component,
                    data = {
                        name: 'dashboard',
                        visible: true
                    },
                    noteResult = {
                        user: [],
                        system: {
                            active: []
                        }
                    };

                DataRequestHandler.setResponse('notification', 'list', true, noteResult);
                DataRequestHandler.listen(c);

                expect(c.$node.find('div.notifications').length).to.eql(0);

                c.trigger('didToggleDisplay', data);

                _.delay(function() {
                    expect(c.$node.find('div.notifications').length).to.eql(1);
                    done();
                }, 300)
            })

            it('should not attach a Notifications component when the dashboard is not toggled', function(done) {
                var c = this.component,
                    data = {
                        name: 'not dashboard',
                        visible: true
                    };

                DataRequestHandler.listen(c);

                expect(c.$node.find('div.notifications').length).to.eql(0);

                c.trigger('didToggleDisplay', data);

                _.delay(function() {
                    expect(c.$node.find('div.notifications').length).to.eql(0);
                    done();
                }, 300)
            })

            it('should not attach a Notifications component when the dashboard is toggled to not visible',
                function(done) {
                var c = this.component,
                    data = {
                        name: 'dashboard',
                        visible: false
                    };

                DataRequestHandler.listen(c);

                expect(c.$node.find('div.notifications').length).to.eql(0);

                c.trigger('didToggleDisplay', data);

                _.delay(function() {
                    expect(c.$node.find('div.notifications').length).to.eql(0);
                    done();
                }, 300)
            })

        })

        describe('on notificationCountUpdated events', function() {

            it('should remove the loading class from the notification badge', function() {
                var c = this.component,
                    badgeSelector = '.badge',
                    loadingClass = 'loading',
                    count = 8,
                    data = {
                        count: count
                    };

                expect(c.$node.find(badgeSelector).hasClass(loadingClass)).to.be.true;

                c.trigger('notificationCountUpdated', data);

                expect(c.$node.find(badgeSelector).hasClass(loadingClass)).to.be.false;
            })

            it('should insert the notification count into the notification badge', function() {
                var c = this.component,
                    badgeSelector = '.badge',
                    loadingClass = 'loading',
                    count = 9,
                    data = {
                        count: count
                    };

                expect(c.$node.find(badgeSelector).html()).to.not.eql(count.toString());

                c.trigger('notificationCountUpdated', data);

                expect(c.$node.find(badgeSelector).html()).to.eql(count.toString());
            })

        })
    })

})

})