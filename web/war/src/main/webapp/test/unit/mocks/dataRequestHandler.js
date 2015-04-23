define([], function() {
        var api = function() {
            var self = this;

            this.responses = {};

            this.listen = function() {
                $(document).on('dataRequest', this.handleRequests);
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
                $(document).off('dataRequest');
                //this.responses = {};
            };
        };

        return new api();
});
