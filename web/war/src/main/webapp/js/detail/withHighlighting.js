
define([
    './dropdowns/termForm/termForm',
    './dropdowns/statementForm/statementForm',
    './dropdowns/propertyForm/propForm',
    'hbs!./text',
    'util/css-stylesheet',
    'util/privileges',
    'util/range',
    'colorjs',
    'util/withDataRequest',
    'util/vertex/formatters',
    'util/popovers/withElementScrollingPositionUpdates',
    'util/range',
    'util/jquery.withinScrollable',
], function(
    TermForm,
    StatementForm,
    PropertyForm,
    textTemplate,
    stylesheet,
    Privileges,
    rangeUtils,
    colorjs,
    withDataRequest,
    F,
    withPositionUpdates,
    range) {
    'use strict';

    var HIGHLIGHT_STYLES = [
            { name: 'None', selector: 'none' },
            { name: 'Icons', selector: 'icons' },
            { name: 'Underline', selector: 'underline' },
            { name: 'Colors', selector: 'colors' }
        ],
        TEXT_PROPERTIES = [
            'http://lumify.io#videoTranscript',
            'http://lumify.io#text'
        ],
        DEFAULT = 2,
        useDefaultStyle = true;

    return WithHighlighting;

    function WithHighlighting() {

        withPositionUpdates.call(this);
        withDataRequest.call(this);

        this.defaultAttrs({
            resolvableSelector: '.text .resolved',
            resolvedSelector: '.text .vertex.resolved',
            highlightedWordsSelector: '.vertex, .property, .term, .artifact',
            draggablesSelector: '.vertex.resolved, .artifact, .generic-draggable',
            textContainerSelector: '.texts',
            textContainerHeaderSelector: '.texts .text-section h1'
        });

        // Automatically refresh draggables when request completes
        this.before('handleCancelling', function(xhr) {
            var self = this;
            xhr.always(function() {
                _.defer(self.updateEntityAndArtifactDraggables.bind(self));
            });
        });

        this.after('teardown', function() {
            $(document).off('selectionchange.detail');
            $(document).off('ignoreSelectionChanges.detail');
            $(document).off('resumeSelectionChanges.detail');
            $(document).off('termCreated');
            if (this.scrollNode) {
                this.scrollNode.off('scrollstop scroll');
            }
        });

        this.before('initialize', function() {
            this.updateEntityAndArtifactDraggablesNoDelay = this.updateEntityAndArtifactDraggables;
            this.updateEntityAndArtifactDraggables = _.throttle(this.updateEntityAndArtifactDraggables.bind(this), 250);
        })

        this.after('initialize', function() {
            var self = this;

            // Allow components to disable selection listening
            $(document).on('ignoreSelectionChanges.detail', function() {
                $(document).off('selectionchange.detail');
            });
            $(document).on('resumeSelectionChanges.detail', function() {
                $(document)
                    .off('selectionchange.detail')
                    .on('selectionchange.detail', self.onSelectionChange.bind(self));
            });
            $(document).trigger('resumeSelectionChanges');

            this.on('finishedLoadingTypeContent', _.once(function() {
                _.defer(function() {
                    self.scrollNode = self.$node.find('.nav-with-background').children().eq(0).scrollParent()
                        .on('scrollstop', self.updateEntityAndArtifactDraggables)
                        .on('scroll', self.updateEntityAndArtifactDraggables);
                })
            }));

            this.on('click', {
                resolvableSelector: this.onResolvableClick,
                textContainerHeaderSelector: this.onTextHeaderClicked,
            });
            this.on('contextmenu', {
                resolvedSelector: this.onResolvedContextClick
            });
            this.on('copy cut', {
                textContainerSelector: this.onCopyText
            });
            this.on('mousedown mouseup click dblclick contextmenu', this.trackMouse.bind(this));
            this.on('updateDraggables', this.updateEntityAndArtifactDraggables.bind(this));
            this.on('dropdownClosed', this.onDropdownClosed);

            this.on(document, 'termCreated', this.updateEntityAndArtifactDraggables.bind(this));
            this.on(document, 'textUpdated', this.onTextUpdated);
            this.on(document, 'verticesUpdated', this.onVerticesUpdatedWithHighlighting);

            this.applyHighlightStyle();
        });

        this.onTextUpdated = function(event, data) {
            if (data.vertexId === this.attr.data.id) {
                this.updateText();
            }
        };

        this.onDropdownClosed = function(event, data) {
            var self = this;
            _.defer(function() {
                self.checkIfReloadNeeded();
            })
        };

        this.checkIfReloadNeeded = function() {
            if (this.reloadText) {
                var func = this.reloadText;
                this.reloadText = null;
                func();
            }
        };

        this.onVerticesUpdatedWithHighlighting = function(event, data) {
            var vertex = _.findWhere(data.vertices, { id: this.attr.data.id });
            if (vertex) {
                this.attr.data = vertex;
                var foundTextLikePropertyChange = _.some(vertex.properties, function(p) {
                    return _.some(TEXT_PROPERTIES, function(name) {
                        return p.name === name;
                    });
                });

                if (foundTextLikePropertyChange) {
                    this.updateText();
                }
            }
        };

        this.onCopyText = function(event) {
            var selection = getSelection(),
                target = event.target;

            if (!selection.isCollapsed && selection.rangeCount === 1) {

                var data = this.transformSelection(selection);
                if (data.startOffset && data.endOffset) {
                    this.trigger('copydocumenttext', data);
                }
            }
        };

        this.onTextHeaderClicked = function(event) {
            var $section = $(event.target)
                    .closest('.text-section')
                    .siblings('.expanded').removeClass('expanded')
                    .end(),
                propertyKey = $section.data('key');

            if ($section.hasClass('expanded')) {
                $section.removeClass('expanded');
            } else {
                this.openText(propertyKey);
            }
        };

        this.transformSelection = function(selection) {
            var $anchor = $(selection.anchorNode),
                $focus = $(selection.focusNode),
                isTranscript = $anchor.closest('.av-times').length,
                offsetsFunction = isTranscript ?
                    'offsetsForTranscript' :
                    'offsetsForText',
                offsets = this[offsetsFunction]([
                    {el: $anchor, offset: selection.anchorOffset},
                    {el: $focus, offset: selection.focusOffset}
                ], '.text', _.identity),
                range = selection.getRangeAt(0),
                contextHighlight = rangeUtils.createSnippetFromRange(
                    range, undefined, $anchor.closest('.text')[0]
                );

            return {
                startOffset: offsets && offsets[0],
                endOffset: offsets && offsets[1],
                snippet: contextHighlight,
                vertexId: this.attr.data.id,
                textPropertyKey: $anchor.closest('.text-section').data('key'),
                text: selection.toString(),
                vertexTitle: F.vertex.title(this.attr.data)
            };
        };

        this.trackMouse = function(event) {
            var $target = $(event.target);

            if ($target.is('.resolved,.vertex')) {
                if (event.type === 'mousedown') {
                    range.clearSelection();
                }
            }

            if (event.type === 'contextmenu') {
                event.preventDefault();
            }

            if (~'mouseup click dblclick contextmenu'.split(' ').indexOf(event.type)) {
                this.mouseDown = false;
            } else {
                this.mouseDown = true;
            }

            if ($(event.target).closest('.opens-dropdown').length === 0 &&
                $(event.target).closest('.underneath').length === 0 &&
                !($(event.target).parent().hasClass('currentTranscript')) &&
                !($(event.target).hasClass('alert alert-error'))) {
                if (event.type === 'mouseup' || event.type === 'dblclick') {
                    this.handleSelectionChange();
                }
            }
        };

        this.highlightNode = function() {
            return this.$node.closest('.content');
        };

        this.getActiveStyle = function() {
            if (useDefaultStyle) {
                if (typeof this.attr.highlightStyle !== 'undefined') {
                    return this.attr.highlightStyle;
                }
                return DEFAULT;
            }

            var content = this.highlightNode(),
                index = 0;
            $.each(content.attr('class').split(/\s+/), function(_, item) {
                var match = item.match(/^highlight-(.+)$/);
                if (match) {
                    return HIGHLIGHT_STYLES.forEach(function(style, i) {
                        if (style.selector === match[1]) {
                            index = i;
                            return false;
                        }
                    });
                }
            });

            return index;
        };

        this.removeHighlightClasses = function() {
            var content = this.highlightNode();
            $.each(content.attr('class').split(/\s+/), function(index, item) {
                if (item.match(/^highlight-(.+)$/)) {
                    content.removeClass(item);
                }
            });
        };

        this.applyHighlightStyle = function() {
            var style = HIGHLIGHT_STYLES[this.getActiveStyle()];
            this.removeHighlightClasses();
            this.highlightNode().addClass('highlight-' + style.selector);

            if (!style.styleApplied) {
                this.dataRequest('ontology', 'concepts').done(function(concepts) {
                    var styleFile = 'tpl!detail/highlight-styles/' + style.selector + '.css',
                        detectedObjectStyleFile = 'tpl!detail/highlight-styles/detectedObject.css';

                    require([styleFile, detectedObjectStyleFile], function(tpl, doTpl) {
                        function apply(concept) {
                            if (concept.color) {
                                var STATES = {
                                        NORMAL: 0,
                                        HOVER: 1,
                                        DIM: 2,
                                        TERM: 3
                                    },
                                    className = concept.rawClassName ||
                                        (concept.className && ('vertex.' + concept.className)),
                                    definition = function(state, template) {
                                        return (template || tpl)({
                                            STATES: STATES,
                                            state: state,
                                            concept: concept,
                                            colorjs: colorjs
                                        });
                                    };

                                if (!className) {
                                    return;
                                }

                                // Dim
                                // (when dropdown is opened and it wasn't this entity)
                                stylesheet.addRule(
                                    '.highlight-' + style.selector + ' .dropdown .' + className + ',' +
                                    '.highlight-' + style.selector + ' .dropdown .resolved.' + className + ',' +
                                    '.highlight-' + style.selector + ' .drag-focus .' + className,
                                    definition(STATES.DIM)
                                );

                                stylesheet.addRule(
                                   '.highlight-' + style.selector + ' .' + className,
                                   definition(STATES.TERM)
                                );

                                // Default style (or focused)
                                stylesheet.addRule(
                                    '.highlight-' + style.selector + ' .resolved.' + className + ',' +
                                    '.highlight-' + style.selector + ' .drag-focus .resolved.' + className + ',' +
                                    '.highlight-' + style.selector + ' .dropdown .focused.' + className,
                                    definition(STATES.NORMAL)
                                );

                                // Drag-drop hover
                                stylesheet.addRule(
                                    '.highlight-' + style.selector + ' .drop-hover.' + className,
                                    definition(STATES.HOVER)
                                );

                                // Detected objects
                                stylesheet.addRule(
                                    '.highlight-' + style.selector + ' .detected-object.' + className + ',' +
                                    '.highlight-' + style.selector + ' .detected-object.resolved.' + className,
                                    definition(STATES.DIM, doTpl)
                                );
                                stylesheet.addRule(
                                    //'.highlight-' + style.selector + ' .detected-object.' + className + ',' +
                                    //'.highlight-' + style.selector + ' .detected-object.resolved.' + className + ',' +
                                    '.highlight-' + style.selector + ' .focused .detected-object.' + className + ',' +
                                    '.highlight-' + style.selector + ' .focused .detected-object.resolved.' + className,
                                    definition(STATES.NORMAL, doTpl)
                                );

                                stylesheet.addRule(
                                    '.concepticon-' + (concept.className || concept.rawClassName),
                                    'background-image: url(' + concept.glyphIconHref + ')'
                                );
                            }
                            if (concept.children) {
                                concept.children.forEach(apply);
                            }
                        }
                        apply(concepts.entityConcept);

                        // Artifacts
                        apply({
                            rawClassName: 'artifact',
                            color: 'rgb(255,0,0)',
                            glyphIconHref: '../img/glyphicons/glyphicons_036_file@2x.png'
                        });

                        style.styleApplied = true;
                    });
                });
            }
        };

        this.onSelectionChange = function(e) {
            var self = this,
                selection = window.getSelection(),
                text = selection.rangeCount === 1 ? $.trim(selection.toString()) : '';

            // Ignore selection events within the dropdown
            if ($(selection.anchorNode).is('.underneath') ||
                 $(selection.anchorNode).parents('.underneath').length ||
                 $(selection.focusNode).is('.underneath') ||
                 $(selection.focusNode).parents('.underneath').length) {
                return;
            }

            if (/None|Caret/.test(selection.type)) {
                this.checkIfReloadNeeded();
                return;
            }

            if ($(selection.anchorNode).closest('.text').length === 0) return;

            // Ignore if mouse cursor still down
            if (this.mouseDown) {
                return;
            }

            // Ignore if selection hasn't change
            if (text.length && text === this.previousSelection) {
                return;
            } else this.previousSelection = text;

            require(['util/actionbar/actionbar'], function(ActionBar) {
                ActionBar.teardownAll();
                self.handleSelectionChange();
            });
        };

        this.handleSelectionChange = _.debounce(function() {
            var sel = window.getSelection(),
                text = sel && sel.rangeCount === 1 ? $.trim(sel.toString()) : '';

            if (text && text.length > 0) {
                var anchor = $(sel.anchorNode),
                    focus = $(sel.focusNode),
                    is = '.detail-pane .text';

                // Ignore outside content text
                if (anchor.parents(is).length === 0 || focus.parents(is).length === 0) {
                    return;
                }

                // Ignore if too long of selection
                var wordLength = text.split(/\s+/).length;
                if (wordLength > 10) {
                    return;
                }

                if (sel.rangeCount === 0) return;

                var range = sel.getRangeAt(0),
                    // Avoid adding dropdown inside of entity
                    endContainer = range.endContainer;

                while (/vertex/.test(endContainer.parentNode.className)) {
                    endContainer = endContainer.parentNode;
                }

                var self = this,
                    selection = sel && {
                        anchor: sel.anchorNode,
                        focus: sel.focusNode,
                        anchorOffset: sel.anchorOffset,
                        focusOffset: sel.focusOffset,
                        range: sel.rangeCount && sel.getRangeAt(0).cloneRange()
                    };

                // Don't show action bar if dropdown opened
                if (this.$node.find('.text.dropdown').length) return;

                if (Privileges.missingEDIT) return;

                require(['util/actionbar/actionbar'], function(ActionBar) {
                    ActionBar.teardownAll();
                    ActionBar.attachTo(self.node, {
                        alignTo: 'textselection',
                        alignWithin: anchor.closest(is),
                        actions: {
                            Entity: 'resolve.actionbar',
                            Property: 'property.actionbar',
                            Comment: 'comment.actionbar'
                        }
                    });

                    self.off('.actionbar')
                        .on('comment.actionbar', function(event) {
                            event.stopPropagation();

                            var data = self.transformSelection(sel);
                            if (data.startOffset && data.endOffset) {
                                self.trigger(self.select('commentsSelector'), 'commentOnSelection', data);
                            }
                        })
                        .on('property.actionbar', function(event) {
                            event.stopPropagation();
                            _.defer(function() {
                                self.dropdownProperty(getNode(endContainer), sel, text);
                            })
                        })
                        .on('resolve.actionbar', function(event) {
                            event.stopPropagation();

                            self.dropdownEntity(true, getNode(endContainer), selection, text);
                        });

                    function getNode(node) {
                        var isEndTextNode = node.nodeType === 1;
                        if (isEndTextNode) {
                            return node;
                        } else {

                            // Move to first space in end so as to not break up word when splitting
                            var i = Math.max(range.endOffset - 1, 0), character = '', whitespaceCheck = /^[^\s]$/;
                            do {
                                character = node.textContent.substring(++i, i + 1);
                            } while (whitespaceCheck.test(character));

                            node.splitText(i);
                            return node;
                        }
                    }
                });
            }
        }, 250);

        this.dropdownEntity = function(creating, insertAfterNode, selection, text) {
            this.tearDownDropdowns();

            var form = $('<div class="underneath"/>'),
                $node = $(insertAfterNode),
                $textSection = $node.closest('.text-section'),
                $textBody = $textSection.children('.text');

            $node.after(form);
            TermForm.attachTo(form, {
                sign: text,
                propertyKey: $textSection.data('key'),
                selection: selection,
                mentionNode: insertAfterNode,
                snippet: selection ?
                    range.createSnippetFromRange(selection.range, undefined, $textBody[0]) :
                    range.createSnippetFromNode(insertAfterNode[0], undefined, $textBody[0]),
                existing: !creating,
                artifactId: this.attr.data.id
            });
        };

        this.dropdownProperty = function(insertAfterNode, selection, text) {
            this.tearDownDropdowns();

            var form = $('<div class="underneath"/>'),
                $node = $(insertAfterNode),
                $textSection = $node.closest('.text-section'),
                $textBody = $textSection.children('.text'),
                dataInfo = $node.data('info');

            $node.after(form);

            PropertyForm.attachTo(form, {
                attemptToCoerceValue: text,
                sourceInfo: selection ?
                    this.transformSelection(selection) :
                    {
                        vertexId: this.attr.data.id,
                        textPropertyKey: $textSection.data('key'),
                        startOffset: dataInfo.start,
                        endOffset: dataInfo.end,
                        snippet: range.createSnippetFromNode($node[0], undefined, $textBody[0]),
                    }
            });
        };

        this.onResolvedContextClick = function(event) {
            var $target = $(event.target).closest('span'),
                vertexId = $target.data('info').resolvedToVertexId;

            range.clearSelection();

            event.preventDefault();
            event.stopPropagation();

            this.trigger($target, 'showVertexContextMenu', {
                vertexId: vertexId,
                position: {
                    x: event.pageX,
                    y: event.pageY
                }
            });
        };

        this.onResolvableClick = function(event) {
            var self = this,
                $target = $(event.target);

            if ($target.is('.underneath') || $target.parents('.underneath').length) {
                return;
            }

            require(['util/actionbar/actionbar'], function(ActionBar) {
                ActionBar.teardownAll();
                self.off('.actionbar');

                if ($target.hasClass('resolved')) {
                    var info = $target.data('info');

                    ActionBar.attachTo($target, {
                        alignTo: 'node',
                        alignWithin: $target.closest('.text'),
                        actions: $.extend({
                            Open: 'open.actionbar',
                            Fullscreen: 'fullscreen.actionbar'
                        }, Privileges.canEDIT && info.termMentionFor === 'VERTEX' && !F.vertex.isPublished(info) ? {
                            Unresolve: 'unresolve.actionbar'
                        } : {})
                    });

                    self.off('.actionbar')
                        .on('open.actionbar', function(event) {
                            event.stopPropagation();
                            self.trigger('selectObjects', { vertexIds: $target.data('info').resolvedToVertexId });
                        })
                        .on('fullscreen.actionbar', function(event) {
                            event.stopPropagation();
                            self.trigger('openFullscreen', { vertices: $target.data('info').resolvedToVertexId });
                        });
                    self.on('unresolve.actionbar', function(event) {
                        event.stopPropagation();
                        _.defer(self.dropdownEntity.bind(self), false, $target);
                    });

                } else if (Privileges.canEDIT) {

                    ActionBar.attachTo($target, {
                        alignTo: 'node',
                        alignWithin: $target.closest('.text'),
                        actions: {
                            Entity: 'resolve.actionbar',
                            Property: 'property.actionbar'
                            // TODO: Comment: 'comment.actionbar'
                        }
                    });

                    self.off('.actionbar')
                        .on('resolve.actionbar', function(event) {
                            _.defer(self.dropdownEntity.bind(self), false, $target);
                            event.stopPropagation();
                        })
                        .on('property.actionbar', function(event) {
                            event.stopPropagation();
                            _.defer(function() {
                                self.dropdownProperty($target, null, $target.text());
                            })
                        })
                        //.on('comment.actionbar', function(event) {
                            //event.stopPropagation();
                        //})
                }
            });
        };

        this.updateEntityAndArtifactDraggables = function() {
            var self = this,
                scrollNode = this.scrollNode,
                words = this.select('draggablesSelector');

            if (words.length === 0 || !scrollNode || scrollNode.length === 0) {
                return;
            }

            this.dataRequest('ontology', 'concepts')
                .done(function(concepts) {

                    // Filter list to those in visible scroll area
                    words
                        .withinScrollable(scrollNode)
                        .each(function() {
                            var $this = $(this),
                                info = $this.data('info'),
                                type = info && info['http://lumify.io#conceptType'],
                                concept = type && concepts.byId[type];

                            if (concept) {
                                $this.removePrefixedClasses('conceptId-').addClass(concept.className)
                            }
                        })
                        .draggable({
                            helper: 'clone',
                            revert: 'invalid',
                            revertDuration: 250,
                            // scroll:true (default) requests position:relative on
                            // detail-pane .content, but that breaks dragging from
                            // detail-pane to graph.
                            scroll: false,
                            zIndex: 100,
                            distance: 10,
                            cursorAt: { left: -10, top: -10 },
                            start: function() {
                                $(this)
                                    .parents('.text').addClass('drag-focus');
                            },
                            stop: function() {
                                $(this)
                                    .parents('.text').removeClass('drag-focus');
                            }
                        });

                    if (Privileges.canEDIT) {

                        words.droppable({
                            activeClass: 'drop-target',
                            hoverClass: 'drop-hover',
                            tolerance: 'pointer',
                            accept: function(el) {
                                var item = $(el),
                                    isEntity = item.is('.vertex.resolved');

                                return isEntity;
                            },
                            drop: function(event, ui) {
                                var destTerm = $(this),
                                    form;

                                if (destTerm.hasClass('opens-dropdown')) {
                                    form = $('<div class="underneath"/>')
                                        .insertAfter(destTerm.closest('.detected-object-labels'));
                                } else {
                                    form = $('<div class="underneath"/>').insertAfter(destTerm);
                                }
                                self.tearDownDropdowns();

                                StatementForm.attachTo(form, {
                                    sourceTerm: ui.draggable,
                                    destTerm: destTerm
                                });
                            }
                        });
                    }
                });
        };

        this.tearDownDropdowns = function() {
            TermForm.teardownAll();
            PropertyForm.teardownAll();
            StatementForm.teardownAll();
        };

        this.updateText = function updateText(d3) {
            if (!d3) {
                return require(['d3'], updateText.bind(this));
            }

            var self = this,
                scrollParent = this.$node.scrollParent(),
                scrollTop = scrollParent.scrollTop(),
                expandedKey = this.$node.find('.text-section.expanded').data('key'),
                textProperties = _.filter(this.attr.data.properties, function(p) {
                    return _.some(TEXT_PROPERTIES, function(name) {
                        return name === p.name;
                    });
                });

            d3.select(self.select('textContainerSelector')[0])
                .selectAll('section.text-section')
                .data(textProperties)
                .call(function() {
                    this.enter()
                        .append('section')
                        .attr('class', 'text-section collapsible')
                        .call(function() {
                            this.append('h1')
                                .call(function() {
                                    this.append('strong')
                                    this.append('span').attr('class', 'badge')
                                })
                            this.append('div').attr('class', 'text').text('1')
                        })

                    this.attr('data-key', function(p) {
                            return p.key;
                        })
                        .each(function() {
                            var p = d3.select(this).datum();
                            $(this).removePrefixedClasses('ts-').addClass('ts-' + F.className.to(p.key));
                        })
                    this.select('h1 strong').text(function(p) {
                        var textDescription = 'http://lumify.io#textDescription';
                        return p[textDescription] || p.metadata[textDescription] || p.key;
                    })

                    this.exit().remove();
                });

            if (textProperties.length) {
                if (this.attr.focus) {
                    this.openText(this.attr.focus.textPropertyKey)
                        .done(function() {
                            var $text = self.$node.find('.' +
                                    F.className.to(self.attr.focus.textPropertyKey) + ' .text'),
                                $transcript = $text.find('.av-times'),
                                focusOffsets = self.attr.focus.offsets;

                            if ($transcript.length) {
                                var start = F.number.offsetValues(focusOffsets[0]),
                                    end = F.number.offsetValues(focusOffsets[1]),
                                    $container = $transcript.find('dd').eq(start.index);

                                rangeUtils.highlightOffsets($container.get(0), [start.offset, end.offset]);
                            } else {
                                rangeUtils.highlightOffsets($text.get(0), focusOffsets);
                            }
                            self.attr.focus = null;
                        });
                } else if (expandedKey || textProperties.length === 1) {
                    this.openText(expandedKey || textProperties[0].key, {
                        scrollToSection: textProperties.length !== 1
                    }).done(function() {
                        scrollParent.scrollTop(scrollTop);
                    });
                } else if (textProperties.length > 1) {
                    this.openText(textProperties[0].key, {
                        expand: false
                    });
                }
            }
        };

        this.openText = function(propertyKey, options) {
            var self = this,
                expand = !options || options.expand !== false,
                $section = this.$node.find('.ts-' + F.className.to(propertyKey)),
                isExpanded = $section.is('.expanded'),
                $badge = $section.find('.badge'),
                selection = getSelection(),
                range = selection.rangeCount && selection.getRangeAt(0),
                hasSelection = isExpanded && range && !range.collapsed,
                hasOpenForm = isExpanded && $section.find('.underneath').length;

            if (hasSelection || hasOpenForm) {
                this.reloadText = this.openText.bind(this, propertyKey, options);
                return Promise.resolve();
            }

            if (expand) {
                $section.closest('.texts').find('.loading').removeClass('loading');
                $badge.addClass('loading');
            }

            if (this.openTextRequest && this.openTextRequest.abort) {
                this.openTextRequest.abort();
            }

            this.openTextRequest = this.dataRequest('vertex', 'highlighted-text', this.attr.data.id, propertyKey);

            return this.openTextRequest.then(function(artifactText) {
                    var html = self.processArtifactText(artifactText);
                    if (expand) {
                        text = selection.rangeCount === 1 ? $.trim(selection.toString()) : '';
                        $section.find('.text').html(html);
                        $section.addClass('expanded');
                        $badge.removeClass('loading');

                        self.updateEntityAndArtifactDraggablesNoDelay();
                        if (!options || options.scrollToSection !== false) {
                            self.scrollToRevealSection($section);
                        }
                    }
                });
        };

        this.offsetsForText = function(input, parentSelector, offsetTransform) {
            var offsets = [];
            input.forEach(function(node) {
                var parentInfo = node.el.closest('.vertex').data('info'),
                    offset = 0;

                if (parentInfo) {
                    offset = offsetTransform(parentInfo.start);
                } else {
                    var previousEntity = node.el.prevAll('.vertex').first(),
                    previousInfo = previousEntity.data('info'),
                    dom = previousInfo ?
                        previousEntity.get(0) :
                        node.el.closest(parentSelector)[0].childNodes[0],
                    el = node.el.get(0);

                    if (previousInfo) {
                        offset = offsetTransform(previousInfo.end);
                        dom = dom.nextSibling;
                    }

                    while (dom && dom !== el) {
                        if (dom.nodeType === 3) {
                            offset += dom.length;
                        } else {
                            offset += dom.textContent.length;
                        }
                        dom = dom.nextSibling;
                    }
                }

                offsets.push(offset + node.offset);
            });

            return _.sortBy(offsets, function(a, b) {
                return a - b
            });
        };

        this.offsetsForTranscript = function(input) {
            var self = this,
                index = input[0].el.closest('dd').data('index'),
                endIndex = input[1].el.closest('dd').data('index');

            if (index !== endIndex) {
                return console.warn('Unable to select across timestamps');
            }

            var rawOffsets = this.offsetsForText(input, 'dd', function(offset) {
                    return F.number.offsetValues(offset).offset;
                }),
                bitMaskedOffset = _.map(rawOffsets, _.partial(F.number.compactOffsetValues, index));

            return bitMaskedOffset;
        };

        this.scrollToRevealSection = function($section) {
            var scrollIfWithinPixelsFromBottom = 150,
                y = $section.offset().top,
                scrollParent = $section.scrollParent(),
                scrollTop = scrollParent.scrollTop(),
                scrollHeight = scrollParent[0].scrollHeight,
                height = scrollParent.outerHeight(),
                maxScroll = height * 0.5,
                fromBottom = height - y;

            if (fromBottom < scrollIfWithinPixelsFromBottom) {
                scrollParent.animate({
                    scrollTop: Math.min(scrollHeight - scrollTop, maxScroll)
                }, 'fast');
            }
        };

        this.processArtifactText = function(text) {
            var self = this,
                warningText = i18n('detail.text.none_available');

            return !text ?  alertTemplate({ warning: warningText }) : this.normalizeString(text);
        }

        this.normalizeString = function(text) {
            return text.replace(/(\n+)/g, '<br><br>$1');
        };

    }
});
