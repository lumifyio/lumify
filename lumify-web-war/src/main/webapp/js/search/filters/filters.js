
define([
    'flight/lib/component',
    'flight/lib/registry',
    'tpl!./filters',
    'tpl!./item',
    'tpl!./entityItem',
    'data',
    'util/formatters',
    'fields/selection/selection',
    'service/ontology'
], function(
    defineComponent,
    registry,
    template,
    itemTemplate,
    entityItemTemplate,
    appData,
    formatters,
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
            removeRowSelector: '.prop-filters button.remove'
        });

        this.after('initialize', function() {
            this.notifyOfFilters = _.debounce(this.notifyOfFilters.bind(this), FILTER_SEARCH_DELAY_SECONDS * 1000);

            this.$node.html(template({}));

            this.on('propertychange', this.onPropertyFieldItemChanged);
            this.on('propertyselected', this.onPropertySelected);
            this.on('clearfilters', this.onClearFilters);
            this.on('click', {
                removeEntityRowSelector: this.onRemoveEntityRow,
                removeRowSelector: this.onRemoveRow
            });
            this.on(document, 'searchByRelatedEntity', this.onSearchByRelatedEntity);

            this.loadPropertyFilters();
        });

        this.onSearchByRelatedEntity = function(event, data) {
            this.onClearFilters();

            this.entityFilters.relatedToVertexId = data.vertexId;
            var vertex = appData.vertex(data.vertexId),
                title = vertex && formatters.vertex.prop(vertex, 'title') || data.vertexId;

            this.$node.find('.entity-filter-header')
                .after(entityItemTemplate({title: title}))
                .closest('.entity-filters').show();
            this.notifyOfFilters();
        }

        this.onClearFilters = function(event, data) {
            var self = this,
                nodes = this.$node.find('.configuration');

            nodes.each(function() {
                self.teardownField($(this));
            }).closest('li:not(.newrow)').remove();

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
                li = target.closest('li'),
                property = data.property;

            require(['fields/' + property.dataType], function(PropertyFieldItem) {
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

        this.createNewRowIfNeeded = function() {
            if (this.$node.find('.newrow').length === 0) {
                this.$node.find('.prop-filters').append(itemTemplate({properties: this.properties}));
                FieldSelection.attachTo(this.select('fieldSelectionSelector'), {
                    properties: this.properties,
                    placeholder: 'Add Filter'
                });
            }
        };

        this.onPropertyFieldItemChanged = function(event, data) {
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
                    if (!info.instance.isValid || info.instance.isValid()) {
                        self.notifyOfFilters();
                    }
                    info.instance.teardown();
                });
            }

            node.empty();
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
