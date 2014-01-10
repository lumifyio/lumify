

define(['service/vertex'], function(Service) {

    describe('Service', function() {

        it('should exist', function() {
            expect(Service).to.be.a('function');
        });

		it('should have some functions', function () {
			var service = new Service({});
			expect(service.artifactSearch).to.be.a("function");
			expect(service.graphVertexSearch).to.be.a("function");
			expect(service.getArtifactById).to.be.a("function");
			expect(service.getVertexProperties).to.be.a("function");
		});

    });

});
