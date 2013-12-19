
define([
    'flight/lib/component',
    'util/formatters',
    'tpl!./help',
    'tpl!./sections'
], function(
    defineComponent,
    formatters,
    helpTemplate,
    sectionsTemplate) {


    return defineComponent(Help);

    function prettyCommand(shortcut) {
        return formatters.string.shortcut(shortcut.character, shortcut);
    }

    function Help() {
        this.after('initialize', function() {
            this.allShortcuts = {};

            this.onRegisterKeyboardShortcuts = _.debounce(this.onRegisterKeyboardShortcuts.bind(this), 1000);

            this.on('escape', this.onEscape);
            this.on(document, 'toggleHelp', this.onDisplay);
            this.on(document, 'keyboardShortcutsRegistered', this.onKeyboardShortcutsRegistered);
            this.on(document, 'registerKeyboardShortcuts', this.onRegisterKeyboardShortcuts);

            this.$node.html(helpTemplate({}));

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: 'Help',
                shortcuts: {
                    'escape': { fire:'escape',     desc:'Close this help dialog' },
                    'shift-/': { fire:'toggleHelp', desc:'Toggle this help dialog' }
                }
            })
            this.trigger(document, 'requestKeyboardShortcuts');

        });

        this.onRegisterKeyboardShortcuts = function() {
            // Shortcuts updated, regenerate list from keyboard.js
            this.trigger(document, 'requestKeyboardShortcuts');
        };

        this.onKeyboardShortcutsRegistered = function(e, data) {
            this.$node.find('ul').html(
                sectionsTemplate({
                    shortcutsByScope:data,
                    prettyCommand: prettyCommand
                })
            );
        };

        this.onDisplay = function(e) {
            if (this.$node.is(':visible')) {
                this.$node.modal('hide');
            } else {
                this.$node.modal();
                _.defer(function() {
                    this.trigger('focusComponent');
                }.bind(this));
            }
        };

        this.onEscape = function(e) {
            this.$node.modal('hide');
            e.stopPropagation();
        };
    }
});
