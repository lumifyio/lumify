define([
    'require',
    'flight/lib/component',
    'hbs!./searchTpl'
], function(
    require,
    defineComponent,
    template) {
    'use strict';

    var SEARCH_TYPES = ['Lumify', 'Workspace'];

    return defineComponent(Search);

    function Search() {

        this.savedQueries = _.indexBy(SEARCH_TYPES.map(function(type) {
            return {
                type: type,
                query: '',
                filters: []
            }
        }), 'type');

        this.defaultAttrs({
            formSelector: '.navbar-search',
            querySelector: '.navbar-search .search-query',
            queryValidationSelector: '.search-query-validation',
            clearSearchSelector: '.search-query-container a',
            segmentedControlSelector: '.segmented-control',
            filtersInfoSelector: '.filter-info',
            searchTypeSelector: '.search-type'
        });

        this.after('initialize', function() {
            this.render();
            this.triggerQueryUpdated = _.debounce(this.triggerQueryUpdated.bind(this), 500);

            this.on('click', {
                segmentedControlSelector: this.onSegmentedControlsClick
            });
            this.on('change keydown keyup paste', this.onQueryChange);
            this.on(this.select('querySelector'), 'focus', this.onQueryFocus);
        });

        this.onQueryChange = function(event) {
            if (event.which === $.ui.keyCode.ENTER) {
                if (event.type === 'keyup') {
                    var searchType = this.getSearchTypeNode();

                    this.trigger(searchType, 'querysubmit', {
                        value: $.trim($(event.target).val())
                    });
                }
            } else {
                this.triggerQueryUpdated();
            }
        };

        this.onSegmentedControlsClick = function(event, data) {
            event.stopPropagation();

            this.switchSearchType(
                $(event.target)
                    .blur()
                    .addClass('active')
                    .siblings('button').removeClass('active').end()
                    .data('type')
            );
            this.select('querySelector').focus();
        };

        this.onQueryFocus = function(event) {
            this.switchSearchType(this.searchType || SEARCH_TYPES[0]);
        };

        this.switchSearchType = function(newSearchType) {
            if (this.searchType === newSearchType) {
                return;
            }

            var self = this,
                $query = this.select('querySelector');

            console.log('switching', this.searchType, $query.val())
            if (this.searchType) {
                this.savedQueries[this.searchType].query = $query.val();
            }
            $query.val(this.savedQueries[newSearchType].query);

            this.searchType = newSearchType;
            var node = this.getSearchTypeNode()
                    .addClass('active')
                    .siblings('.search-type').removeClass('active').end();

            require(['./types/type' + newSearchType], function(SearchType) {
                SearchType.attachTo(node);

                self.trigger('searchtypeloaded', { type: newSearchType });
            });
        };

        this.triggerQueryUpdated = function() {
            var $query = this.select('querySelector'),
                searchType = this.getSearchTypeNode();

            this.trigger(searchType, 'queryupdated', {
                value: $.trim($query.val())
            });
        };

        this.getSearchTypeNode = function() {
            return this.$node.find('.search-type-' + this.searchType.toLowerCase());
        };

        this.render = function() {
            var self = this;

            this.$node.html(template({
                types: SEARCH_TYPES.map(function(type, i) {
                    return {
                        cls: type.toLowerCase(),
                        name: type,
                        selected: i === 0
                    }
                }),
            }));
        };
    }
});
