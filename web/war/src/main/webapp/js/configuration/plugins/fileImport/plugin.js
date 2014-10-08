define([], function() {
    'use strict';

    var fileHandlers = {};

    return {
        fileHandlers: fileHandlers,

        registerMimeTypeFileImportHandler: function(mimeType, handler) {
            if (!mimeType || !handler) {
                throw new Error('MimeType and handler required');
            }

            fileHandlers[mimeType] = handler;
        }
    };
});
