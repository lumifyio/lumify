
define(['util/promise'], function() {
    'use strict';

    var nextResponse = null,
        ajax = function(action, url) {
            var r = nextResponse;
            nextResponse = null;
            return Promise.resolve(r);
        };

    ajax.setNextResponse = function(response) {
        nextResponse = response;
    };

    return ajax;
});
