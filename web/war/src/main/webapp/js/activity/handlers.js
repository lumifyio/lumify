
define([], function() {
    return [
        {
            type: 'saveWorkspace',
            kind: 'eventWatcher',
            eventNames: ['workspaceSaving', 'workspaceSaved'],
            titleRenderer: function(el, datum) {
                el.textContent = datum.eventData.title;
            },
            autoDismiss: true
        },
        {
            type: 'findPath',
            kind: 'longRunningProcess',
            titleRenderer: function(el, process) {
                require(['data', 'util/formatters'], function(appData, F) {
                    $.when(
                        appData.getVertexTitle(process.sourceVertexId, process.workspaceId),
                        appData.getVertexTitle(process.destVertexId, process.workspaceId)
                    ).done(function(source, dest) {
                        el.textContent = source + ' → ' + dest;
                        $('<div>')
                            .css({ fontSize: '90%' })
                            .text(i18n('popovers.find_path.hops.option', process.hops))
                            .appendTo(el);
                    });
                });
            },
            finishedComponentPath: 'activity/builtin/findPath'
        }
    ];
})
