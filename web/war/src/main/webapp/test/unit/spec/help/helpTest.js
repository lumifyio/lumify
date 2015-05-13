describeComponent('help/help', function(Help) {

        beforeEach(function() {
            setupComponent(this);
        })

        describe('Help', function() {

            describe('on initialize', function(){

                it('should initialize', function() {
                    var c = this.component
                })

            })

            describe('on escape events', function() {

                it('should hide the help dialog', function() {
                    var c = this.component,
                        esc = $.Event('keydown');

                    esc.which = esc.keyCode = $.ui.keyCode.ESCAPE;

                    showHelp(c);

                    expect(c.$node.is(':visible')).to.be.true;

                    c.onEscape(esc);

                    expect(c.$node.is(':visible')).to.be.false;
                })

            })

            describe('on toggleHelp events', function() {

                it('should hide the help dialog when visible', function(done) {
                    var c = this.component;

                    showHelp(c);

                    expect(c.$node.is(':visible')).to.be.true;

                    c.trigger('toggleHelp');

                    _.defer(function() {
                        expect(c.$node.is(':visible')).to.be.false;
                        done();
                    })
                })

                it('should show the help dialog when not visible', function(done) {
                    var c = this.component;

                    showHelp(c);
                    hideHelp(c);

                    expect(c.$node.is(':visible')).to.be.false;

                    c.trigger('toggleHelp');

                    _.defer(function() {
                        expect(c.$node.is(':visible')).to.be.true;
                        done();
                    })
                })

            })

            describe('on keyboardShortcutsRegistered events', function() {

                describe('when no shortcuts are currently registered', function() {

                    it('should add the given shortcuts to the list', function() {
                        var c = this.component,
                            data = {
                                test1: {},
                                test2: {},
                                test3: {}
                            };

                        expect(getShortcuts(c).length).to.eql(0);

                        c.trigger('keyboardShortcutsRegistered', data);

                        _.defer(function() {
                            expect(getShortcuts(c).length).to.eql(3);
                        })
                    })

                })

                describe('when there are registered shortcuts', function() {

                    beforeEach(function() {
                        var c = this.component,
                            data = {
                                test1: {},
                                test2: {},
                                test3: {}
                            };

                        c.trigger('keyboardShortcutsRegistered', data);
                    })

                    it('should remove the old shortcuts and add the given shortcuts to the list', function() {
                        var c = this.component,
                            data = {
                                test4: {},
                                test5: {}
                            };

                        expect(getShortcuts(c).length).to.eql(3);

                        c.trigger('keyboardShortcutsRegistered', data);

                        _.defer(function() {
                            expect(getShortcuts(c).length).to.eql(2);
                        })
                    })

                })

            })

            describe('on registerKeyboardShortcuts events', function() {

                it('should trigger a requestKeyboardShortcuts event', function(done) {
                    var c = this.component;

                    c.on(document, 'requestKeyboardShortcuts', function() {
                        done();
                    })

                    c.trigger('registerKeyboardShortcuts');
                })

            })

        })

        function showHelp(c) {
            c.$node.modal();
        }

        function hideHelp(c) {
            c.$node.modal('hide');
        }

        function getShortcuts(c) {
            return c.$node.find('div.modal-body').find('ul').find('li');
        }

})