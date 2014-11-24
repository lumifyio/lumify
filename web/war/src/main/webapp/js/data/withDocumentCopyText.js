define([], function() {
    'use strict';

    return withDocumentCopyText;

    function withDocumentCopyText() {

        var copiedDocumentText,
            copiedDocumentTextStorageKey = 'SESSION_copiedDocumentText';

        this.after('initialize', function() {
            this.on('copydocumenttext', this.onDocumentTextCopy);
            this.updateCopiedDocumentText();
        });

        this.onDocumentTextCopy = function(event, data) {
            copiedDocumentText = data;
            if ('localStorage' in window) {
                try {
                    localStorage.setItem(copiedDocumentTextStorageKey, JSON.stringify(data));
                } catch(e) {
                    console.error('Unable to set localStorage item');
                }
            }
            this.updateCopiedDocumentText();
        };

        this.updateCopiedDocumentText = function() {
            var text;
            if ('localStorage' in window) {
                text = localStorage.getItem(copiedDocumentTextStorageKey);
                if (text) {
                    text = JSON.parse(text);
                }
            }

            if (text === null || _.isUndefined(text)) {
                text = copiedDocumentText;
            }

            this.setPublicApi('copiedDocumentText', text);
        }
    }
});
