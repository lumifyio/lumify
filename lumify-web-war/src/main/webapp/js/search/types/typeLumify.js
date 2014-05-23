define([
    'flight/lib/component',
    './withSearch'
], function(
    defineComponent,
    withSearch) {
    'use strict';

    return defineComponent(SearchTypeLumify, withSearch);

    function SearchTypeLumify() {

        this.after('initialize', function() {
            this.on('querysubmit', this.onQuerySubmit);
        });

        this.onQuerySubmit = function(event, data) {
            var self = this;

            if (this.currentRequest) {
                this.currentRequest.cancel();
                this.currentRequest = null;
            }

            this.trigger('searchRequestBegan');
            this.currentRequest = this.serviceRequest('vertex', 'graphVertexSearch', data.value, [])
                .fail(function(error) {
                    self.trigger('searchRequestCompleted', { success: false, error: error });
                })
                .done(function(result) {
                    self.trigger('searchRequestCompleted', { success: true, result: result });
                });
        };

    }
});
