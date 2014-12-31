define(['util/messages'], function(i18n) {
    'use strict';

    var pagesById = {},
        api = {
            pages: [],

            registerUserAccountPage: function(page) {
                if ('identifier' in page) {
                    if ('pageComponentPath' in page) {
                        page.displayName = i18n('useraccount.page.' + page.identifier + '.displayName');
                        pagesById[page.identifier] = page;
                        api.pages = _.sortBy(_.values(pagesById), 'displayName');
                    } else throw new Error('pageComponentPath required in page', page);
                } else throw new Error('identifier required in page', page);
            }
        },
        bundled = [
            { identifier: 'access', pageComponentPath: 'workspaces/userAccount/bundled/access/access' }
        ];

    bundled.forEach(function(page) {
        api.registerUserAccountPage(page);
    });

    return api;
});
