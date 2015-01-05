define([], function() {
    'use strict';

    var optionsById = {},
        bundled = [
            { identifier: 'toggleEdgeLabel', optionComponentPath: 'graph/options/edgeLabel' }
        ],
        api = {
            options: [],

            registerGraphOption: function(option) {
                if ('identifier' in option) {
                    if ('optionComponentPath' in option) {
                        optionsById[option.identifier] = option;
                        api.options = _.values(optionsById);
                    } else throw new Error('optionComponentPath required in option', option);
                } else throw new Error('identifier required in option', option);
            }
        };

    bundled.forEach(function(option) {
        api.registerGraphOption(option);
    });

    return api;
});
