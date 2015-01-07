require([
    'configuration/plugins/exportWorkspace/plugin',
    'util/messages',
    'util/formatters',
    'util/withDataRequest'
], function (p, i18n, F, withDataRequest) {
    var componentPath = 'io.lumify.analystsNotebook.AnalystsNotebook';
    var versions = {};

    withDataRequest.dataRequest('config', 'properties')
        .done(function (config) {

            Object.getOwnPropertyNames(config).forEach(function (key) {
                var labelMatch = key.match(/analystsNotebookExport\.menuOption\.(\w+)\.label/);
                if (labelMatch && labelMatch.length == 2) {
                    var label = config['analystsNotebookExport.menuOption.' + labelMatch[1] + '.label'],
                        value = config['analystsNotebookExport.menuOption.' + labelMatch[1] + '.value'];
                    if (label && value) {
                        versions[label] = value;
                    }
                }
            });

            p.registerWorkspaceExporter({
                menuItem: i18n('analystsnotebook.menuitem.title'),
                componentPath: componentPath
            });

            define(componentPath, ['flight/lib/component'], function (defineComponent) {

                return defineComponent(AnalystsNotebook);

                function AnalystsNotebook() {

                    this.defaultAttrs({
                        dropdownSelector: 'select',
                        buttonSelector: 'button'
                    });

                    this.after('initialize', function () {
                        var self = this;
                        this.on('click', {
                            buttonSelector: this.onExport
                        });

                        this.$node.html('<select class="analystsNotebook"></select>');
                        Object.getOwnPropertyNames(versions).forEach(function (label) {
                            self.$node.find('select').append(new Option('Version ' + label, versions[label]));
                        });

                        this.$node.append(
                            '<button class="btn btn-primary">' + i18n('analystsnotebook.menuitem.button') + '</button>'
                        );
                    });

                    this.onExport = function () {
                        var timeZone = F.timezone.currentTimezone(),
                            params = {
                                version: this.$node.find('.analystsNotebook').val(),
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
});
