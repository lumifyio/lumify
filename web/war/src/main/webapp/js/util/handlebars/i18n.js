define([
    'hbs/handlebars'
], function(Handlebars) {
    'use strict';

    Handlebars.registerHelper('i18n', function(str) {
        return i18n.apply(null, arguments);
    });

});
