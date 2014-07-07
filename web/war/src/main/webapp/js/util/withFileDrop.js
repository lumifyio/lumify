define([
    'util/privileges'
], function(Privileges) {
    'use strict';

    return withFileDrop;

    function withFileDrop() {

        this.after('initialize', function() {
            var self = this;

            if (!this.handleFilesDropped) {
                return console.warn('Implement handleFilesDropped');
            }

            this.node.ondragover = function() {
                if (Privileges.missingEDIT) {
                    return;
                }

                $(this).addClass('file-hover'); return false;
            };
            this.node.ondragenter = function() {
                if (Privileges.missingEDIT) {
                    return;
                }

                $(this).addClass('file-hover'); return false;
            };
            this.node.ondragleave = function() {
                if (Privileges.missingEDIT) {
                    return;
                }

                $(this).removeClass('file-hover'); return false;
            };
            this.node.ondrop = function(e) {
                if (Privileges.missingEDIT) {
                    return;
                }

                if (e.dataTransfer && e.dataTransfer.files) {
                    e.preventDefault();
                    e.stopPropagation();

                    if (self.$node.hasClass('uploading')) return;
                    if (e.dataTransfer.files.length === 0) return;

                    self.handleFilesDropped(e.dataTransfer.files, e);
                }
            };
        });
    }
});
