define(['util/promise'], function(Promise) {

    withDataRequest.dataRequest = function(service, method) {
        return new Promise(function(fulfill, reject) {
            require(['service/' + service], function(Service) {
                Service[method]().then(function(result) {
                    fulfill(result);
                });
            });
        });
    }

    return withDataRequest;

    function withDataRequest() {
        this.dataRequest = withDataRequest.dataRequest;
    }
})