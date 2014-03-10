

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
                        expect(concepts.entityConcept.title).to.equal('entity')
                        expect(concepts.entityConcept.pluralDisplayName).to.equal('Entities')

                        done();
                    });
            })

        })

        describe('relationships', function() {

            it('should return relationships for concept types', function() {
                expect(this.service).to.have.property('relationships').that.is.a.function
            })

            shouldHaveRelationship('person', 'location', 'personLivesAtLocation')
            shouldHaveRelationship('person', 'contact', 'personHasContactInformation')
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
                this.service.propertiesByRelationshipLabel('has entity')
                    .done(function(properties) {
                        done();
                    })
            })

            function shouldHaveProperties(name, expectedProperties, negate) {
                it('should have concept ' + name + ' that has properties ' + expectedProperties.join(','), function(done) {
                    var service = this.service;

                    service.concepts().done(function(concepts) {
                        var conceptId = _.findWhere(concepts.byTitle, { title:name }).id;

                        service.propertiesByConceptId(conceptId)
                            .done(function(properties) {
                                expectedProperties.forEach(function(expectedProperty) {
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
       "properties":[
          {
             "id":"00ac5fc9192d44a3a8f90c981e670dda",
             "dataType":"date",
             "title":"date",
             "displayName":"date"
          },
          {
             "id":"0ee3f1b223db4247a88558d4aa2404da",
             "dataType":"string",
             "title":"_geoLocationDescription",
             "displayName":"Geo Location Description"
          },
          {
             "id":"12c4ddb749a44df680c67fac31967cc9",
             "dataType":"string",
             "title":"alias",
             "displayName":"alias"
          },
          {
             "id":"1743f51d23cc4b1698f6471ce2b5b6b0",
             "dataType":"date",
             "title":"creationDate",
             "displayName":"creationDate"
          },
          {
             "id":"1bfb8b48a51d4f4c9cacc08076e49942",
             "dataType":"string",
             "title":"gender",
             "displayName":"gender"
          },
          {
             "id":"45ee5c0f771d4bef8fbe64e22a30f174",
             "dataType":"image",
             "title":"_glyphIcon",
             "displayName":"glyph icon"
          },
          {
             "id":"50cf8aac59284ccaa2a3f447a6b8baa1",
             "dataType":"date",
             "title":"birthDate",
             "displayName":"birth date"
          },
          {
             "id":"74458b1c1d164f84be868c7eb2940f85",
             "dataType":"string",
             "title":"source",
             "displayName":"source"
          },
          {
             "id":"815e3372194e4cb1ae90e1a98149b7ec",
             "dataType":"date",
             "title":"publishedDate",
             "displayName":"published date"
          },
          {
             "id":"92ae83b2bf8345968c1a57b71014859f",
             "dataType":"string",
             "title":"author",
             "displayName":"Author"
          },
          {
             "id":"9ff5c51a2fc34f3b90bd40749c755682",
             "dataType":"string",
             "title":"_conceptType",
             "displayName":"Type"
          },
          {
             "id":"d79b5e8d621240f2ae702b7e8da9b225",
             "dataType":"image",
             "title":"_mapGlyphIcon",
             "displayName":"map glyph icon"
          },
          {
             "id":"e013298ce9e94385a0f1f696fb1ded83",
             "dataType":"date",
             "title":"formationDate",
             "displayName":"formation date"
          },
          {
             "id":"e5797b825a444ce4b22036543fafc028",
             "dataType":"currency",
             "title":"netIncome",
             "displayName":"net income"
          },
          {
             "id":"ee3429390860472ba24886ccc8524516",
             "dataType":"string",
             "title":"abbreviation",
             "displayName":"abbreviation"
          },
          {
             "id":"f4e5a6171c7c4ac48618d89485f3d8a9",
             "dataType":"geoLocation",
             "title":"geoLocation",
             "displayName":"Geo Location"
          },
          {
             "id":"fbec470446234463818a4c5d2c7b0ad9",
             "dataType":"string",
             "title":"title",
             "displayName":"Title"
          }
       ],
       "relationships":[
          {
             "id":"0934358da50d4f128b8da76d2b55c0a5",
             "title":"organizationHeadquarteredAtLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"headquartered at"
          },
          {
             "id":"0bcb1de9a5c246b1b52b070bea735a48",
             "title":"eventHappenedInLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"8011f34ad46d4397b8317004eb660aa5",
             "displayName":"happened in"
          },
          {
             "id":"12bd8d3e03bb4e869b5fb2bd5b33e74f",
             "title":"personHasUnclePerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has uncle"
          },
          {
             "id":"16b43b8673094c358e2106a482fce913",
             "title":"personHasPhoneNumber",
             "dest":"1c5c1f1f09ee4856a568261f69266af8",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has phone number"
          },
          {
             "id":"1f1f9eb063dc4fba93b2ba3b6fd47308",
             "title":"personHasNiecePerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has niece"
          },
          {
             "id":"230d34f432384de0b3ff051049d67668",
             "title":"personKnowsPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"knows"
          },
          {
             "id":"2798b98dabad45808de4f861929c2385",
             "title":"personHasFriendPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has friend"
          },
          {
             "id":"28ad676a51c1438b8d5cc0f1a3bbe4a1",
             "title":"organizationHasEmailAddress",
             "dest":"fc3968a6edc3443ab6e8e668d1a919e1",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"has email address"
          },
          {
             "id":"35443e30fb5e415bbefe8978f1a3480f",
             "title":"locationHasLeaderPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"2715f07c317e49108756ee6d0fee4487",
             "displayName":"has leader"
          },
          {
             "id":"36addcdb7591482fb1a38230ee649055",
             "title":"personGrandchildPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has grandchild"
          },
          {
             "id":"36f3b83351dd4b49b0c8ffd70a412efa",
             "title":"personAuthoredRaw",
             "dest":"3ab2648c41074f98a1f2922a9a01375f",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"authored"
          },
          {
             "id":"39c694adde104345a4a8dead36965f8d",
             "title":"organizationHasOfficeInLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"has office in"
          },
          {
             "id":"3c6723dbe36b4b29bdfda347f6292d2e",
             "title":"personHasWorkEmailAddress",
             "dest":"fc3968a6edc3443ab6e8e668d1a919e1",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has work address"
          },
          {
             "id":"3f82603315a44a90ae74d634e3302258",
             "title":"personHasContactInformation",
             "dest":"3c57da9ddc27460fad033bfa10a51616",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has contact information"
          },
          {
             "id":"43ebeaed54604b499c76c5b8fb61eddc",
             "title":"personHasNephewPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has nephew"
          },
          {
             "id":"458a565e7c8940729a6472885498efbe",
             "title":"organizationHasEmployeePerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"has employee"
          },
          {
             "id":"47c220cc62934c5db99207b69c30bda8",
             "title":"organizationHasLeaderPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"has leader"
          },
          {
             "id":"48a146a91da843eeaac324daf07e96c6",
             "title":"personSupportsOrganization",
             "dest":"c1188ec1bafd45068c7acea5b482f1e3",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"supports"
          },
          {
             "id":"4923d6fbe9c34999aab65995d6c291a3",
             "title":"personWorksAtCompany",
             "dest":"92af2132fece4943b1511dc2850cea31",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"works at"
          },
          {
             "id":"4bf9a6168ef448c2af1896aed878072c",
             "title":"http://lumify.io/workspace/toUser",
             "dest":"276b0e0297e94804891b6d8e5fc2317b",
             "source":"eef34867877d49869694bee88ac5c4b5",
             "displayName":"workspace to user"
          },
          {
             "id":"4e1c39c896e44eb894a33c741d06c5f2",
             "title":"personHasInLawPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has in-law"
          },
          {
             "id":"4ee8cce8b4c548c080a201347e75c637",
             "title":"organizationPublishedRaw",
             "dest":"3ab2648c41074f98a1f2922a9a01375f",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"published"
          },
          {
             "id":"54104a59473841be8e5337d6f74a2e81",
             "title":"personHasWorkAddressLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has work address"
          },
          {
             "id":"578aed25266e4782b8305a3a35101812",
             "title":"personIsFromLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"is from"
          },
          {
             "id":"5d4da950f9ee4aa086b9786df25feb09",
             "title":"rawHasSourceOrganization",
             "dest":"c1188ec1bafd45068c7acea5b482f1e3",
             "source":"3ab2648c41074f98a1f2922a9a01375f",
             "displayName":"has author"
          },
          {
             "id":"5f843c14176c4aa7b9eb5b112b4ef4ed",
             "title":"personHasSiblingPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has sibling"
          },
          {
             "id":"691715257aff4e1ab762331176435431",
             "title":"locationIsInLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"2715f07c317e49108756ee6d0fee4487",
             "displayName":"is in"
          },
          {
             "id":"69e639cecc4b4272ba1f5c8583b38e95",
             "title":"personHasWorkPhoneNumber",
             "dest":"1c5c1f1f09ee4856a568261f69266af8",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has work phone number"
          },
          {
             "id":"69ec83c9061f4a77b2b00e6698406adf",
             "title":"personHasHomeEmailAddress",
             "dest":"fc3968a6edc3443ab6e8e668d1a919e1",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has home address"
          },
          {
             "id":"6a414bc3ee1646d38732e662c9ef344e",
             "title":"organizationHasContactInformation",
             "dest":"3c57da9ddc27460fad033bfa10a51616",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"has contact information"
          },
          {
             "id":"6aaded095a6a4193ba28ddce5888ad38",
             "title":"eventPlannedByOrganization",
             "dest":"8011f34ad46d4397b8317004eb660aa5",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"planned by"
          },
          {
             "id":"6aefe1ffac6a4670933092ecc72b52ba",
             "title":"personOwnsLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"owns"
          },
          {
             "id":"71a5f5e5c3c842a0afe45667d089a2fa",
             "title":"eventWasPlannedByOrganization",
             "dest":"c1188ec1bafd45068c7acea5b482f1e3",
             "source":"8011f34ad46d4397b8317004eb660aa5",
             "displayName":"is planned by"
          },
          {
             "id":"765459e7ae1b4362a4e593db0792c060",
             "title":"rawHasSourcePerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"3ab2648c41074f98a1f2922a9a01375f",
             "displayName":"has author"
          },
          {
             "id":"7bebedcdbcd84eaaa3864204325b3543",
             "title":"personHasCoworkerPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has coworker"
          },
          {
             "id":"800882a02fc247689ddc84cec52af174",
             "title":"personHasHomeAddressLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has home address"
          },
          {
             "id":"8927cbde1f5646108f5abaa5edb8084e",
             "title":"personHasChildPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has child"
          },
          {
             "id":"8b697c7d75cb466393c14d63222c8cba",
             "title":"eventWasPlannedByPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"8011f34ad46d4397b8317004eb660aa5",
             "displayName":"is planned by"
          },
          {
             "id":"8f63a62e53104976b3089a24ef8e7dbe",
             "title":"rawContainsImageOfEntity",
             "dest":"276b0e0297e94804891b6d8e5fc2317b",
             "source":"3ab2648c41074f98a1f2922a9a01375f",
             "displayName":"contains image of"
          },
          {
             "id":"91c53fd1b2a04676a6b1d3b537dd8bce",
             "title":"personHasFaxNumber",
             "dest":"1c5c1f1f09ee4856a568261f69266af8",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has fax number"
          },
          {
             "id":"999488b576f046a6ade27602d1bbfc73",
             "title":"organizationAuthoredRaw",
             "dest":"3ab2648c41074f98a1f2922a9a01375f",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"authored"
          },
          {
             "id":"9a2adc3f16f246ebbccd5d81c8a541f7",
             "title":"personHasParentPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has parent"
          },
          {
             "id":"a0def2adb75f46e091e587604ffca81a",
             "title":"locationIsAtLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"2715f07c317e49108756ee6d0fee4487",
             "displayName":"is at"
          },
          {
             "id":"a8527bc5cc884270aa7323444acb77d1",
             "title":"personLivesAtLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"lives at"
          },
          {
             "id":"acf52ab718f9400291ab115103d2d5e2",
             "title":"personHasVisitedLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has home address"
          },
          {
             "id":"b429f40d936d48dc8be223bd5661cb3e",
             "title":"entityHasImageRaw",
             "dest":"3ab2648c41074f98a1f2922a9a01375f",
             "source":"276b0e0297e94804891b6d8e5fc2317b",
             "displayName":"has image"
          },
          {
             "id":"b52dafc9e5234c8386a5d63ae359b331",
             "title":"personHasAuntPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has aunt"
          },
          {
             "id":"bc721e12102e4094ab21910208f4e509",
             "title":"http://lumify.io/workspace/toEntity",
             "dest":"276b0e0297e94804891b6d8e5fc2317b",
             "source":"eef34867877d49869694bee88ac5c4b5",
             "displayName":"workspace to entity"
          },
          {
             "id":"cd1169ea94104b1980aa6f9e96ac8ed7",
             "title":"personIsMemberOfOrganization",
             "dest":"c1188ec1bafd45068c7acea5b482f1e3",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"is member of"
          },
          {
             "id":"cf15ad690eb84bafa59e7e2ceebd9b1b",
             "title":"organizationHasKeyLeader",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"has key leader"
          },
          {
             "id":"d0afabfaa1ba4ef5966a96c65ab73724",
             "title":"personHasEmailAddress",
             "dest":"fc3968a6edc3443ab6e8e668d1a919e1",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has email address"
          },
          {
             "id":"d1474ce3d75e4f08a14333eb496a8e9f",
             "title":"organizationHasPhoneNumber",
             "dest":"1c5c1f1f09ee4856a568261f69266af8",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"has phone number"
          },
          {
             "id":"d3afa6c308df40cb8a8ab6805140c531",
             "title":"personLivesWithPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"lives with"
          },
          {
             "id":"d80a34aa64804deab644d2439a4dac72",
             "title":"personHasHomePhoneNumber",
             "dest":"1c5c1f1f09ee4856a568261f69266af8",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has home phone number"
          },
          {
             "id":"db9c98840cbc44489892ede34397a1a8",
             "title":"rawHasEntity",
             "dest":"276b0e0297e94804891b6d8e5fc2317b",
             "source":"3ab2648c41074f98a1f2922a9a01375f",
             "displayName":"has entity"
          },
          {
             "id":"dcc626ee343c4157a4423eee547a1c9a",
             "title":"personHasGrandparentPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has grandparent"
          },
          {
             "id":"de1619f5456d4dd5abe0269c8f32c002",
             "title":"organizationHasOfficeAtLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"has office at"
          },
          {
             "id":"dff3062a30f64a219e7dbad220ad6db7",
             "title":"personHasCousinPerson",
             "dest":"a883053f7d4e41be877218121836d0dc",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has cousin"
          },
          {
             "id":"e0a693e0fb1a4bc59c3e43cc1c005d65",
             "title":"personIsLeaderOfOrganization",
             "dest":"c1188ec1bafd45068c7acea5b482f1e3",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"is leader of"
          },
          {
             "id":"e19a8095e9524f4e89b1a86dc83b5d40",
             "title":"locationIsCaptialOfLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"2715f07c317e49108756ee6d0fee4487",
             "displayName":"is capital of"
          },
          {
             "id":"ec23896f39cd4e01809a7c128a65cdeb",
             "title":"personHasLivedLocation",
             "dest":"2715f07c317e49108756ee6d0fee4487",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has lived in"
          },
          {
             "id":"ee408afac35f404abca68af812f2c738",
             "title":"personIsEmployedByOrganizaton",
             "dest":"c1188ec1bafd45068c7acea5b482f1e3",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"is employed by"
          },
          {
             "id":"f3a75e061dac429e975cd82b7cbac965",
             "title":"personHasCitizenshipInCountry",
             "dest":"c31b69c6def4402e9264cc0fc6773628",
             "source":"a883053f7d4e41be877218121836d0dc",
             "displayName":"has citizenship in"
          },
          {
             "id":"f8620277cd564f11b3a918a9fb04aa80",
             "title":"organizationHasFaxNumber",
             "dest":"1c5c1f1f09ee4856a568261f69266af8",
             "source":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"has fax number"
          }
       ],
       "concepts":[
          {
             "id":"1c5c1f1f09ee4856a568261f69266af8",
             "glyphIconHref":"/resource/1c5c1f1f09ee4856a568261f69266af8",
             "title":"phoneNumber",
             "color":"rgb(225, 225, 24)",
             "pluralDisplayName":"Phone Numbers",
             "properties":[
                "1743f51d23cc4b1698f6471ce2b5b6b0"
             ],
             "parentConcept":"3c57da9ddc27460fad033bfa10a51616",
             "displayName":"Phone Number"
          },
          {
             "id":"2715f07c317e49108756ee6d0fee4487",
             "glyphIconHref":"/resource/2715f07c317e49108756ee6d0fee4487",
             "title":"location",
             "color":"rgb(160, 7, 206)",
             "pluralDisplayName":"Locations",
             "properties":[
                "74458b1c1d164f84be868c7eb2940f85",
                "f4e5a6171c7c4ac48618d89485f3d8a9"
             ],
             "parentConcept":"276b0e0297e94804891b6d8e5fc2317b",
             "displayName":"Location"
          },
          {
             "id":"276b0e0297e94804891b6d8e5fc2317b",
             "glyphIconHref":"/resource/276b0e0297e94804891b6d8e5fc2317b",
             "title":"entity",
             "pluralDisplayName":"Entities",
             "properties":[
                "9ff5c51a2fc34f3b90bd40749c755682",
                "fbec470446234463818a4c5d2c7b0ad9"
             ],
             "parentConcept":"71755900a93045b7b83e0e8a088e0af1",
             "displayName":"Entity"
          },
          {
             "id":"3ab2648c41074f98a1f2922a9a01375f",
             "glyphIconHref":"/resource/3ab2648c41074f98a1f2922a9a01375f",
             "title":"raw",
             "color":"rgb(28, 137, 28)",
             "pluralDisplayName":"Raws",
             "properties":[

             ],
             "parentConcept":"276b0e0297e94804891b6d8e5fc2317b",
             "displayName":"Raw"
          },
          {
             "id":"3c57da9ddc27460fad033bfa10a51616",
             "glyphIconHref":"/resource/3c57da9ddc27460fad033bfa10a51616",
             "title":"contact",
             "color":"rgb(225, 128, 0)",
             "pluralDisplayName":"Contact Informations",
             "properties":[
                "1743f51d23cc4b1698f6471ce2b5b6b0",
                "74458b1c1d164f84be868c7eb2940f85"
             ],
             "parentConcept":"276b0e0297e94804891b6d8e5fc2317b",
             "displayName":"Contact Information"
          },
          {
             "id":"414a4b6e4e0f4a54a80b0dd356169df4",
             "glyphIconHref":"/resource/414a4b6e4e0f4a54a80b0dd356169df4",
             "title":"document",
             "color":"rgb(28, 137, 28)",
             "displayType":"document",
             "pluralDisplayName":"Documents",
             "properties":[
                "74458b1c1d164f84be868c7eb2940f85",
                "815e3372194e4cb1ae90e1a98149b7ec"
             ],
             "parentConcept":"3ab2648c41074f98a1f2922a9a01375f",
             "displayName":"Document"
          },
          {
             "id":"71755900a93045b7b83e0e8a088e0af1",
             "title":"rootConcept",
             "pluralDisplayName":"rootConcepts",
             "properties":[
                "45ee5c0f771d4bef8fbe64e22a30f174",
                "d79b5e8d621240f2ae702b7e8da9b225"
             ],
             "displayName":"rootConcept"
          },
          {
             "id":"8011f34ad46d4397b8317004eb660aa5",
             "glyphIconHref":"/resource/8011f34ad46d4397b8317004eb660aa5",
             "title":"event",
             "color":"rgb(23, 30, 239)",
             "pluralDisplayName":"Events",
             "properties":[
                "00ac5fc9192d44a3a8f90c981e670dda",
                "74458b1c1d164f84be868c7eb2940f85",
                "f4e5a6171c7c4ac48618d89485f3d8a9"
             ],
             "parentConcept":"276b0e0297e94804891b6d8e5fc2317b",
             "displayName":"Event"
          },
          {
             "id":"8db669c91f8d4abfba4b3b497fa0c820",
             "glyphIconHref":"/resource/8db669c91f8d4abfba4b3b497fa0c820",
             "title":"state",
             "color":"rgb(153, 0, 153)",
             "pluralDisplayName":"States",
             "properties":[

             ],
             "parentConcept":"2715f07c317e49108756ee6d0fee4487",
             "displayName":"State"
          },
          {
             "id":"92af2132fece4943b1511dc2850cea31",
             "glyphIconHref":"/resource/92af2132fece4943b1511dc2850cea31",
             "title":"company",
             "color":"rgb(210, 52, 32)",
             "pluralDisplayName":"Companies",
             "properties":[
                "e5797b825a444ce4b22036543fafc028"
             ],
             "parentConcept":"c1188ec1bafd45068c7acea5b482f1e3",
             "displayName":"Company"
          },
          {
             "id":"9d7208266708467cac44a336380e1dc6",
             "glyphIconHref":"/resource/9d7208266708467cac44a336380e1dc6",
             "title":"video",
             "color":"rgb(149, 138, 218)",
             "displayType":"video",
             "pluralDisplayName":"Videos",
             "properties":[
                "74458b1c1d164f84be868c7eb2940f85",
                "815e3372194e4cb1ae90e1a98149b7ec"
             ],
             "parentConcept":"3ab2648c41074f98a1f2922a9a01375f",
             "displayName":"Video"
          },
          {
             "id":"a883053f7d4e41be877218121836d0dc",
             "glyphIconHref":"/resource/a883053f7d4e41be877218121836d0dc",
             "title":"person",
             "color":"rgb(28, 137, 28)",
             "pluralDisplayName":"Persons",
             "properties":[
                "12c4ddb749a44df680c67fac31967cc9",
                "1bfb8b48a51d4f4c9cacc08076e49942",
                "50cf8aac59284ccaa2a3f447a6b8baa1",
                "74458b1c1d164f84be868c7eb2940f85"
             ],
             "parentConcept":"276b0e0297e94804891b6d8e5fc2317b",
             "displayName":"Person"
          },
          {
             "id":"ab723867790547f88eff9e9306b1ada1",
             "glyphIconHref":"/resource/ab723867790547f88eff9e9306b1ada1",
             "title":"city",
             "color":"rgb(191, 13, 191)",
             "pluralDisplayName":"Cities",
             "properties":[

             ],
             "parentConcept":"2715f07c317e49108756ee6d0fee4487",
             "displayName":"City"
          },
          {
             "id":"c1188ec1bafd45068c7acea5b482f1e3",
             "glyphIconHref":"/resource/c1188ec1bafd45068c7acea5b482f1e3",
             "title":"organization",
             "color":"rgb(137, 39, 26)",
             "pluralDisplayName":"Organizations",
             "properties":[
                "74458b1c1d164f84be868c7eb2940f85",
                "e013298ce9e94385a0f1f696fb1ded83",
                "ee3429390860472ba24886ccc8524516"
             ],
             "parentConcept":"276b0e0297e94804891b6d8e5fc2317b",
             "displayName":"Organization"
          },
          {
             "id":"c31b69c6def4402e9264cc0fc6773628",
             "glyphIconHref":"/resource/c31b69c6def4402e9264cc0fc6773628",
             "title":"country",
             "color":"rgb(112, 0, 112)",
             "pluralDisplayName":"Countries",
             "properties":[

             ],
             "parentConcept":"2715f07c317e49108756ee6d0fee4487",
             "displayName":"Country"
          },
          {
             "id":"de80411ce98b4630afbe33ae474e681a",
             "glyphIconHref":"/resource/de80411ce98b4630afbe33ae474e681a",
             "title":"address",
             "color":"rgb(219, 63, 219)",
             "pluralDisplayName":"Addresses",
             "properties":[

             ],
             "parentConcept":"2715f07c317e49108756ee6d0fee4487",
             "displayName":"Address"
          },
          {
             "id":"ec39fc903bfd456b8a97322516d09ee0",
             "glyphIconHref":"/resource/ec39fc903bfd456b8a97322516d09ee0",
             "title":"image",
             "color":"rgb(176, 87, 53)",
             "displayType":"image",
             "pluralDisplayName":"Images",
             "properties":[
                "74458b1c1d164f84be868c7eb2940f85",
                "815e3372194e4cb1ae90e1a98149b7ec"
             ],
             "parentConcept":"3ab2648c41074f98a1f2922a9a01375f",
             "displayName":"Image"
          },
          {
             "id":"eef34867877d49869694bee88ac5c4b5",
             "title":"http://lumify.io/workspace",
             "pluralDisplayName":"workspaces",
             "properties":[

             ],
             "displayName":"workspace"
          },
          {
             "id":"f7a8d27ce58049b7a9c2f5574dc08996",
             "title":"http://lumify.io/user",
             "pluralDisplayName":"lumifyUsers",
             "properties":[

             ],
             "displayName":"lumifyUser"
          },
          {
             "id":"fc3968a6edc3443ab6e8e668d1a919e1",
             "glyphIconHref":"/resource/fc3968a6edc3443ab6e8e668d1a919e1",
             "title":"emailAddress",
             "color":"rgb(203, 130, 4)",
             "pluralDisplayName":"Email Addresses",
             "properties":[
                "1743f51d23cc4b1698f6471ce2b5b6b0"
             ],
             "parentConcept":"3c57da9ddc27460fad033bfa10a51616",
             "displayName":"Email Address"
          }
       ]
    };
});
