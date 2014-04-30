
define([], function() {
    'use strict';

    return {

        createRange: function(node, str) {
            node.html(str);

            var range = window.document.createRange(),
                start = { node: null, offset: -1 },
                end = { node: null, offset: -1 };

            $.each(node[0].childNodes, function(i, node) {
                var textNode = node;
                while (textNode && textNode.nodeType !== 3) {
                    textNode.normalize();
                    textNode = node.childNodes[0];
                }
                if (textNode) {
                    var startIndex = textNode.textContent.indexOf('[');
                    if (startIndex >= 0) {
                        textNode.textContent = textNode.textContent.replace('[', '');
                        start.node = textNode;
                        start.offset = startIndex;
                    }

                    var endIndex = textNode.textContent.indexOf(']');
                    if (endIndex >= 0) {
                        textNode.textContent = textNode.textContent.replace(']', '');
                        end.node = textNode;
                        end.offset = endIndex;
                    }
                }
            });

            range.setStart(start.node, start.offset);
            range.setEnd(end.node, end.offset);

            return range;
        },

        selectRange: function(range) {
            setTimeout(function() {
                window.getSelection().addRange(range);
            }, 100);
        }
    };
});
