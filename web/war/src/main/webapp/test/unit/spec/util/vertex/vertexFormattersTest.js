
define(['util/vertex/formatters'], function(f) {
    var V = f.vertex,
        VERTEX_ERROR = 'Vertex is invalid',
        PROPERTY_NAME_ERROR = 'Property name is invalid',
        PROPERTY_NAME_FIRST = 'http://lumify.io/dev#firstName',
        PROPERTY_NAME_LAST = 'http://lumify.io/dev#lastName',
        PROPERTY_NAME_TITLE = 'http://lumify.io#title',
        PROPERTY_NAME_BOOLEAN = 'http://lumify.io/dev#boolean',
        PROPERTY_NAME_DOUBLE = 'http://lumify.io/dev#duration',
        PROPERTY_NAME_DATE = 'http://lumify.io/dev#dateOnly',
        PROPERTY_NAME_DATETIME = 'http://lumify.io/dev#dateAndTime',
        PROPERTY_NAME_HEADING = 'http://lumify.io/testing#heading1',
        PROPERTY_NAME_INTEGER = 'http://lumify.io/testing#integer1',
        PROPERTY_NAME_NUMBER = 'http://lumify.io/testing#number1',
        PROPERTY_NAME_CURRENCY = 'http://lumify.io/dev#netIncome',
        PROPERTY_NAME_GENDER = 'http://lumify.io/dev#gender',
        PROPERTY_NAME_GEO = 'http://lumify.io/dev#geolocation',
        PROPERTY_NAME_CONCEPT = 'http://lumify.io#conceptType',
        COMPOUND_PROPERTY_NAME = 'http://lumify.io/dev#name',
        COMPOUND_TEST_PROPERTY_NAME = 'http://lumify.io/testing#compound1',

        keyIdent = 0,
        vertexIdent = 0,
        addMetadata = function(property, key, value) {
            var newProp = _.extend({}, property);
            if (!newProp.metadata) {
                newProp.metadata = {};
            }
            newProp.metadata[key] = value;
            return newProp;
        },
        created = function(property, date) {
            return addMetadata(property, 'http://lumify.io#createDate', date.getTime());
        },
        confidence = function(property, confidence) {
            return addMetadata(property, 'http://lumify.io#confidence', confidence);
        },
        propertyFactory = function(name, optionalKey, value) {
            if (arguments.length === 2) {
                value = optionalKey;
                optionalKey = null;
            }
            return {
                name: name,
                key: optionalKey || ('pKey' + keyIdent++),
                value: value
            };
        },
        vertexFactory = function(id, properties) {
            if (_.isObject(id)) {
                properties = id;
                id = null;
            }
            return {
                id: id || ('testVertex' + vertexIdent++),
                properties: properties || []
            }
        }

    describe('vertex formatters', function() {

        describe('prop', function() {
            it('should have prop function', function() {
                expect(V).to.have.property('prop').that.is.a.function
            })
            it('should get display values for boolean', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_BOOLEAN, 'k1', true),
                        propertyFactory(PROPERTY_NAME_BOOLEAN, 'k2', false),
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_BOOLEAN);

                expect(prop()).to.equal('boolean.true')
                expect(prop('k2')).to.equal('boolean.false')
            })
            it('should get display values for numbers', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_DOUBLE, 'k1', 22 / 7),
                        propertyFactory(PROPERTY_NAME_DOUBLE, 'k2', 1000000000000),
                        propertyFactory(PROPERTY_NAME_NUMBER, 'k3', 2),
                        propertyFactory(PROPERTY_NAME_INTEGER, 'k4', 3),
                        propertyFactory(PROPERTY_NAME_CURRENCY, 'k5', 4),
                    ]),
                    prop = _.partial(V.prop, vertex);

                expect(prop(PROPERTY_NAME_DOUBLE)).to.equal('3.14')
                expect(prop(PROPERTY_NAME_DOUBLE, 'k2')).to.equal('1,000,000,000,000')
                expect(prop(PROPERTY_NAME_NUMBER, 'k3')).to.equal('2')
                expect(prop(PROPERTY_NAME_INTEGER, 'k4')).to.equal('3')
                expect(prop(PROPERTY_NAME_CURRENCY, 'k5')).to.equal('4')
            })
            it('should get display values for dates no time', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_DATE, new Date(2015, 01, 9).getTime()),
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_DATE);

                expect(prop()).to.equal('2015-02-09')
            })
            it('should get display values for dates with time', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_DATETIME, new Date(2015, 01, 9, 8, 42).getTime()),
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_DATETIME);

                expect(prop()).to.include('2015-02-09 08:42')
            })
            it('should get display values for heading', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_HEADING, 123),
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_HEADING);

                expect(prop()).to.equal('field.heading.southeast 123°')
            })
            it('should get display values for gender (possibleValues)', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_GENDER, 'M'),
                        propertyFactory(PROPERTY_NAME_GENDER, 'k1', 'F'),
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_GENDER);

                expect(prop()).to.equal('Male')
                expect(prop('k1')).to.equal('Female')
            })
            it('should get display values for geolocation', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_GEO, 'point[80,-40]'),
                        propertyFactory(PROPERTY_NAME_GEO, 'k1', {
                            latitude: 82.3413,
                            longitude: -43.2326
                        })
                    ]),
                    prop = _.partial(V.prop, vertex, PROPERTY_NAME_GEO);

                expect(prop()).to.equal('80.000, -40.000')
                expect(prop('k1')).to.equal('82.341, -43.233')
            })
            it('should get display compound properties', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'jason'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k2', 'harwig')
                    ]),
                    prop = _.partial(V.prop, vertex, COMPOUND_PROPERTY_NAME);

                expect(prop()).to.equal('undefined, jason')
                expect(prop('k1')).to.equal('harwig, jason')
                expect(prop('k2')).to.equal('harwig, undefined')
            })
        })

        describe('props', function() {
            it('should throw error if invalid property name', function() {
                expect(V.props.bind(null, vertexFactory())).to.throw(PROPERTY_NAME_ERROR)
            });
            it('should throw error if invalid vertex', function() {
                expect(V.props.bind(null, {})).to.throw(VERTEX_ERROR)
                expect(V.props.bind(null, {id:1})).to.throw(VERTEX_ERROR)
                expect(V.props.bind(null, {properties:[]})).to.throw(VERTEX_ERROR)
                expect(V.props.bind(null, {id:1,properties:1})).to.throw(VERTEX_ERROR)
            });
            it('should return all props for vertex', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'k2', 'jason2'),
                    ]),
                    value = V.props(vertex, PROPERTY_NAME_FIRST);

                expect(value).to.be.an('array').and.have.property('length').that.equals(2);
                expect(value[0].value).to.equal('jason')
                expect(value[1].value).to.equal('jason2')
            })
            it('should return single property for vertex when key provided', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'k2', 'jason2'),
                    ]),
                    property = V.props(vertex, PROPERTY_NAME_FIRST, 'k2');

                expect(property.value).to.equal('jason2')
            })
            it('should return undefined for vertex when key provided and no match', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'k2', 'jason2'),
                    ]),
                    property = V.props(vertex, PROPERTY_NAME_FIRST, 'k3');

                expect(property).to.be.undefined
            })
            it('should throw error if key matches multiple', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason2')
                    ]);

                expect(function() {
                    V.props(vertex, PROPERTY_NAME_FIRST, 'k1');
                }).to.throw('multiple properties with same name')
            })
            it('should throw error if key is passed but is undefined', function() {
                expect(function() {
                    V.props(vertexFactory(), PROPERTY_NAME_FIRST, undefined);
                }).to.throw('Undefined key')
            })
        })

        describe('longestProp', function() {
            if ('should return only userVisible properties', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://lumify.io/dev#person')
                    ]);

                expect(V.longestProp(vertex)).to.be.undefined
            })
            it('should return longest userVisible property value if no params', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'a'),
                        propertyFactory(PROPERTY_NAME_LAST, 'aa'),
                    ]);

                expect(V.longestProp(vertex)).to.equal('aa')
            })
            it('should return longest userVisible property value restricted to name', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'a'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'aa'),
                        propertyFactory(PROPERTY_NAME_LAST, 'last longer'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'aaa'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'bbb'),
                    ]);

                expect(V.longestProp(vertex, PROPERTY_NAME_FIRST)).to.equal('aaa')
            })
            it('should return undefined if no properties', function() {
                var vertex = vertexFactory([]);
                expect(V.longestProp(vertex)).to.be.undefined
            })
            it('should return undefined if no properties matching name', function() {
                var vertex = vertexFactory([
                    propertyFactory(PROPERTY_NAME_FIRST, 'a')
                ]);
                expect(V.longestProp(vertex, PROPERTY_NAME_LAST)).to.be.undefined
            })
        })

        describe('title', function() {
            it('should get a title even if it refers to compound property', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'k1', 'harwig'),
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://lumify.io/dev#person')
                    ]);

                expect(V.title(vertex)).to.equal('harwig, jason')
            })
        })

        describe('propFromAudit', function() {
            it('should format values for audit')
        })

        describe('propRaw', function() {
            it('should have propRaw function', function() {
                expect(V).to.have.property('propRaw').that.is.a.function
            })

            it('should expand property name', function() {
                var vertex = vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'jason'),
                        propertyFactory(PROPERTY_NAME_LAST, 'harwig'),
                        propertyFactory(PROPERTY_NAME_CONCEPT, 'http://lumify.io/dev#person')
                    ]);

                expect(V.propRaw(vertex, 'conceptType')).to.equal('http://lumify.io/dev#person')
            })

            it('should get prop values', function() {
                var value = V.propRaw(
                    vertexFactory([
                        propertyFactory(PROPERTY_NAME_FIRST, 'jason'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'jason1'),
                        propertyFactory(PROPERTY_NAME_FIRST, 'jason2')
                    ]),
                    PROPERTY_NAME_FIRST
                )
                expect(value).to.equal('jason')
            })

            it('should return undefined if no default value', function() {
                var vertex = vertexFactory(),
                    value = V.propRaw(vertex, PROPERTY_NAME_FIRST);

                expect(value).to.equal(undefined)
            })

            it('should return default if passed defaultValue', function() {
                var vertex = vertexFactory(),
                    defaultValue = 'defaultValueTest',
                    value = V.propRaw(vertex, PROPERTY_NAME_FIRST, null, {
                        defaultValue: defaultValue
                    });

                expect(value).to.equal(defaultValue)
            })

            it('should accept no key but with options', function() {
                var vertex = vertexFactory(),
                    defaultValue = 'defaultValueTest',
                    value = V.propRaw(vertex, PROPERTY_NAME_FIRST, {});

                expect(value).to.equal(undefined)
            })

            it('should throw error if vertex is invalid', function() {
                expect(_.partial(V.propRaw, null, 'title')).to.throw(VERTEX_ERROR)
                expect(_.partial(V.propRaw, {}, 'title')).to.throw(VERTEX_ERROR)
                expect(_.partial(V.propRaw, { properties: null }, 'title')).to.throw(VERTEX_ERROR)
                expect(_.partial(V.propRaw, { id: 'testing' }, 'title')).to.throw(VERTEX_ERROR)
            })

            it('should throw error if name is invalid', function() {
                var vertex = vertexFactory();
                expect(_.partial(V.propRaw, vertex)).to.throw(PROPERTY_NAME_ERROR)
                expect(_.partial(V.propRaw, vertex, {})).to.throw(PROPERTY_NAME_ERROR)
                expect(_.partial(V.propRaw, vertex, '')).to.throw(PROPERTY_NAME_ERROR)
            })

            it('should get property with most confidence', function() {
                var vertex = vertexFactory([
                    confidence(propertyFactory(PROPERTY_NAME_FIRST, 'first'), 0.5),
                    confidence(propertyFactory(PROPERTY_NAME_FIRST, 'most confident'), 0.6),
                    confidence(propertyFactory(PROPERTY_NAME_FIRST, 'middle'), 0.55)
                ]);

                //_justificationMetadata: 0.5
                //http://lumify.io#createDate: 1421344394956
                //http://lumify.io#createdBy: "USER_fd588514ce824ee7a35668046447512d"
                //http://lumify.io#modifiedBy: "USER_fd588514ce824ee7a35668046447512d"
                //http://lumify.io#modifiedDate: 1421344394956
                //http://lumify.io#visibilityJson: Object

                expect(V.propRaw(vertex, PROPERTY_NAME_FIRST)).to.equal('most confident')
            })
            it('should get property with most confidence above those with no confidence', function() {
                var vertex = vertexFactory([
                    propertyFactory(PROPERTY_NAME_FIRST, 'first'),
                    confidence(propertyFactory(PROPERTY_NAME_FIRST, 'most confident'), 0.6),
                    confidence(propertyFactory(PROPERTY_NAME_FIRST, 'middle'), 0.55)
                ]);
                expect(V.propRaw(vertex, PROPERTY_NAME_FIRST)).to.equal('most confident')
            })
            it('should get property with most confidence even if created earliest', function() {
                var vertex = vertexFactory([
                    created(propertyFactory(PROPERTY_NAME_FIRST, 'recent'), new Date(2015, 03, 01)),
                    created(
                        confidence(propertyFactory(PROPERTY_NAME_FIRST, 'most confident'), 0.6),
                        new Date(2015, 01, 01)
                    ),
                    created(
                        confidence(propertyFactory(PROPERTY_NAME_FIRST, 'middle'), 0.55),
                        new Date(2015, 02, 01)
                    )
                ]);
                expect(V.propRaw(vertex, PROPERTY_NAME_FIRST)).to.equal('most confident')
            })
            it('should get property most recently edited when confidence same', function() {
                var vertex = vertexFactory([
                    created(propertyFactory(PROPERTY_NAME_FIRST, 'recent'), new Date(2015, 03, 01)),
                    created(
                        confidence(propertyFactory(PROPERTY_NAME_FIRST, 'b confident'), 0.6),
                        new Date(2015, 01, 01)
                    ),
                    created(confidence(propertyFactory(PROPERTY_NAME_FIRST, 'a'), 0.6), new Date(2015, 02, 01))
                ]);
                expect(V.propRaw(vertex, PROPERTY_NAME_FIRST)).to.equal('a')
            })

            describe('Compound properties', function() {

                it('should handle compound properties and return array of values', function() {
                    var sharedKey = 'A',
                        vertex = vertexFactory([
                            // lastname defined first in ontology for /dev#name
                            propertyFactory(PROPERTY_NAME_LAST, sharedKey, 'smith'),
                            propertyFactory(PROPERTY_NAME_FIRST, sharedKey, 'john')
                        ]),
                        values = V.propRaw(vertex, COMPOUND_PROPERTY_NAME);

                    expect(values).to.be.an('array').that.has.property('length').that.equals(2)
                    expect(values[0]).to.equal(vertex.properties[0].value)
                    expect(values[1]).to.equal(vertex.properties[1].value)
                })

                it('should handle pairing compound properties', function() {
                    var vertex = vertexFactory([
                            propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                            propertyFactory(PROPERTY_NAME_LAST, 'k2', 'smith'),
                            propertyFactory(PROPERTY_NAME_FIRST, 'k2', 'john')
                        ]),
                        values = V.propRaw(vertex, COMPOUND_PROPERTY_NAME);

                    expect(values).to.be.an('array').that.has.property('length').that.equals(2)
                    expect(values[0]).to.be.undefined
                    expect(values[1]).to.equal('jason')

                    values = V.propRaw(vertex, COMPOUND_PROPERTY_NAME, 'k2');
                    expect(values[0]).to.equal('smith')
                    expect(values[1]).to.equal('john')
                })

                it('should handle getting highest confidence compound property', function() {
                    var vertex = vertexFactory([
                            propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                            confidence(propertyFactory(PROPERTY_NAME_LAST, 'k2', 'smith'), 0.5),
                            confidence(propertyFactory(PROPERTY_NAME_FIRST, 'k2', 'john'), 0.5)
                        ]),
                        values = V.propRaw(vertex, COMPOUND_PROPERTY_NAME);

                    expect(values).to.be.an('array').that.has.property('length').that.equals(2)
                    expect(values[0]).to.equal('smith')
                    expect(values[1]).to.equal('john')

                    values = V.propRaw(vertex, COMPOUND_PROPERTY_NAME, 'k1');
                    expect(values[0]).to.be.undefined
                    expect(values[1]).to.equal('jason')
                })

                it('should throw errors for compound that depends on compound', function() {
                    var vertex = vertexFactory([
                            propertyFactory(PROPERTY_NAME_TITLE, 'jason'),
                            propertyFactory(PROPERTY_NAME_FIRST, 'k1', 'jason'),
                            propertyFactory(PROPERTY_NAME_LAST, 'k1', 'smith'),
                        ]);

                    expect(function() {
                        V.propRaw(vertex, COMPOUND_TEST_PROPERTY_NAME);
                    }).to.throw('compound properties')
                })
            })
        })
    });
});
