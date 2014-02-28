

define([
    'flight/lib/component',
    '../withDropdown',
    'detail/properties',
    'tpl!./termForm',
    'tpl!./concept-options',
    'tpl!./entity',
    'service/vertex',
    'service/ontology',
    'util/jquery.removePrefixedClasses'
], function(defineComponent, withDropdown, Properties, dropdownTemplate, conceptsTemplate, entityTemplate, VertexService, OntologyService) {
    'use strict';

    return defineComponent(TermForm, withDropdown);


    function TermForm() {
        this.vertexService = new VertexService();
        this.ontologyService = new OntologyService();

        this.defaultAttrs({
            entityConceptMenuSelector: '.underneath .dropdown-menu a',
            actionButtonSelector: '.btn.btn-small.btn-primary',
            buttonDivSelector: '.buttons',
            objectSignSelector: '.object-sign',
            graphVertexSelector: '.graphVertexId',
            conceptSelector: 'select',
            helpSelector: '.help',
            addNewPropertiesSelector: '.none'
        });

        this.after('teardown', function() {
            if (this.promoted && this.promoted.length) {
                this.demoteSpanToTextVertex(this.promoted);
            }

            var info = $(this.attr.mentionNode).removeClass('focused').data('info');

            if (info) {
                this.updateConceptLabel(info._conceptType || '');
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
            this.select('objectSignSelector').typeahead('lookup');
        };

        this.onKeyPress = function(event) {
            if (!this.lastQuery || this.lastQuery === this.select('objectSignSelector').val()) {
                if (!this.select('actionButtonSelector').is(":disabled")) {
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
            this.select('conceptSelector').attr('disabled', true).hide();
            this.select('actionButtonSelector').hide();
            this.updateResolveImageIcon();
        };

        this.graphVertexChanged = function(newGraphVertexId, item, initial) {
            var self = this;

            this.currentGraphVertexId = newGraphVertexId;
            if (!initial || newGraphVertexId) {
                this.select('graphVertexSelector').val(newGraphVertexId);
                var info = _.isObject(item) ? item.properties || item : $(this.attr.mentionNode).data('info');

                if (newGraphVertexId) {
                    this.select('conceptSelector').attr('disabled', true);
                } else {
                    this.select('conceptSelector').attr('disabled', false);
                }
                var conceptType = (info && ((info._conceptType && info._conceptType.value) || info._conceptType || info.properties._conceptType.value)) || '';
                this.updateConceptSelect(conceptType).show();

                if (this.unresolve){
                    this.select('actionButtonSelector')
                        .text('Unresolve')
                        .show();
                } else {
                    this.select('actionButtonSelector')
                        .text(newGraphVertexId && !initial && !this.attr.coords ? 'Resolve as Existing' : 'Resolve as New')
                        .show();
                }
                this.select('helpSelector').hide();

                require(['configuration/plugins/visibility/visibilityEditor'], function(Visibility) {
                    Visibility.attachTo(self.$node.find('.visibility'), {
                        value: ''
                    });
                });
            }

            if (newGraphVertexId) {
                this.vertexService.getVertexProperties(newGraphVertexId)
                    .done(this.updateResolveImageIcon.bind(this));
            } else this.updateResolveImageIcon();

        };

        this.updateConceptSelect = function(val) {
            var conceptSelect = this.select('conceptSelector').val(val);

            if (val) {
                this.select('actionButtonSelector').removeAttr('disabled');
            } else {
                this.select('actionButtonSelector').attr('disabled', true);
            }

            return conceptSelect;
        };

        this.onButtonClicked = function (event) {
            if (!this.attr.detectedObject) {
                this.termModification (event);
            } else {
                this.detectedObjectModification (event);
            }
        }

        this.termModification = function(event) {
            var self = this,
                $mentionNode = $(this.attr.mentionNode),
                newObjectSign = $.trim(this.select('objectSignSelector').val()),
                mentionStart,
                mentionEnd;

            if (this.attr.existing){
                var dataInfo = $mentionNode.data('info');
                mentionStart = dataInfo.start;
                mentionEnd = dataInfo.end;
            } else {
                mentionStart = this.selectedStart;
                mentionEnd = this.selectedEnd;
            }
            var parameters = {
                sign: newObjectSign,
                conceptId: this.select('conceptSelector').val(),
                mentionStart: mentionStart,
                mentionEnd: mentionEnd,
                artifactId: this.attr.artifactId,
                visibilitySource: this.visibilitySource || ''
            };

            if (this.currentGraphVertexId) {
                parameters.graphVertexId = this.currentGraphVertexId;
            }

            _.defer(this.buttonLoading.bind(this));

            if ( !parameters.conceptId || parameters.conceptId.length === 0) {
                this.select('conceptSelector').focus();
                return;
            }

            if (newObjectSign.length) {
                parameters.objectSign = newObjectSign;
                $mentionNode.attr('title', newObjectSign);
            }

            if (!this.unresolve) {
                this.vertexService.resolveTerm(parameters)
                    .done(function(data) {
                        self.highlightTerm(data);
                        self.trigger('termCreated', data);

                        self.trigger(document, 'refreshRelationships');

                        _.defer(self.teardown.bind(self));
                    });
            } else {
                this.vertexService.unresolveTerm(parameters)
                    .done(function(data) {
                        self.highlightTerm(data);

                        if (data.deleteEdge) {
                            self.trigger(document, 'edgesDeleted', { edgeId: data.edgeId });
                        }

                        self.trigger(document, 'refreshRelationships');

                        _.defer(self.teardown.bind(self));
                    });
            }
        };

        this.detectedObjectModification = function (event){
            var self = this,
                newSign = $.trim(this.select('objectSignSelector').val()),
                parameters = {
                    title: newSign,
                    conceptId: this.select('conceptSelector').val(),
                    graphVertexId: this.attr.resolvedVertex ? this.attr.resolvedVertex.id : this.currentGraphVertexId,
                    rowKey: this.attr.dataInfo.value._rowKey,
                    artifactId: this.attr.artifactData.id,
                    x1: parseFloat(this.attr.dataInfo.value.x1),
                    y1: parseFloat(this.attr.dataInfo.value.y1),
                    x2: parseFloat(this.attr.dataInfo.value.x2),
                    y2: parseFloat(this.attr.dataInfo.value.y2),
                    existing: !!this.currentGraphVertexId
                };

            _.defer(this.buttonLoading.bind(this));
            if (this.unresolve) {
                self.unresolveDetectedObject (parameters);
            } else {
                self.resolveDetectedObject (parameters);
            }

        }

        this.resolveDetectedObject = function (parameters) {
            var self = this;
            this.vertexService.resolveDetectedObject(parameters)
                .done(function(data) {
                    var resolvedVertex = data.entityVertex;

                    var $focused = $('.focused');
                    if ($focused) {
                        var $tag = $focused.find('.label-info');

                        $tag.text(data.entityVertex.properties.title.value).removeAttr('data-info').data('info', data.entityVertex).removePrefixedClasses('conceptType-');
                        $tag.addClass('resolved entity conceptType-' + resolvedVertex.properties._conceptType.value);

                        $focused.removeClass('focused');
                    } else {
                        // Temporarily creating a new tag to show on ui prior to backend update
                        var $allDetectedObjects = self.$node.closest('.type-content').find('.detected-object-labels');
                        var $allDetectedObjectLabels =$allDetectedObjects.find('.detected-object-tag .label-info');
                        var $parentSpan = $('<span>').addClass ('label detected-object-tag focused');

                        var classes = $allDetectedObjectLabels.attr('class');
                        if (!classes){
                            classes = 'label-info detected-object'
                        }
                        var $tag = $("<a>").addClass(classes + ' resolved entity').attr("href", "#").text(data.entityVertex.properties.title.value);

                        var added = false;

                        $parentSpan.append($tag);
                        $parentSpan.after(' ');

                        $allDetectedObjectLabels.each(function(){
                            if(parseFloat($(this).data("info").value.x1) > data.x1){
                                $tag.removePrefixedClasses('conceptType-').addClass('conceptType-' + parameters.conceptId).parent().insertBefore($(this).parent()).after(' ');
                                added = true;
                                return false;
                            }
                        });

                        if (!added){
                            $tag.addClass('conceptType-' + parameters.conceptId);
                            $allDetectedObjects.append($parentSpan);
                        }
                        $tag.attr('data-info', JSON.stringify(resolvedVertex));
                        $parentSpan.removeClass('focused');
                    }

                    self.trigger('termCreated', resolvedVertex);

                    self.trigger(document, 'updateVertices', { vertices: [resolvedVertex] });
                    self.trigger(document, 'refreshRelationships');

                    _.defer(self.teardown.bind(self));
                });
        };

        this.unresolveDetectedObject = function (parameters) {
            var self = this;
            this.vertexService.unresolveDetectedObject(parameters).done(function(data) {
                var $focused = $('.focused');
                var $tag = $focused.find('.label-info');
                if (data._rowKey) {
                    $tag.text(data.classifierConcept).removeAttr('data-info').data('info', data).removeClass();
                    $tag.addClass('label-info detected-object opens-dropdown');
                } else {
                    $tag.remove();
                }

                if (data.deleteEdge) {
                    self.trigger(document, 'edgesDeleted', { edgeId: data.edgeId });
                }

                self.trigger(document, 'updateVertices', { vertices: [data.artifactVertex] });
                self.trigger(document, 'refreshRelationships');
                _.defer(self.teardown.bind(self));
            });
        };

        this.onConceptChanged = function(event) {
            var select = $(event.target);

            this.updateConceptLabel(select.val());
        };

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data.value;
            // TODO: inspect valid
        };

        this.updateConceptLabel = function(conceptId, vertex) {
            if (conceptId === '') {
                this.select('actionButtonSelector').attr('disabled', true);
                return;
            }
            this.select('actionButtonSelector').removeAttr('disabled');

            if (this.allConcepts && this.allConcepts.length) {

                vertex = $(vertex || this.promoted || this.attr.mentionNode);
                var classPrefix = 'conceptType-',
                    labels = this.allConcepts.map(function(c) {
                        return classPrefix + c.id;
                    });

                vertex.removeClass(labels.join(' '))
                    .addClass(classPrefix + conceptId);
                this.updateResolveImageIcon(null, conceptId);
            }
        };

        this.setupContent = function() {

            var self = this,
                vertex = this.$node,
                existingEntity,
                objectSign = '',
                sign,
                data, graphVertexId, title;

            if (!this.attr.detectedObject){
                var mentionVertex = $(this.attr.mentionNode);
                data = mentionVertex.data('info');
                existingEntity = this.attr.existing ? mentionVertex.addClass('focused').hasClass('resolved') : false;
                graphVertexId = data && (data.id || data.graphVertexId);
                title = $.trim(data && data.title || '');

                if (this.attr.selection && !existingEntity) {
                    this.trigger(document, 'ignoreSelectionChanges.detail');
                    this.promoted = this.promoteSelectionToSpan();

                    // Promoted span might have been auto-expanded to avoid nested
                    // spans
                    sign = this.promoted.text();

                    setTimeout(function() {
                        self.trigger(document, 'resumeSelectionChanges.detail');
                    }, 10);
                }

                if (existingEntity && mentionVertex.hasClass('resolved')) {
                    objectSign = title;
                    this.unresolve = true;
                } else {
                    objectSign = this.attr.sign || mentionVertex.text();
                }
            } else {
                data = this.attr.resolvedVertex;
                objectSign = data ? data.properties.title.value : '';
                existingEntity = this.attr.existing;
                graphVertexId = data ? data.id : '';
                this.unresolve = graphVertexId && graphVertexId != '';
            }

            vertex.html(dropdownTemplate({
                sign: $.trim(objectSign),
                graphVertexId: graphVertexId,
                objectSign: $.trim(objectSign) || '',
                buttonText: existingEntity ? 'Resolve as Existing' : 'Resolve as New'
            }));

            this.graphVertexChanged(graphVertexId, data, true);

            if (objectSign) {
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
                info = $(self.attr.mentionNode).data('info');
            if (vertex) {
                $.extend(vertex, {}, vertex.properties);
            }

            if (!vertex && info && !conceptId) {
                self.deferredConcepts.done(function(allConcepts) {
                    var concept = self.conceptForConceptType(info._conceptType, allConcepts);
                    if (concept) {
                        updateCss(concept.glyphIconHref);
                    }
                });
            } else if (vertex && vertex.properties._glyphIcon) {
                updateCss(vertex && vertex.properties._glyphIcon.value);
            } else {
                self.deferredConcepts.done(function(allConcepts) {
                    var conceptType = (vertex && vertex.properties._conceptType.value) || conceptId;
                    if (conceptType) {
                        var concept = self.conceptForConceptType(conceptType, allConcepts);
                        if (concept) {
                            updateCss(concept.glyphIconHref);
                            return;
                        }
                    }

                    updateCss();
                });
            }

            function updateCss(src) {
                var preview = self.$node.find('.resolve-wrapper > .preview');

                if (src) {
                    var url = 'url("' + src  + '")';

                    if (preview.css('background-image') !== url) {
                        preview.css('background-image', url);
                    }
                } else {
                    preview.css('backgroundImage', ' ').addClass('icon-unknown');
                }
            }
        };

        this.conceptForConceptType = function(conceptType, allConcepts) {
            return _.findWhere(allConcepts, { id:conceptType });
        };

        this.registerEvents = function() {

            this.on('visibilitychange', this.onVisibilityChange);

            this.on('change', {
                conceptSelector: this.onConceptChanged
            });

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
                objectSignSelector: this.onKeyPress,
                conceptSelector: this.onKeyPress
            });

            this.on('opened', function() {
                this.loadConcepts()
                    .done(this.setupObjectTypeAhead.bind(this))
                    .done(function() {
                        this.deferredConcepts.resolve(this.allConcepts);
                    }.bind(this));
            });
        };

        this.loadConcepts = function() {
            var self = this;
            self.allConcepts = [];
            return self.ontologyService.concepts().done(function(concepts) {
                var vertexInfo;

                if (self.attr.detectedObject) {
                    vertexInfo = self.attr.resolvedVertex;
                } else {
                    var mentionVertex = $(self.attr.mentionNode);
                    vertexInfo = mentionVertex.data('info');
                }

                self.allConcepts = concepts.byTitle;

                self.select('conceptSelector').html(conceptsTemplate({
                    concepts: self.allConcepts,
                    selectedConceptId: (vertexInfo && (vertexInfo._conceptType || (vertexInfo.properties && vertexInfo.properties._conceptType.value))) || ''
                }));

                if (self.select('conceptSelector').val() === '') {
                    self.select('actionButtonSelector').attr('disabled', true);
                }
            });
        };


        this.runQuery = function(query) {
            query = $.trim(query || '');
            if (!this.queryCache) this.queryCache = {};
            if (this.queryCache[query]) return this.queryCache[query];

            var badge = this.select('objectSignSelector').nextAll('.badge');

            badge.addClass('loading');

            this.queryCache[query] = this.vertexService.graphVertexSearch(query)
                .then(function(response) {
                    badge.removeClass('loading');
                    return _.filter(response.vertices, function(v) {
                        return ~v.properties.title.value.toLowerCase().indexOf(query.toLowerCase());
                    });
                }).done(this.updateQueryCountBadge.bind(this));

            return this.queryCache[query];
        };

        this.updateQueryCountBadge = function(vertices) {
            this.$node.find('.badge')
                .attr('title', vertices.length + ' match' + (vertices.length === 1 ? '' : 'es') + ' found')
                .text(vertices.length);
        };

        this.setupObjectTypeAhead = function() {
            var self = this,
                items = [],
                input = this.select('objectSignSelector'),
                createNewText = 'Resolve as new entity';


            self.ontologyService.properties().done(function(ontologyProperties) {
                var field = input.typeahead({
                    items: 50,
                    source: function(query, callback) {

                        if (self.lastQuery && query !== self.lastQuery) {
                            self.reset();
                        }

                        if (!self.sourceCache) self.sourceCache = {};
                        else if (self.sourceCache[query]) {
                            self.sourceCache[query](callback);
                            return;
                        }

                        self.lastQuery = query;
                        var instance = this;

                        self.runQuery(query).done(function(entities) {
                            var all = _.map(entities, function(e) {
                                return $.extend({
                                    toLowerCase: function() { return e.properties.title.value.toLowerCase(); },
                                    toString: function() { return e.id; },
                                    indexOf: function(s) { return e.properties.title.value.indexOf(s); }
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
                        });
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
                            label = matchingItem[0].properties ? matchingItem[0].properties.title.value : matchingItem;

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
                                item : Object.getPrototypeOf(this).highlighter.apply(this, [item.properties.title.value]),
                            icon = '',
                            concept = _.find(self.allConcepts, function(c) {
                                return item.properties && c.id === item.properties._conceptType.value;
                            });

                        if (item.properties) {
                            icon = item._glyphIcon || (item.properties._glyphIcon && item.properties._glyphIcon.value) || concept.glyphIconHref;
                        }
                        return entityTemplate({
                            html: html,
                            item: item,
                            properties: item.properties && Properties.filterPropertiesForDisplay(item.properties, ontologyProperties),
                            iconSrc: icon,
                            concept: concept
                        });
                    }
                });

                var typeahead = field.data('typeahead'),
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
                            if (scrollNeeded > scrollTotalHeight) {
                                // TODO: add padding or grow upwards?
                            }

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
                    .addClass(data.cssClasses.join(' '))
                    .removeClass('focused');
                this.promoted = null;
            }
        };

        this.promoteSelectionToSpan = function() {
            var textVertex = this.node,
                range = this.attr.selection.range,
                el,
                tempTextNode;

            var span = document.createElement('span');
            span.className = 'entity focused';

            var newRange = document.createRange();
            newRange.setStart(range.startContainer, range.startOffset);
            newRange.setEnd(range.endContainer, range.endOffset);

            var r = range.cloneRange();
            r.selectNodeContents($(".detail-pane .text").get(0));
            r.setEnd(range.startContainer, range.startOffset);
            var l = r.toString().length;

            this.selectedStart = l;
            this.selectedEnd = l + range.toString().length;

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
