define([
    'util/keyboard',
], function(Keyboard) {
    'use strict';

    return withKeyboardRegistration;

    function withKeyboardRegistration() {

        this.after('setupMessages', function() {
            Keyboard.attachTo(this.node);

            this.trigger('registerKeyboardShortcuts', {
                scope: ['graph.help.scope', 'map.help.scope'].map(i18n),
                shortcuts: {
                    'meta-a': { fire: 'selectAll', desc: i18n('lumify.help.select_all') },
                    'delete': {
                        fire: 'deleteSelected',
                        desc: i18n('lumify.help.delete')
                    },
                }
            });

            this.trigger('registerKeyboardShortcuts', {
                scope: ['graph.help.scope', 'map.help.scope', 'search.help.scope'].map(i18n),
                shortcuts: {
                    'alt-r': { fire: 'addRelatedItems', desc: i18n('lumify.help.add_related') },
                    'alt-t': { fire: 'searchTitle', desc: i18n('lumify.help.search_title') },
                    'alt-s': { fire: 'searchRelated', desc: i18n('lumify.help.search_related') },
                }
            });
        });

    }
});
