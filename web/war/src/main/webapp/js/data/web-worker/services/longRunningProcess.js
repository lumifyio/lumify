
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    var api = {

        get: function(processId) {
            return ajax('GET', '/long-running-process', {
                longRunningProcessId: processId
            });
        }
    };

    return api;
});
