
define(function() {
    'use strict';
    return {
        load: function(name, req, load) { // , config
            req([name], function(result) {
                if (result &&
                    typeof result === 'object' &&
                    typeof result.then === 'function') {

                    var complete = result.done || result.then;
                    complete.call(result, function() {
                        load.apply(this, arguments);
                    });
                    result.catch(function() {
                        load.error.apply(this, arguments);
                    });
                } else {
                    load(result);
                }
            });
        }
    }
});
