
define(['util/formatters'], function(f) {

    describe('formatters', function() {

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

        describe('for strings', function() {

            it('should be able to format plural phrases with plural provided', function() {
                f.string.plural(0, 'phrase', 'x').should.equal('No x')
                f.string.plural(1, 'phrase', 'x').should.equal('1 phrase')
                f.string.plural(2, 'phrase', 'x').should.equal('2 x')
            })

            it('should be able to format plural phrases automatically without plural provided', function() {
                f.string.plural(0, 'phrase').should.equal('No phrases')
                f.string.plural(1, 'phrase').should.equal('1 phrase')
                f.string.plural(2, 'phrase').should.equal('2 phrases')
            })

        })

        describe('for geoLocations', function() {

            it('should be able to parse', function() {
                var result = f.geoLocation.parse('point[123.32,-43.66]')

                expect(result).to.exist;

                result.should.have.property('latitude').equal('123.32')
                result.should.have.property('longitude').equal('-43.66')
            })

            it('should return undefined if no match', function() {
                expect(f.geoLocation.parse('geoPoint')).to.be.undefined;
            })
        })


        describe('for dates', function() {

            it('should format to prefered format', function() {
                var now = new Date();

                var month = String(now.getMonth() + 1);
                if (month.length === 1) month = '0' + month;

                var day = String(now.getDate());
                if (day.length === 1) day = '0' + day;

                f.date.dateString(now.getTime()).should.equal(now.getFullYear() + '-' + month + '-' + day);

                f.date.dateString('' + now.getTime()).should.equal(now.getFullYear() + '-' + month + '-' + day);

                f.date.dateString(now).should.equal(now.getFullYear() + '-' + month + '-' + day);
            })

            shouldBeRelative({seconds:30}, 'moments ago')
            shouldBeRelative({seconds:59}, 'moments ago')
            shouldBeRelative({seconds:60}, 'a minute ago')
            shouldBeRelative({minutes:2}, '2 minutes ago')
            shouldBeRelative({minutes:60}, 'an hour ago')
            shouldBeRelative({minutes:110}, 'an hour ago')
            shouldBeRelative({hours:2}, '2 hours ago')
            shouldBeRelative({hours:23}, '23 hours ago')
            shouldBeRelative({hours:24}, 'a day ago')
            shouldBeRelative({days:2}, '2 days ago')
            shouldBeRelative({days:40}, 'a month ago')
            shouldBeRelative({days:60}, '2 months ago')
            shouldBeRelative({days:363}, '12 months ago')
            shouldBeRelative({days:367}, 'a year ago')
            shouldBeRelative({days:366*2}, '2 years ago')

            function shouldBeRelative(time, string) {
                var unit = _.keys(time)[0],
                    mult = { };

                mult.seconds = 1000;
                mult.minutes = mult.seconds * 60;
                mult.hours = mult.minutes * 60;
                mult.days = mult.hours * 24;
                mult.years = mult.days * 365;

                it('should format ' + time[unit] + ' ' + unit + ' relative to now as ' + string, function() {
                    var now = new Date(),
                        nowUtc = f.date.utc(now),
                        older = new Date(nowUtc.getTime() - (mult[unit] * time[unit]));

                    var old = Date.now;
                    Date.now = function() {return now;}
                    f.date.relativeToNow(older).should.equal(string)
                    Date.now = old;
                })
            }
        })

        describe('className', function() {

            it('should be able to transform to and from className', function() {
                var str = 's$#!!3456';

                f.className.to(str).should.equal('id0')
                f.className.from('id0').should.equal(str)

                f.className.to(str + '2').should.equal('id1')
                f.className.from('id1').should.equal(str + '2')
            })

        })
    });
});
