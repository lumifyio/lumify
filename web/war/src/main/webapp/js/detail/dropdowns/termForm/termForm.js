define([
    'flight/lib/component',
    '../withDropdown',
    'detail/properties/properties',
    'tpl!./termForm',
    'tpl!./entity',
    'tpl!util/alert',
    'util/vertex/formatters',
    'util/ontology/conceptSelect',
    'util/withDataRequest',
    'util/jquery.removePrefixedClasses'
], function(
    defineComponent,
    withDropdown,
    Properties,
    dropdownTemplate,
    entityTemplate,
    alertTemplate,
    F,
    ConceptSelector,
    withDataRequest) {
    'use strict';

    return defineComponent(TermForm, withDropdown, withDataRequest);

    function TermForm() {

        this.defaultAttrs({
            entityConceptMenuSelector: '.underneath .dropdown-menu a',
            actionButtonSelector: '.btn.btn-small.btn-primary',
            buttonDivSelector: '.buttons',
            objectSignSelector: '.object-sign',
            graphVertexSelector: '.graphVertexId',
            visibilitySelector: '.visibility',
            conceptContainerSelector: '.concept-container',
            helpSelector: '.help',
            addNewPropertiesSelector: '.none'
        });

        this.after('teardown', function() {
            if (this.promoted && this.promoted.length) {
                this.demoteSpanToTextVertex(this.promoted);
            }

            var info = $(this.attr.mentionNode).removeClass('focused').data('info');

            if (info) {
                this.updateConceptLabel(info['http://lumify.io#conceptType'] || '');
            }

            // Remove extra textNodes
            if (this.node.parentNode) {
                this.node.parentNode.normalize();
            }
        });

        this.after('initialize', function() {
            this.deferredConcepts = $.Deferred();
            this.setupContent();
            this.registerEvents();
        });

        this.showTypeahead = function() {
            if (!this.unresolve) {
                this.select('objectSignSelector').typeahead('lookup');
            }
        };

        this.onKeyPress = function(event) {
            if (!this.lastQuery || this.lastQuery === this.select('objectSignSelector').val()) {
                if (!this.select('actionButtonSelector').is(':disabled')) {
                    switch (event.which) {
                        case $.ui.keyCode.ENTER:
                            this.onButtonClicked(event);
                    }
                }
                return;
            }

            if (!this.debouncedLookup) {
                this.debouncedLookup = _.debounce(function() {
                    this.select('objectSignSelector').typeahead('lookup');
                }.bind(this), 100);
            }

            this.debouncedLookup();
        };

        this.reset = function() {
            this.currentGraphVertexId = null;
            this.select('helpSelector').show();
            this.select('visibilitySelector').hide();
            this.select('conceptContainerSelector').hide();
            this.select('actionButtonSelector').hide();
            this.updateResolveImageIcon();
        };

        this.graphVertexChanged = function(newGraphVertexId, item, initial) {
            var self = this;

            this.currentGraphVertexId = newGraphVertexId;
            if (!initial || newGraphVertexId) {
                this.select('graphVertexSelector').val(newGraphVertexId);
                var info = _.isObject(item) ? item.properties || item : $(this.attr.mentionNode).data('info');

                this.trigger(this.select('conceptContainerSelector'), 'enableConcept', {
                    enable: !newGraphVertexId
                })

                var conceptType = _.isArray(info) ?
                    _.findWhere(info, { name: 'http://lumify.io#conceptType' }) :
                    (info && (info['http://lumify.io#conceptType'] || info.concept));
                conceptType = conceptType && conceptType.value || conceptType || '';

                if (conceptType === '' && self.attr.restrictConcept) {
                    conceptType = self.attr.restrictConcept;
                }

                this.deferredConcepts.done(function() {
                    self.trigger(self.select('conceptContainerSelector').show(), 'selectConcept', {
                        conceptId: conceptType
                    })
                    self.updateConceptLabel(conceptType)
                });

                if (this.unresolve) {
                    this.select('actionButtonSelector')
                        .text(i18n('detail.resolve.form.button.unresolve'))
                        .show();
                    this.$node.find('input,select').attr('disabled', true);
                } else {
                    this.select('actionButtonSelector')
                        .text(newGraphVertexId && !initial && !this.attr.coords ?
                              i18n('detail.resolve.form.button.resolve.existing') :
                              i18n('detail.resolve.form.button.resolve.new'))
                        .show();
                }
                this.select('helpSelector').hide();
                this.select('visibilitySelector').show();

                require(['configuration/plugins/visibility/visibilityEditor'], function(Visibility) {
                    Visibility.attachTo(self.$node.find('.visibility'), {
                        value: '',
                        readonly: self.unresolve
                    });
                });
            } else if (this.attr.restrictConcept) {
                this.deferredConcepts.done(function() {
                    self.trigger(self.select('conceptContainerSelector'), 'selectConcept', {
                        conceptId: self.attr.restrictConcept
                    })
                });
            }

            if (newGraphVertexId) {
                this.dataRequest('vertex', 'store', { vertexIds: newGraphVertexId })
                    .done(function(v) {
                        self.updateResolveImageIcon(v);
                    });
            } else this.updateResolveImageIcon();

        };

        this.onButtonClicked = function(event) {
            if (!this.attr.detectedObject) {
                this.termModification(event);
            } else {
                this.detectedObjectModification(event);
            }
        }

        this.termModification = function(event) {
            var self = this,
                $mentionNode = $(this.attr.mentionNode),
                newObjectSign = $.trim(this.select('objectSignSelector').val()),
                mentionStart,
                mentionEnd;

            if (this.attr.existing) {
                var dataInfo = $mentionNode.data('info');
                mentionStart = dataInfo.start;
                mentionEnd = dataInfo.end;
            } else {
                mentionStart = this.selectedStart;
                mentionEnd = this.selectedEnd;
            }
            var parameters = {
                sign: newObjectSign,
                propertyKey: this.attr.propertyKey,
                conceptId: this.selectedConceptId,
                mentionStart: mentionStart,
                mentionEnd: mentionEnd,
                artifactId: this.attr.artifactId,
                visibilitySource: this.visibilitySource || ''
            };

            if (this.currentGraphVertexId) {
                parameters.resolvedVertexId = this.currentGraphVertexId;
                parameters.edgeId = $mentionNode.data('info') ? $mentionNode.data('info').edgeId : null;
            }

            _.defer(this.buttonLoading.bind(this));

            if (!parameters.conceptId || parameters.conceptId.length === 0) {
                this.select('conceptContainerSelector').find('select').focus();
                return;
            }

            if (newObjectSign.length) {
                parameters.objectSign = newObjectSign;
                $mentionNode.attr('title', newObjectSign);
            }

            if (!this.unresolve) {
                this.dataRequest('vertex', 'resolveTerm', parameters)
                    .then(function(data) {
                        self.highlightTerm(data);
                        self.trigger('termCreated', data);

                        self.trigger(document, 'loadEdges');

                        _.defer(self.teardown.bind(self));
                    })
                    .catch(this.requestFailure.bind(this))
            } else {
                parameters.termMentionId = this.termMentionId;
                this.dataRequest('vertex', 'unresolveTerm', parameters)
                    .then(function(data) {
                        self.highlightTerm(data);

                        self.trigger(document, 'loadEdges');

                        _.defer(self.teardown.bind(self));
                    })
                    .catch(this.requestFailure.bind(this))
            }
        };

        this.requestFailure = function(request, message, error) {
            this.markFieldErrors(error);
            _.defer(this.clearLoading.bind(this));
        };

        this.detectedObjectModification = function(event) {
            var self = this,
                newSign = $.trim(this.select('objectSignSelector').val()),
                parameters = {
                    title: newSign,
                    conceptId: this.selectedConceptId,
                    originalPropertyKey: this.attr.dataInfo.originalPropertyKey,
                    graphVertexId: this.attr.dataInfo.resolvedVertexId ?
                        this.attr.dataInfo.resolvedVertexId :
                        this.currentGraphVertexId,
                    artifactId: this.attr.artifactData.id,
                    x1: parseFloat(this.attr.dataInfo.x1),
                    y1: parseFloat(this.attr.dataInfo.y1),
                    x2: parseFloat(this.attr.dataInfo.x2),
                    y2: parseFloat(this.attr.dataInfo.y2),
                    visibilitySource: this.visibilitySource || ''
                };

            _.defer(this.buttonLoading.bind(this));
            if (this.unresolve) {
                self.unresolveDetectedObject({
                    vertexId: this.attr.artifactData.id,
                    multiValueKey: this.attr.dataInfo.propertyKey
                });
            } else {
                self.resolveDetectedObject(parameters);
            }
        }

        this.resolveDetectedObject = function(parameters) {
            var self = this;
            this.dataRequest('vertex', 'resolveDetectedObject', parameters)
                .then(function(data) {
                    self.trigger('termCreated', data);
                    self.trigger(document, 'loadEdges');
                    _.defer(self.teardown.bind(self));
                })
                .catch(this.requestFailure.bind(this))
        };

        this.unresolveDetectedObject = function(parameters) {
            var self = this;
            this.dataRequest('vertex', 'unresolveDetectedObject', parameters)
                .then(function(data) {
                    self.trigger(document, 'loadEdges');
                    _.defer(self.teardown.bind(self));
                })
                .catch(this.requestFailure.bind(this))
        };

        this.onConceptSelected = function(event, data) {
            this.selectedConceptId = data && data.concept && data.concept.id || '';
            this.updateConceptLabel(this.selectedConceptId);

            if (this.selectedConceptId) {
                this.select('actionButtonSelector').removeAttr('disabled');
            } else {
                this.select('actionButtonSelector').attr('disabled', true);
            }
        };

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data.value;
            // TODO: inspect valid
        };

        this.updateConceptLabel = function(conceptId, vertex) {
            var self = this;

            if (conceptId === '') {
                this.select('actionButtonSelector').attr('disabled', true);
                this.updateResolveImageIcon();
                return;
            }
            this.select('actionButtonSelector').removeAttr('disabled');

            this.deferredConcepts.done(function(allConcepts) {
                self.updateResolveImageIcon(null, conceptId);
            })
        };

        this.setupContent = function() {

            var self = this,
                vertex = this.$node,
                existingEntity,
                objectSign = '',
                sign,
                data, graphVertexId, title;

            if (!this.attr.detectedObject) {
                var mentionVertex = $(this.attr.mentionNode);
                data = mentionVertex.data('info');
                existingEntity = this.attr.existing ? mentionVertex.addClass('focused').hasClass('resolved') : false;
                graphVertexId = data && data.resolvedToVertexId;
                title = $.trim(data && data.title || '');

                if (this.attr.selection && !existingEntity) {
                    this.trigger(document, 'ignoreSelectionChanges.detail');
                    this.promoted = this.promoteSelectionToSpan();

                    // Promoted span might have been auto-expanded to avoid nested
                    // spans
                    sign = this.promoted.text();

                    _.defer(function() {
                        self.trigger(document, 'resumeSelectionChanges.detail');
                    });
                }

                if (existingEntity && mentionVertex.hasClass('resolved')) {
                    objectSign = title;
                    this.unresolve = true;
                    this.termMentionId = data && data.id;
                } else {
                    objectSign = this.attr.sign || mentionVertex.text();
                }
            } else {
                data = this.attr.dataInfo;
                objectSign = data && data.title;
                existingEntity = this.attr.existing;
                graphVertexId = data && data.resolvedToVertexId;
                this.unresolve = graphVertexId && graphVertexId !== '';
            }

            vertex.html(dropdownTemplate({
                sign: $.trim(objectSign),
                graphVertexId: graphVertexId,
                objectSign: $.trim(objectSign) || '',
                buttonText: existingEntity ?
                    i18n('detail.resolve.form.button.resolve.existing') :
                    i18n('detail.resolve.form.button.resolve.new')
            }));

            ConceptSelector.attachTo(this.select('conceptContainerSelector').toggle(!!graphVertexId), {
                restrictConcept: this.attr.restrictConcept
            });

            this.graphVertexChanged(graphVertexId, data, true);

            if (!this.unresolve && objectSign) {
                var input = this.select('objectSignSelector');
                input.attr('disabled', true);
                this.runQuery(objectSign).done(function() {
                    input.removeAttr('disabled');
                });
            }

            this.sign = objectSign;
            this.startSign = objectSign;
        };

        this.updateResolveImageIcon = function(vertex, conceptId) {
            var self = this,
                info = $(self.attr.mentionNode).data('info') ||
                    (this.attr.existing ? this.attr.dataInfo : '');

            if (!vertex && (info || conceptId)) {
                self.deferredConcepts.done(function(allConcepts) {
                    var type = info ?
                            info['http://lumify.io#conceptType'] : conceptId,
                        concept = self.conceptForConceptType(type, allConcepts);

                    if (concept) {
                        updateCss(concept.glyphIconHref);
                    }
                });
            } else if (vertex && !conceptId) {
                updateCss(F.vertex.image(vertex));
            } else updateCss();

            function updateCss(src) {
                var preview = self.$node.find('.resolve-wrapper > .preview');

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

        this.conceptForConceptType = function(conceptType, allConcepts) {
            return _.findWhere(allConcepts, { id: conceptType });
        };

        this.registerEvents = function() {

            this.on('visibilitychange', this.onVisibilityChange);

            this.on('conceptSelected', this.onConceptSelected);

            this.on('click', {
                entityConceptMenuSelector: this.onEntityConceptSelected,
                actionButtonSelector: this.onButtonClicked,
                objectSignSelector: this.showTypeahead,
                helpSelector: function() {
                    this.select('objectSignSelector').focus();
                    this.showTypeahead();
                }
            });

            this.on('keydown', {
                objectSignSelector: this.onKeyPress
            });

            this.on('opened', function() {
                var self = this;

                this.loadConcepts()
                    .then(function() {
                        self.setupObjectTypeAhead();
                        self.deferredConcepts.resolve(self.allConcepts);
                    })
            });
        };

        this.loadConcepts = function() {
            var self = this;
            self.allConcepts = [];
            return this.dataRequest('ontology', 'concepts')
                .then(function(concepts) {
                    var vertexInfo;

                    if (self.attr.detectedObject) {
                        vertexInfo = self.attr.dataInfo;
                    } else {
                        var mentionVertex = $(self.attr.mentionNode);
                        vertexInfo = mentionVertex.data('info');
                    }

                    self.allConcepts = _.filter(concepts.byTitle, function(c) {
                        return c.userVisible !== false;
                    });

                    self.selectedConceptId = vertexInfo && (
                        vertexInfo['http://lumify.io#conceptType'] ||
                        (
                            vertexInfo.properties &&
                            vertexInfo.properties['http://lumify.io#conceptType'].value
                        )
                    ) || '';

                    self.trigger(self.select('conceptContainerSelector'), 'selectConcept', {
                        conceptId: self.selectedConceptId
                    })

                    if (!self.selectedConceptId) {
                        self.select('actionButtonSelector').attr('disabled', true);
                    }
                });
        };

        this.runQuery = function(query) {
            var self = this;

            query = $.trim(query || '');
            if (!this.queryCache) this.queryCache = {};
            if (this.queryCache[query]) return this.queryCache[query];

            var badge = this.select('objectSignSelector').nextAll('.badge')
                    .addClass('loading'),
                request = this.dataRequest('vertex', 'search', {
                    query: query,
                    conceptFilter: this.attr.restrictConcept
                })
                    .then(function(response) {
                        var splitUpString = function(str) {
                                return F.string.normalizeAccents(str.toLowerCase())
                                    .replace(/[^a-zA-Z0-9]/g, ' ')
                                    .split(/\s+/);
                            },
                            queryParts = splitUpString(query);
                            vertices =  _.reject(response.vertices, function(v) {
                                var queryPartsMissingFromTitle = _.difference(
                                    queryParts,
                                    splitUpString(F.vertex.title(v))
                                ).length;

                                return queryPartsMissingFromTitle;
                            });

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
                      i18n('detail.resolve.form.entity_search.found' +
                           (vertices.length === 1 ? '' : '.plural'), vertices.length) :
                      i18n('detail.resolve.form.error'))
                .text(hasVertices ? vertices.length : '!');
        };

        this.setupObjectTypeAhead = function() {
            var self = this,
                items = [],
                input = this.select('objectSignSelector'),
                createNewText = i18n('detail.resolve.form.entity_search.resolve_as_new');

            this.dataRequest('ontology', 'properties')
                .done(function(ontologyProperties) {
                    var debouncedQuery = _.debounce(function(instance, query, callback) {
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

                                    items = $.extend(true, [], items, _.indexBy(all, 'id'));
                                    items[createNewText] = [query];

                                    self.sourceCache[query] = function(aCallback) {
                                        var list = [createNewText].concat(all);
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
                                var matchingItem = items[item],
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
                                self.graphVertexChanged(graphVertexId, matchingItem);
                                return label;
                            },
                            highlighter: function(item) {

                                var html = (item === createNewText) ?
                                        item :
                                        Object.getPrototypeOf(this).highlighter.apply(
                                            this,
                                            [F.vertex.title(item)]
                                        ),
                                    concept = _.find(self.allConcepts, function(c) {
                                        return item.properties && c.id === F.vertex.prop(item, 'conceptType');
                                    });

                                return entityTemplate({
                                    html: html,
                                    item: item,
                                    F: F,
                                    // TODO: show some properties
                                    properties: [],
                                    iconSrc: F.vertex.image(item),
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

        this.highlightTerm = function(data) {
            var mentionVertex = $(this.attr.mentionNode),
                updatingEntity = this.attr.existing;

            if (updatingEntity) {
                mentionVertex.removeClass();
                if (data.cssClasses) {
                    mentionVertex.addClass(data.cssClasses.join(' '));
                }
                mentionVertex.data('info', data.info).removeClass('focused');

            } else if (this.promoted) {
                this.promoted.data('info', data.info)
                    .addClass((data.cssClasses && data.cssClasses.join(' ')) || '')
                    .removeClass('focused');
                this.promoted = null;
            }
        };

        this.promoteSelectionToSpan = function() {
            var textVertex = this.node,
                isTranscript = this.$node.closest('.av-times').length,
                range = this.attr.selection.range,
                el,
                tempTextNode,
                transcriptIndex = 0,
                span = document.createElement('span');

            span.className = 'entity focused';

            var newRange = document.createRange();
            newRange.setStart(range.startContainer, range.startOffset);
            newRange.setEnd(range.endContainer, range.endOffset);

            var r = range.cloneRange();

            if (isTranscript) {
                var dd = this.$node.closest('dd');
                r.selectNodeContents(dd.get(0));
                transcriptIndex = dd.data('index');
            } else {
                r.selectNodeContents(this.$node.closest('.text').get(0));
            }
            r.setEnd(range.startContainer, range.startOffset);
            var l = r.toString().length;

            this.selectedStart = l;
            this.selectedEnd = l + range.toString().length;

            if (isTranscript) {
                this.selectedStart = F.number.compactOffsetValues(transcriptIndex, this.selectedStart);
                this.selectedEnd = F.number.compactOffsetValues(transcriptIndex, this.selectedEnd);
            }

            // Special case where the start/end is inside an inner span
            // (surroundsContents will fail so expand the selection
            if (/entity/.test(range.startContainer.parentNode.className)) {
                el = range.startContainer.parentNode;
                var previous = el.previousSibling;

                if (previous && previous.nodeType === 3) {
                    newRange.setStart(previous, previous.textContent.length);
                } else {
                    tempTextNode = document.createTextNode('');
                    el.parentNode.insertBefore(tempTextNode, el);
                    newRange.setStart(tempTextNode, 0);
                }
            }
            if (/entity/.test(range.endContainer.parentNode.className)) {
                el = range.endContainer.parentNode;
                var next = el.nextSibling;

                if (next && next.nodeType === 3) {
                    newRange.setEnd(next, 0);
                } else {
                    tempTextNode = document.createTextNode('');
                    if (next) {
                        el.parentNode.insertBefore(tempTextNode, next);
                    } else {
                        el.appendChild(tempTextNode);
                    }
                    newRange.setEnd(tempTextNode, 0);
                }
            }
            newRange.surroundContents(span);

            return $(span).find('.entity').addClass('focused').end();
        };

        this.demoteSpanToTextVertex = function(vertex) {

            while (vertex[0].childNodes.length) {
                $(vertex[0].childNodes[0]).removeClass('focused');
                vertex[0].parentNode.insertBefore(vertex[0].childNodes[0], vertex[0]);
            }
            vertex.remove();
        };
    }
});
