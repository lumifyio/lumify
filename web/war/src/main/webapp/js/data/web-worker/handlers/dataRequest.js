define([], function() {
    'use strict';

    return function(message) {
        require(['data/web-worker/services/' + message.data.service],
        function(Service) {
            if (!(message.data.method in Service)) {
                throw new Error('Service: ' + message.data.service + ' is missing method: ' + message.data.method);
            }

            Service[message.data.method].apply(undefined, message.data.parameters || [])
                .progress(function(p) {
                    dispatchMain('dataRequestProgress', {
                        progress: p,
                        requestId: message.data.requestId,
                        originalRequest: _.pick(message.data, 'service', 'method')
                    });
                })
                .then(function(result) {
                    dispatchMain('dataRequestCompleted', {
                        success: true,
                        result: result,
                        requestId: message.data.requestId,
                        originalRequest: _.pick(message.data, 'service', 'method')
                    });
                })
                .catch(function(error) {
                    if (error && error instanceof Error) {
                        error = error.message;
                    }
                    dispatchMain('dataRequestCompleted', {
                        success: false,
                        error: error,
                        requestId: message.data.requestId,
                        originalRequest: _.pick(message.data, 'service', 'method')
                    })
                })
        });
    };
})
