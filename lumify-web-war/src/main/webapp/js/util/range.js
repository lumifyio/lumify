
define([], function() {
    return {
        expandRangeByWords: function(range, numberWords, splitBeforeAfterOutput) {
            var expanded = range.cloneRange(),
                start = expanded.startContainer,
                startOffset = expanded.startOffset,
                end = expanded.endContainer,
                endOffset = expanded.endOffset,
                beforeRange = start.textContent.substring(0, startOffset),
                afterRange = end.textContent.substring(endOffset),
                wordRegex = /[.\s;-]/,
                wordsBefore = $.trim(beforeRange).split('').reverse().join('') + ' ',
                wordsAfter = $.trim(afterRange) + ' ',
                index = 0, 
                foundIndex = 0;

            for (var i = 0, previousIndex = 0; i < numberWords; i++) {
                foundIndex = wordsBefore.indexOf(' ', previousIndex);
                if (~foundIndex) {
                    index = foundIndex;
                    previousIndex = foundIndex + 1;
                }
            }

            expanded.setStart(start, startOffset - index - 1);
            if (splitBeforeAfterOutput) {
                splitBeforeAfterOutput.before = wordsBefore.substring(0, index).split('').reverse().join('');
            }

            index = 0;
            for (i = 0, previousIndex = 0; i < numberWords; i++) {
                foundIndex = wordsAfter.indexOf(' ', previousIndex);
                if (~foundIndex) {
                    index = foundIndex;
                    previousIndex = foundIndex + 1;
                }
            }

            expanded.setEnd(end, endOffset + index + 1);
            if (splitBeforeAfterOutput) {
                splitBeforeAfterOutput.after = wordsAfter.substring(0, index);
            }

            return expanded;
        }
    };
});
