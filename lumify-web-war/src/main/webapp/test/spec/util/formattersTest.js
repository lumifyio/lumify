
define(['util/formatters'], function(f) {

    describe.only('formatters', function() {

        describe('for numbers', function() {

            it('should have pretty function', function() {
                expect(f.number.pretty(0)).to.equal('0');
                expect(f.number.pretty(1)).to.equal('1');
                expect(f.number.pretty(1321)).to.equal('1,321');
                expect(f.number.pretty(12321)).to.equal('12,321');

                expect(f.number.pretty(1123456)).to.equal('1,123,456');
                expect(f.number.pretty(1123456789)).to.equal('1,123,456,789');
            })

            it('should have prettyApproximate function', function() {
                expect(f.number.prettyApproximate(0)).to.equal('0');
                expect(f.number.prettyApproximate(1)).to.equal('1');

                expect(f.number.prettyApproximate(1024)).to.equal('1K');
                expect(f.number.prettyApproximate(1100)).to.equal('1.1K');
                expect(f.number.prettyApproximate(1150)).to.equal('1.2K');

                expect(f.number.prettyApproximate(10000)).to.equal('10K');
                expect(f.number.prettyApproximate(10550)).to.equal('10.6K');

                expect(f.number.prettyApproximate(100000)).to.equal('100K');

                expect(f.number.prettyApproximate(1000000)).to.equal('1M');
                expect(f.number.prettyApproximate(6235987)).to.equal('6.2M');

                expect(f.number.prettyApproximate(1000000000)).to.equal('1B');
                expect(f.number.prettyApproximate(6740000000)).to.equal('6.7B');
            })
        });
    });
});
