
define([], function() {
    return function abortPrevious(func) {
        var previousPromise;
        return function() {
            if (previousPromise && previousPromise.abort) {
                previousPromise.abort();
            }
            previousPromise = func.apply(null, Array.prototype.slice.call(arguments, 0));
            return previousPromise;
        }
    };
});
