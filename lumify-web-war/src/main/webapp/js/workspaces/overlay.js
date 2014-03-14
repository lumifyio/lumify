
define([
    'flight/lib/component',
    'tpl!./overlay',
    'util/formatters',
    'service/workspace',
    'data'
], function(defineComponent, template, formatters, WorkspaceService, appData) {
    'use strict';

    var LAST_SAVED_UPDATE_FREQUENCY_SECONDS = 30;
    var MENUBAR_WIDTH = 30;
    var UPDATE_WORKSPACE_DIFF_SECONDS = 3;

    return defineComponent(WorkspaceOverlay);

    function WorkspaceOverlay() {

        var workspaceService = new WorkspaceService();

        this.defaultAttrs({
            userSelector: '.user',
            nameSelector: '.name',
            subtitleSelector: '.subtitle'
        });

        this.after('initialize', function() {
            var self = this;

            requestAnimationFrame(function() {
                MENUBAR_WIDTH = $('.menubar-pane').width();
            })

            this.userDeferred = $.Deferred();
            this.workspaceDeferred = $.Deferred();
            this.updateDiffBadge = _.throttle(this.updateDiffBadge.bind(this), UPDATE_WORKSPACE_DIFF_SECONDS * 1000)

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
            this.on(document, 'switchWorkspace', this.onSwitchWorkspace);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
            this.on(document, 'currentUserChanged', this.onCurrentUserChanged)
            this.on(document, 'relationshipsLoaded', this.onRelationshipsLoaded)
            this.on(document, 'ajaxComplete', this.onAjaxComplete);
        });

        this.onAjaxComplete = function(event, xhr, settings) {
            // Automatically call diff after every POST
            if (/post/i.test(settings.type)) {
                this.updateDiffBadge();
            }
        };

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

        this.onSwitchWorkspace = function() {
            this.$node.find('.badge').popover('destroy').remove();
        };

        this.onWorkspaceLoaded = function(event, data) {
            this.workspaceDeferred.resolve();
            this.setContent(data.title, data.isEditable, 'no changes');
            clearTimeout(this.updateTimer);
            this.updateWorkspaceTooltip(data);
            this.updateDiffBadge();
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

        this.onDiffBadgeMouse = function(event) {
            this.trigger(
                event.type === 'mouseenter' ?  'focusVertices' : 'defocusVertices',
                { vertexIds: this.currentDiffIds || [] }
            );
        };

        this.updateDiffBadge = function() {
            var self = this,
                node = this.select('nameSelector'),
                badge = this.$node.find('.badge');
                
            if (!badge.length) {
                badge = $('<span class="badge"></span>')
                    .insertAfter(node)
                    .on('mouseenter mouseleave', this.onDiffBadgeMouse.bind(this))
            }

            workspaceService.diff(appData.workspaceId)
                .fail(function() {
                    badge.removePrefixedClasses('badge-').addClass('badge-important')
                        .attr('title', 'An error occured')
                        .text('!');
                })
                .done(function(diff) {
                    var vertexDiffsById = _.indexBy(diff.diffs, function(diff) {
                            return diff.vertexId;    
                        }),
                        countOfTitleChanges = 0,
                        filteredDiffs = _.filter(diff.diffs, function(diff) {
                            if (diff.type !== 'PropertyDiffItem') return true;
                            if (/^[_]/.test(diff.name)) return false;
                            if (diff.name === 'title' && vertexDiffsById[diff.elementId]) {
                                countOfTitleChanges++;
                            }
                            return true;
                        }), 
                        count = filteredDiffs.length - countOfTitleChanges,
                        formattedCount = formatters.number.pretty(count); 

                    self.currentDiffIds =_.uniq(filteredDiffs.map(function(diff) {
                        return diff.vertexId || diff.elementId || diff.edgeId;
                    }));

                    require(['workspaces/diff/diff'], function(Diff) {
                        var popover = badge.data('popover'),
                            tip = popover && popover.tip();

                        if (tip && tip.is(':visible')) {
                            self.trigger(popover.tip().find('.popover-content'), 'diffsChanged', { diffs: filteredDiffs });
                            popover.show();
                        } else {
                            badge
                                .popover('destroy')
                                .popover({placement:'top', content:'Loading...', title: 'Unpublished Changes'})

                            popover = badge.data('popover');
                            tip = popover.tip();

                            var left = 10;
                            tip.find('.arrow').css({
                                left: parseInt(badge.position().left - (left / 2), 10) + 'px',
                                marginLeft: 0
                            })

                            // We fill in our own content
                            popover.setContent = function() {}
                            badge.on('shown', function() {
                                var top = parseInt(tip.css('top'));
                                tip.css({ top: (top - 10) + 'px'})
                            })

                            Diff.teardownAll();
                            Diff.attachTo(tip.find('.popover-content'), {
                                diffs: filteredDiffs
                            });
                        }
                    });

                    var previousCount = badge.text();
                    badge.removePrefixedClasses('badge-').addClass('badge-info')
                        .attr('title', formatters.string.plural(formattedCount, 'unpublished change'))
                        .text(count > 0 ? formattedCount : '');
                    if (previousCount && formattedCount && formattedCount != previousCount) {
                        badge.removeClass('flash');
                        requestAnimationFrame(function() {
                            badge.addClass('flash');
                        })
                    }
                })
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
