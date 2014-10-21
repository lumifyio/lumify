
describeMixin('util/withAsyncQueue', function() {

    var MY_QUEUE_OBJECT = { valid: true };

    beforeEach(function() {
        setupComponent(this);
    })

    it('should provide implementations', function() {
        this.component.setupAsyncQueue('myQueue');
        this.component.should.have.property('myQueueIsReady').that.is.a.function
        this.component.should.have.property('myQueueMarkReady').that.is.a.function
        this.component.should.have.property('myQueueReady').that.is.a.function
        this.component.should.have.property('myQueueUnload').that.is.a.function
    })

    it('should give error if marked ready with nothing', function() {
        this.component.setupAsyncQueue('myQueue');
        expect(this.component.myQueueMarkReady).to.throw(Error)
    })

    it('should give be able to unload', function() {
        this.component.setupAsyncQueue('myQueue');
        this.component.myQueueIsReady().should.be.false
        this.component.myQueueMarkReady(MY_QUEUE_OBJECT)
        this.component.myQueueIsReady().should.be.true
        this.component.myQueueUnload();
        this.component.myQueueIsReady().should.be.false
    })

    it('should be able mark ready, then wait for ready with callback', function(done) {
        this.component.setupAsyncQueue('myQueue');
        this.component.myQueueIsReady().should.be.false
        this.component.myQueueMarkReady(MY_QUEUE_OBJECT)
        this.component.myQueueIsReady().should.be.true

        this.component.myQueueReady(function(obj)  {
            obj.should.equal(MY_QUEUE_OBJECT)

            done();
        });
    })

    it('should be able mark ready, after waiting for ready with callback', function(done) {
        this.component.setupAsyncQueue('myQueue');
        this.component.myQueueReady(function(obj)  {
            obj.should.equal(MY_QUEUE_OBJECT)

            done();
        });
        this.component.myQueueMarkReady(MY_QUEUE_OBJECT)
    })

    it('should be able mark ready, then wait for ready with promise', function(done) {
        this.component.setupAsyncQueue('myQueue');
        this.component.myQueueMarkReady(MY_QUEUE_OBJECT)
        this.component.myQueueReady().done(function(obj)  {
            obj.should.equal(MY_QUEUE_OBJECT)

            done();
        });

    })

    it('should be able mark ready, after waiting for ready with promise', function(done) {
        this.component.setupAsyncQueue('myQueue');
        this.component.myQueueReady().done(function(obj)  {
            obj.should.equal(MY_QUEUE_OBJECT)

            done();
        });
        this.component.myQueueMarkReady(MY_QUEUE_OBJECT)
    })
});
