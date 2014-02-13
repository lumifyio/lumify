
define([
    'rangy',
    'rangy-text'
], function(
    rangy,
    rangyText
) {

    if (!rangy.initialized) rangy.init();

    return {
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
