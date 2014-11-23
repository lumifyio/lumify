
define([], function() {
    return function abortPrevious(func) {
        var previousPromise;
        return function() {
            if (previousPromise && previousPromise.abort) {
                console.log('aborting', func)
                previousPromise.abort();
            }
            previousPromise = func.apply(null, Array.prototype.slice.call(arguments, 0));
            return previousPromise;
        }
    };
});
