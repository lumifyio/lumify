
define([
    'flight/lib/component',
    'tpl!./selection'
], function(
    defineComponent,
    template) {
    'use strict';

    return defineComponent(FieldSelection);

    function FieldSelection() {

        var PLACEHOLDER = 'Type to filter properties';

        this.defaultAttrs({
            findPropertySelection: 'input'
        });

        this.after('initialize', function() {
            var self = this;

            this.$node.html(template({placeholder:this.attr.placeholder}));

            if (this.attr.properties.length === 0) {
                this.select('findPropertySelection')
                    .attr('placeholder', 'No valid properties')
                    .attr('disabled', true);
            } else {
                this.select('findPropertySelection')
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
                        source: _.map(this.attr.properties, function(p) {
                            return p.displayName || p.title;
                        }),
                        matcher: function(item) {
                            if (this.query === ' ') return -1;
                            if (
                                this.query && 
                                self.currentProperty &&
                                    (self.currentProperty.displayName === this.query || 
                                     self.currentProperty.title === this.query)
                            ) {
                                return 1;
                            }
                            return Object.getPrototypeOf(this).matcher.apply(this, arguments);
                        },
                        sorter: function(items) {
                            var sorted = Object.getPrototypeOf(this).sorter.apply(this, arguments),
                                index;

                            return sorted;
                        },
                        updater: function(item) {
                            self.propertySelected(item);
                            return item;
                        }
                    })
                    .data('typeahead').lookup = allowEmptyLookup;
            }
        });

        this.propertySelected = function(name) {
            var property = _.findWhere(this.attr.properties, { displayName:name }) ||
                           _.findWhere(this.attr.properties, { title:name });

            if (property) {
                this.currentProperty = property;
                this.trigger('propertyselected', { property:property });
                _.defer(function() {
                    this.select('findPropertySelection').blur();
                }.bind(this));
            }
        };
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
