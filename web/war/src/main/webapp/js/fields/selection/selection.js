
define([
    'flight/lib/component',
    'tpl!./selection'
], function(
    defineComponent,
    template) {
    'use strict';

    var HIDE_PROPERTIES = ['http://lumify.io/comment#entry'];

    return defineComponent(FieldSelection);

    function FieldSelection() {

        var PLACEHOLDER = i18n('field.selection.placeholder');

        this.defaultAttrs({
            findPropertySelection: 'input'
        });

        this.after('initialize', function() {
            var self = this;

            this.updatePropertiesSource();
            this.on('filterProperties', this.onFilterProperties);

            this.$node.html(template({placeholder: this.attr.placeholder}));

            if (this.attr.properties.length === 0 || this.attr.properties.length.value === 0) {
                this.select('findPropertySelection')
                    .attr('placeholder', i18n('field.selection.no_valid'))
                    .attr('disabled', true);
            } else {
                this.queryPropertyMap = {};

                var typeahead = this.select('findPropertySelection')
                    .on('focus', function(e) {
                        var target = $(e.target);
                        target.attr('placeholder', PLACEHOLDER)
                    })
                    .on('click', function(e) {
                        var target = $(e.target);

                        if (target.val()) {
                            target.typeahead('lookup').select();
                        } else {
                            target.typeahead('lookup');
                        }

                        target.attr('placeholder', PLACEHOLDER);
                    })
                    .on('change blur', function(e) {
                        var target = $(e.target);
                        if (self.currentProperty) {
                            target.val(self.currentProperty.displayName || self.currentProperty.title);
                        } else {
                            target.val('');
                        }
                        target.attr('placeholder', self.attr.placeholder);
                    })
                    .typeahead({
                        minLength: 0,
                        items: 100,
                        source: function() {
                            return _.chain(self.propertiesForSource)
                                    .filter(function(p) {
                                        var visible = p.userVisible !== false,
                                            addable = p.addable !== false;

                                        if (self.attr.unsupportedProperties &&
                                            ~self.attr.unsupportedProperties.indexOf(p.title)) {
                                            return false;
                                        }

                                        if (~HIDE_PROPERTIES.indexOf(p.title)) {
                                            return false;
                                        }

                                        if (~self.dependentPropertyIris.indexOf(p.title)) {
                                            return false;
                                        }

                                        if (self.attr.onlySearchable) {
                                            if (p.title === 'http://lumify.io#text') {
                                                return true;
                                            }
                                            return visible && p.searchable !== false;
                                        }

                                        return visible && addable;
                                    })
                                    .map(function(p) {
                                        var name = displayName(p),
                                            duplicates = self.groupedByDisplay[name].length > 1;

                                        self.queryPropertyMap[p.title] = p;

                                        return JSON.stringify({
                                            displayName: name,
                                            title: p.title,
                                            propertyGroup: p.propertyGroup,
                                            duplicates: duplicates
                                        });
                                    })
                                    .sortBy(function(itemJson) {
                                        var item = JSON.parse(itemJson),
                                            lower = item.displayName.toLowerCase();

                                        if (item.propertyGroup) {
                                            return '1' + item.propertyGroup + lower;
                                        }
                                        return '0' + lower;
                                    })
                                    .value()
                        },
                        matcher: function(itemJson) {
                            if (this.query === ' ') return -1;

                            var item = JSON.parse(itemJson);

                            if (
                                this.query &&
                                self.currentProperty &&
                                self.currentProperty.title === item.title) {
                                return 1;
                            }
                            return Object.getPrototypeOf(this).matcher.apply(this, [item.displayName]);
                        },
                        highlighter: function(itemJson) {
                            var item = JSON.parse(itemJson);
                            if (item.duplicates) {
                                return item.displayName +
                                    _.template('<div title="{title}" class="subtitle">{title}</div>')(item)
                            }
                            return item.displayName;
                        },
                        sorter: function(items) {
                            var query = this.query;

                            return _.sortBy(items, function(json) {
                                var item = JSON.parse(json),
                                    displayName = item.displayName,
                                    group = item.propertyGroup;

                                if (query) {
                                    if (displayName === query) {
                                        return '0';
                                    }

                                    return '1' + displayName;
                                } else {
                                    if (group) {
                                        return '1' + displayName;
                                    }
                                    return '0' + displayName;
                                }
                            });
                        },
                        updater: function(itemJson) {
                            var item = JSON.parse(itemJson);
                            self.propertySelected(item);
                            return item;
                        }
                    })
                    .data('typeahead');

                typeahead.lookup = allowEmptyLookup;
                typeahead.render = function(items) {
                    var self = this,
                        $items = $(),
                        lastGroup;

                    items.forEach(function(item, i) {
                        var itemJson = JSON.parse(item);
                        if (itemJson.propertyGroup && lastGroup !== itemJson.propertyGroup) {
                            lastGroup = itemJson.propertyGroup;
                            $items = $items.add($('<li class="divider">'));
                            $items = $items.add($('<li class="nav-header">').text(itemJson.propertyGroup)[0]);
                        }

                        var $item = $(self.options.item).attr('data-value', item)
                            .toggleClass('active', i === 0)
                            .find('a').html(self.highlighter(item))
                            .end();
                        $items = $items.add($item);
                    })

                    this.$menu.empty().append($items)
                    return this;
                };
            }
        });

        this.onFilterProperties = function(event, data) {
            this.updatePropertiesSource(data.properties);
        };

        this.propertySelected = function(item) {
            var property = this.queryPropertyMap[item.title];

            if (property) {
                this.currentProperty = property;
                this.trigger('propertyselected', { property: property });
                _.defer(function() {
                    this.select('findPropertySelection').blur();
                }.bind(this));
            }
        };

        this.updatePropertiesSource = function(filtered) {
            var properties = filtered || this.attr.properties;

            this.groupedByDisplay = _.groupBy(properties, displayName);
            this.propertiesForSource = properties;
            this.dependentPropertyIris = _.chain(properties)
                .pluck('dependentPropertyIris')
                .compact()
                .flatten()
                .value();
        }
    }

    function displayName(p) {
        return p.displayName || p.title;
    }

    function allowEmptyLookup() {
        var items;

        this.query = this.$element.val();

        // Remove !this.query check to allow empty values to open dropdown
        if (this.query.length < this.options.minLength) {
            return this.shown ? this.hide() : this;
        }

        items = $.isFunction(this.source) ? this.source(this.query, $.proxy(this.process, this)) : this.source;

        return items ? this.process(items) : this;
    }
});
