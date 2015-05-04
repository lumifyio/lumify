define([
    'dataRequestHandler',
    'tpl!search/filters/item',
    'tpl!search/filters/entityItem'
    ], function(
        DataRequestHandler,
        itemTemplate,
        entityItemTemplate
    ) {

    describeComponent('search/filters/filters', function(Filters) {

        var testId = 1;

        beforeEach(function() {
            setupComponent(this);
        })

        describe('Filters', function() {

            describe('on initialize', function(){

                it('should initialize', function() {
                    var c = this.component
                })

            })

            describe('on propertychange events', function(){

                it('should trigger filterschange when isScrubbing is true', function(done){
                    var c = this.component,
                        data = {
                            id: 2,
                            options: {
                                isScrubbing: true
                            }
                        },
                         oProperties = {
                             byTitle: []
                         };

                   c.on(document, 'filterschange', function() {
                        c.off(document, 'filterschange');
                        done();
                    })

                    c.ontologyProperties = oProperties;

                    c.trigger('propertychange', data);
                })
            })

            describe('on propertyselected events', function(){

                it('should add fIdx class to the list item and remove the newrow class', function(done) {
                    var c = this.component,
                        fieldSelection,
                        testFilterId = 4,
                        data = {
                            property: {
                                dataType: 'boolean'
                            }
                        };

                    addItem(c);
                    c.properties = {length: 0};
                    c.createFieldSelection();
                    c.filterId = testFilterId;
                    fieldSelection = c.select('fieldSelectionSelector');

                    expect(getItems(c).select('li').hasClass('fId' + testFilterId)).to.be.false;
                    expect(getItems(c).select('li').hasClass('newrow')).to.be.true;


                    // Need to delay the expectation. Overwriting function called at the end of method
                    // whose functionality doesn't pertain to this particular unit test
                    c.createNewRowIfNeeded = function() {
                        expect(getItems(c).select('li').hasClass('newrow')).to.be.false;
                        done();
                    };
                    fieldSelection.trigger('propertyselected', data);
                    expect(getItems(c).select('li').hasClass('fId' + testFilterId)).to.be.true;
                })

                it('should attach an input', function(done) {
                    var c = this.component,
                        fieldSelection,
                        testFilterId = 7,
                        inputSelector = '.predicate-row',
                        data = {
                            property: {
                                dataType: 'boolean'
                            }
                        };

                    addItem(c);
                    c.properties = {length: 0};
                    c.createFieldSelection();
                    c.filterId = testFilterId;
                    fieldSelection = c.select('fieldSelectionSelector');


                    // Need to delay the expectation. Overwriting function called at the end of method
                    // whose functionality doesn't pertain to this particular unit test
                    c.createNewRowIfNeeded = function() {
                        expect(getItem(c, testFilterId).find(inputSelector).length).to.eq(1);
                        done();
                    };
                    fieldSelection.trigger('propertyselected', data);
                })

            })

            describe('on propertyinvalid events', function(){

                it('should add invalid class to the list item', function() {
                    var c = this.component,
                        fieldSelection,
                        testFilterId = 9,
                        data = {
                            id: testFilterId
                        };

                    addItem(c);
                    c.properties = {length: 0};
                    c.createFieldSelection();
                    c.filterId = testFilterId;
                    c.select('fieldSelectionSelector').trigger('propertyselected', {property: {dataType: 'boolean'}});

                    expect(getItem(c, testFilterId).select('li').hasClass('invalid')).to.be.false;
                    c.trigger('propertyinvalid', data);
                    expect(getItem(c, testFilterId).select('li').hasClass('invalid')).to.be.true;
                })

            })

            describe('on clearfilters events', function(){

               it('should remove all entity filters', function() {
                   var c = this.component,
                       data = {
                        triggerUpdates: false
                        },
                        numItems = 3;

                   for(var i = 0; i < numItems; i++) {
                        addEntityItem(c);
                   }

                   expect(getEntityItems(c).length).to.eql(numItems);
                   c.trigger('clearfilters', data);
                   expect(getEntityItems(c).length).to.eql(0);
               })

               it('should create a property filter', function() {
                    var c = this.component,
                       data = {
                            triggerUpdates: false
                       },
                       properties = {
                            length: 0
                       };

                   c.properties = properties;

                   expect(getItems(c).length).to.eql(0);
                   c.trigger('clearfilters', data);
                   expect(getItems(c).length).to.eql(1);
               })

                it('should trigger clearSelectedConcept on the concept dropdown', function(done) {
                    var c = this.component,
                        data = {
                            triggerUpdates: false
                        }
                        conceptDropdown = c.select('conceptDropdownSelector');

                    conceptDropdown.on('clearSelectedConcept', function() {
                        done();
                    })

                    c.trigger('clearfilters', data);
                })

            })

            describe('on click events', function(){

                it('should remove item when remove button is clicked', function() {
                    var c = this.component,
                        removeButton;

                    addItem(c);

                    removeButton = c.$node.find('button.remove');

                    expect(getItems(c).length).to.eql(1);
                    removeButton.click();
                    expect(getItems(c).length).to.eql(0);
                })

                it('should remove entity item when remove button is clicked', function() {
                    var c = this.component,
                        removeButton;

                        addEntityItem(c);

                        removeButton = c.$node.find('button.remove');

                        expect(getEntityItems(c).length).to.eql(1);
                        removeButton.click();
                        expect(getEntityItems(c).length).to.eql(0);
                })

            })

            describe('on conceptSelected events', function(){

                it('should trigger filterProperties on items', function(done) {
                    var c = this.component,
                        data = {};

                        addItem(c);

                        getItems(c).on('filterProperties', function() {
                            done();
                        })

                        c.trigger('conceptSelected', data);
                })

            })

            describe('on searchByRelatedEntity events', function(){

                describe('when there are zero results', function() {

                    beforeEach(function() {
                        var response = [];
                        DataRequestHandler.setResponse('vertex', 'store', true, response);
                        DataRequestHandler.listen(this.component);
                    })

                    it('should add an entity item', function(done) {
                        var c = this.component,
                            data = {};


                        expect(getEntityItems(c).length).to.eql(0);
                        c.trigger('searchByRelatedEntity', data);

                        _.defer(function() {
                            expect(getEntityItems(c).length).to.eql(1);
                            done();
                        });
                    })

                })

                describe('when there is one result', function() {

                    beforeEach(function() {
                        var response = [testVertex()];
                        DataRequestHandler.setResponse('vertex', 'store', true, response);
                        DataRequestHandler.listen(this.component);
                    })

                    it('should add an entity item', function(done) {
                        var c = this.component,
                            data = {};


                        expect(getEntityItems(c).length).to.eql(0);
                        c.trigger('searchByRelatedEntity', data);

                        _.defer(function() {
                            expect(getEntityItems(c).length).to.eql(1);
                            done();
                        });
                    })

                    it('should set the correct title on the new entity item', function(done) {
                        var c = this.component,
                            data = {};


                        expect(getEntityItems(c).length).to.eql(0);
                        c.trigger('searchByRelatedEntity', data);

                        _.defer(function() {
                            expect(getEntityItems(c).find('.configuration').find('input').attr('value')).to.eql('Steimel, Tommy');
                            done();
                        });
                    })

                })

                describe('when there are multiple results', function() {

                    beforeEach(function() {
                        var response = [testVertex(), testVertex(), testVertex()];
                        DataRequestHandler.setResponse('vertex', 'store', true, response);
                        DataRequestHandler.listen(this.component);
                    })

                    it('should add an entity item', function(done) {
                        var c = this.component,
                            data = {};


                        expect(getEntityItems(c).length).to.eql(0);
                        c.trigger('searchByRelatedEntity', data);

                        _.defer(function() {
                            expect(getEntityItems(c).length).to.eql(1);
                            done();
                        });
                    })

                    it('should set the correct title on the new entity item', function(done) {
                        var c = this.component,
                            data = {};


                        expect(getEntityItems(c).length).to.eql(0);
                        c.trigger('searchByRelatedEntity', data);

                        _.defer(function() {
                            expect(getEntityItems(c).find('.configuration').find('input').attr('value'))
                                .to.eql('search.filters.title_multiple');
                            done();
                        });
                    })

                })


            })
        })

        function addItem(c, properties) {
            var propFilters = c.$node.find('.prop-filters');

            properties = properties || {};

            propFilters.append(itemTemplate({properties: properties}));
        }

        function getItems(c) {
            var propFilters = c.$node.find('.prop-filters'),
                itemSelector = '.newrow';

            return propFilters.find(itemSelector);
        }

        function getItem(c, fId) {
            var propFilters = c.$node.find('.prop-filters'),
                itemSelector = '.fId' + fId;

            return propFilters.find(itemSelector);
        }

        function addEntityItem(c, title) {
            var entityFilters = c.$node.find('.entity-filters');

            title = title || '';

            entityFilters.append(entityItemTemplate({title: title}));
        }

        function getEntityItems(c) {
            var entityFilters = c.$node.find('.entity-filters'),
                itemSelector = '.entity-filter-row';

            return entityFilters.find(itemSelector);
        }

        function testVertex() {
            return {
                id: testId++,
                properties: [
                    {name: 'http://lumify.io/dev#firstName', value: 'Tommy'},
                    {name: 'http://lumify.io/dev#lastName', value: 'Steimel'},
                    {name: 'http://lumify.io#conceptType', value: 'http://lumify.io/dev#person'}
                ]
            };
        }

    })
});
