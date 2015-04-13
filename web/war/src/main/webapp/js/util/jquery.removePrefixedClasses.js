
define(['jquery'], function() {
    'use strict';

    $.fn.removePrefixedClasses = function(prefix) {

        return this.each(function() {
            var el = $(this),
                classes = el[0].className.split(/\s+/),
                prefixes = prefix.split(/\s+/),
                toRemove = [];

            classes.forEach(function(cls) {
                prefixes.forEach(function(prefix) {
                    if (cls.indexOf(prefix) === 0) {
                        toRemove.push(cls);
                    }
                });
            });
            el.removeClass(toRemove.join(' '));
        });
    };
});
