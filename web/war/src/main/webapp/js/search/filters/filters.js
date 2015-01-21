
define([
    'flight/lib/component',
    'flight/lib/registry',
    'hbs!./filtersTpl',
    'tpl!./item',
    'tpl!./entityItem',
    'util/vertex/formatters',
    'util/ontology/conceptSelect',
    'util/withDataRequest',
    'fields/selection/selection'
], function(
    defineComponent,
    registry,
    template,
    itemTemplate,
    entityItemTemplate,
    F,
    ConceptSelector,
    withDataRequest,
    FieldSelection) {
    'use strict';

    var FILTER_SEARCH_DELAY_SECONDS = 0.5;

    return defineComponent(Filters, withDataRequest);

    function Filters() {
        this.propertyFilters = {};
        this.entityFilters = {};
        this.filterId = 0;

        this.defaultAttrs({
            fieldSelectionSelector: '.newrow .add-property',
            removeEntityRowSelector: '.entity-filters button.remove',
            removeRowSelector: '.prop-filters button.remove',
            conceptDropdownSelector: '.concepts-dropdown'
        });

        this.after('initialize', function() {
            this.throttledNotifyOfFilters = _.throttle(this.notifyOfFilters.bind(this), 100);
            this.notifyOfFilters = _.debounce(this.notifyOfFilters.bind(this), FILTER_SEARCH_DELAY_SECONDS * 1000);

            this.$node.html(template({}));

            this.on('propertychange', this.onPropertyFieldItemChanged);
            this.on('propertyselected', this.onPropertySelected);
            this.on('propertyinvalid', this.onPropertyInvalid);
            this.on('clearfilters', this.onClearFilters);
            this.on('click', {
                removeEntityRowSelector: this.onRemoveEntityRow,
                removeRowSelector: this.onRemoveRow
            });
            this.on('conceptSelected', this.onConceptChange);
            this.on('searchByRelatedEntity', this.onSearchByRelatedEntity);

            this.loadPropertyFilters();
            this.loadConcepts();
        });

        this.onSearchByRelatedEntity = function(event, data) {
            event.stopPropagation();
            var self = this;
            this.dataRequest('vertex', 'store', { vertexIds: data.vertexId })
                .done(function(vertex) {
                    var title = vertex && F.vertex.title(vertex) || vertex.id;

                    self.onClearFilters();

                    self.entityFilters.relatedToVertexId = vertex.id;
                    self.conceptFilter = data.conceptId || '';
                    self.trigger(self.select('conceptDropdownSelector'), 'selectConceptId', {
                        conceptId: data.conceptId || ''
                    });
                    self.$node.find('.entity-filters')
                        .append(entityItemTemplate({title: title})).show();
                    self.notifyOfFilters();
                });
        };

        this.onConceptChange = function(event, data) {
            var self = this,
                deferred = $.Deferred().done(function(properties) {
                    self.select('fieldSelectionSelector').each(function() {
                        self.trigger(this, 'filterProperties', {
                            properties: properties && properties.list
                        });
                    });
                });

            this.conceptFilter = data.concept && data.concept.id || '';

            // Publish change to filter properties typeaheads
            if (this.conceptFilter) {
                this.dataRequest('ontology', 'propertiesByConceptId', this.conceptFilter)
                    .done(deferred.resolve);
            } else {
                deferred.resolve();
            }

            this.notifyOfFilters();
        };

        this.onClearFilters = function(event, data) {
            var self = this,
                nodes = this.$node.find('.configuration');

            nodes.each(function() {
                self.teardownField($(this));
            }).closest('li:not(.newrow)').remove();

            this.trigger(this.select('conceptDropdownSelector'), 'clearSelectedConcept');
            this.conceptFilter = '';

            this.createNewRowIfNeeded();

            this.entityFilters = {};
            this.$node.find('.entity-filters').hide().empty();
            if (!data || data.triggerUpdates !== false) {
                this.notifyOfFilters();
            }
        };

        this.notifyOfFilters = function(options) {
            this.trigger('filterschange', {
                entityFilters: this.entityFilters,
                conceptFilter: this.conceptFilter,
                propertyFilters: _.map(this.propertyFilters, function(filter) {
                    return {
                        propertyId: filter.propertyId,
                        predicate: filter.predicate,
                        values: filter.values
                    };
                }),
                options: options
            });
        };

        this.onRemoveEntityRow = function(event, data) {
            var target = $(event.target),
                row = target.closest('.entity-filter-row'),
                section = row.closest('.entity-filters'),
                key = row.data('filterKey');

            row.remove();
            section.hide();
            delete this.entityFilters[key];
            this.notifyOfFilters();
        };

        this.onRemoveRow = function(event, data) {
            var target = $(event.target);
            this.teardownField(target.next('.configuration'));
            target.closest('li').remove();
            this.createNewRowIfNeeded();
        };

        this.onPropertySelected = function(event, data) {
            var self = this,
                target = $(event.target),
                li = target.closest('li').addClass('fId' + self.filterId),
                property = data.property;

            if (property.title === 'http://lumify.io#text') {
                property.dataType = 'boolean';
            }

            var fieldComponent = property.possibleValues ?
                    'fields/restrictValues' :
                    'fields/' + property.dataType;

            require([fieldComponent], function(PropertyFieldItem) {
                var node = li.find('.configuration').removeClass('alternate');

                self.teardownField(node);

                PropertyFieldItem.attachTo(node, {
                    property: property,
                    id: self.filterId++,
                    predicates: true,
                    supportsHistogram: self.attr.supportsHistogram
                });

                li.removeClass('newrow');

                self.createNewRowIfNeeded();
            });
        };

        this.onPropertyInvalid = function(event, data) {
            var li = this.$node.find('li.fId' + data.id);
            li.addClass('invalid');

            delete this.propertyFilters[data.id];
            this.notifyOfFilters();
        };

        this.createFieldSelection = function() {
            FieldSelection.attachTo(this.select('fieldSelectionSelector'), {
                properties: this.properties,
                unsupportedProperties: this.attr.supportsHistogram ?
                    [] :
                    ['http://lumify.io#text'],
                onlySearchable: true,
                placeholder: i18n('search.filters.add_filter.placeholder')
            });
        }

        this.createNewRowIfNeeded = function() {
            if (!this.properties) {
                return;
            }
            if (this.$node.find('.newrow').length === 0) {
                this.$node.find('.prop-filters').append(itemTemplate({properties: this.properties}));
                this.createFieldSelection();
            }
        };

        this.onPropertyFieldItemChanged = function(event, data) {
            this.$node.find('li.fId' + data.id).removeClass('invalid');
            this.propertyFilters[data.id] = data;
            if (data && data.options && data.options.isScrubbing === true) {
                this.throttledNotifyOfFilters(data.options);
            } else {
                this.notifyOfFilters();
            }
            event.stopPropagation();
        };

        this.teardownField = function(node) {
            var self = this,
                instanceInfo = registry.findInstanceInfoByNode(node[0]);
            if (instanceInfo && instanceInfo.length) {
                instanceInfo.forEach(function(info) {
                    delete self.propertyFilters[info.instance.attr.id];
                    self.notifyOfFilters();
                    info.instance.teardown();
                });
            }

            node.empty();
        };

        this.loadConcepts = function() {
            ConceptSelector.attachTo(this.select('conceptDropdownSelector'), {
                onlySearchable: true,
                defaultText: i18n('search.filters.all_concepts')
            })
        };

        this.loadPropertyFilters = function() {
            var self = this;

            this.dataRequest('ontology', 'properties')
                .done(function(properties) {
                    self.properties = properties.list;
                    self.$node.find('.prop-filter-header').after(itemTemplate({}));
                    self.createFieldSelection();
                })
        };
    }
});
