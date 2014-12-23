define(['util/messages'], function(i18n) {
    'use strict';

    var layouts = [];

    return {
        layouts: layouts,

        registerGraphLayout: function(name, layout) {
            layouts.push(layout);
            layout.identifier = name;
            layout.displayName = i18n('graph.layout.' + name + '.displayName');
        }
    };
});
