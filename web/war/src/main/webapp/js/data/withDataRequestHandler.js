define([], function() {
    'use strict';

    return withDataRequestHandler;

    function withDataRequestHandler() {

        this.after('initialize', function() {
            this.on('dataRequest', this.handleDataRequest);
            this.on('dataRequestCancel', this.handleDataRequestCancel);
            this.trigger('readyForDataRequests');
            this.lumifyData.readyForDataRequests = true;
        });

        this.handleDataRequestCancel = function(event, data) {
            // TODO
            //this.worker.postMessage({
                //type: 'cancelDataRequest',
                //data: data
            //});
        };

        this.handleDataRequest = function(event, data) {
            var self = this;

            this.trigger('dataRequestStarted', _.pick(data, 'requestId'));

            this.worker.postMessage({
                type: event.type,
                data: data
            });
        };

        this.dataRequestCompleted = function(message) {
            this.trigger(message.type, message);
        };

    }
});
