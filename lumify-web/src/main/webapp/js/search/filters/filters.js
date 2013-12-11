

define([
    'flight/lib/component',
    'flight/lib/registry',
    'tpl!./filters',
    'tpl!./item',
    'fields/selection/selection',
    'service/ontology'
], function(
    defineComponent,
    registry,
    template,
    itemTemplate,
    FieldSelection,
    OntologyService) {
    'use strict';

    var FILTER_SEARCH_DELAY_SECONDS = 0.25;

    return defineComponent(Filters);

    function Filters() {
        this.currentFilters = {};
        this.filterId = 0;

        this.ontologyService = new OntologyService();

        this.defaultAttrs({
            fieldSelectionSelector: '.newrow .add-property',
            removeRowSelector: 'button.remove'
        });

        this.after('initialize', function() {
            this.notifyOfFilters = _.debounce(this.notifyOfFilters.bind(this), FILTER_SEARCH_DELAY_SECONDS * 1000);

            this.$node.html(template({}));

            this.on('propertychange', this.onPropertyFieldItemChanged);
            this.on('propertyselected', this.onPropertySelected);
            this.on('clearfilters', this.onClearFilters);
            this.on('click', {
                removeRowSelector: this.onRemoveRow
            });

            this.loadPropertyFilters();
        });

        this.onClearFilters = function() {
            var self = this,
                nodes = this.$node.find('.configuration');

            nodes.each(function() {
                self.teardownField($(this));
            }).closest('li:not(.newrow)').remove();

            this.createNewRowIfNeeded();
        };

        this.notifyOfFilters = function() {
            this.trigger('filterschange', {
                filters: _.map(this.currentFilters, function(filter) {
                    return {
                        propertyId: filter.propertyId,
                        predicate: filter.predicate,
                        values: filter.values
                    };
                })
            });
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
                this.$node.find('ul.nav').append(itemTemplate({properties:this.properties}));
                FieldSelection.attachTo(this.select('fieldSelectionSelector'), {
                    properties: this.properties,
                    placeholder: 'Add Filter'
                });
            }
        };

        this.onPropertyFieldItemChanged = function(event, data) {
            this.currentFilters[data.id] = data;
            this.notifyOfFilters();
            event.stopPropagation();
        };

        this.teardownField = function(node) {
            var self = this, 
                instanceInfo = registry.findInstanceInfoByNode(node[0]);
            if (instanceInfo && instanceInfo.length) {
                instanceInfo.forEach(function(info) {
                    delete self.currentFilters[info.instance.attr.id];
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

            this.ontologyService.properties(function(err, properties) {
                if(err) {
                    console.error('Error', err);
                    return self.trigger(document, 'error', { message: err.toString() });
                }

                self.properties = _.filter(properties.list, function(p) { 
                    if (p.title === 'boundingBox') return false; 
                    if (/^_/.test(p.title)) return false;
                    return true; 
                });
                self.$node.find('.nav-header').after(itemTemplate({}));
                FieldSelection.attachTo(self.select('fieldSelectionSelector'), {
                    properties: self.properties,
                    placeholder: 'Add Filter'
                });
            });
        };
    }
});
