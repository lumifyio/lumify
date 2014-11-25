define([], function() {

    // Override in plugin to define caching behavior
    //
    // Note:
    //
    // This runs in worker thread with no access to DOM, events, etc.
    // Don't requirejs more than you need to answer

    return {
        shouldCacheObjectsAtUrl: function(url) {
            return true;
        },
        shouldCacheVertexAtUrl: function(vertex, url) {
            return true;
        },
        shouldCacheEdgeAtUrl: function(edge, url) {
            return true;
        }
    }
})
