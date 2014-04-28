/**
 * Within Scrollable jQuery Plugin
 *
 * @description Select elements within the visible scroll area of parent
 * @author      Jason Harwig
 * @version     0.0.1
 * @date        2013-07-10
 */
define(['jquery'], function() {

    'use strict';

    $.fn.withinScrollable = function(scrollable) {
        var elems = [],
            width = scrollable.outerWidth(true),
            height = scrollable.outerHeight(true),
            top = scrollable.scrollTop(),
            left = scrollable.scrollLeft();

        this.each(function() {
            if (withinScrollable(this, top, left, width, height)) {
                elems.push(this);
            }
        });
        return $(elems);
    };

    function withinScrollable(element, top, left, width, height) {
        return (element.offsetTop + element.offsetHeight) >= top && element.offsetTop <= top + height;
    }

});
