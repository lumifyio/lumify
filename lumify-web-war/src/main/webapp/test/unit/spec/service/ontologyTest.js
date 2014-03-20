

define(['service/ontology'], function(OntologyService) {

    describe('OntologyService', function() {

        before(function() {
            this.service = new OntologyService();
            this.service.ontology();
            this.service._ajaxRequests('ontology').resolve(defaultDevOntology)
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
                        expect(image).to.have.property('glyphIconHref').that.equals('/resource/http%3A%2F%2Flumify.io%2Fdev%23raw')
 
                        var byIdImage = concepts.byId['http://lumify.io/dev#image'];
                        expect(byIdImage).to.exist
                        byIdImage.should.have.property('glyphIconHref').that.equals('/resource/http%3A%2F%2Flumify.io%2Fdev%23raw')

                        var byTitleImage = _.findWhere(concepts.byTitle, { id: 'http://lumify.io/dev#image' })
                        expect(byTitleImage).to.exist
                        byTitleImage.should.have.property('glyphIconHref').that.equals('/resource/http%3A%2F%2Flumify.io%2Fdev%23raw')
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
                        email.should.have.property('glyphIconHref').that.equals('/resource/http%3A%2F%2Flumify.io%2Fdev%23emailAddress')

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

            // Check inverse not true
            shouldNotHaveRelationship('city', 'person', 'personLivesAtLocation')
            shouldNotHaveRelationship('location', 'person', 'personLivesAtLocation')

            function shouldHaveRelationship(sourceName, destName, title, negate) {
                sourceName = 'http://lumify.io/dev#' + sourceName;
                destName = 'http://lumify.io/dev#' + destName;
                title = 'http://lumify.io/dev#' + title;

                it('should' + (negate ? ' NOT' : '') + ' have ' + title + ' relationship from ' + sourceName + '->' + destName, function(done) {
                    var service = this.service;

                    service.concepts().done(function(concepts) {
                        var source = _.findWhere(concepts.byTitle, { title:sourceName }).id,
                            dest = _.findWhere(concepts.byTitle, { title:destName }).id

                        service.conceptToConceptRelationships(source, dest)
                            .done(function(relationships) {
                                var result = _.findWhere(relationships, { title:title });
                                if (negate) expect(result).to.be.undefined
                                else expect(result).to.exist
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
            
            shouldHaveProperties('company', ['netIncome', 'formationDate', 'abbreviation', 'source'])
            shouldNotHaveProperties('organization', ['netIncome'])

            it('should have properties by relationshipLabel function', function() {
                expect(this.service).to.have.property('propertiesByRelationshipLabel').that.is.a.function
            })

            it('should return properties by relationship label', function(done) {
                this.service.propertiesByRelationshipLabel('Has Entity')
                    .done(function(properties) {
                        done();
                    })
            })

            function shouldHaveProperties(name, expectedProperties, negate) {
                name = 'http://lumify.io/dev#' + name;

                it('should have concept ' + name + ' that has properties ' + expectedProperties.join(','), function(done) {
                    var service = this.service;

                    service.concepts().done(function(concepts) {
                        var conceptId = _.findWhere(concepts.byTitle, { title:name }).id;

                        service.propertiesByConceptId(conceptId)
                            .done(function(properties) {
                                expectedProperties.forEach(function(expectedProperty) {
                                    expectedProperty = 'http://lumify.io/dev#' + expectedProperty;
                                    if (negate) expect(properties.byTitle[expectedProperty]).to.be.undefined
                                    else expect(properties.byTitle[expectedProperty]).to.exist
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



    var defaultDevOntology = {
        "properties": [{
            "userVisible": true,
            "id": "092f8c86b90c432684e88f3c4894ccfb",
            "dataType": "string",
            "title": "http://lumify.io/dev#gender",
            "displayName": "Gender"
        }, {
            "userVisible": true,
            "id": "1ae71684a9054dabb5134d3021ef67e1",
            "dataType": "date",
            "title": "http://lumify.io/dev#formationDate",
            "displayName": "Formation Date"
        }, {
            "userVisible": true,
            "id": "4195fbb940c64c0e9e0fd29a9a1498f7",
            "dataType": "string",
            "title": "http://lumify.io/dev#abbreviation",
            "displayName": "Abbreviation"
        }, {
            "userVisible": true,
            "id": "699a5fe6363c408ea94708221fb7d058",
            "dataType": "string",
            "title": "http://lumify.io/dev#source",
            "displayName": "Source"
        }, {
            "userVisible": true,
            "id": "6b0217299a074d99aa890c48799d61a1",
            "dataType": "currency",
            "title": "http://lumify.io/dev#netIncome",
            "displayName": "Net Income"
        }, {
            "userVisible": false,
            "id": "6fe0254c242549dd8e44f8b23eb03503",
            "dataType": "image",
            "title": "http://lumify.io#mapGlyphIcon",
            "displayName": "map glyph icon"
        }, {
            "userVisible": false,
            "id": "a496972626f3486383b83505305c34fb",
            "dataType": "string",
            "title": "http://lumify.io#conceptType",
            "displayName": "Type"
        }, {
            "userVisible": true,
            "id": "a57ef79775474f7ebf4149d3357d72a2",
            "dataType": "string",
            "title": "http://lumify.io/dev#alias",
            "displayName": "Alias"
        }, {
            "userVisible": true,
            "id": "ab999e2d225e4f9fbad9e0eb56795cc8",
            "dataType": "date",
            "title": "http://lumify.io/dev#date",
            "displayName": "Date"
        }, {
            "userVisible": true,
            "id": "ca69262d0c2a4b79a9a91b95a675ee87",
            "dataType": "geoLocation",
            "title": "http://lumify.io/dev#geolocation",
            "displayName": "Geolocation"
        }, {
            "userVisible": false,
            "id": "d20b5521cce3485f9e97063e9c7dca2f",
            "dataType": "image",
            "title": "http://lumify.io#glyphIcon",
            "displayName": "glyph icon"
        }, {
            "userVisible": true,
            "id": "d2f1949dfd9e4162ad92c7eeb2a22f3c",
            "dataType": "date",
            "title": "http://lumify.io/dev#birthDate",
            "displayName": "Birth Date"
        }, {
            "userVisible": true,
            "id": "e359bf2dd00c4ee69a80a89071584864",
            "dataType": "date",
            "title": "http://lumify.io/dev#publishedDate",
            "displayName": "Published Date"
        }, {
            "userVisible": true,
            "id": "e73777436a2a4b4a97a52579ae72f237",
            "dataType": "string",
            "title": "http://lumify.io#title",
            "displayName": "Title"
        }],
        "relationships": [{
            "id": "http://lumify.io/dev#entityHasImageRaw",
            "title": "http://lumify.io/dev#entityHasImageRaw",
            "dest": "http://lumify.io/dev#image",
            "source": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Has Image"
        }, {
            "id": "http://lumify.io/dev#eventHappenedInLocation",
            "title": "http://lumify.io/dev#eventHappenedInLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#event",
            "displayName": "Happened In"
        }, {
            "id": "http://lumify.io/dev#eventPlannedByOrganization",
            "title": "http://lumify.io/dev#eventPlannedByOrganization",
            "dest": "http://lumify.io/dev#organization",
            "source": "http://lumify.io/dev#event",
            "displayName": "Planned By"
        }, {
            "id": "http://lumify.io/dev#locationHasLeaderPerson",
            "title": "http://lumify.io/dev#locationHasLeaderPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#location",
            "displayName": "Has Leader"
        }, {
            "id": "http://lumify.io/dev#locationIsCaptialOfLocation",
            "title": "http://lumify.io/dev#locationIsCaptialOfLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#location",
            "displayName": "Is Capital Of"
        }, {
            "id": "http://lumify.io/dev#locationIsInLocation",
            "title": "http://lumify.io/dev#locationIsInLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#location",
            "displayName": "Is In"
        }, {
            "id": "http://lumify.io/dev#organizationHasKeyLeader",
            "title": "http://lumify.io/dev#organizationHasKeyLeader",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#organization",
            "displayName": "Has Key Leader"
        }, {
            "id": "http://lumify.io/dev#organizationHasOfficeAtLocation",
            "title": "http://lumify.io/dev#organizationHasOfficeAtLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#organization",
            "displayName": "Has Office At"
        }, {
            "id": "http://lumify.io/dev#organizationHeadquarteredAtLocation",
            "title": "http://lumify.io/dev#organizationHeadquarteredAtLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#organization",
            "displayName": "Headquartered At"
        }, {
            "id": "http://lumify.io/dev#personHasChildPerson",
            "title": "http://lumify.io/dev#personHasChildPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Child"
        }, {
            "id": "http://lumify.io/dev#personHasCitizenshipInCountry",
            "title": "http://lumify.io/dev#personHasCitizenshipInCountry",
            "dest": "http://lumify.io/dev#country",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Citizenship In"
        }, {
            "id": "http://lumify.io/dev#personHasCoworkerPerson",
            "title": "http://lumify.io/dev#personHasCoworkerPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Coworker"
        }, {
            "id": "http://lumify.io/dev#personHasEmailAddress",
            "title": "http://lumify.io/dev#personHasEmailAddress",
            "dest": "http://lumify.io/dev#emailAddress",
            "source": "http://lumify.io/dev#organization",
            "displayName": "Has Email Address"
        }, {
            "id": "http://lumify.io/dev#personHasFriendPerson",
            "title": "http://lumify.io/dev#personHasFriendPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Friend"
        }, {
            "id": "http://lumify.io/dev#personHasHomeAddressLocation",
            "title": "http://lumify.io/dev#personHasHomeAddressLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Home Address"
        }, {
            "id": "http://lumify.io/dev#personHasLivedLocation",
            "title": "http://lumify.io/dev#personHasLivedLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Lived In"
        }, {
            "id": "http://lumify.io/dev#personHasParentPerson",
            "title": "http://lumify.io/dev#personHasParentPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Parent"
        }, {
            "id": "http://lumify.io/dev#personHasPhoneNumber",
            "title": "http://lumify.io/dev#personHasPhoneNumber",
            "dest": "http://lumify.io/dev#phoneNumber",
            "source": "http://lumify.io/dev#organization",
            "displayName": "Has Phone Number"
        }, {
            "id": "http://lumify.io/dev#personHasSiblingPerson",
            "title": "http://lumify.io/dev#personHasSiblingPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Sibling"
        }, {
            "id": "http://lumify.io/dev#personHasVisitedLocation",
            "title": "http://lumify.io/dev#personHasVisitedLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Visited"
        }, {
            "id": "http://lumify.io/dev#personIsEmployedByOrganizaton",
            "title": "http://lumify.io/dev#personIsEmployedByOrganizaton",
            "dest": "http://lumify.io/dev#organization",
            "source": "http://lumify.io/dev#person",
            "displayName": "Is Employed By"
        }, {
            "id": "http://lumify.io/dev#personIsFromLocation",
            "title": "http://lumify.io/dev#personIsFromLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Is From"
        }, {
            "id": "http://lumify.io/dev#personIsMemberOfOrganization",
            "title": "http://lumify.io/dev#personIsMemberOfOrganization",
            "dest": "http://lumify.io/dev#organization",
            "source": "http://lumify.io/dev#person",
            "displayName": "Is Member Of"
        }, {
            "id": "http://lumify.io/dev#personKnowsPerson",
            "title": "http://lumify.io/dev#personKnowsPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Knows"
        }, {
            "id": "http://lumify.io/dev#personLivesAtLocation",
            "title": "http://lumify.io/dev#personLivesAtLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Lives At"
        }, {
            "id": "http://lumify.io/dev#personLivesWithPerson",
            "title": "http://lumify.io/dev#personLivesWithPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Lives With"
        }, {
            "id": "http://lumify.io/dev#personOwnsLocation",
            "title": "http://lumify.io/dev#personOwnsLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Owns"
        }, {
            "id": "http://lumify.io/dev#personSupportsOrganization",
            "title": "http://lumify.io/dev#personSupportsOrganization",
            "dest": "http://lumify.io/dev#organization",
            "source": "http://lumify.io/dev#person",
            "displayName": "Supports"
        }, {
            "id": "http://lumify.io/dev#personWorksAtCompany",
            "title": "http://lumify.io/dev#personWorksAtCompany",
            "dest": "http://lumify.io/dev#organization",
            "source": "http://lumify.io/dev#person",
            "displayName": "Works At"
        }, {
            "id": "http://lumify.io/dev#rawContainsImageOfEntity",
            "title": "http://lumify.io/dev#rawContainsImageOfEntity",
            "dest": "http://www.w3.org/2002/07/owl#Thing",
            "source": "http://lumify.io/dev#raw",
            "displayName": "Contains Image of"
        }, {
            "id": "http://lumify.io/dev#rawHasEntity",
            "title": "http://lumify.io/dev#rawHasEntity",
            "dest": "http://www.w3.org/2002/07/owl#Thing",
            "source": "http://lumify.io/dev#raw",
            "displayName": "Has Entity"
        }, {
            "id": "http://lumify.io/dev#rawHasSourceOrganization",
            "title": "http://lumify.io/dev#rawHasSourceOrganization",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#raw",
            "displayName": "Has Author"
        }, {
            "id": "http://lumify.io/dev#rawHasSourcePerson",
            "title": "http://lumify.io/dev#rawHasSourcePerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#raw",
            "displayName": "Has Author"
        }, {
            "id": "http://lumify.io/workspace/toEntity",
            "title": "http://lumify.io/workspace/toEntity",
            "dest": "http://lumify.io#root",
            "source": "http://lumify.io/workspace",
            "displayName": "workspace to entity"
        }, {
            "id": "http://lumify.io/workspace/toUser",
            "title": "http://lumify.io/workspace/toUser",
            "dest": "http://lumify.io#root",
            "source": "http://lumify.io/workspace",
            "displayName": "workspace to user"
        }],
        "concepts": [{
            "id": "http://lumify.io#root",
            "title": "http://lumify.io#root",
            "pluralDisplayName": "roots",
            "properties": [],
            "displayName": "root"
        }, {
            "id": "http://lumify.io/dev#address",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23address",
            "title": "http://lumify.io/dev#address",
            "color": "rgb(219, 63, 219)",
            "pluralDisplayName": "Addresses",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#location",
            "displayName": "Address"
        }, {
            "id": "http://lumify.io/dev#city",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23city",
            "title": "http://lumify.io/dev#city",
            "color": "rgb(191, 13, 191)",
            "pluralDisplayName": "Cities",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#location",
            "displayName": "City"
        }, {
            "id": "http://lumify.io/dev#company",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23company",
            "title": "http://lumify.io/dev#company",
            "color": "rgb(210, 52, 32)",
            "pluralDisplayName": "Companies",
            "properties": ["6b0217299a074d99aa890c48799d61a1"],
            "parentConcept": "http://lumify.io/dev#organization",
            "displayName": "Company"
        }, {
            "id": "http://lumify.io/dev#contactInformation",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23contactInformation",
            "title": "http://lumify.io/dev#contactInformation",
            "color": "rgb(225, 128, 0)",
            "pluralDisplayName": "Contact Informations",
            "properties": [],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Contact Information"
        }, {
            "id": "http://lumify.io/dev#country",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23country",
            "title": "http://lumify.io/dev#country",
            "color": "rgb(112, 0, 112)",
            "pluralDisplayName": "Countries",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#location",
            "displayName": "Country"
        }, {
            "id": "http://lumify.io/dev#document",
            "title": "http://lumify.io/dev#document",
            "color": "rgb(28, 137, 28)",
            "displayType": "document",
            "pluralDisplayName": "Documents",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#raw",
            "displayName": "Document"
        }, {
            "id": "http://lumify.io/dev#emailAddress",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23emailAddress",
            "title": "http://lumify.io/dev#emailAddress",
            "color": "rgb(203, 130, 4)",
            "pluralDisplayName": "Email Addresses",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#contactInformation",
            "displayName": "Email Address"
        }, {
            "id": "http://lumify.io/dev#event",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23event",
            "title": "http://lumify.io/dev#event",
            "color": "rgb(23, 30, 239)",
            "pluralDisplayName": "Events",
            "properties": ["ab999e2d225e4f9fbad9e0eb56795cc8", "ca69262d0c2a4b79a9a91b95a675ee87"],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Event"
        }, {
            "id": "http://lumify.io/dev#image",
            "title": "http://lumify.io/dev#image",
            "color": "rgb(176, 87, 53)",
            "displayType": "image",
            "pluralDisplayName": "Images",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#raw",
            "displayName": "Image"
        }, {
            "id": "http://lumify.io/dev#location",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23location",
            "title": "http://lumify.io/dev#location",
            "color": "rgb(160, 7, 206)",
            "pluralDisplayName": "Locations",
            "properties": ["ca69262d0c2a4b79a9a91b95a675ee87"],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Location"
        }, {
            "id": "http://lumify.io/dev#organization",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23organization",
            "title": "http://lumify.io/dev#organization",
            "color": "rgb(137, 39, 26)",
            "pluralDisplayName": "Organizations",
            "properties": ["1ae71684a9054dabb5134d3021ef67e1", "4195fbb940c64c0e9e0fd29a9a1498f7"],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Organization"
        }, {
            "id": "http://lumify.io/dev#person",
            "title": "http://lumify.io/dev#person",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23person",
            "pluralDisplayName": "Persons",
            "properties": ["092f8c86b90c432684e88f3c4894ccfb", "a57ef79775474f7ebf4149d3357d72a2", "d2f1949dfd9e4162ad92c7eeb2a22f3c"],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Person"
        }, {
            "id": "http://lumify.io/dev#phoneNumber",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23phoneNumber",
            "title": "http://lumify.io/dev#phoneNumber",
            "color": "rgb(225, 225, 24)",
            "pluralDisplayName": "Phone Numbers",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#contactInformation",
            "displayName": "Phone Number"
        }, {
            "id": "http://lumify.io/dev#raw",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23raw",
            "title": "http://lumify.io/dev#raw",
            "color": "rgb(28, 137, 28)",
            "pluralDisplayName": "Raws",
            "properties": ["e359bf2dd00c4ee69a80a89071584864"],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Raw"
        }, {
            "id": "http://lumify.io/dev#state",
            "glyphIconHref": "/resource/http%3A%2F%2Flumify.io%2Fdev%23state",
            "title": "http://lumify.io/dev#state",
            "color": "rgb(153, 0, 153)",
            "pluralDisplayName": "States",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#location",
            "displayName": "State"
        }, {
            "id": "http://lumify.io/dev#video",
            "title": "http://lumify.io/dev#video",
            "color": "rgb(149, 138, 218)",
            "displayType": "video",
            "pluralDisplayName": "Videos",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#raw",
            "displayName": "Video"
        }, {
            "id": "http://lumify.io/dev#videoSub",
            "title": "http://lumify.io/dev#videoSub",
            "color": "rgb(149, 138, 218)",
            "pluralDisplayName": "VideoSubs",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#video",
            "displayName": "VideoSub"
        }, {
            "id": "http://lumify.io/user",
            "title": "http://lumify.io/user",
            "pluralDisplayName": "lumifyUsers",
            "properties": [],
            "displayName": "lumifyUser"
        }, {
            "id": "http://lumify.io/workspace",
            "title": "http://lumify.io/workspace",
            "pluralDisplayName": "workspaces",
            "properties": [],
            "displayName": "workspace"
        }, {
            "id": "http://www.w3.org/2002/07/owl#Thing",
            "glyphIconHref": "/resource/http%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23Thing",
            "title": "http://www.w3.org/2002/07/owl#Thing",
            "pluralDisplayName": "things",
            "properties": ["699a5fe6363c408ea94708221fb7d058", "6fe0254c242549dd8e44f8b23eb03503", "a496972626f3486383b83505305c34fb", "d20b5521cce3485f9e97063e9c7dca2f", "e73777436a2a4b4a97a52579ae72f237"],
            "parentConcept": "http://lumify.io#root",
            "displayName": "thing"
        }]
    }
});
