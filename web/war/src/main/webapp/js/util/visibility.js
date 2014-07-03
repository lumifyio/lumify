
define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(Visibility);

    function Visibility() {

        this.after('initialize', function() {
            this.observeHidden();
        });

        this.isVisible = function() {
            return !document[this.hiddenProperty];
        };

        this.isHidden = function() {
            return !this.isVisible();
        };

        this.observeHidden = function() {
            var hidden, visibilityChange;
            if (typeof document.hidden !== 'undefined') { // Opera 12.10 and Firefox 18 and later support
                hidden = 'hidden';
                visibilityChange = 'visibilitychange';
            } else if (typeof document.mozHidden !== 'undefined') {
                hidden = 'mozHidden';
                visibilityChange = 'mozvisibilitychange';
            } else if (typeof document.msHidden !== 'undefined') {
                hidden = 'msHidden';
                visibilityChange = 'msvisibilitychange';
            } else if (typeof document.webkitHidden !== 'undefined') {
                hidden = 'webkitHidden';
                visibilityChange = 'webkitvisibilitychange';
            }

            this.hiddenProperty = hidden;
            this.hiddenEvent = visibilityChange;

            this.on(visibilityChange, this.onVisibilityChange);
        };

        this.onVisibilityChange = function(event) {
            this.trigger('window-visibility-change', {
                visible: this.isVisible(),
                hidden: this.isHidden()
            });
        };
    }
});
