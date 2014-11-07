define([], function() {
    'use strict';
    return function(data) {
        var body = data.responseBody,
            data = JSON.parse(body);

        console.info('message', data);
        // TODO: propagate to main in terms of events
    }
});
