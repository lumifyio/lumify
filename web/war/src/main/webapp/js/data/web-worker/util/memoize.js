
define([], function() {
    // Maybe expand this to expire caches?
    return function memoize(func, hashFunction) {
        return _.memoize(func, hashFunction);
    };
});
