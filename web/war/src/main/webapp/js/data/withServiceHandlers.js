define([], function() {
    'use strict';

    var serviceCache = {};

    return withServiceHandlers;

    function withServiceHandlers() {

        this.after('initialize', function() {
            this.on(document, 'serviceRequest', this.handleServiceRequest);
            this.on(document, 'serviceRequestCancel', this.handleServiceRequestCancel);
        });

        this.handleServiceRequestCancel = function(event, data) {
            // TODO
        };

        this.handleServiceRequest = function(event, data) {
            var self = this;

            this.trigger('serviceRequestStarted', _.pick(data, 'requestId'));

            this.getService(data.service)
                .then(this.invokeService.bind(this, data))
                .fail(function(xhr, status, error) {
                    self.trigger('serviceRequestCompleted', {
                        success: false,
                        error: error,
                        requestId: data.requestId
                    });
                })
                .done(function(result) {
                    self.trigger('serviceRequestCompleted', {
                        success: true,
                        result: result,
                        requestId: data.requestId
                    });
                })
        };

        this.getService = function(serviceName) {
            if (serviceCache[serviceName]) {
                return serviceCache[serviceName];
            }

            var d = $.Deferred();
            require(['service/' + serviceName], function(Service) {
                d.resolve(new Service());
            });
            return (serviceCache[serviceName] = d.promise());
        }

        this.invokeService = function(options, service) {
            if (!service[options.method]) {
                return $.Deferred().reject(null, null, options.method + ' does not exist');
            }
            return service[options.method].apply(service, options.parameters)
        }

    }
});
