
define(['data/web-worker/services/vertex'], function(vertex) {

    describe('Service', function() {

        it('should exist', function() {
            expect(vertex).to.be.a('object');
        });

		it('should have some functions', function() {
			expect(vertex.search).to.be.a('function');
			expect(vertex.properties).to.be.a('function');
		});

    });

});
