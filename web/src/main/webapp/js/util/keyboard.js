

define(['flight/lib/component'],
function(defineComponent) {
    'use strict';

    var CODES = {
            27: 'escape',
            13: 'return',
            38: 'up',
            40: 'down',
            37: 'left',
            39: 'right',
            8: 'delete',
            46: 'delete',
            191: 'forwardSlash',

            'CTRL-65': 'select-all'
        }, 
        shouldFilter = function(e) {
            return $(e.target).is('input,select,textarea:not(.clipboardManager)');
        };

    return defineComponent(Keyboard);


    function Keyboard() {
        this.after('initialize', function() {

            this.fireEvent = _.debounce(this.fireEvent.bind(this), 100);

            this.on('keydown', this.onKeyDown);
            this.on('focusLostByClipboard', this.onFocusLostByClipboard);

        });

        this.onFocusLostByClipboard = function(e) {
            this.triggerElement = e.target;
        };

        this.onKeyDown = function(e) {
            if (shouldFilter(e)) return;

            var eventToFire = CODES[e.which];
            if (e.metaKey || e.ctrlKey) {
                eventToFire = CODES['CTRL-' + e.which];
            }

            if (eventToFire) {
                e.preventDefault();
                this.fireEvent(this.triggerElement || this.node, eventToFire, _.pick(e, 'metaKey', 'ctrlKey', 'shiftKey'));
            }
        }

        this.fireEvent = function(node, name, data) {
            console.log('triggering', name)
            this.trigger(node, name, data);
        }
    }
});
