

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
                        email.should.have.property('glyphIconHref').that.equals('resource?id=http%3A%2F%2Flumify.io%2Fdev%23emailAddress')

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
            "dataType": "date",
            "title": "http://lumify.io/dev#formationDate",
            "displayName": "Formation Date"
        }, {
            "userVisible": true,
            "dataType": "string",
            "title": "http://lumify.io/dev#alias",
            "displayName": "Alias"
        }, {
            "userVisible": true,
            "dataType": "currency",
            "title": "http://lumify.io/dev#netIncome",
            "displayName": "Net Income"
        }, {
            "userVisible": true,
            "dataType": "string",
            "title": "http://lumify.io/dev#abbreviation",
            "displayName": "Abbreviation"
        }, {
            "userVisible": false,
            "dataType": "string",
            "title": "http://lumify.io#title",
            "displayName": "Title"
        }, {
            "userVisible": true,
            "dataType": "date",
            "title": "http://lumify.io/dev#publishedDate",
            "displayName": "Published Date"
        }, {
            "userVisible": true,
            "dataType": "date",
            "title": "http://lumify.io/dev#birthDate",
            "displayName": "Birth Date"
        }, {
            "userVisible": false,
            "dataType": "image",
            "title": "http://lumify.io#glyphIcon",
            "displayName": "Glyph Icon"
        }, {
            "userVisible": true,
            "dataType": "string",
            "title": "http://lumify.io/dev#source",
            "displayName": "Source"
        }, {
            "userVisible": true,
            "dataType": "string",
            "title": "http://lumify.io#source",
            "displayName": "Source"
        }, {
            "userVisible": false,
            "dataType": "string",
            "title": "http://lumify.io#rowKey",
            "displayName": "Row Key"
        }, {
            "userVisible": false,
            "dataType": "image",
            "title": "http://lumify.io#mapGlyphIcon",
            "displayName": "Map Glyph Icon"
        }, {
            "userVisible": true,
            "dataType": "string",
            "title": "http://lumify.io/dev#gender",
            "displayName": "Gender"
        }, {
            "userVisible": true,
            "dataType": "date",
            "title": "http://lumify.io/dev#date",
            "displayName": "Date"
        }, {
            "userVisible": true,
            "dataType": "date",
            "title": "http://lumify.io#publishedDate",
            "displayName": "Published Date"
        }, {
            "userVisible": false,
            "dataType": "string",
            "title": "http://lumify.io#conceptType",
            "displayName": "Concept Type"
        }, {
            "userVisible": false,
            "dataType": "string",
            "title": "http://lumify.io#mimeType",
            "displayName": "Mime Type"
        }, {
            "userVisible": true,
            "dataType": "geoLocation",
            "title": "http://lumify.io/dev#geolocation",
            "displayName": "Geolocation"
        }],
        "relationships": [{
            "title": "http://lumify.io/dev#entityHasImageRaw",
            "dest": "http://lumify.io/dev#image",
            "source": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Has Image"
        }, {
            "title": "http://lumify.io/dev#eventHappenedInLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#event",
            "displayName": "Happened In"
        }, {
            "title": "http://lumify.io/dev#eventPlannedByOrganization",
            "dest": "http://lumify.io/dev#organization",
            "source": "http://lumify.io/dev#event",
            "displayName": "Planned By"
        }, {
            "title": "http://lumify.io/dev#locationHasLeaderPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#location",
            "displayName": "Has Leader"
        }, {
            "title": "http://lumify.io/dev#locationIsCaptialOfLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#location",
            "displayName": "Is Capital Of"
        }, {
            "title": "http://lumify.io/dev#locationIsInLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#location",
            "displayName": "Is In"
        }, {
            "title": "http://lumify.io/dev#organizationHasKeyLeader",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#organization",
            "displayName": "Has Key Leader"
        }, {
            "title": "http://lumify.io/dev#organizationHasOfficeAtLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#organization",
            "displayName": "Has Office At"
        }, {
            "title": "http://lumify.io/dev#organizationHeadquarteredAtLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#organization",
            "displayName": "Headquartered At"
        }, {
            "title": "http://lumify.io/dev#personHasChildPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Child"
        }, {
            "title": "http://lumify.io/dev#personHasCitizenshipInCountry",
            "dest": "http://lumify.io/dev#country",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Citizenship In"
        }, {
            "title": "http://lumify.io/dev#personHasCoworkerPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Coworker"
        }, {
            "title": "http://lumify.io/dev#personHasEmailAddress",
            "dest": "http://lumify.io/dev#emailAddress",
            "source": "http://lumify.io/dev#organization",
            "displayName": "Has Email Address"
        }, {
            "title": "http://lumify.io/dev#personHasFriendPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Friend"
        }, {
            "title": "http://lumify.io/dev#personHasHomeAddressLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Home Address"
        }, {
            "title": "http://lumify.io/dev#personHasLivedLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Lived In"
        }, {
            "title": "http://lumify.io/dev#personHasParentPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Parent"
        }, {
            "title": "http://lumify.io/dev#personHasPhoneNumber",
            "dest": "http://lumify.io/dev#phoneNumber",
            "source": "http://lumify.io/dev#organization",
            "displayName": "Has Phone Number"
        }, {
            "title": "http://lumify.io/dev#personHasSiblingPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Sibling"
        }, {
            "title": "http://lumify.io/dev#personHasVisitedLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Has Visited"
        }, {
            "title": "http://lumify.io/dev#personIsEmployedByOrganizaton",
            "dest": "http://lumify.io/dev#organization",
            "source": "http://lumify.io/dev#person",
            "displayName": "Is Employed By"
        }, {
            "title": "http://lumify.io/dev#personIsFromLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Is From"
        }, {
            "title": "http://lumify.io/dev#personIsMemberOfOrganization",
            "dest": "http://lumify.io/dev#organization",
            "source": "http://lumify.io/dev#person",
            "displayName": "Is Member Of"
        }, {
            "title": "http://lumify.io/dev#personKnowsPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Knows"
        }, {
            "title": "http://lumify.io/dev#personLivesAtLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Lives At"
        }, {
            "title": "http://lumify.io/dev#personLivesWithPerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#person",
            "displayName": "Lives With"
        }, {
            "title": "http://lumify.io/dev#personOwnsLocation",
            "dest": "http://lumify.io/dev#location",
            "source": "http://lumify.io/dev#person",
            "displayName": "Owns"
        }, {
            "title": "http://lumify.io/dev#personSupportsOrganization",
            "dest": "http://lumify.io/dev#organization",
            "source": "http://lumify.io/dev#person",
            "displayName": "Supports"
        }, {
            "title": "http://lumify.io/dev#personWorksAtCompany",
            "dest": "http://lumify.io/dev#organization",
            "source": "http://lumify.io/dev#person",
            "displayName": "Works At"
        }, {
            "title": "http://lumify.io/dev#rawContainsImageOfEntity",
            "dest": "http://www.w3.org/2002/07/owl#Thing",
            "source": "http://lumify.io/dev#raw",
            "displayName": "Contains Image of"
        }, {
            "title": "http://lumify.io/dev#rawHasEntity",
            "dest": "http://www.w3.org/2002/07/owl#Thing",
            "source": "http://lumify.io/dev#raw",
            "displayName": "Has Entity"
        }, {
            "title": "http://lumify.io/dev#rawHasSourceOrganization",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#raw",
            "displayName": "Has Author"
        }, {
            "title": "http://lumify.io/dev#rawHasSourcePerson",
            "dest": "http://lumify.io/dev#person",
            "source": "http://lumify.io/dev#raw",
            "displayName": "Has Author"
        }, {
            "title": "http://lumify.io/workspace/toEntity",
            "dest": "http://lumify.io#root",
            "source": "http://lumify.io/workspace",
            "displayName": "workspace to entity"
        }, {
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
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23address",
            "title": "http://lumify.io/dev#address",
            "color": "rgb(219, 63, 219)",
            "pluralDisplayName": "Addresses",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#location",
            "displayName": "Address"
        }, {
            "id": "http://lumify.io/dev#audio",
            "title": "http://lumify.io/dev#audio",
            "color": "rgb(149, 138, 218)",
            "displayType": "audio",
            "pluralDisplayName": "Audios",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#raw",
            "displayName": "Audio"
        }, {
            "id": "http://lumify.io/dev#city",
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23city",
            "title": "http://lumify.io/dev#city",
            "color": "rgb(191, 13, 191)",
            "pluralDisplayName": "Cities",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#location",
            "displayName": "City"
        }, {
            "id": "http://lumify.io/dev#company",
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23company",
            "title": "http://lumify.io/dev#company",
            "color": "rgb(210, 52, 32)",
            "pluralDisplayName": "Companies",
            "properties": ["http://lumify.io/dev#netIncome"],
            "parentConcept": "http://lumify.io/dev#organization",
            "displayName": "Company"
        }, {
            "id": "http://lumify.io/dev#contactInformation",
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23contactInformation",
            "title": "http://lumify.io/dev#contactInformation",
            "color": "rgb(225, 128, 0)",
            "pluralDisplayName": "Contact Informations",
            "properties": [],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Contact Information"
        }, {
            "id": "http://lumify.io/dev#country",
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23country",
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
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23emailAddress",
            "title": "http://lumify.io/dev#emailAddress",
            "color": "rgb(203, 130, 4)",
            "pluralDisplayName": "Email Addresses",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#contactInformation",
            "displayName": "Email Address"
        }, {
            "id": "http://lumify.io/dev#event",
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23event",
            "title": "http://lumify.io/dev#event",
            "color": "rgb(23, 30, 239)",
            "pluralDisplayName": "Events",
            "properties": ["http://lumify.io/dev#date", "http://lumify.io/dev#geolocation"],
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
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23location",
            "title": "http://lumify.io/dev#location",
            "color": "rgb(160, 7, 206)",
            "pluralDisplayName": "Locations",
            "properties": ["http://lumify.io/dev#geolocation"],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Location"
        }, {
            "id": "http://lumify.io/dev#organization",
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23organization",
            "title": "http://lumify.io/dev#organization",
            "color": "rgb(137, 39, 26)",
            "pluralDisplayName": "Organizations",
            "properties": ["http://lumify.io/dev#formationDate", "http://lumify.io/dev#abbreviation"],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Organization"
        }, {
            "id": "http://lumify.io/dev#person",
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23person",
            "title": "http://lumify.io/dev#person",
            //"color": "rgb(28, 137, 28)",
            "pluralDisplayName": "Persons",
            "properties": ["http://lumify.io/dev#alias", "http://lumify.io/dev#birthDate", "http://lumify.io/dev#gender"],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Person"
        }, {
            "id": "http://lumify.io/dev#phoneNumber",
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23phoneNumber",
            "title": "http://lumify.io/dev#phoneNumber",
            "color": "rgb(225, 225, 24)",
            "pluralDisplayName": "Phone Numbers",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#contactInformation",
            "displayName": "Phone Number"
        }, {
            "id": "http://lumify.io/dev#raw",
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23raw",
            "title": "http://lumify.io/dev#raw",
            "color": "rgb(28, 137, 28)",
            "pluralDisplayName": "Raws",
            "properties": ["http://lumify.io/dev#publishedDate"],
            "parentConcept": "http://www.w3.org/2002/07/owl#Thing",
            "displayName": "Raw"
        }, {
            "id": "http://lumify.io/dev#state",
            "glyphIconHref": "resource?id=http%3A%2F%2Flumify.io%2Fdev%23state",
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
            "pluralDisplayName": "VideosSub",
            "properties": [],
            "parentConcept": "http://lumify.io/dev#video",
            "displayName": "Video"
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
            "glyphIconHref": "resource?id=http%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23Thing",
            "title": "http://www.w3.org/2002/07/owl#Thing",
            "pluralDisplayName": "things",
            "properties": ["http://lumify.io#title", "http://lumify.io#glyphIcon", "http://lumify.io/dev#source", "http://lumify.io#source", "http://lumify.io#rowKey", "http://lumify.io#mapGlyphIcon", "http://lumify.io#publishedDate", "http://lumify.io#conceptType", "http://lumify.io#mimeType"],
            "parentConcept": "http://lumify.io#root",
            "displayName": "thing"
        }]
    }
});
