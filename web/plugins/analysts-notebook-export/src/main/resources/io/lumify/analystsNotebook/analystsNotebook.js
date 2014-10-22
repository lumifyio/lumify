require([
    'configuration/plugins/exportWorkspace/plugin',
    'util/messages'
], function(p, i18n) {
    var componentPath = 'io.lumify.analystsNotebook.AnalystsNotebook';

    p.registerWorkspaceExporter({
        menuItem: i18n('analystsnotebook.menuitem.title'),
        componentPath: componentPath
    })

    define(componentPath, ['flight/lib/component'], function(defineComponent) {

        return defineComponent(AnalystsNotebook);

        function AnalystsNotebook() {

            this.defaultAttrs({
                buttonSelector: 'button'
            })

            this.after('initialize', function() {

                this.on('click', {
                    buttonSelector: this.onExport
                })

                this.$node.html(
                    '<button class="btn btn-primary">' + i18n('analystsnotebook.menuitem.button') + '</button>'
                );
            });

            this.onExport = function() {
                window.open('export/analysts-notebook?' + $.param({
                    workspaceId: this.attr.workspaceId
                }))
                this.trigger('closePopover')
            }
        }
    })
})
