define([], function() {
    'use strict';

    return withSelect;

    function withSelect() {
        this.allowEmptyLookup = function(typeaheadField) {
            typeaheadField.data('typeahead').lookup = allowEmptyLookup;
        }
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
