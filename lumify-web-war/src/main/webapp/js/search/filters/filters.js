
define([
    'flight/lib/component',
    'flight/lib/registry',
    'hbs!./filtersTpl',
    'tpl!./item',
    'tpl!./entityItem',
    'data',
    'util/vertex/formatters',
    'hbs!util/ontology/concept-options',
    'fields/selection/selection',
    'service/ontology'
], function(
    defineComponent,
    registry,
    template,
    itemTemplate,
    entityItemTemplate,
    appData,
    F,
    conceptsTemplate,
    FieldSelection,
    OntologyService) {
    'use strict';

    var FILTER_SEARCH_DELAY_SECONDS = 0.5;

    return defineComponent(Filters);

    function Filters() {
        this.propertyFilters = {};
        this.entityFilters = {};
        this.filterId = 0;

        this.ontologyService = new OntologyService();

        this.defaultAttrs({
            fieldSelectionSelector: '.newrow .add-property',
            removeEntityRowSelector: '.entity-filters button.remove',
            removeRowSelector: '.prop-filters button.remove',
            conceptsSelector: '.concepts-dropdown select'
        });

        this.after('initialize', function() {
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
            this.on('change', {
                conceptsSelector: this.onConceptChange
            });
            this.on('searchByRelatedEntity', this.onSearchByRelatedEntity);

            this.loadPropertyFilters();
            this.loadConcepts();
        });

        this.onSearchByRelatedEntity = function(event, data) {
            event.stopPropagation();

            this.onClearFilters();

            this.entityFilters.relatedToVertexId = data.vertexId;
            var vertex = appData.vertex(data.vertexId),
                title = vertex && F.vertex.title(vertex) || data.vertexId;

            this.$node.find('.entity-filter-header')
                .after(entityItemTemplate({title: title}))
                .closest('.entity-filters').show();
            this.notifyOfFilters();
        };

        this.onConceptChange = function(event) {
            var self = this,
                deferred = $.Deferred().done(function(properties) {
                    self.select('fieldSelectionSelector').each(function() {
                        self.trigger(this, 'filterProperties', {
                            properties: properties && properties.list
                        });
                    });
                });

            this.conceptFilter = $(event.target).val();

            // Publish change to filter properties typeaheads
            if (this.conceptFilter) {
                this.ontologyService.propertiesByConceptId(this.conceptFilter)
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

            this.select('conceptsSelector').val('');
            this.conceptFilter = '';

            this.createNewRowIfNeeded();

            this.entityFilters = {};
            this.$node.find('.entity-filter-header').nextAll().remove();
            this.$node.find('.entity-filter-header').closest('.entity-filters').hide();
            if (!data || data.triggerUpdates !== false) {
                this.notifyOfFilters();
            }
        };

        this.notifyOfFilters = function() {
            this.trigger('filterschange', {
                entityFilters: this.entityFilters,
                conceptFilter: this.conceptFilter,
                propertyFilters: _.map(this.propertyFilters, function(filter) {
                    return {
                        propertyId: filter.propertyId,
                        predicate: filter.predicate,
                        values: filter.values
                    };
                })
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
                property = data.property,
                fieldComponent = property.possibleValues ?
                    'fields/restrictValues' :
                    'fields/' + property.dataType;

            require([fieldComponent], function(PropertyFieldItem) {
                var node = li.find('.configuration');

                self.teardownField(node);

                PropertyFieldItem.attachTo(node, {
                    property: property,
                    id: self.filterId++,
                    predicates: true
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

        this.createNewRowIfNeeded = function() {
            if (this.$node.find('.newrow').length === 0) {
                this.$node.find('.prop-filters').append(itemTemplate({properties: this.properties}));
                FieldSelection.attachTo(this.select('fieldSelectionSelector'), {
                    properties: this.properties,
                    onlySearchable: true,
                    placeholder: 'Add Filter'
                });
            }
        };

        this.onPropertyFieldItemChanged = function(event, data) {
            this.$node.find('li.fId' + data.id).removeClass('invalid');
            this.propertyFilters[data.id] = data;
            this.notifyOfFilters();
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
            var self = this;

            this.ontologyService.concepts().done(function(concepts) {
                self.select('conceptsSelector').html(
                    conceptsTemplate({
                        defaultText: 'All Concepts',
                        concepts: _.filter(concepts.byTitle, function(c) {
                            return c.userVisible !== false;
                        })
                    })
                );
            });
        };

        this.loadPropertyFilters = function() {
            var self = this;

            this.ontologyService.properties().done(function(properties) {
                self.properties = _.filter(properties.list, function(p) {
                    if (p.title === 'boundingBox') return false;
                    if (/^_/.test(p.title)) return false;
                    return true;
                });
                self.$node.find('.prop-filter-header').after(itemTemplate({}));
                FieldSelection.attachTo(self.select('fieldSelectionSelector'), {
                    properties: self.properties,
                    placeholder: 'Add Filter'
                });
            });
        };
    }
});
