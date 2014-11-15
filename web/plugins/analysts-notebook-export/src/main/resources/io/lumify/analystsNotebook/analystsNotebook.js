require([
    'configuration/plugins/exportWorkspace/plugin',
    'util/messages',
    'util/formatters',
    'util/withDataRequest'
], function(p, i18n, F, withDataRequest) {
    var componentPath = 'io.lumify.analystsNotebook.AnalystsNotebook';

    withDataRequest.dataRequest('config', 'properties')
        .done(function(config) {
            var versions = {};

            Object.getOwnPropertyNames(config).forEach(function(key) {
                var labelMatch = key.match(/analystsNotebookExport\.menuOption\.(\w+)\.label/);
                if (labelMatch && labelMatch.length == 2) {
                    var label = config['analystsNotebookExport.menuOption.' + labelMatch[1] + '.label'],
                        value = config['analystsNotebookExport.menuOption.' + labelMatch[1] + '.value'];
                    if (label && value) {
                        versions[label] = value;
                    }
                }
            });

            Object.getOwnPropertyNames(versions).forEach(function(label) {
                p.registerWorkspaceExporter({
                    menuItem: i18n('analystsnotebook.menuitem.title', label),
                    componentPath: componentPath
                });
            });
        });

    define(componentPath, ['flight/lib/component'], function(defineComponent) {

        return defineComponent(AnalystsNotebook);

        function AnalystsNotebook() {

            this.defaultAttrs({
                buttonSelector: 'button'
            });

            this.after('initialize', function() {

                this.on('click', {
                    buttonSelector: this.onExport
                });

                this.$node.html(
                    '<button class="btn btn-primary">' + i18n('analystsnotebook.menuitem.button') + '</button>'
                );
            });

            this.onExport = function() {
                var timeZone = F.timezone.currentTimezone(),
                    params = {
                        workspaceId: this.attr.workspaceId
                    };

                if (timeZone && timeZone.name) {
                    params.timeZone = timeZone.name;
                }

                window.open('analysts-notebook/export?' + $.param(params));
                this.trigger('closePopover');
            }
        }
    })
});
