
define([
    'flight/lib/component',
    'flight/lib/registry',
    'data',
    'service/service',
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
    Service,
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
        this.service = new Service();
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
                badge = this.updateCountBadgeForSubtype('concept-' + concept.id, resultsCount),
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
            if(!query) {
                this.select('resultsSummarySelector').empty();
                return $searchQueryValidation.html(alertTemplate({ error: 'Query cannot be empty' }));
            }
            this.trigger('search', { query: query });
            return false;
        };

        this.getConceptChildrenHtml = function(concept, indent) {
            var self = this,
                html = "";
            (concept.children || []).forEach(function(concept) {
                html += conceptItemTemplate({
                    concept: concept,
                    indent: indent
                });
                if(concept.children && concept.children.length > 0) {
                    html += self.getConceptChildrenHtml(concept, indent + 15);
                }
            });
            return html;
        };

        this.updateCountBadgeForSubtype = function(subType, count) {
            return this.$node.find('.' + subType + ' .badge')
                .removeClass('loading')
                .data('count', count)
                .text(formatters.number.pretty(count));
        };

        this.doSearch = function(evt, query) {
            if (!this.searchResults) {
                this.searchResults = {};
            }
            if (this.select('querySelector').val() != query.query) {
                this.select('querySelector').val(query.query);
            }

            var self = this;

            this.ontologyService.concepts(function(err, concepts) {
                this.updateConceptSections(concepts);

                var paging = { offset:0, size:100 };
                $.when(
                    this.service.artifactSearch(query || this.select('querySelector').val(), this.filters, null, paging),
                    this.service.graphVertexSearch(query || this.select('querySelector').val(), this.filters, null, paging)
                ).done(function(artifactSearch, vertexSearch) {
                    var results = {
                        artifact: artifactSearch[0],
                        entity:{}
                    };

                    'document video image'.split(' ').forEach(function(type) {
                        if (!results.artifact[type]) results.artifact[type] = [];
                        if (!results.artifact.counts[type]) results.artifact.counts[type] = 0;
                    });

                    var artifactCounts = artifactSearch[0].counts;
                    delete results.artifact.counts;

                    var sortVerticesIntoResults = function(v) {
                        var props = v.properties,
                            type = props._type,
                            subType = props._subType,
                            addToSearchResults = function(subType) {
                                if (!results[type]) results[type] = {};
                                if (!results[type][subType]) results[type][subType] = [];

                                // Check for an existing result with the same id
                                var resultFound = results[type][subType].some(function(result) { return result.id === v.id; });

                                // Only store unique results
                                if (resultFound === false) {
                                    results[type][subType].push(v);
                                }
                            };

                        if (type === 'artifact') return;

                        var vertexConcept = concepts.byId[subType];
                        while (vertexConcept) {
                            addToSearchResults(vertexConcept.id, v);
                            vertexConcept = vertexConcept.parentId ? concepts.byId[vertexConcept.parentId] : null;
                        }
                    };
                    vertexSearch[0].vertices.forEach(sortVerticesIntoResults);
                    self.searchResults = results;

                    Object.keys(results).forEach(function(type) {
                        if (type === 'artifact') {
                            Object.keys(results[type]).forEach(function(subType) {
                                self.updateCountBadgeForSubtype(subType, artifactCounts[subType]);
                            });
                        }
                    });

                    var counts = _.values(vertexSearch[0].verticesCount);
                    if (counts.length === 0 || Math.max.apply([], counts) === 0) {
                        var headerTextNode = self.$node.find('.search-results-summary li.entities');
                        if (headerTextNode.length) {
                            headerTextNode[0].normalize();
                            headerTextNode[0].textContent = 'No Entities';
                        }
                    } else {
                        var countMap = vertexSearch[0].verticesCount,
                            summaryNode = self.select('resultsSummarySelector');

                        concepts.byTitle.forEach(function(concept) {
                            var count = countMap[concept.id] || 0,
                                childrenCounts = _.pick(countMap, _.pluck(concept.children, 'id')),
                                total = _.reduce(childrenCounts, function(memo, i) {return memo + i}, 0);

                            self.onEntitySearchResultsForConcept(summaryNode, concept, results.entity, count + total);
                        });
                    }

                }).fail(function() {
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

            var itemPath = $target.attr('item-path').split('.');
            var _type = itemPath[0];
            var _subType = itemPath[1];
            this.trigger('showSearchResults', {
                _type: _type,
                _subType: _subType,
                count: count
            });
        };

        this.onShowSearchResults = function(evt, data) {
            var self = this,
                $searchResults = this.select('resultsSelector'),
                vertexIds = (this.searchResults[data._type][data._subType] || []).map(function(v) {
                    return v.graphVertexId || v.id;
                }),
                vertices = appData.vertices(vertexIds);

            this.hideSearchResults();
            this.select('filtersSelector').hide();

            if (data.count) {
                VertexList.attachTo($searchResults.find('.content'), {
                    vertices: vertices,
                    infiniteScrolling: true,
                    verticesType: data._type,
                    verticesSubType: data._subType,
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
            var filters = this.select('filtersSelector');
            Filters.attachTo(filters.find('.content'));

            this.makeResizable(filters);
            filters.show();
            this.hideSearchResults();
            this.$node.find('.search-results-summary .active').removeClass('active');
        };

        this.hideSearchResults = function() {
            registry.findInstanceInfoByNode(this.select('resultsSelector').hide().find('.content')[0]).forEach(function(info) {
                info.instance.teardown();
            });
            this.trigger(document, 'paneResized');
        };

        this.onQueryChange = function (evt, data) {
            if (!data.remoteEvent) {
                return;
            }

            this.select('querySelector').val(data.query);
            this.currentQuery = data.query;
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

            this.select('filtersSelector').hide();
            this.hideSearchResults();

            this.on('filterschange', this.onFiltersChange);
            this.on('infiniteScrollRequest', this.onInfiniteScrollRequest);

            this.on(document,'search', this.doSearch);
            this.on(document,'showSearchResults', this.onShowSearchResults);
            this.on(document,'searchQueryChanged',this.onQueryChange);
            this.on(document, 'menubarToggleDisplay', this.onMenubarToggle);
            this.on(document, 'searchPaneVisible', this.onPaneVisible);
            this.on('submit', {
                formSelector: this.onFormSearch
            });
            this.on('click', {
                summaryResultItemSelector: this.onSummaryResultItemClick,
                filtersInfoSelector: this.onFiltersInfoRemoveClick
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
            var self = this,
                query = this.select('querySelector').val();

            if (data.verticesType === 'artifact') {
                this.service.artifactSearch(
                        query,
                        this.filters,
                        data.verticesSubType,
                        data.paging
                ).done(function(results) {

                    var newVertices = results[data.verticesSubType];

                    self.trigger(
                        self.select('resultsSelector').find('.content'),
                        'addInfiniteVertices', 
                        { 
                            vertices:newVertices,
                            total:results.counts[data.verticesSubType]
                        }
                    );
                });
            } else {
                this.service.graphVertexSearch(
                        query,
                        this.filters,
                        data.verticesSubType,
                        data.paging
                ).done(function(results) {

                    self.trigger(
                        self.select('resultsSelector').find('.content'),
                        'addInfiniteVertices', 
                        { 
                            vertices: results.vertices
                        }
                    );
                });
            }
        };

        this.onFiltersChange = function(evt, data) {
            this.filters = data.filters;

            var query = this.select('querySelector').val() || '*';

            var filterInfo = this.select('filtersInfoSelector'),
                numberOfFilters = this.filters.length;

            filterInfo.find('.message').text(formatters.string.plural(numberOfFilters, 'filter') + ' applied');
            filterInfo.toggle(numberOfFilters > 0);

            this.trigger(document, 'search', { query:query });
        };

        this.onFiltersInfoRemoveClick = function() {
            this.trigger(this.select('filtersSelector').find('.content'), 'clearfilters');
        };
    }
});
