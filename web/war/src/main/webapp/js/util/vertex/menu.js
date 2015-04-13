
define([
    'flight/lib/component',
    'tpl!./menu',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(defineComponent, template, F, withDataRequest) {
    'use strict';

    return defineComponent(Menu, withDataRequest);

    function Menu() {

        var DIVIDER = 'DIVIDER',
            items = [
                {
                    cls: 'requires-EDIT',
                    label: i18n('vertex.contextmenu.connect'),
                    shortcut: 'CTRL+drag',
                    event: 'startVertexConnection',
                    selection: 1,
                    args: {
                        connectionType: 'CreateConnection'
                    },
                    shouldDisable: function(selection, vertexId, target) {
                        return $(target).closest('.graph-pane').length === 0;
                    }
                },

                {
                    label: i18n('vertex.contextmenu.find_path'),
                    shortcut: 'CTRL+drag',
                    event: 'startVertexConnection',
                    selection: 1,
                    args: {
                        connectionType: 'FindPath'
                    },
                    shouldDisable: function(selection, vertexId, target) {
                        return $(target).closest('.graph-pane').length === 0;
                    }
                },

                DIVIDER,

                {
                    label: i18n('vertex.contextmenu.search'),
                    submenu: [
                        { label: '\"{ title }\"', shortcut: 'alt+t', event: 'searchTitle', selection: 1 },
                        { label: 'Related Items', shortcut: 'alt+s', event: 'searchRelated', selection: 1 }
                    ]
                },

                {
                    label: i18n('vertex.contextmenu.add_related'),
                    event: 'addRelatedItems',
                    shouldDisable: function(selection, vertexId, target) {
                        return !lumifyData.currentWorkspaceEditable;
                    }
                },

                DIVIDER,

                {
                    label: i18n('vertex.contextmenu.remove'),
                    shortcut: 'delete',
                    subtitle: i18n('vertex.contextmenu.remove.subtitle'),
                    event: 'deleteSelected',
                    selection: 2,
                    shouldDisable: function(selection, vertexId, target) {
                        return !lumifyData.currentWorkspaceEditable || false;
                        // TODO:  !inWorkspace(vertexId);
                    }
                }

            ];

        this.defaultAttrs({
            menuSelector: '.vertex-menu a'
        });

        this.after('teardown', function() {
            this.$menu.remove();
            $('.draggable-wrapper').remove();
            $(document).off('.vertexMenu');
        });

        this.after('initialize', function() {
            this.on(document, 'closeVertexMenu', function() {
                this.teardown();
            });

            this.on('click', {
                menuSelector: this.onMenuItemClick
            });

            this.dataRequest('vertex', 'store', { vertexIds: this.attr.vertexId })
                .done(this.setupMenu.bind(this));
        });

        this.onClose = function() {
            this.teardown();
        };

        this.onMenuItemClick = function(event) {
            event.preventDefault();

            var anchor = $(event.target).closest('a'),
                args = anchor.data('args'),
                eventName = anchor.data('event');

            if (anchor.closest('li.disabled').length) {
                return;
            }

            this.trigger(this.attr.element, eventName,
                _.extend({ vertexId: this.attr.vertexId }, args)
            );
        };

        this.setupMenu = function(vertex) {
            var self = this,
                title = F.string.truncate(F.vertex.title(vertex), 3),
                wrapper = $('.draggable-wrapper');

            if (wrapper.length === 0) {
                wrapper = $('<div class="draggable-wrapper"/>').appendTo(document.body);
            }

            this.$node.append(template({
                items: items,
                vertex: vertex,
                shouldDisable: function(item) {
                    var currentSelection = lumifyData.selectedObjects.vertexIds,
                        shouldDisable = _.isFunction(item.shouldDisable) ? item.shouldDisable(
                            currentSelection,
                            self.attr.vertexId,
                            self.attr.element) : false;

                    return shouldDisable;
                },
                processLabel: function(item) {
                    return _.template(item.label)({
                        title: title
                    })
                }
            }))

            this.$menu = this.$node.find('.vertex-menu')
            this.$menu.find('.shortcut').each(function() {
                    var $this = $(this),
                        command = $this.text();

                    $this.text(F.string.shortcut($this.text()));
                });

            this.$menu.find('.dropdown-menu li.divider:last-child').remove();

            this.positionMenu(this.attr.position);

            _.defer(function() {
                $(document).off('.vertexMenu').on('click.vertexMenu', function() {
                    $(document).off('.vertexMenu');
                    self.teardown();
                });
            })
        }

        this.positionMenu = function(position) {

            var padding = 10,
                windowSize = { x: $(window).width(), y: $(window).height() },
                menu = this.$menu.children('.dropdown-menu'),
                menuSize = { x: menu.outerWidth(true), y: menu.outerHeight(true) },
                submenu = menu.find('li.dropdown-submenu ul'),
                submenuSize = menuSize,
                placement = {
                    left: Math.min(
                        position.x,
                        windowSize.x - menuSize.x - padding
                    ),
                    top: Math.min(
                        position.y,
                        windowSize.y - menuSize.y - padding
                    )
                },
                submenuPlacement = { left: '100%', right: 'auto', top: 0, bottom: 'auto' };
            if ((placement.left + menuSize.x + submenuSize.x + padding) > windowSize.x) {
                submenuPlacement = $.extend(submenuPlacement, { right: '100%', left: 'auto' });
            }
            if ((placement.top + menuSize.y + (submenu.children('li').length * 26) + padding) > windowSize.y) {
                submenuPlacement = $.extend(submenuPlacement, { top: 'auto', bottom: '0' });
            }

            menu.parent('div')
                .addClass('open')
                .css($.extend({ position: 'absolute' }, placement));
            submenu.css(submenuPlacement);
        };
    }
});
