
define([
    'flight/lib/component',
    'data',
    'tpl!./menu',
    'service/ontology',
    'util/formatters'
], function(defineComponent, appData, template, OntologyService, formatters) {
    'use strict';

    return defineComponent(Menu);

    function Menu() {

        var ontologyService = new OntologyService(),
            DIVIDER = 'DIVIDER',
            relatedSubmenuItems = null,
            items = [
                {
                    label: 'Connect',
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
                    label: 'Find Path',
                    shortcut: 'CTRL+drag',
                    event: 'startVertexConnection',
                    selection: 1,
                    args: {
                        connectionType: 'FindPath',
                        hops: 1
                    },
                    shouldDisable: function(selection, vertexId, target) {
                        return $(target).closest('.graph-pane').length === 0;
                    }
                },

                DIVIDER,

                { 
                    label: 'Search',
                    submenu: [
                        { label: '\"{ title }\"', shortcut: 'alt+t', event: 'searchTitle', selection:1 },
                        { label: 'Related Items', shortcut: 'alt+s', event: 'searchRelated', selection:1 }
                    ]
                },

                { 
                    label: 'Add Related',
                    submenu: (relatedSubmenuItems = [
                        { label: 'Items', shortcut: 'alt+r', event: 'addRelatedItems', selection:1 }
                    ])
                },

                DIVIDER,

                {
                    label: 'Remove',
                    shortcut: 'delete',
                    subtitle: 'Remove from Workspace',
                    event: 'deleteSelected',
                    selection: 2,
                    shouldDisable: function(selection, vertexId, target) {
                        return !appData.inWorkspace(vertexId);
                    }
                }

            ],
            concepts = ontologyService.concepts().done(function(concepts) {
                var list = concepts.entityConcept.children || [];

                if (list.length) {
                    relatedSubmenuItems.push(DIVIDER);
                
                    list.forEach(function(concept) {
                        relatedSubmenuItems.push({
                            label: concept.pluralDisplayName,
                            event: relatedSubmenuItems[0].event,
                            selection: 1,
                            args: {
                                limitParentConceptId: concept.id 
                            }
                        })
                    })
                }
            });

        this.defaultAttrs({
            menuSelector: '.vertex-menu a'
        });

        this.after('teardown', function() {
            this.$menu.remove();
            $(document).off('.vertexMenu');
        });

        this.after('initialize', function() {
            appData
                .getVertexTitle(this.attr.vertexId)
                .done(this.setupMenu.bind(this));

            this.on('click', {
                menuSelector: this.onMenuItemClick
            });
        });

        this.onMenuItemClick = function(event) {
            event.preventDefault();

            var anchor = $(event.target).closest('a'),
                args = anchor.data('args'),
                eventName = anchor.data('event');

            this.trigger(this.attr.element, eventName, 
                _.extend({ vertexId: this.attr.vertexId }, args)
            );
        };

        this.setupMenu = function(title) {
            var self = this;

            if (title.length > 15) {
                title = title.substring(0, 15) + '...';
            }

            this.$node.append(template({
                items: items,
                shouldDisable: function(item) {
                    var currentSelection = appData.selectedVertexIds,
                        thisVertex = self.attr.vertexId,
                        multi = item.selection !== 1;

                    if (!multi && 
                        currentSelection.length &&
                        !_.isEqual(currentSelection, [thisVertex])) {
                        return true;
                    }

                    return _.isFunction(item.shouldDisable) ? item.shouldDisable(
                        currentSelection,
                        self.attr.vertexId,
                        self.attr.element) : false;
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

                    $this.text(formatters.string.shortcut($this.text()));
                });

            this.positionMenu(this.attr.position);

            $(document).off('.vertexMenu').on('click.vertexMenu', function() {
                $(document).off('.vertexMenu');
                self.teardown();
            });
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
                submenuPlacement = { left:'100%', right:'auto', top:0, bottom:'auto' };
            if ((placement.left + menuSize.x + submenuSize.x + padding) > windowSize.x) {
                submenuPlacement = $.extend(submenuPlacement, { right: '100%', left:'auto' });
            }
            if ((placement.top + menuSize.y + (submenu.children('li').length * 26) + padding) > windowSize.y) {
                submenuPlacement = $.extend(submenuPlacement, { top: 'auto', bottom:'0' });
            }

            menu.parent('div')
                .addClass('open')
                .css($.extend({ position:'absolute' }, placement));
            submenu.css(submenuPlacement);
        };
    }
});

