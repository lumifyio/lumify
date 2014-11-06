
describeMixin('util/withDataRequest', function() {

    beforeEach(function() {
        setupComponent(this)
    })

    describe('Data Requests', function() {

        it('should provide dataRequest method', function() {
            this.component
                .should.have.property('dataRequest')
                .that.is.a.function
        })

        it('should trigger event on dataRequest with no params and timeout if not responded to', function(done) {
            this.component.on('dataRequest', function(event, data) {
                data.requestId.should.be.empty
                data.service.should.equal('serviceName')
                data.method.should.equal('methodName')
                data.should.have.property('parameters').that.is.a.array
                data.parameters.should.be.empty

            })
            this.component.on(document, 'dataRequestCompleted', function(event, data) {
                data.success.should.be.false
                data.error.should.not.be.empty
                done()
            })
            this.component.dataRequest('serviceName', 'methodName')
        })

        it('should not timeout if responded to', function(done) {
            this.component.on('dataRequest', function(event, data) {
                this.trigger('dataRequestStarted', { requestId: 0 })
                _.defer(done)
            })
            this.component.dataRequest('serviceName', 'methodName')
                .fail(function() {
                    done(new Error('Should not timeout if request started'))
                })
        })

        it('should trigger event on dataRequest with params', function(done) {
            this.component.on('dataRequest', function(event, data) {
                data.should.have.property('parameters').that.is.a.array
                data.parameters.should.deep.equal(['first', 2])
                done()
            })
            this.component.dataRequest('_', '_', 'first', 2)
        })

        it('should return a promise', function() {
            var promise = this.component.dataRequest('_', '_', 'first', 2)

            promise.should.have.property('done').that.is.a.function
            promise.should.have.property('fail').that.is.a.function
            promise.should.have.property('always').that.is.a.function
        })

        it('should call resolve promise on success', function(done) {
            this.component.dataRequest('_', '_', 'first', 2)
                .done(function(result) {
                    result.should.equal(1)
                    done()
                })

            this.component.trigger('dataRequestCompleted', {
                success: true,
                result: 1,
                requestId: 0
            })
        })

        it('should call resolve promise on failure', function(done) {
            this.component.dataRequest('_', '_', 'first', 2)
                .done(function() {
                    done(new Error('Should not be success'))
                })
                .fail(function(error) {
                    error.should.equal('my error')
                    done()
                })

            this.component.trigger('dataRequestCompleted', {
                success: false,
                error: 'my error',
                requestId: 0
            })
        })

        it('should trigger cancel event on promise cancel', function(done) {
            this.component.on('dataRequestCancel', function(event, data) {
                _.defer(done)
            })

            var promise = this.component.dataRequest('_', '_', 'first', 2)
            promise.should.have.property('cancel').that.is.a.function

            promise.always(function() {
                done(new Error("Promise callbacks shouldn't be called after cancel"))
            })
            promise.cancel()
        })
    })
});
