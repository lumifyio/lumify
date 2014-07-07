define([], function() {
    'use strict';

    return withPendingChanges;

    /**
     * Mixin that monitors for data being sent to the server and
     * warns the user if they are trying to leave the page
     * before all of the requests have completed.
     */
    function withPendingChanges() {
        var pendingSaveWorkspace = false,
            pendingAjaxWrites = 0;

        this.after('initialize', function() {
            var onSaveWorkspaceInternalOriginal = this.onSaveWorkspaceInternal;
            this.onSaveWorkspaceInternal = function() {
              pendingSaveWorkspace = true;
              onSaveWorkspaceInternalOriginal.apply(self, arguments);
            };

            this.on('workspaceSaving.pendingSave', function() {
              pendingSaveWorkspace = false;
            });

            $(document).trigger('registerBeforeUnloadHandler', {
                priority: 1,
                scope: this,
                fn: this.onBeforeUnload
            });

            $(document).on('ajaxSend', this.onAjaxSend);
        });

        this.pendingChangesPresent = function() {
            return pendingSaveWorkspace || pendingAjaxWrites > 0;
        };

        this.onBeforeUnload = function() {
            if (this.pendingChangesPresent()) {
                return 'Some changes have not yet finished saving.';
            }
        };

        this.onAjaxSend = function(event, jqXHR, ajaxOptions) {
            if (ajaxOptions.type !== 'GET') {
                pendingAjaxWrites = pendingAjaxWrites + 1;
                jqXHR.always(function() {
                    pendingAjaxWrites = pendingAjaxWrites - 1;
                });
            }
        };
    }
});
