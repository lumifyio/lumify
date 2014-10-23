
define([], function() {
    return {
        findPath: function(el, process) {
            require(['data', 'util/formatters'], function(appData, F) {
                $.when(
                    appData.getVertexTitle(process.sourceVertexId),
                    appData.getVertexTitle(process.destVertexId)
                ).done(function(source, dest) {
                    el.textContent = source + ' → ' + dest + ' (' + F.string.plural(process.hops, 'hop') + ')';
                });
            });
        }
    };
})
