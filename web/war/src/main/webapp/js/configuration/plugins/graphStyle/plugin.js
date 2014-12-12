define(['jquery', 'underscore'], function($, _) {
    'use strict';

    var stylers = [];

    return {
        stylers: stylers,

        registerGraphStyler: function(styler) {
            stylers.push(styler);
        }
    };
});
