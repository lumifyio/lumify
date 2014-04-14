define([
    'hbs/handlebars',
    'util/formatters'
], function(Handlebars, formatters) {
    'use strict';

    Handlebars.registerHelper('date', function(date) {
        return formatters.date.dateTimeString(date);
    });

    Handlebars.registerHelper('relativedate', function(date) {
        return formatters.date.relativeToNow(new Date(date));
    });

});
