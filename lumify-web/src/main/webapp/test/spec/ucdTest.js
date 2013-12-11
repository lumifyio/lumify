

define(['service/ucd'], function(UCD) {

    describe('UCD', function() {

        it('should exist', function() {
            expect(UCD).to.be.a('function');
        });

		it('should have some functions', function () {
			var ucd = new UCD({});
			expect(ucd.artifactSearch).to.be.a("function");
			expect(ucd.graphVertexSearch).to.be.a("function");
			expect(ucd.getArtifactById).to.be.a("function");
			expect(ucd.getVertexProperties).to.be.a("function");
		});

    });

});
