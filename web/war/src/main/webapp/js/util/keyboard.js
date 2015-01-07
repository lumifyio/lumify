
define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(Keyboard);

    function shouldFilter(e) {
        return $(e.target).is('input,select,textarea:not(.clipboardManager)');
    }

    function Keyboard() {
        this.after('initialize', function() {
            this.shortcutsByScope = {};
            this.shortcuts = {};
            this.focusElementStack = [];

            this.fireEventMetas = this.fireEvent;

            this.fireEventUp = _.debounce(this.fireEvent.bind(this), 100);
            this.fireEvent = _.debounce(this.fireEvent.bind(this), 100, true);

            this.on('keydown', this.onKeyDown);
            this.on('keyup', this.onKeyUp);
            this.on('click', this.onClick);
            this.on('mousemove', this.onMouseMove);
            this.on('didToggleDisplay', this.onToggleDisplay);
            this.on('focusLostByClipboard', this.onFocusLostByClipboard);
            this.on('focusComponent', this.onFocus);

            this.on(document, 'requestKeyboardShortcuts', this.onRequestKeyboardShortcuts);
            this.on(document, 'registerKeyboardShortcuts', this.onRegisterKeyboardShortcuts);
        });

        this.onRequestKeyboardShortcuts = function() {
            this.trigger('keyboardShortcutsRegistered', this.shortcutsByScope);
        };

        this.onToggleDisplay = function(e, data) {
            if (!data.visible) return;
            if (data.name === 'map') {
                this.pushToStackIfNotLast($('.map-pane').get(0));
            } else if (data.name === 'graph') {
                this.pushToStackIfNotLast($('.graph-pane:visible').get(0));
            }
        };

        this.onRegisterKeyboardShortcuts = function(e, data) {
            var self = this,
                scopes = ['Global'],
                shortcuts = this.shortcuts,
                shortcutsByScope = this.shortcutsByScope;

            if (data.scope) {
                if (_.isArray(data.scope)) {
                    scopes = data.scope;
                } else scopes = [data.scope];
            }

            require(['util/formatters'], function(F) {
                scopes.forEach(function(scope) {
                    Object.keys(data.shortcuts).forEach(function(key) {
                        var shortcut = $.extend({}, data.shortcuts[key], F.object.shortcut(key));

                        if (!shortcutsByScope[scope]) shortcutsByScope[scope] = {};
                        shortcuts[shortcut.forEventLookup] = shortcutsByScope[scope][shortcut.normalized] = shortcut;
                    });
                });
            })
        };

        this.onFocus = function(e) {
            this.pushToStackIfNotLast(e.target);
        };

        this.onClick = function(e) {
            this.pushToStackIfNotLast(e.target);
        };

        this.onFocusLostByClipboard = function(e) {
            var $target = $(e.target);

            if ($target.is('.clipboardManager')) return;
            if ($target.closest('.menubar-pane').length) return;

            this.pushToStackIfNotLast(e.target);
        };

        this.shortcutForEvent = function(event) {
            var w = event.which,
                keys = {
                    16: 'shiftKey',
                    17: 'controlKey',
                    18: 'altKey',
                    38: 'up',
                    40: 'down',
                    91: 'metaKey',
                    93: 'metaKey'
                };

            if (keys[w]) {
                return { preventDefault: false, fire: keys[w] };
            }
            if (event.metaKey || event.ctrlKey) {
                return this.shortcuts['CTRL-' + w] || this.shortcuts['META-' + w];
            }
            if (event.altKey) {

                // ALT+N keyCode is 229 instead of 78
                if (w === 229) {
                    return this.shortcuts['ALT-' + w] ||
                        this.shortcuts['ALT-' + 78];
                }

                return this.shortcuts['ALT-' + w];
            }
            if (event.shiftKey) {
                return this.shortcuts['SHIFT-' + w];
            }

            return this.shortcuts[w];
        };

        this.onKeyUp = function(e) {
            if (shouldFilter(e)) return;

            var shortcut = this.shortcutForEvent(e);

            if (shortcut) {
                var f = this.fireEventUp;
                if (shortcut.preventDefault !== false) {
                    e.preventDefault();
                    f = this.fireEventMetas;
                }

                f.call(this, shortcut.fire + 'Up', _.pick(e, 'metaKey', 'ctrlKey', 'shiftKey'));
            }
        };

        this.onKeyDown = function(e) {
            if (shouldFilter(e)) return;

            var shortcut = this.shortcutForEvent(e);

            if (shortcut) {
                var f = this.fireEvent;
                if (shortcut.preventDefault !== false) {
                    e.preventDefault();
                    f = this.fireEventMetas;
                }

                f.call(this, shortcut.fire, _.pick(e, 'metaKey', 'ctrlKey', 'shiftKey'));
            }
        }

        this.onMouseMove = function(e) {
            window.lastMousePositionX = this.mousePageX = e.pageX || 0;
            window.lastMousePositionY = this.mousePageY = e.pageY || 0;
        }

        this.pushToStackIfNotLast = function(el) {
            if (!this.focusElementStack.length || this.focusElementStack[this.focusElementStack.length - 1] !== el) {
                this.focusElementStack.push(el);
            }
        };

        this.getTriggerElement = function() {
            var triggerElement;

            while (this.focusElementStack.length && !triggerElement) {
                var lastElement = _.last(this.focusElementStack),
                isVisible = $(lastElement).is(':visible');

                if (isVisible) {
                    triggerElement = lastElement;
                } else {
                    this.focusElementStack.pop();
                }
            }

            return triggerElement || this.$node;
        };

        this.fireEvent = function(name, data) {
            var te = this.getTriggerElement();
            data.pageX = this.mousePageX;
            data.pageY = this.mousePageY;
            this.trigger(te, name, data);
        }
    }
});
