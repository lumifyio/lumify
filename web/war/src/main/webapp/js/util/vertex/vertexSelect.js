define([
    'flight/lib/component',
    'hbs!./selectTemplate',
    'tpl!./vertexSelectEntity',
    'util/withDataRequest',
    'util/vertex/formatters'
], function(
    defineComponent,
    template,
    entityTemplate,
    withDataRequest,
    F) {
    'use strict';

    return defineComponent(VertexSelector, withDataRequest);

    function VertexSelector() {

        this.defaultAttrs({
            defaultText: i18n('vertex.field.placeholder'),
            fieldSelector: 'input'
        });

        this.after('initialize', function() {
            this.$node.html(template(this.attr));

            this.on('click', {
                fieldSelector: this.onClick
            });
            //this.on('keydown', {
                //fieldSelector: this.onKeyPress
            //});

            this.on('showTypeahead', this.onShowTypeahead);
            this.on('disableAndSearch', this.onDisableAndSearch);
            this.on('setConcept', this.onSetConcept);

            this.setupTypeahead();
            this.updateResolveImageIcon();
        });

        this.onClick = function(event) {
            this.showTypeahead({ focus:true })
        };

        //this.onKeyPress = function(event) {
            //console.log(this.lastQuery, this.sign)
            //if (!this.lastQuery || this.lastQuery === this.sign) {
                //switch (event.which) {
                    //case $.ui.keyCode.ENTER:
                        //this.onButtonClicked(event);
                //}
                //return;
            //}

            //if (!this.debouncedLookup) {
                //this.debouncedLookup = _.debounce(function() {
                    //this.showTypeahead();
                //}.bind(this), 100);
            //}

            //this.debouncedLookup();
        //};

        this.onDisableAndSearch = function(event, data) {
            var field = this.select('fieldSelector').attr('disabled', true);

            this.runQuery(data.query).done(function() {
                field.removeAttr('disabled');
            });
        };

        this.onShowTypeahead = function(event, data) {
            this.showTypeahead(data);
        };

        this.showTypeahead = function(options) {
            var field = this.select('fieldSelector');
            if (options && options.focus) {
                _.defer(function() {
                    field.focus()
                    field.typeahead('lookup');
                })
            } else {
                field.typeahead('lookup');
            }
        };

        this.onSetConcept = function(event, data) {
            event.stopPropagation();

            var conceptId = data && data.conceptId;

            if (this.selectedItem && this.attr.allowNewText &&
               this.selectedItem === this.attr.allowNewText) {
                this.updateResolveImageIcon(null, conceptId);
            }
        };

        this.reset = function() {
            this.updateResolveImageIcon();
            this.trigger('resetTypeahead');
        }

        this.updateResolveImageIcon = function(vertex, conceptId) {
            var self = this;

            if (vertex && !vertex.properties) {
                vertex = null;
            }

            if (vertex) {
                updateCss(F.vertex.image(vertex));
            } else if (conceptId) {
                var concept = this.ontology.concepts.byId[conceptId];
                if (concept) {
                    updateCss(concept.glyphIconHref);
                } else {
                    updateCss();
                }
            } else {
                updateCss();
            }

            function updateCss(src) {
                var preview = self.$node.find('.preview');

                if (src) {
                    var url = 'url("' + src + '")';

                    if (preview.css('background-image') !== url) {
                        preview.css('background-image', url);
                    }
                } else {
                    preview.css({backgroundImage: ''}).addClass('icon-unknown');
                }
            }
        };

        this.setupTypeahead = function() {
            var self = this,
                vertices = [],
                input = this.select('fieldSelector'),
                createNewText = this.attr.allowNewText;

            this.sourceCache = {};
            this.dataRequest('ontology', 'ontology')
                .done(function(ontology) {
                    self.ontology = ontology;

                    var ontologyConcepts = ontology.concepts,
                        conceptsById = _.indexBy(ontologyConcepts.byTitle, 'title'),
                        ontologyProperties = ontology.properties,
                        debouncedQuery = _.debounce(function(instance, query, callback) {
                            self.runQuery(query)
                                .then(function(entities) {
                                    var all = _.map(entities, function(e) {
                                        return $.extend({
                                            toLowerCase: function() {
                                                return F.vertex.title(e).toLowerCase();
                                            },
                                            toString: function() {
                                                return e.id;
                                            },
                                            indexOf: function(s) {
                                                return F.vertex.title(e).indexOf(s);
                                            }
                                        }, e);
                                    });

                                    vertices = $.extend(true, [], vertices, _.indexBy(all, 'id'));
                                    if (self.attr.allowNew) {
                                        vertices[createNewText] = [query];
                                    }

                                    self.sourceCache[query] = function(aCallback) {
                                        var list = [].concat(all);
                                        if (self.attr.allowNew) {
                                            list.splice(0, 0, createNewText);
                                        }
                                        aCallback(list);

                                        var selectedId = self.currentGraphVertexId;
                                        if (selectedId) {
                                            var shouldSelect = instance.$menu.find('.gId-' + selectedId).closest('li');
                                            if (shouldSelect.length) {
                                                instance.$menu.find('.active').not(shouldSelect).removeClass('active');
                                                shouldSelect.addClass('active');
                                            }
                                        }

                                        self.updateQueryCountBadge(all);
                                    };

                                    self.sourceCache[query](callback);
                                })
                                .catch(function() {
                                    callback([]);
                                })
                        }, 500),
                        field = input.typeahead({
                            items: 50,
                            source: function(query, callback) {

                                if (self.lastQuery && query !== self.lastQuery) {
                                    self.reset();
                                }

                                if (!self.sourceCache) {
                                    self.sourceCache = {};
                                } else if (self.sourceCache[query]) {
                                    self.sourceCache[query](callback);
                                    return;
                                }

                                self.lastQuery = query;
                                debouncedQuery(this, query, callback);
                            },
                            matcher: function(item) {
                                if (item === createNewText) return true;
                                return true;
                            },
                            sorter: function(items) {
                                var sorted = Object.getPrototypeOf(this).sorter.apply(this, arguments),
                                    index;

                                sorted.forEach(function(item, i) {
                                    if (item === createNewText) {
                                        index = i;
                                        return false;
                                    }
                                });

                                if (index) {
                                    sorted.splice(0, 0, sorted.splice(index, 1)[0]);
                                }

                                return sorted;
                            },
                            updater: function(item) {
                                var matchingItem = vertices[item],
                                    graphVertexId = '',
                                    label = item;

                                if (!matchingItem.length) {
                                    matchingItem = [matchingItem];
                                }

                                if (matchingItem && matchingItem.length) {
                                    graphVertexId = item;
                                    label = matchingItem[0].properties ?
                                        F.vertex.title(matchingItem[0]) :
                                        matchingItem;

                                    if (graphVertexId == createNewText) {
                                        graphVertexId = '';
                                        label = this.$element.val();
                                    } else {
                                        self.sign = label;
                                    }

                                    matchingItem = matchingItem[0];
                                }

                                self.lastQuery = label;
                                self.trigger('vertexSelected', {
                                    vertexId: graphVertexId,
                                    item: matchingItem,
                                    sign: label
                                })
                                self.selectedItem = matchingItem.properties ? matchingItem : item;
                                self.updateResolveImageIcon(matchingItem);

                                return label;
                            },
                            highlighter: function(item) {

                                var html = (item === createNewText) ?
                                        item :
                                        Object.getPrototypeOf(this).highlighter.apply(
                                            this,
                                            [F.vertex.title(item)]
                                        ),
                                        rawConcept = item.properties && F.vertex.concept(item),
                                        concept = rawConcept && conceptsById[rawConcept.title];

                                return entityTemplate({
                                    html: html,
                                    item: item,
                                    properties: [],
                                    F: F,
                                    iconSrc: item.properties && F.vertex.image(item),
                                    concept: concept
                                });
                            }
                        }),
                        typeahead = field.data('typeahead'),
                        show = typeahead.show,
                        hide = typeahead.hide;

                    typeahead.$menu.on('mousewheel DOMMouseScroll', function(e) {
                        var delta = e.wheelDelta || (e.originalEvent && e.originalEvent.wheelDelta) || -e.detail,
                            bottomOverflow = this.scrollTop + $(this).outerHeight() - this.scrollHeight >= 0,
                            topOverflow = this.scrollTop <= 0;

                        if ((delta < 0 && bottomOverflow) || (delta > 0 && topOverflow)) {
                            e.preventDefault();
                        }
                    });

                    typeahead.hide = function() {
                        hide.apply(typeahead);
                        typeahead.$menu.css('max-height', 'none');
                    };

                    typeahead.show = function() {
                        show.apply(typeahead);

                        if (~typeahead.$menu.css('max-height').indexOf('px')) {
                            typeahead.$menu.css('max-height', 'none');
                            _.defer(scrollToShow);
                            return;
                        } else {
                            scrollToShow();
                        }

                        function scrollToShow() {

                            var scrollParent = typeahead.$element.scrollParent(),
                                scrollTotalHeight = scrollParent[0].scrollHeight,
                                scrollTop = scrollParent.scrollTop(),
                                scrollHeight = scrollParent.outerHeight(true),
                                menuHeight = Math.min(scrollHeight - 100, typeahead.$menu.outerHeight(true)),
                                menuMaxY = menuHeight + typeahead.$menu.offset().top,
                                bottomSpace = scrollHeight - menuMaxY,
                                padding = 10;

                            typeahead.$menu.css({
                                maxHeight: (menuHeight - padding) + 'px',
                                overflow: 'auto'
                            });

                            if (bottomSpace < 0) {
                                var scrollNeeded = scrollTop + Math.abs(bottomSpace) + padding;
                                scrollParent.animate({
                                    scrollTop: scrollNeeded
                                });
                            }
                        }
                    };
                });
        };

        this.runQuery = function(query) {
            var self = this;

            query = $.trim(query || '');
            if (!this.queryCache) this.queryCache = {};
            if (this.queryCache[query]) return this.queryCache[query];

            var badge = this.select('fieldSelector').nextAll('.badge').addClass('loading'),
                request = this.dataRequest('vertex', 'search', {
                    query: query,
                    conceptFilter: this.attr.restrictConcept
                })
                    .then(function(response) {
                        var vertices = response.vertices;
                        if (self.attr.filterResultsToTitleField) {
                            var splitUpString = function(str) {
                                    return F.string.normalizeAccents(str.toLowerCase())
                                        .replace(/[^a-zA-Z0-9]/g, ' ')
                                        .split(/\s+/);
                                },
                                queryParts = splitUpString(query);

                            vertices =  _.reject(vertices, function(v) {
                                var queryPartsMissingFromTitle = _.difference(
                                    queryParts,
                                    splitUpString(F.vertex.title(v))
                                ).length;

                                return queryPartsMissingFromTitle;
                            });
                        }

                        self.updateQueryCountBadge(vertices);
                        self.queryCache[query] = request;

                        return vertices;
                    })
                    .catch(function() {
                        self.updateQueryCountBadge();
                    })
                    .finally(function() {
                        badge.removeClass('loading');
                    });
            return request;
        };

        this.updateQueryCountBadge = function(vertices) {
            var hasVertices = !_.isUndefined(vertices);
            this.$node.find('.badge')
                .attr('title', hasVertices ?
                      i18n('vertex.field.found' +
                           (vertices.length === 1 ? '' : '.plural'), vertices.length) :
                      i18n('vertex.field.error'))
                .text(hasVertices ? vertices.length : '!');
        };

    }
});
