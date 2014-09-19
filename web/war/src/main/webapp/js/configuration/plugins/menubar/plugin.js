define(['jquery', 'underscore'], function($, _) {
    'use strict';

    var items = [];

    return {
        items: items,

        registerMenubarItem: function(item) {
            var required = 'title identifier action icon'.split(' '),
                given = _.keys(item),
                intersection = _.intersection(required, given);

            if (intersection.length < required.length) {
                throw new Error('Menubar extensions require: ' + required);
            }

            items.push(item);
        }
    };
});
