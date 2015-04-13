define([], function() {
    'use strict';

    return withDataRequestHandler;

    function fixParameter(obj) {
        if (obj instanceof FileList) {
            return _.map(obj, function(o) {
                return o;
            });
        }

        return obj;
    }

    function withDataRequestHandler() {

        this.after('initialize', function() {
            this.on('dataRequest', this.handleDataRequest);
            this.on('dataRequestCancel', this.handleDataRequestCancel);
            this.lumifyData.readyForDataRequests = true;
            this.trigger('readyForDataRequests');
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

            if (data.parameters) {
                data.parameters = _.map(data.parameters, fixParameter);
            }
            if (data && data.service === 'config') {
                var l = {};
                if (typeof localStorage !== 'undefined') {
                    l.language = localStorage.getItem('language');
                    l.country = localStorage.getItem('country');
                    l.variant = localStorage.getItem('variant');
                    data.parameters.push(l);
                }
            }
            this.worker.postMessage({
                type: event.type,
                data: data
            });
        };

        this.dataRequestCompleted = function(message) {
            this.trigger(message.type, message);
        };

        this.dataRequestProgress = function(message) {
            this.trigger(message.type, message);
        };

    }
});
