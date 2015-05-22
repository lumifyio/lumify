describeComponent('menubar/activity/activity', function(Activity) {

        beforeEach(function() {
            setupComponent(this);
        })

        describe('Activity', function() {

            describe('on initialize', function(){

                it('should initialize', function() {
                    var c = this.component
                })

            })

            describe('on activityUpdated events', function() {

                describe('when the badge does not exist', function() {

                    it('should do nothing when the count is zero', function() {
                        var c = this.component,
                            count = 0,
                            data = {
                                count: count
                            };

                        expect(getBadge(c).length).to.eql(0);

                        c.trigger('activityUpdated', data);

                        _.defer(function() {
                            expect(getBadge(c).length).to.eql(0);
                        })
                    })

                    it('should create the badge and set the count when the count is non-zero', function() {
                        var c = this.component,
                            count = 2,
                            data = {
                                count: count
                            };

                        expect(getBadge(c).length).to.eql(0);

                        c.trigger('activityUpdated', data);

                        _.defer(function() {
                            expect(getBadge(c).length).to.eql(1);
                            expect(getBadge(c).html()).to.eql(count.toString());
                        })
                    })

                })

                describe('when the badge exists', function() {

                    beforeEach(function() {
                        var c = this.component,
                            count = 7,
                            data = {
                                count: count
                            };

                        c.trigger('activityUpdated', data);
                    })

                    it('should set the count when the count is zero', function() {
                        var c = this.component,
                            count = 0,
                            data = {
                                count: count
                            };

                        expect(getBadge(c).length).to.eql(1);

                        c.trigger('activityUpdated', data);

                        _.defer(function() {
                            expect(getBadge(c).length).to.eql(1);
                            expect(getBadge(c).html()).to.eql(count.toString());
                        })
                    })

                    it('should remove the animating class when the count is zero', function() {
                        var c = this.component,
                            count = 0,
                            data = {
                                count: count
                            };

                        expect(c.$node.hasClass('animating')).to.be.true;

                        c.trigger('activityUpdated', data);

                        _.defer(function() {
                            expect(c.$node.hasClass('animating')).to.be.false;
                        })
                    })

                    it('should set the count when the count is non-zero', function() {
                        var c = this.component,
                            count = 2,
                            data = {
                                count: count
                            };

                        expect(getBadge(c).length).to.eql(1);

                        c.trigger('activityUpdated', data);

                        _.defer(function() {
                            expect(getBadge(c).length).to.eql(1);
                            expect(getBadge(c).html()).to.eql(count.toString());
                        })
                    })

                    it('should not remove the animating class when the count is non-zero', function() {
                        var c = this.component,
                            count = 2,
                            data = {
                                count: count
                            };

                        expect(c.$node.hasClass('animating')).to.be.true;

                        c.trigger('activityUpdated', data);

                        _.defer(function() {
                            expect(c.$node.hasClass('animating')).to.be.true;
                        })
                    })

                })

            })

        })

        function getBadge(c) {
            return c.$node.find('div.activityCount');
        }

})