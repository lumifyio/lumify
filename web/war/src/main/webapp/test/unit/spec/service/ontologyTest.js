
define(['service/ontology'], function(OntologyService) {

    describe.only('OntologyService', function() {

        before(function(done) {
            this.service = new OntologyService();
            this.service.ontology().done(function() {
                done()
            })
        })

        describe('concepts', function() {

            it('should return concepts in multiple formats', function(done) {
                expect(this.service).to.have.property('concepts').that.is.a.function

                this.service.concepts()
                    .done(function(concepts) {

                        expect(concepts.byTitle).to.exist
                        expect(concepts.entityConcept).to.exist
                        expect(concepts.entityConcept.title).to.equal('http://www.w3.org/2002/07/owl#Thing')
                        expect(concepts.entityConcept.pluralDisplayName).to.equal('things')

                        done()
                    });
            })

            it('should add flattenedDisplayName properties', function(done) {
                this.service.concepts()
                    .done(function(concepts) {
                        var image = _.findWhere(concepts.byTitle, { id: 'http://lumify.io/dev#image' });

                        expect(image).to.exist
                        image.should.have.property('flattenedDisplayName').that.equals('Raw/Image')

                        done()
                    })
            })

            it('should set glyphicon based on parent if not defined', function(done) {
                this.service.concepts()
                    .done(function(concepts) {
                        var root = concepts.entityConcept,
                            raw = _.findWhere(root.children, { id: 'http://lumify.io/dev#raw' }),
                            image = _.findWhere(raw.children, { id: 'http://lumify.io/dev#image' });

                        root.should.have.property('children')
                        expect(raw).to.exist
                        expect(image).to.exist
                        expect(image).to.have.property('glyphIconHref').that.equals(
                            'resource?id=http%3A%2F%2Flumify.io%2Fdev%23raw'
                        )

                        var byIdImage = concepts.byId['http://lumify.io/dev#image'];
                        expect(byIdImage).to.exist
                        byIdImage.should.have.property('glyphIconHref').that.equals(
                            'resource?id=http%3A%2F%2Flumify.io%2Fdev%23raw'
                        )

                        var byTitleImage = _.findWhere(concepts.byTitle, { id: 'http://lumify.io/dev#image' })
                        expect(byTitleImage).to.exist
                        byTitleImage.should.have.property('glyphIconHref').that.equals(
                            'resource?id=http%3A%2F%2Flumify.io%2Fdev%23raw'
                        )
                        done()
                    })
            })

            it('should set displayType based on parent if not defined', function(done) {
                this.service.concepts()
                    .done(function(concepts) {
                        var root = concepts.entityConcept,
                            raw = _.findWhere(root.children, { id: 'http://lumify.io/dev#raw' }),
                            video = _.findWhere(raw.children, { id: 'http://lumify.io/dev#video' }),
                            videoSub = _.findWhere(video.children, { id: 'http://lumify.io/dev#videoSub' });

                        expect(video).to.exist
                        expect(videoSub).to.exist
                        expect(videoSub).to.have.property('displayType').that.equals('video')

                        var byId = concepts.byId['http://lumify.io/dev#videoSub'];
                        expect(byId).to.exist
                        byId.should.have.property('displayType').that.equals('video')

                        var byTitle = _.findWhere(concepts.byTitle, { id: 'http://lumify.io/dev#videoSub' })
                        expect(byTitle).to.exist
                        byTitle.should.have.property('displayType').that.equals('video')

                        done()
                    })
            })

            it('should leave glyphicon if set on concept', function(done) {
                this.service.concepts()
                    .done(function(concepts) {
                        var email = _.findWhere(concepts.byTitle, { id: 'http://lumify.io/dev#emailAddress' });

                        expect(email).to.exist
                        email.should.have.property('glyphIconHref')
                            .that.equals('resource?id=http%3A%2F%2Flumify.io%2Fdev%23emailAddress')

                        done()
                    })
            })

            it('should put color on concepts based on parent', function(done) {
                this.service.concepts()
                    .done(function(concepts) {
                        var person = _.findWhere(concepts.byTitle, { id: 'http://lumify.io/dev#person' });

                        expect(person).to.exist
                        person.should.have.property('color').that.equals('rgb(0, 0, 0)')

                        done()
                    })
            })

            it('should put class safe property on concepts', function(done) {
                this.service.concepts()
                    .done(function(concepts) {
                        var email = _.findWhere(concepts.byTitle, { id: 'http://lumify.io/dev#emailAddress' });

                        expect(email).to.exist

                        email.should.have.property('className')

                        var clsName = email.className,
                            byClsNameEmail = concepts.byClassName[clsName];

                        expect(byClsNameEmail).to.exist

                        done()
                    })
            })

        })

        describe('relationships', function() {

            it('should return relationships for concept types', function() {
                expect(this.service).to.have.property('relationships').that.is.a.function
            })

            shouldHaveRelationship('person', 'location', 'personLivesAtLocation')
            shouldHaveRelationship('person', 'country', 'personHasCitizenshipInCountry')
            shouldNotHaveRelationship('person', 'location', 'personHasCitizenshipInCountry')

            // Check child concepts that rely on a parent relationship
            // (location/city)
            shouldHaveRelationship('person', 'city', 'personLivesAtLocation')
            shouldHaveRelationship('person', 'country', 'personLivesAtLocation')
            shouldHaveRelationship('person', 'state', 'personLivesAtLocation')
            shouldHaveRelationship('person', 'city', 'personLivesAtLocation')
            shouldHaveRelationship('person', 'address', 'personLivesAtLocation')

            shouldHaveRelationship('company', 'city', 'organizationHeadquarteredAtLocation')
            shouldHaveRelationship('company', 'location', 'organizationHeadquarteredAtLocation')

            shouldHaveRelationship('nonprofit', 'location', 'organizationHeadquarteredAtLocation')
            shouldHaveRelationship('nonprofit', 'city', 'organizationHeadquarteredAtLocation')

            shouldHaveRelationship('organization', 'emailAddress', 'hasEmailAddress')
            shouldHaveRelationship('person', 'emailAddress', 'hasEmailAddress')

            // Check inverse not true
            shouldNotHaveRelationship('city', 'person', 'personLivesAtLocation')
            shouldNotHaveRelationship('location', 'person', 'personLivesAtLocation')

            function shouldHaveRelationship(sourceName, destName, title, negate) {
                sourceName = 'http://lumify.io/dev#' + sourceName;
                destName = 'http://lumify.io/dev#' + destName;
                title = 'http://lumify.io/dev#' + title;

                it('should' + (negate ? ' NOT' : '') +
                   ' have ' + title +
                   ' relationship from ' + sourceName + '->' + destName, function(done) {
                    var service = this.service;

                    service.concepts().done(function(concepts) {
                        var source = _.findWhere(concepts.byTitle, { title: sourceName }).id,
                            dest = _.findWhere(concepts.byTitle, { title: destName }).id

                        service.conceptToConceptRelationships(source, dest)
                            .done(function(relationships) {
                                var result = _.findWhere(relationships, { title: title });
                                if (negate) {
                                    expect(result).to.be.undefined
                                } else {
                                    expect(result).to.exist
                                }
                                done();
                            });
                    });
                })
            }
            function shouldNotHaveRelationship(s,d,t) {
                shouldHaveRelationship(s,d,t,true)
            }

        })

        describe('Properties', function() {
            it('should have properties', function() {
                expect(this.service).to.have.property('properties').that.is.a.function
            })

            it('should have properties by conceptId', function() {
                expect(this.service).to.have.property('propertiesByConceptId').that.is.a.function
            })

            shouldHaveProperties('company', ['netIncome', 'formationDate', 'abbreviation'])
            shouldHaveProperties('nonprofit', ['netIncome', 'formationDate', 'abbreviation'])
            shouldNotHaveProperties('organization', ['netIncome'])

            it('should have properties by relationshipLabel function', function() {
                expect(this.service).to.have.property('propertiesByRelationshipLabel').that.is.a.function
            })

            it('should return properties by relationship label', function(done) {
                this.service.propertiesByRelationshipLabel('http://lumify.io/dev#rawHasEntity')
                    .done(function(properties) {
                        expect(properties).to.have.property('list')
                        expect(properties).to.have.property('byTitle')
                        // Relationships can't have properties
                        expect(properties.list).to.have.length(0)
                        done();
                    })
            })

            function shouldHaveProperties(name, expectedProperties, negate) {
                name = 'http://lumify.io/dev#' + name;

                it('should have concept ' + name +
                   ' that has properties ' + expectedProperties.join(','), function(done) {
                    var service = this.service;

                    service.concepts().done(function(concepts) {
                        var conceptId = _.findWhere(concepts.byTitle, { title: name }).id;

                        service.propertiesByConceptId(conceptId)
                            .done(function(properties) {
                                expectedProperties.forEach(function(expectedProperty) {
                                    expectedProperty = 'http://lumify.io/dev#' + expectedProperty;
                                    if (negate) {
                                        expect(properties.byTitle[expectedProperty]).to.be.undefined
                                    } else {
                                        expect(properties.byTitle[expectedProperty]).to.exist
                                    }
                                })

                                done();
                            });
                    });
                })
            }
            function shouldNotHaveProperties(s,p) {
                shouldHaveProperties(s,p,true)
            }
        })
    })

});
