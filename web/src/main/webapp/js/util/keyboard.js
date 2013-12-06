

define(['flight/lib/component'],
function(defineComponent) {
    'use strict';

    var SYSTEM_WIDE_CODES = {
            13: 'return',

            17: 'controlKey',
            18: 'altKey',

            27: 'escape',

            37: 'left',
            38: 'up',
            39: 'right',
            40: 'down',

            46: 'delete',
            8: 'delete',

            191: 'forwardSlash',

            'CTRL-A': 'select-all'
        }, 
        shouldFilter = function(e) {
            return $(e.target).is('input,select,textarea:not(.clipboardManager)');
        };

    return defineComponent(Keyboard);


    function Keyboard() {
        this.after('initialize', function() {
            this.codes = SYSTEM_WIDE_CODES;

            this.fireEventUp = _.debounce(this.fireEvent.bind(this), 100);
            this.fireEvent = _.debounce(this.fireEvent.bind(this), 100, true);

            this.on('keydown', this.onKeyDown);
            this.on('keyup', this.onKeyUp);
            this.on('focusLostByClipboard', this.onFocusLostByClipboard);
            this.on('addKeyboardShortcuts', this.onAddKeyboardShortcuts);
        });

        this.onAddKeyboardShortcuts = function(e, data) {
            var self = this;

            if (data && data.shortcuts) {
                Object.keys(data.shortcuts).forEach(function(shortcut) {
                    self.codes[shortcut] = data.shortcuts[shortcuts];
                });
            }
        };

        this.onFocusLostByClipboard = function(e) {
            this.triggerElement = e.target;
        };

        this.codeKeyForEvent = function(event) {
            var w = event.which,
                s = String.fromCharCode(w);

            if (event.metaKey || event.ctrlKey) {
                return this.codes['CTRL-' + w] || this.codes['CTRL-' + s] || this.codes[w];
            }

            return this.codes[w] || this.codes[s];
        };

        this.onKeyUp = function(e) {
            if (shouldFilter(e)) return;

            var eventToFire = this.codeKeyForEvent(e);

            if (eventToFire) {
                e.preventDefault();
                this.fireEventUp(this.triggerElement || this.node, eventToFire + 'Up', _.pick(e, 'metaKey', 'ctrlKey', 'shiftKey'));
            }
        };

        this.onKeyDown = function(e) {
            if (shouldFilter(e)) return;

            var eventToFire = this.codeKeyForEvent(e);

            if (eventToFire) {
                e.preventDefault();
                this.fireEvent(this.triggerElement || this.node, eventToFire, _.pick(e, 'metaKey', 'ctrlKey', 'shiftKey'));
            }
        }

        this.fireEvent = function(node, name, data) {
            this.trigger(node, name, data);
        }
    }
});
