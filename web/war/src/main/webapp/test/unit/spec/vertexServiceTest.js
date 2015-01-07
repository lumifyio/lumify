
define(['service/vertex'], function(Service) {

    describe('Service', function() {

        it('should exist', function() {
            expect(Service).to.be.a('function');
        });

		it('should have some functions', function() {
			var service = new Service({});
			expect(service.search).to.be.a('function');
			expect(service.getVertexProperties).to.be.a('function');
		});

    });

});
