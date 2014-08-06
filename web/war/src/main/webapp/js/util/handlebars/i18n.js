define([
    'hbs/handlebars',
    'util/messages'
], function(Handlebars, i18n) {
    'use strict';

    Handlebars.registerHelper('i18n', function(str) {
        return i18n.apply(null, arguments);
    });

});
