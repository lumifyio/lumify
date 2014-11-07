define(['promise-polyfill'], function() {
    if (typeof Promise.prototype.always !== 'function') {
        Promise.prototype.always = function(onEither) {
            return this.then(onEither, onEither);
        };
    }

    if (typeof Promise.prototype.require !== 'function') {
        Promise.require = function() {
            var deps = Array.prototype.slice.call(arguments, 0);

            return new Promise(function(fulfill, reject) {
                require(deps, fulfill, reject);
            });
        };
    }

    return Promise;
});
