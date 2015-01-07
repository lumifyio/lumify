define([
    'hbs/handlebars',
    'util/formatters'
], function(Handlebars, F) {
    'use strict';

    Handlebars.registerHelper('date', function(date) {
        return F.date.dateTimeString(date);
    });

    Handlebars.registerHelper('json', function(obj) {
        return JSON.stringify(obj);
    });

    Handlebars.registerHelper('dateonly', function(date) {
        return F.date.dateString(date);
    });

    Handlebars.registerHelper('relativedate', function(date) {
        return F.date.relativeToNow(F.date.utc(date));
    });

    Handlebars.registerHelper('prettyBytes', function(value) {
        return F.bytes.pretty(value);
    });

});
