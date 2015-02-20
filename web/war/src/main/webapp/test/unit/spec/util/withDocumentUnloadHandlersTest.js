describeMixin('util/withDocumentUnloadHandlers', function() {
    var firstCallback = function(){},
        secondCallback = function(){},
        thirdCallback = function(){};

    beforeEach(function() {
        setupComponent(this);
    });

    xdescribe('onDocumentUnload', function() {
        it('should do nothing if there are no callbacks', function() {
            expect(this.component.onDocumentUnload()).to.be.undefined;
        });

        it('should exec the callbacks in order', function() {
            var step = 1;
            var makeCallback = function(num) {
                return function() {
                    step.should.be.eql(num);
                    step = step + 1;
                }
            };
            $(document).trigger('registerBeforeUnloadHandler', { fn:makeCallback(2), priority:42 });
            $(document).trigger('registerBeforeUnloadHandler', makeCallback(3));
            $(document).trigger('registerBeforeUnloadHandler', { fn:makeCallback(1), priority:4 });

            expect(this.component.onDocumentUnload()).to.be.undefined;
            step.should.be.eql(4);            
        });

        it('should exec the stop calling callbacks if one returns a string', function() {
            var step = 1;
            var makeCallback = function(result) {
                return function() {
                    step = step + 1;
                    return result;
                }
            };
            $(document).trigger('registerBeforeUnloadHandler', { fn:makeCallback('stop right here'), priority:42 });
            $(document).trigger('registerBeforeUnloadHandler', makeCallback());
            $(document).trigger('registerBeforeUnloadHandler', { fn:makeCallback(), priority:4 });

            var evt = {};
            this.component.onDocumentUnload(evt).should.be.eql('stop right here');
            evt.should.have.property('returnValue').that.eqls('stop right here');
            step.should.be.eql(3);
        });
    });
    
    describe('onRegisterBeforeUnloadHandler', function() {
        it('should do nothing if a callback is not provided', function() {
            $(document).trigger('registerBeforeUnloadHandler');
            this.component.getUnloadHandlers().should.be.empty;
        });

        it('should allow registering a bare function', function() {
            $(document).trigger('registerBeforeUnloadHandler', firstCallback);
            this.component.getUnloadHandlers().should.have.length(1);
            this.component.getUnloadHandlers()[0].should.have.property('fn').that.equals(firstCallback);
            this.component.getUnloadHandlers()[0].should.have.property('priority').that.is.a('number');
        });

        it('should allow registering a function only', function() {
            $(document).trigger('registerBeforeUnloadHandler', { fn:firstCallback });
            this.component.getUnloadHandlers().should.have.length(1);
            this.component.getUnloadHandlers()[0].should.have.property('fn').that.equals(firstCallback);
            this.component.getUnloadHandlers()[0].should.have.property('priority').that.is.a('number');
        });

        it('should allow registering a function and priority', function() {
            $(document).trigger('registerBeforeUnloadHandler', { fn:firstCallback, priority:42 });
            this.component.getUnloadHandlers().should.have.length(1);
            this.component.getUnloadHandlers()[0].should.have.property('fn').that.equals(firstCallback);
            this.component.getUnloadHandlers()[0].should.have.property('priority').that.eqls(42);
        });
        
        it('should allow ignore duplicate registrations', function() {
            $(document).trigger('registerBeforeUnloadHandler', firstCallback);
            this.component.getUnloadHandlers().should.have.length(1);
            this.component.getUnloadHandlers()[0].should.have.property('fn').that.equals(firstCallback);

            $(document).trigger('registerBeforeUnloadHandler', firstCallback);
            this.component.getUnloadHandlers().should.have.length(1);
        });

        it('should allow keep the queue of callbacks properly ordered', function() {
            $(document).trigger('registerBeforeUnloadHandler', { fn:firstCallback, priority:42 });
            $(document).trigger('registerBeforeUnloadHandler', secondCallback);
            $(document).trigger('registerBeforeUnloadHandler', { fn:thirdCallback, priority:4 });
            this.component.getUnloadHandlers().should.have.length(3);
            _.pluck(this.component.getUnloadHandlers(), 'fn').should.eql([thirdCallback, firstCallback, secondCallback]);
        });
    });
    
    describe('onUnregisterBeforeUnloadHandler', function() {
        beforeEach(function() {
            this.component.getUnloadHandlers().push({ fn:firstCallback}, { fn:secondCallback});
        });

        it('should do nothing if there are no callbacks registered', function() {
            this.component.clearUnloadHandlers();
            $(document).trigger('unregisterBeforeUnloadHandler', thirdCallback);
            this.component.getUnloadHandlers().should.be.empty;
        });
        
        it('should do nothing if a callback is not provided', function() {
            $(document).trigger('unregisterBeforeUnloadHandler');
            this.component.getUnloadHandlers().should.have.length(2);
            this.component.getUnloadHandlers()[0].should.have.property('fn').that.equals(firstCallback);
            this.component.getUnloadHandlers()[1].should.have.property('fn').that.equals(secondCallback);
        });

        it('should do nothing if the callback is not found', function() {
            $(document).trigger('unregisterBeforeUnloadHandler', thirdCallback);
            this.component.getUnloadHandlers().should.have.length(2);
            this.component.getUnloadHandlers()[0].should.have.property('fn').that.equals(firstCallback);
            this.component.getUnloadHandlers()[1].should.have.property('fn').that.equals(secondCallback);
        });

        it('should allow unregistering a bare function', function() {
            $(document).trigger('unregisterBeforeUnloadHandler', firstCallback);
            this.component.getUnloadHandlers().should.have.length(1);
            this.component.getUnloadHandlers()[0].should.have.property('fn').that.equals(secondCallback);
        });

        it('should allow unregistering a function in an object', function() {
            $(document).trigger('unregisterBeforeUnloadHandler', { fn:firstCallback });
            this.component.getUnloadHandlers().should.have.length(1);
            this.component.getUnloadHandlers()[0].should.have.property('fn').that.equals(secondCallback);
        });
    });
});
