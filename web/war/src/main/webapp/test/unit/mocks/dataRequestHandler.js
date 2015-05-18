define([], function() {
        var api = function() {
            var self = this,
                timeoutMilliseconds = 8000;

            this.responses = {};
            this.components = [];

            this.listen = function(component) {
                self.components.push(component);
                component.on(document, 'dataRequest', this.handleRequests);
            };

            this.handleRequests = function(event, data) {
                var success = true,
                    result = {},
                    service = data['service'],
                    method = data['method'];

                if(self.responses[service] && self.responses[service][method]) {
                    var response = self.responses[service][method];
                    success = response['success'];
                    result = response['result'];
                }

                $(document).trigger('dataRequestStarted', _.pick(data, 'requestId'));

                $(document).trigger('dataRequestCompleted', {
                  success: success,
                  result: result,
                  requestId: data.requestId,
                  originalRequest: _.pick(data, 'service', 'method')
                });
            };

            this.setResponse = function(service, method, success, result) {
                this.responses[service] = this.responses[service] || {};
                this.responses[service][method] = {};
                this.responses[service][method]['success'] = success;
                this.responses[service][method]['result'] = result;
            };

            this.stop = function() {
                for(var i = 0; i < self.components.length; i++) {
                    self.components[i].off(document, 'dataRequest');
                }
                self.components = [];
                //this.responses = {};
            };

            // Prevent unhandled data requests from breaking everything
            this.clearRequestTimeouts = function() {

                window.origSetTimeout = window.setTimeout;
                window.setTimeout = function(func, delay) {
                    var id = window.origSetTimeout(func, delay);

                    // Assume if it's the correct delay, it's for the data timeout
                    if(delay == timeoutMilliseconds) {
                        window.origSetTimeout(function() {
                            window.clearTimeout(id);
                        }, 7000);
                    }
                    return id;
                }
            }
        };

        return new api();
});
