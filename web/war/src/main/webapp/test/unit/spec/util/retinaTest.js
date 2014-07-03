
define(['util/retina'], function(r) {

    describe('retina', function() {

        var xValue = 10,
            yValue = xValue + 2,
            zValue = yValue + 2,
            wValue = zValue + 2,
            hValue = wValue + 2,
            values = {
                xValue: xValue,
                yValue: yValue,
                zValue: zValue,
                wValue: wValue,
                hValue: hValue
            },
            inputs = {
                onlyX: { x: xValue },
                onlyY: { y: yValue },
                onlyZ: { z: zValue },
                onlyW: { w: wValue },
                onlyH: { h: hValue },
                position: { x: xValue, y: yValue },
                position3: { x: xValue, y: yValue, z: zValue },
                size: { w: wValue, h: hValue }
            },
            checkProperties = function(f, transformValue, ratio) {
                var oldRatio = r.devicePixelRatio;
                r.devicePixelRatio = ratio;
                _.keys(inputs).forEach(function(input) {
                    var result = f(inputs[input])

                    switch (input) {
                        case 'position':
                            expect(result.w).to.be.undefined
                            expect(result.h).to.be.undefined
                            expect(result.z).to.be.undefined
                            result.x.should.equal(transformValue(xValue))
                            result.y.should.equal(transformValue(yValue))
                            break;

                        case 'position3':
                            expect(result.w).to.be.undefined
                            expect(result.h).to.be.undefined
                            result.x.should.equal(transformValue(xValue))
                            result.y.should.equal(transformValue(yValue))
                            result.z.should.equal(transformValue(zValue))
                            break;

                        case 'size':
                            expect(result.x).to.be.undefined
                            expect(result.y).to.be.undefined
                            expect(result.z).to.be.undefined
                            result.w.should.equal(transformValue(wValue))
                            result.h.should.equal(transformValue(hValue))
                            break;

                        default:
                            var propertyName = input.substring(4).toLowerCase();
                            result[propertyName].should.equal(transformValue(values[propertyName + 'Value']))
                    }
                })
                r.devicePixelRatio = oldRatio;
            };

        describe('pointsToPixels', function() {
            var f = r.pointsToPixels,
                transformer = function(v) {
                    return v * 2;
                },
                identity = function(v) {
                    return v;
                },
                check = checkProperties.bind(null, f)

            it('should have function', function() {
                expect(f).to.be.a.function
            })

            it('should accept undefined argument', function() {
                var result = f()
                result.should.have.property('x').that.equals(0)
                result.should.have.property('y').that.equals(0)
            })

            it('should modify all valid properties when dpr = 1', function() {
                check(identity, 1)
            })

            it('should modify all valid properties when dpr = 2', function() {
                check(transformer, 2)
            })
        })

        describe('pixelsToPoints', function() {
            var f = r.pixelsToPoints,
                transformer = function(v) {
                    return v / 2;
                },
                identity = function(v) {
                    return v;
                },
                check = checkProperties.bind(null, f)

            it('should have function', function() {
                expect(f).to.be.a.function
            })

            it('should accept undefined argument', function() {
                var result = f()
                result.should.have.property('x').that.equals(0)
                result.should.have.property('y').that.equals(0)
            })

            it('should modify all valid properties when dpr = 1', function() {
                check(identity, 1)
            })

            it('should modify all valid properties when dpr = 2', function() {
                check(transformer, 2)
            })
        })
    })
})
