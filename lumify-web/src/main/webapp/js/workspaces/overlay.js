
define([
    'flight/lib/component',
    'tpl!./overlay',
    'util/formatters'
], function(defineComponent, template, formatters) {
    'use strict';

    var LAST_SAVED_UPDATE_FREQUENCY_SECONDS = 30;
    var MENUBAR_WIDTH = 30;

    return defineComponent(WorkspaceOverlay);

    function WorkspaceOverlay() {

        this.defaultAttrs({
            nameSelector: '.name',
            subtitleSelector: '.subtitle'
        });

        this.after('initialize', function() {
            this.$node.html(template({}));

            this.on(document, 'workspaceSaving', this.onWorkspaceSaving);
            this.on(document, 'workspaceSaved', this.onWorkspaceSaved);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
        });

        this.onGraphPaddingUpdated = function(event, data) {
            this.$node.css('left', data.padding.l + MENUBAR_WIDTH);
        };

        this.setContent = function(title, isEditable, subtitle) {
            this.select('nameSelector').text(title);
            this.select('subtitleSelector').html(isEditable === false ? 'read only' : subtitle);
        };

        this.onWorkspaceLoaded = function(event, data) {
            this.setContent(data.title, data.isEditable, 'no changes');
            clearTimeout(this.updateTimer);
        };

        this.onWorkspaceSaving = function(event, data) {
            this.select('subtitleSelector').text('saving...');
            clearTimeout(this.updateTimer);
        };

        this.onWorkspaceSaved = function(event, data) {
            clearTimeout(this.updateTimer);
            this.lastSaved = formatters.date.utc(Date.now());

            if (data.title) {
                this.select('nameSelector').text(data.title);
            }

            var prefix = 'last saved ',
                subtitle = this.select('subtitleSelector').text(prefix + 'moments ago'),
                setTimer = function() {
                    this.updateTimer = setTimeout(function () {

                        var time = formatters.date.relativeToNow(this.lastSaved);
                        subtitle.text(prefix + time);

                        setTimer();
                    }.bind(this), LAST_SAVED_UPDATE_FREQUENCY_SECONDS * 1000);
                }.bind(this);

            setTimer();
        };
    }
});
