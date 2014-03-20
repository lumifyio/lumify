

define([
    './dropdowns/termForm/termForm',
    './dropdowns/statementForm/statementForm',
    'util/css-stylesheet',
    'colorjs',
    'service/vertex',
    'service/ontology',
    'util/jquery.withinScrollable'
], function(TermForm, StatementForm, stylesheet, colorjs, VertexService, OntologyService) {
    'use strict';

    var HIGHLIGHT_STYLES = [
            { name: 'None', selector:'none' },
            { name: 'Icons', selector:'icons' },
            { name: 'Underline', selector:'underline' },
            { name: 'Colors', selector:'colors' }
        ],
        DEFAULT = 2,
        useDefaultStyle = true;

    return WithHighlighting;

    function WithHighlighting() {
        this.vertexService = new VertexService();
        this.ontologyService = new OntologyService();

        this.defaultAttrs({
            resolvableSelector: '.text .entity',
            highlightedWordsSelector: '.entity, .term, .artifact',
            draggablesSelector: '.resolved, .artifact, .generic-draggable'
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
            this.highlightNode().off('scrollstop');
        });

        this.after('initialize', function() {
            var self = this;

            // Allow components to disable selection listening
            $(document).on('ignoreSelectionChanges.detail', function() {
                $(document).off('selectionchange.detail');
            });
            $(document).on('resumeSelectionChanges.detail', function() {
                $(document).off('selectionchange.detail').on('selectionchange.detail', self.onSelectionChange.bind(self));
            });
            $(document).trigger('resumeSelectionChanges');

            this.highlightNode().on('scrollstop', this.updateEntityAndArtifactDraggables.bind(this));
            this.on('click', {
                resolvableSelector: this.onResolvableClicked
            });
            this.on('mousedown mouseup click dblclick', this.trackMouse.bind(this));
            this.on(document, 'termCreated', this.updateEntityAndArtifactDraggables.bind(this));
            this.on('updateDraggables', this.updateEntityAndArtifactDraggables.bind(this));

            this.applyHighlightStyle();
        });

        this.trackMouse = function(event) {
            var $target = $(event.target);

            if (event.type === 'mouseup' || event.type === 'click' || event.type === 'dblclick') {
                this.mouseDown = false;
            } else {
                this.mouseDown = true;
            }

            if (event.type === 'mousedown' && $target.closest('.tooltip').length === 0 && this.ActionBar) {
                this.ActionBar.teardownAll();
            }

            if ($(event.target).closest('.opens-dropdown').length === 0 && $(event.target).closest('.underneath').length === 0 && !($(event.target).parent().hasClass('currentTranscript')) && !($(event.target).hasClass('alert alert-error'))) {
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
            $.each( content.attr('class').split(/\s+/), function(_, item) {
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
            $.each( content.attr('class').split(/\s+/), function(index, item) {
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

                this.ontologyService.concepts().done(function(concepts) {
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
                                    className = concept.rawClassName || (concept.className && ('entity.' + concept.className)),
                                    definition = function(state, template) {
                                        return (template || tpl)({ STATES:STATES, state:state, concept:concept, colorjs:colorjs });
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
                                /*
                                stylesheet.addRule(
                                    '.highlight-' + style.selector + ' .detected-object.' + className + '::hover' + ',' +
                                    '.highlight-' + style.selector + ' .detected-object.resolved.' + className + '::hover',
                                    definition(STATES.HOVER, doTpl)
                                );
                                */

                                stylesheet.addRule('.concepticon-' + (concept.className || concept.rawClassName), 'background-image: url(' + concept.glyphIconHref + ')');
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
                            glyphIconHref: '/img/glyphicons/glyphicons_036_file@2x.png'
                        });

                        style.styleApplied = true;
                    });
                });
            }
        };


        this.onSelectionChange = function(e) {
            var selection = window.getSelection(),
                text = selection.rangeCount === 1 ? $.trim(selection.toString()) : '';

            // Ignore selection events within the dropdown
            if ( selection.type == 'None' ||
                 $(selection.anchorNode).is('.underneath') ||
                 $(selection.anchorNode).parents('.underneath').length ||
                 $(selection.focusNode).is('.underneath') ||
                 $(selection.focusNode).parents('.underneath').length) {
                return;
            }
            
            if ( $(selection.anchorNode).closest('.text').length === 0 ) return;

            // Ignore if mouse cursor still down
            if (this.mouseDown) {
                return;
            }

            // Ignore if selection hasn't change
            if (text.length && text === this.previousSelection) {
                return;
            } else this.previousSelection = text;

            this.handleSelectionChange();
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

                var range = sel.getRangeAt(0);

                // Avoid adding dropdown inside of entity
                var endContainer = range.endContainer;
                while (/entity/.test(endContainer.parentNode.className)) {
                    endContainer = endContainer.parentNode;
                }

                var self = this,
                    selection = sel && { 
                        anchor:sel.anchorNode,
                        focus:sel.focusNode,
                        anchorOffset: sel.anchorOffset,
                        focusOffset: sel.focusOffset,
                        range:sel.rangeCount && sel.getRangeAt(0).cloneRange() 
                    };

                // Don't show action bar if dropdown opened
                if (this.$node.find('.text.dropdown').length) return;

                require(['util/actionbar/actionbar'], function(ActionBar) {
                    self.ActionBar = ActionBar;
                    ActionBar.teardownAll();
                    ActionBar.attachTo(self.node, {
                        alignTo: 'textselection',
                        actions: {
                            Resolve: 'resolve.actionbar'
                        }
                    });

                    self.off('.actionbar').on('resolve.actionbar', function () {

                        var isEndTextNode = endContainer.nodeType === 1;
                        if (isEndTextNode) {
                            self.dropdownEntity(true, endContainer, selection, text);
                        } else {

                            // Move to first space in end so as to not break up word when splitting
                            var i = Math.max(range.endOffset - 1, 0), character = '', whitespaceCheck = /^[^\s]$/;
                            do {
                                character = endContainer.textContent.substring(++i, i+1);
                            } while (whitespaceCheck.test(character));

                            endContainer.splitText(i);
                            self.dropdownEntity(true, endContainer, selection, text);
                        }
                    });
                });
            }
        }, 250);

        this.dropdownEntity = function(creating, insertAfterNode, selection, text) {
            this.tearDownDropdowns();

            var form = $('<div class="underneath"/>');
            $(insertAfterNode).after(form);
            TermForm.attachTo(form, {
                sign: text,
                selection: selection,
                mentionNode: insertAfterNode,
                existing: !creating,
                artifactId: this.attr.data.id
            });
        };

        this.onResolvableClicked = function(event) {
            var $target = $(event.target);
            if ($target.is('.underneath') || $target.parents('.underneath').length) {
                return;
            }
            _.defer(this.dropdownEntity.bind(this), false, $target);
        };

        this.updateEntityAndArtifactDraggables = function() {
            var self = this,
                words = this.select('draggablesSelector');

            // Filter list to those in visible scroll area
            words
                .withinScrollable(this.$node.closest('.content'))
                .draggable({
                    helper:'clone',
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
                })
                .droppable({
                    activeClass: 'drop-target',
                    hoverClass: 'drop-hover',
                    tolerance: 'pointer',
                    accept: function(el) {
                        var item = $(el),
                            isEntity = item.is('.entity');

                        return isEntity;
                    },
                    drop: function(event, ui) {
                        var destTerm = $(this);
                        var form;

                        if (destTerm.hasClass('opens-dropdown')) {
                            form = $('<div class="underneath"/>').insertAfter (destTerm.closest('.detected-object-labels'));
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
        };

        this.tearDownDropdowns = function() {
            TermForm.teardownAll();
            StatementForm.teardownAll();
        };

    }
});
