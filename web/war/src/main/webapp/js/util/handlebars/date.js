define([
    'hbs/handlebars',
    'util/formatters'
], function(Handlebars, F) {
    'use strict';

    Handlebars.registerHelper('date', function(date) {
        return F.date.dateTimeString(date);
    });

    Handlebars.registerHelper('relativedate', function(date) {
        return F.date.relativeToNow(F.date.utc(date));
    });

});
