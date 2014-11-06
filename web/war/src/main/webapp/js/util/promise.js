define(['promise-polyfill'], function() {
    if (typeof Promise.prototype.always !== 'function') {
        Promise.prototype.always = function(onEither) {
            return this.then(onEither, onEither);
        }
    }

    return Promise;
});
