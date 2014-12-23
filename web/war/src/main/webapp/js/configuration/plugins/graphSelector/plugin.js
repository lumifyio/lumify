define(['util/messages'], function(i18n) {
    'use strict';

    var selectors = [],
        byId = {},
        VISIBILITIES = [
            'selected',
            'none-selected',
            'always'
        ];

    return {
        selectors: selectors,

        selectorsById: byId,

        registerGraphSelector: function(identifier, selector, visibility) {
            if (!visibility) {
                visibility = 'always';
            }
            if (VISIBILITIES.indexOf(visibility) === -1) {
                throw new Error('selector "' + identifier + '": visibility only supports ' +
                VISIBILITIES.join(', '));
            }
            selector.identifier = identifier;
            selector.displayName = i18n('graph.selector.' + identifier + '.displayName');
            selector.visibility = visibility;
            selectors.push(selector);
            byId[identifier] = selector;
        }
    };
});
