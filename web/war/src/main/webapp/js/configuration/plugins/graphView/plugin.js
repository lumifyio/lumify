define([], function() {
    'use strict';

    var i = 0,
        views = [];

    return {
        views: views,

        registerGraphView: function(viewComponentPath, viewClassName /*optional*/) {
            if (!viewComponentPath) {
                throw new Error('Component path is required');
            }

            if (!viewClassName) {
                viewClassName = 'graph-view-anon-' + (i++);
            }

            views.push({
                componentPath: viewComponentPath,
                className: viewClassName
            });
        }
    };
});
