define(['require'], function(require) {
    return function() {
        require(['../util/store'], function(store) {
            store.logStatistics();
        })
    }
})
