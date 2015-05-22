describeComponent('menubar/menubar', function(Menubar) {

        beforeEach(function() {
            setupComponent(this);
        })

        describe('Menubar', function() {

            var BUTTONS = 'dashboard graph map search workspaces admin activity logout'.split(' ');

            describe('on initialize', function(){

                it('should initialize', function() {
                    var c = this.component
                })

                it('should create the buttons', function() {
                    var c = this.component;

                    BUTTONS.forEach(function(name) {
                        var found = false;
                        for(var i = 0; i < getTopButtons(c).length; i++) {
                            if($(getTopButtons(c)[i]).hasClass(name)) {
                                found = true;
                            }
                        }
                        for(var i = 0; i < getBottomButtons(c).length; i++) {
                            if($(getBottomButtons(c)[i]).hasClass(name)) {
                                found = true;
                            }
                        }
                        expect(found).to.be.true;
                    });
                })

            })

            describe('on menubarToggleDisplay events', function() {

                BUTTONS.forEach(function(name) {

                    describe('when ' + name + ' is toggled', function() {

                        describe('when ' + name + ' is already active', function() {

                            beforeEach(function() {
                                var c = this.component;
                                activate(c, name);
                            })

                            it('should deactivate the option', function(done) {
                                var c = this.component,
                                    data = {
                                        name: name
                                    };

                                expect(isActive(c, name)).to.be.true;

                                c.trigger('menubarToggleDisplay', data);

                                _.delay(function() {
                                    expect(isActive(c, name)).to.be.false;
                                    done();
                                }, 500);
                            })

                        })

                        describe('when ' + name + ' is not active', function() {

                            beforeEach(function() {
                                var c = this.component;
                                deactivate(c, name);
                            })

                            it('should activate the option', function() {
                                var c = this.component,
                                    data = {
                                        name: name
                                    };

                                expect(isActive(c, name)).to.be.false;

                                c.trigger('menubarToggleDisplay', data);

                                expect(isActive(c, name)).to.be.true;
                            })

                        })

                    })

                })




            })

            describe('on click events', function() {

                BUTTONS.forEach(function(name) {

                    describe('when ' + name + ' is clicked', function() {

                        it('should trigger a menubarToggleDisplay event', function(done) {
                            var c = this.component;
                                selector = name + 'IconSelector',
                                oldTooltip = $.fn.tooltip;

                            c.on(document, 'menubarToggleDisplay', function() {
                                $.fn.tooltip = oldTooltip;
                                done();
                            })

                            $.fn.tooltip = function(){/* noop to avoid tooltip init issues on this test */};
                            c.select(selector).click();
                        })

                    })

                })

            })

        })

        function getTopButtons(c) {
            return c.$node.find('ul.menu-top').find('li').not('.divider');
        }

        function getBottomButtons(c) {
            return c.$node.find('ul.menu-bottom').find('li').not('.divider');
        }

        function activate(c, name) {
            toggleActivate(c, name, true);
        }

        function deactivate(c, name) {
            toggleActivate(c, name, false);
        }

        function toggleActivate(c, name, active) {
            c.select(name + 'IconSelector').toggleClass('active', active);
        }

        function isActive(c, name) {
            return c.select(name + 'IconSelector').hasClass('active');
        }

})