
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    var api = {

        get: function(processId) {
            return ajax('GET', '/long-running-process', {
                longRunningProcessId: processId
            });
        },

        cancel: function(processId) {
            return ajax('POST', '/long-running-process/cancel', {
                longRunningProcessId: processId
            });
        },

        'delete': function(processId) {
            return ajax('DELETE', '/long-running-process', {
                longRunningProcessId: processId
            });
        }
    };

    return api;
});
