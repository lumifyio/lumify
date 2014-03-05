
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
            userSelector: '.user',
            nameSelector: '.name',
            subtitleSelector: '.subtitle'
        });

        this.after('initialize', function() {
            var self = this;

            this.userDeferred = $.Deferred();
            this.workspaceDeferred = $.Deferred();

            $.when(this.userDeferred, this.workspaceDeferred).done(function() {
                self.$node.show();

                requestAnimationFrame(function() {
                    self.$node.addClass('visible');
                });
            })

            this.$node.hide().html(template({}));

            this.on(document, 'workspaceSaving', this.onWorkspaceSaving);
            this.on(document, 'workspaceSaved', this.onWorkspaceSaved);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
            this.on(document, 'currentUserChanged', this.onCurrentUserChanged)
            this.on(document, 'relationshipsLoaded', this.onRelationshipsLoaded)
        });

        this.onCurrentUserChanged = function(event, data) {
            this.userDeferred.resolve();
            this.updateUserTooltip(data);
        }

        this.onGraphPaddingUpdated = function(event, data) {
            this.$node.css('left', data.padding.l + MENUBAR_WIDTH);
        };

        this.setContent = function(title, isEditable, subtitle) {
            this.select('nameSelector').text(title);
            this.select('subtitleSelector').html(isEditable === false ? 'read only' : subtitle);
        };

        this.onWorkspaceLoaded = function(event, data) {
            this.workspaceDeferred.resolve();
            this.setContent(data.title, data.isEditable, 'no changes');
            clearTimeout(this.updateTimer);
            this.updateWorkspaceTooltip(data);
        };

        this.onRelationshipsLoaded = function(event, data) {
            this.updateWorkspaceTooltip(data);
        };

        this.onWorkspaceSaving = function(event, data) {
            this.select('subtitleSelector').text('saving...');
            clearTimeout(this.updateTimer);
            this.updateWorkspaceTooltip(data);
        };

        this.onWorkspaceSaved = function(event, data) {
            clearTimeout(this.updateTimer);
            this.lastSaved = formatters.date.utc(Date.now());

            if (data.title) {
                this.select('nameSelector').text(data.title);
            }

            this.updateWorkspaceTooltip(data);

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

        this.updateUserTooltip = function(data) {
            if (data && data.user) {
                this.select('userSelector').text(data.user.userName)
                    .tooltip('destroy')
                    .tooltip({
                        placement: 'right',
                        html: true,
                        title: '<span style="white-space:nowrap">Authorizations: ' + (data.user.authorizations.join(', ') || 'none') + '</span>',
                        trigger: 'hover',
                        delay: { show:500, hide:0 }
                    })
            }
        }

        this.updateWorkspaceTooltip = function(data) {
            if (data && data.data && data.data.vertices) {
                this.verticesCount = data.data.vertices.length;
            }
            if (this.verticesCount === 0) {
                this.edgesCount = 0;
            } else if (data.relationships) {
                this.edgesCount = data.relationships.length;
            } else {
                this.edgesCount = $('.cytoscape-container').cytoscape('get').edges().length;
            }

            var name = this.select('nameSelector'),
                tooltip = name.data('tooltip'),
                tip = tooltip && tooltip.tip(),
                text = 'Vertices: ' + formatters.number.pretty(this.verticesCount || 0) + 
                    ', Edges: ' + formatters.number.pretty(this.edgesCount || 0)

            if (tip && tip.is(':visible')) {
                tip.find('.tooltip-inner span').text(text);
            } else {
                name
                    .tooltip('destroy')
                    .tooltip({
                        placement: 'right',
                        html: true,
                        title: '<span style="white-space:nowrap">' + text + '</span>',
                        trigger: 'hover',
                        delay: { show:500, hide:0 }
                    });
            }

        }
    }
});
