
define([], function() {
    'use strict';

    return withContextMenu;

    function withContextMenu() {

        this.after('initialize', function() {
        });

        this.bindContextMenuClickEvent = function() {
            this.$node.find('.dropdown-menu a').off('click.bindCtxMenu');
            this.$node.find('.dropdown-menu a').on('click.bindCtxMenu', this.onContextMenuClick.bind(this));
        };

        this.onContextMenuClick = function(event) {
            var target = $(event.target),
                name = target.data('func'),
                functionName = name && 'onContextMenu' + name.substring(0, 1).toUpperCase() + name.substring(1),
                func = functionName && this[functionName],
                args = target.data('args');

            if (target.closest('li.disabled').length) {
                event.stopPropagation();
                return;
            }
            if (func) {
                if (!args) {
                    args = [];
                }
                func.apply(this, args);
            } else if (!functionName) {
                event.stopPropagation();
                event.preventDefault();
                target.blur();
                return;
            } else {
                console.error('No function exists for context menu command: ' + functionName);
            }

            setTimeout(function() {
                target.blur();
                this.$node.find('.dropdown-menu').blur().parent().removeClass('open');
            }.bind(this), 0);
        };

        this.closeMenu = function() {
            this.$node.find('.dropdown-menu').blur().parent().removeClass('open');
        };

        this.toggleMenu = function(position, menuElement) {
            this.bindContextMenuClickEvent();

            var offset = this.$node.offset(),
                padding = 10,
                windowSize = { x: $(window).width(), y: $(window).height() },
                menu = menuElement || this.$node.find('.dropdown-menu'),
                menuSize = { x: menu.outerWidth(true), y: menu.outerHeight(true) },
                submenu = menu.find('li.dropdown-submenu ul'),
                submenuSize = menuSize,
                placement = {
                    left: Math.min(
                        position.positionInVertex ?
                            position.positionInVertex.x :
                            (position.positionUsingEvent.originalEvent.pageX - offset.left),
                        windowSize.x - offset.left - menuSize.x - padding
                    ),
                    top: Math.min(
                        position.positionInVertex ?
                            position.positionInVertex.y :
                            (position.positionUsingEvent.originalEvent.pageY - offset.top),
                        windowSize.y - offset.top - menuSize.y - padding
                    )
                },
                submenuPlacement = { left: '100%', right: 'auto', top: 0, bottom: 'auto' };
            if ((placement.left + menuSize.x + submenuSize.x + padding) > windowSize.x) {
                submenuPlacement = $.extend(submenuPlacement, { right: '100%', left: 'auto' });
            }
            if ((placement.top + menuSize.y + (submenu.children('li').length * 26) + padding) > windowSize.y) {
                submenuPlacement = $.extend(submenuPlacement, { top: 'auto', bottom: '0' });
            }

            menu.parent('div').css($.extend({ position: 'absolute' }, placement));
            submenu.css(submenuPlacement);

            menu.dropdown('toggle');
        };
    }
});
