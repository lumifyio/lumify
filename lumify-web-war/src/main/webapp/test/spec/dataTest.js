
define(['data'], function(data) {

    describe.only('data', function() {

        it('should bind to document node', function() {
            expect(data.node).to.equal(document);
        });

        it('should register keyboard shortcuts', function(wait) {
            data.on('registerKeyboardShortcuts', function(e, d) {
                expect(d.shortcuts['meta-a'].fire).to.equal('selectAll');
                expect(d.shortcuts['delete'].fire).to.equal('deleteSelected');
                wait();
            });
            
            data.trigger('applicationReady');
        });
    });
});
