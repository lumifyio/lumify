define([], function() {
    'use strict';
    return function(data) {
        var body = data.responseBody,
            json = JSON.parse(body);

        console.info('message', json);
        // TODO: propagate to main in terms of events
    }
});
