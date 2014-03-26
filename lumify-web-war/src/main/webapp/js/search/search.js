
define([
    'flight/lib/component',
    'flight/lib/registry',
    'data',
    'service/vertex',
    'service/ontology',
    'util/vertexList/list',
    'util/formatters',
    './filters/filters',
    'tpl!./search',
    'tpl!./conceptItem',
    'tpl!./conceptSections',
    'tpl!util/alert',
    'util/jquery.ui.draggable.multiselect',
    'sf'
], function(
    defineComponent,
    registry,
    appData,
    VertexService,
    OntologyService,
    VertexList,
    formatters,
    Filters,
    template,
    conceptItemTemplate,
    conceptSectionsTemplate,
    alertTemplate,
    multiselect,
    sf) {
    'use strict';

    return defineComponent(Search);

    function Search() {
        this.vertexService = new VertexService();
        this.ontologyService = new OntologyService();
        this.currentQuery = null;

        this.defaultAttrs({
            formSelector: '.navbar-search',
            querySelector: '.navbar-search .search-query',
            queryValidationSelector: '.search-query-validation',
            filtersInfoSelector: '.filter-info',
            resultsSummarySelector: '.search-results-summary',
            entitiesHeaderBadgeSelector: '.search-results-summary li.entities .badge',
            summaryResultItemSelector: '.search-results-summary li',
            resultsSelector: '.search-results',
            filtersSelector: '.search-filters'
        });

        this.searchResults = null;

        this.onEntitySearchResultsForConcept = function($searchResultsSummary, concept, entities, count, parentPropertyListElements) {
            var self = this,
                resultsCount = count,
                badge = this.updateCountBadgeForConcept(concept.id, resultsCount),
                li = badge.closest('li').toggle(resultsCount > 0);

            parentPropertyListElements = parentPropertyListElements || $();

            this.select('entitiesHeaderBadgeSelector').removeClass('loading');

            if (resultsCount) {
                parentPropertyListElements.show();
            }

            if(concept.children && concept.children.length > 0) {
                var parentLis = parentPropertyListElements.add(li);
                concept.children.forEach(function(childConcept) {
                    self.onEntitySearchResultsForConcept(
                        $searchResultsSummary,
                        childConcept,
                        entities,
                        count,
                        parentLis
                    );
                });
            }
        };

        this.onFormSearch = function(evt) {
            evt.preventDefault();
            var $searchQueryValidation = this.select('queryValidationSelector');
            $searchQueryValidation.html('');

            var query = this.select('querySelector').val();
            if(query) {
                query = $.trim(query);
            }
            if(!query && !this.relatedToVertexId) {
                this.select('resultsSummarySelector').empty();
                return $searchQueryValidation.html(alertTemplate({ error: 'Query cannot be empty' }));
            }
            this.trigger('search', { query: query });
            return false;
        };

        var conceptToClassMap = {}, classNameToConceptMap = {}, clsIndex = 0;
        this.classSafeConceptId = function(conceptId) {
            if (conceptToClassMap[conceptId]) {
                return conceptToClassMap[conceptId];
            }

            conceptToClassMap[conceptId] = 'cId' + (clsIndex++);
            classNameToConceptMap[conceptToClassMap[conceptId]] = conceptId;

            return conceptToClassMap[conceptId];
        };

        this.getConceptChildrenHtml = function(concept, indent) {
            var self = this,
                html = "";
            (concept.children || []).forEach(function(concept) {
                html += conceptItemTemplate({
                    concept: concept,
                    classSafeConceptId: self.classSafeConceptId(concept.id),
                    indent: indent
                });
                if(concept.children && concept.children.length > 0) {
                    html += self.getConceptChildrenHtml(concept, indent + 15);
                }
            });
            return html;
        };

        this.updateCountBadgeForConcept = function(conceptId, count) {
            return this.$node.find('.' + this.classSafeConceptId(conceptId) + ' .badge')
                .removeClass('loading')
                .data('count', count)
                .attr('title', formatters.number.pretty(count))
                .text(formatters.number.prettyApproximate(count));
        };

        this.popoutIfNeeded = function() {
            if (!this.$node.closest(':data(menubarName)').hasClass('visible')) {
                this.trigger ('menubarToggleDisplay', { name : 'search' });
            }
        }

        this.onSearchByEntity = function (evt, data) {
            this.select('querySelector').val(data.query);
            this.clearFilters({triggerUpdates:false});
            this.trigger ('search', { query : data.query});
        };

        this.onSearchByRelatedEntity = function (evt, data) {
            this.select('querySelector').val('');
        };

        this.onClearSearch = function() {
            this.select('resultsSummarySelector').empty();
            this.clearFilters();
            this.select('querySelector').val('').focus();
        }

        this.onSearch = function(evt, data) {
            data = data || {};
            var query = data.query || this.select('querySelector').val();

            if (!this.searchResults) {
                this.searchResults = {};
            }

            this.select('queryValidationSelector').empty();
            this.hideSearchResults();
            this.popoutIfNeeded();

            if (this.previousSearch) {
                this.previousSearch.abort();
            }

            if (this.infiniteSearchRequest) {
                this.infiniteSearchRequest.abort();
            }

            if (query != data.query) {
                this.select('querySelector').val(data.query);
            }

            var relatedToVertexId = this.entityFilters && this.entityFilters.relatedToVertexId;
            if (relatedToVertexId) {
                query = {
                    query: _.isUndefined(query.query) ? query : query.query,
                    relatedToVertexId: relatedToVertexId
                };
            }

            var self = this;

            this.ontologyService.concepts().done(function(concepts) {
                this.updateConceptSections(concepts);

                var paging = { offset:0, size:100 },
                    subTypeFilter = null;

                this.previousSearch = this.vertexService.graphVertexSearch(query, this.filters, subTypeFilter, paging)
                    .done(function(vertexResults) {
                        self.previousSearch = null;

                        var results = {},
                            sortVerticesIntoResults = function(v) {
                                var props = v.properties,
                                    conceptType = props['http://lumify.io#conceptType'] ? props['http://lumify.io#conceptType'].value : '',
                                    addToSearchResults = function(conceptType) {
                                        if (!results[conceptType]) results[conceptType] = [];

                                        // Check for an existing result with the same id
                                        var resultFound = results[conceptType].some(function(result) { return result.id === v.id; });

                                        // Only store unique results
                                        if (resultFound === false) {
                                            results[conceptType].push(v);
                                        }
                                    };

                                var vertexConcept = concepts.byId[conceptType];
                                while (vertexConcept) {
                                    addToSearchResults(vertexConcept.id, v);
                                    vertexConcept = vertexConcept.parentId ? concepts.byId[vertexConcept.parentId] : null;
                                }
                            };
                        vertexResults.vertices.forEach(sortVerticesIntoResults);

                        self.searchResults = results;

                        var counts = _.values(vertexResults.verticesCount);
                        if (counts.length === 0 || Math.max.apply([], counts) === 0) {
                            var headerTextNode = self.$node.find('.search-results-summary li.entities');
                            if (headerTextNode.length) {
                                headerTextNode[0].normalize();
                                headerTextNode[0].textContent = 'No Entities';
                            }
                        } else {
                            var countMap = vertexResults.verticesCount,
                                summaryNode = self.select('resultsSummarySelector');

                            concepts.byTitle.forEach(function(concept) {
                                var count = countMap[concept.id] || 0,
                                    childrenCounts = _.pick(countMap, _.pluck(concept.children, 'id')),
                                    total = _.reduce(childrenCounts, function(memo, i) {return memo + i}, 0);

                                self.onEntitySearchResultsForConcept(summaryNode, concept, results.entity, count + total);
                            });
                        }

                    }).fail(function(request, reason) {
                        if (reason === 'abort') return;
                        var $searchQueryValidation = self.select('queryValidationSelector');
                        self.select('resultsSummarySelector').empty();
                        return $searchQueryValidation.html(alertTemplate({ error: 'Invalid query' }));
                    });
            }.bind(this));
        };

        this.updateConceptSections = function(concepts) {
            var $searchResultsSummary = this.select('resultsSummarySelector'),
                resultsHtml = this.getConceptChildrenHtml(concepts.entityConcept, 15);

            $searchResultsSummary.html(conceptSectionsTemplate({ resultsHtml: resultsHtml }));
            $('.badge', $searchResultsSummary).addClass('loading');
        };

        this.onSummaryResultItemClick = function(evt) {
            evt.preventDefault();

            var $target = $(evt.target).parents('li');
            if ($target.hasClass('active')) {
                return this.close(evt);
            }

            var count = $target.find('.badge').data('count');
            if (count === 0) {
                return this.close(evt);
            }

            this.$node.find('.search-results-summary .active').removeClass('active');
            $target.addClass('active');

            this.trigger('showSearchResults', {
                conceptId: $target.data('conceptId'),
                count: count
            });
        };

        this.onShowSearchResults = function(evt, data) {
            var self = this,
                $searchResults = this.select('resultsSelector'),
                vertexIds = _.pluck(this.searchResults[data.conceptId] || [], 'id'),
                vertices = appData.vertices(vertexIds);

            this.hideSearchResults();
            this.select('filtersSelector').hide();

            if (data.count) {
                VertexList.attachTo($searchResults.find('.content'), {
                    vertices: vertices,
                    infiniteScrolling: true,
                    verticesConceptId: data.conceptId,
                    total: data.count
                });
                this.makeResizable($searchResults);
                $searchResults.show();
                $searchResults.find('.multi-select').focus();
            }
            this.trigger(document, 'paneResized');
        };

        this.makeResizable = function(node) {
            var self = this;

            // Add splitbar to search results
            return node.resizable({
                handles: 'e',
                minWidth: 200,
                maxWidth: 350,
                resize: function() {
                    self.trigger(document, 'paneResized');
                }
            });
        };

        this.onKeyUp = function (evt) {
            var search = this.select('querySelector'),
                query = search.val();

            if (query != this.currentQuery) {
                this.trigger("searchQueryChanged", { query: query});
                this.currentQuery = query;
            }

            if (evt.which === 27) {
                search.blur();
            }
        };

        this.onQueryFocus = function (evt, data) {
            var filters = this.select('filtersSelector').show();
            this.hideSearchResults();
            this.$node.find('.search-results-summary .active').removeClass('active');
        };

        this.hideSearchResults = function() {
            registry.findInstanceInfoByNode(this.select('resultsSelector').hide().find('.content')[0]).forEach(function(info) {
                info.instance.teardown();
            });
            this.trigger(document, 'paneResized');
        };

        this.close = function(e) {
            this.hideSearchResults();
            this.$node.find('.search-results-summary .active').removeClass('active');
        };

        this.onPaneVisible = function() {
            this.select('querySelector').focus().select();
        };

        this.after('initialize', function() {
            var self = this;
            this.searchResults = {};
            this.$node.html(template({}));

            this.hideSearchResults();

            var filters = this.select('filtersSelector').hide();
            Filters.attachTo(filters.find('.content'));
            this.makeResizable(filters);

            this.on('filterschange', this.onFiltersChange);
            this.on('infiniteScrollRequest', this.onInfiniteScrollRequest);

            this.on(document, 'searchByEntity', this.onSearchByEntity);
            this.on(document, 'searchByRelatedEntity', this.onSearchByRelatedEntity);
            this.on(document, 'search', this.onSearch);
            this.on(document, 'clearSearch', this.onClearSearch);
            this.on(document, 'showSearchResults', this.onShowSearchResults);
            this.on(document, 'menubarToggleDisplay', this.onMenubarToggle);
            this.on(document, 'searchPaneVisible', this.onPaneVisible);
            this.on('submit', {
                formSelector: this.onFormSearch
            });
            this.on('click', {
                summaryResultItemSelector: this.onSummaryResultItemClick,
                filtersInfoSelector: this.onFiltersInfoRemoveClick,
            });
            this.on('keyup', {
                querySelector: this.onKeyUp
            });

            this.select('querySelector').on('focus', this.onQueryFocus.bind(this));

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: 'Search',
                shortcuts: {
                    'meta-a': { fire:'selectAll', desc:'Select all search results' },
                    'up': { fire:'up', desc:'Select previous result'},
                    'down': { fire:'down', desc:'Select next result'}
                }
            });
        });


        this.onMenubarToggle = function(evt, data) {
            var pane = this.$node.closest(':data(menubarName)');
            if (data.name === pane.data('menubarName')) {
                if (!pane.hasClass('visible')) {
                    this.$node.find('.search-results-summary .active').removeClass('active');
                    this.select('filtersSelector').hide();
                    this.hideSearchResults();
                }
            }
        };

        this.onInfiniteScrollRequest = function(evt, data) {
            var query = this.select('querySelector').val(),
                trigger = this.trigger.bind(this, 
                   this.select('resultsSelector').find('.content'),
                   'addInfiniteVertices'
                );

            if (this.infiniteSearchRequest) {
                this.infiniteSearchRequest.abort();
            }

            if (this.entityFilters && this.entityFilters.relatedToVertexId) {
                query = { query:query, relatedToVertexId:this.entityFilters.relatedToVertexId };
            }

            this.infiniteSearchRequest = this.vertexService.graphVertexSearch(
                    query,
                    this.filters,
                    data.conceptType,
                    data.paging
            )
            .fail(function() {
                trigger({ success: false });
            })
            .done(function(results) {
                trigger({ success: true, vertices: results.vertices });
            });
        };

        this.onFiltersChange = function(evt, data) {
            this.filters = data.propertyFilters;
            this.entityFilters = data.entityFilters;
        
            var filterInfo = this.select('filtersInfoSelector'),
                numberOfFilters = this.filters.length + _.keys(this.entityFilters).length,
                query = this.select('querySelector').val();

            filterInfo.find('.message').text(formatters.string.plural(numberOfFilters, 'filter') + ' applied');
            filterInfo.toggle(numberOfFilters > 0);

            if (!query && !this.entityFilters.relatedToVertexId) {
                this.select('resultsSummarySelector').empty();
                return;
            }

            this.trigger('search', { query:query || '' });
        };

        this.onFiltersInfoRemoveClick = function() {
            this.clearFilters();
        };

        this.clearFilters = function(options) {
            this.trigger(this.select('filtersSelector').find('.content'), 'clearfilters', options);
        };
    }
});
