define([], function() {
    'use strict';

    return withPublicApi;

    function withPublicApi() {

        this.before('initialize', function() {
            this.lumifyData = {};
            window.lumifyData = this.lumifyData;
        });

        this.setPublicApi = function(key, obj, options) {
            options = _.extend({
                    onlyIfNull: false
                }, options || {});

            if (options.onlyIfNull && (key in this.lumifyData)) {
                return;
            }

            if (typeof obj === 'undefined') {
                delete this.lumifyData[key];
            } else {
                this.lumifyData[key] = obj;
                this.trigger(key + 'LumifyDataUpdated', {
                    key: key,
                    object: obj
                });
            }

            this.worker.postMessage({
                type: 'publicApi',
                key: key,
                obj: obj
            });
        }
    }
});
