describeMixin('data/withPendingChanges', function() {
    beforeEach(function() {
        var self = this;
        setupComponent(this);
    });

    describe('onAjaxSend', function() {
        xit('should not count a get request', function() {
            this.component.onAjaxSend(null, null, { type: 'GET' });
            this.component.pendingChangesPresent().should.be.false;
        });

        xit('should count a successful post request', function() {
            var promise = $.Deferred();
            this.component.onAjaxSend(null, promise, { type: 'POST' });
            this.component.pendingChangesPresent().should.be.true;
            
            promise.resolve();
            this.component.pendingChangesPresent().should.be.false;
        });

        xit('should count a failed post request', function() {
            var promise = $.Deferred();
            this.component.onAjaxSend(null, promise, { type: 'POST' });
            this.component.pendingChangesPresent().should.be.true;
            
            promise.reject();
            this.component.pendingChangesPresent().should.be.false;
        });
    });
    
    describe('onBeforeUnload', function() {
        xit('should not return anything if there are no changes', function() {
            expect(this.component.onBeforeUnload()).to.be.undefined;
        });

        xit('should return a warning message if there are ajax requests', function() {
            var promise = $.Deferred();
            this.component.onAjaxSend(null, promise, { type: 'POST' });
            this.component.onBeforeUnload().should.be.a('string');

            promise.resolve();
            expect(this.component.onBeforeUnload()).to.be.undefined;
        });
        
        xit('should return a warning message if there are workspace changes', function() {
            this.component.onSaveWorkspaceInternal();
            this.component.onBeforeUnload().should.be.a('string');
            
            this.component.trigger('workspaceSaving');
            expect(this.component.onBeforeUnload()).to.be.undefined;
        });        
    });
    
    xit('should still call the original onSaveWorkspaceInternal', function() {
        this.component.onSaveWorkspaceInternal();
        this.component.originalSaveCalled.should.be.true;
    });
});
