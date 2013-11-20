
define([

    './connection',
    './findPath'

    // Add context menu mixins here...

], function() {
    'use strict';

    var args = $.makeArray(arguments);

    return withContextMenu;

    function withContextMenu() {
        var self = this;

        args.forEach(function(mixin) {
            mixin.call(self);
        });
    }
});
