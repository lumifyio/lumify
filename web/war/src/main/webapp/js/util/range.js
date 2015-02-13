
define([
    'rangy-core',
    'rangy-text',
    'rangy-highlighter'
], function(rangy) {
    'use strict';

    if (!rangy.initialized) {
        console.warn('Rangy should have been initialized by ClipboardManager...')
        rangy.init();
    }

    return {

        clearSelection: function() {
            rangy.getSelection().removeAllRanges();
        },

        highlightOffsets: function(textElement, offsets) {
            textElement.normalize();

            var childNodes = textElement.childNodes,
                len = childNodes.length,
                node,
                startOffset = 0,
                endOffset = 0,
                startContainer = null,
                endContainer = null,
                i = 0;

            for (; i < len; i++) {
                node = childNodes[i];

                var toAdd = 0;
                if (node.nodeType === node.TEXT_NODE) {
                    toAdd = node.length;
                } else {
                    toAdd = node.textContent.length;
                    node = node.childNodes[0];
                }

                if (!startContainer) {
                    if ((startOffset + toAdd) >= offsets[0]) {
                        startContainer = node;
                        if (endContainer) break;
                    } else startOffset += toAdd;
                }
                if (!endContainer) {
                    if ((endOffset + toAdd) > offsets[1]) {
                        endContainer = node;
                        if (startContainer) break;
                    } else endOffset += toAdd;
                }
            }

            var range = rangy.createRange(),
                highlighter = rangy.createHighlighter();

            highlighter.addClassApplier(rangy.createCssClassApplier('highlight', {
                ignoreWhiteSpace: true,
                tagNames: ['span']
            }));

            range.setStart(startContainer, offsets[0] - startOffset);
            range.setEnd(endContainer, offsets[1] - endOffset)
            range.select()

            var highlight = highlighter.highlightSelection('highlight');

            rangy.getSelection().removeAllRanges()

            var newEl = highlight[0].getHighlightElements()[0],
                $newEl = $(newEl),
                scrollParent = $newEl.scrollParent(),
                scrollTo = newEl.offsetTop;

            scrollParent.clearQueue().animate({
                scrollTop: scrollTo - 100
            }, {
                duration: 'fast',
                easing: 'easeInOutQuad',
                complete: function() {
                    $newEl.on(ANIMATION_END, function(e) {
                        highlighter.removeAllHighlights();
                    });
                    $newEl.addClass('fade-slow');
                }
            });
        },

        expandRangeByWords: function(range, numberWords, splitBeforeAfterOutput) {

            var e = rangy.createRange();
            e.setStart(range.startContainer, range.startOffset);
            e.setEnd(range.endContainer, range.endOffset);

            // Move range start to include n more of words
            e.moveStart('word', -numberWords);

            // Move range end to include n more words
            e.moveEnd('word', numberWords);

            // Calculate what we just included and send that back
            if (splitBeforeAfterOutput) {
                var output = rangy.createRange();
                output.setStart(e.startContainer, e.startOffset);
                output.setEnd(range.startContainer, range.startOffset);
                splitBeforeAfterOutput.before = output.text();

                output.setStart(range.endContainer, range.endOffset);
                output.setEnd(e.endContainer, e.endOffset);
                splitBeforeAfterOutput.after = output.text();
            }

            return e;
        }
    };
});
