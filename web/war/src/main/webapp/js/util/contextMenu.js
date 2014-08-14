define([
    'flight/lib/component',
], function(defineComponent) {
    'use strict';

    return defineComponent(ContextMenu);

    function distance(p1, p2) {
        return Math.abs(
            Math.sqrt(
                Math.pow(p2[0] - p1[0], 2) +
                Math.pow(p2[1] - p1[1], 2)
            )
        );
    }

    function ContextMenu() {

        this.after('initialize', function() {
            var self = this,
                altKey = false,
                ctrlKey = false,
                downPosition,
                queueContextMenuEvent,
                mousedown = false;

            document.addEventListener('mousedown', function(event) {
                downPosition = [event.pageX, event.pageY];
                altKey = event.altKey;
                ctrlKey = event.ctrlKey;
                mousedown = true;
            }, true);

            document.addEventListener('mouseup', function(event) {
                mousedown = false;
                if (altKey || ctrlKey || queueContextMenuEvent) {
                    if (queueContextMenuEvent && distance([event.pageX, event.pageY], downPosition) < 20) {
                        self.triggerContextMenu(event);
                    }
                }
                _.delay(function() {
                    altKey = false;
                    ctrlKey = false;
                    queueContextMenuEvent = false;
                }, 250);
            }, true);

            document.addEventListener('click', function(event) {
                if ((altKey || ctrlKey) && !queueContextMenuEvent) {
                    event.stopPropagation();
                    self.triggerContextMenu(event);
                }
                altKey = false;
            }, true);

            var delay;

            document.addEventListener('contextmenu', function(event) {
                var originalTabindex = event.target.getAttribute('tabindex'),
                    handler;

                event.target.setAttribute('tabindex', -1);
                queueContextMenuEvent = true;

                _.delay(function() {
                    self.off(event.target, 'blur', handler);
                }, 500);
                self.on(event.target, 'blur', handler = function blurHandler(blurEvent) {
                    queueContextMenuEvent = false;
                    hideMenu = true;
                    self.trigger(event.target, 'hideMenu');

                    // TODO: warn
                    self.off(event.target, 'blur', blurHandler);
                    if (originalTabindex) {
                        event.target.setAttribute('tabindex', originalTabindex);
                    } else {
                        event.target.removeAttribute('tabindex');
                    }
                });

            }, true);
        });

        var hideMenu;

        this.triggerContextMenu = function(event) {
            if (!hideMenu) {
                this.trigger(
                    event.target,
                    'showMenu',
                    _.pick(event, 'type', 'originalEvent', 'pageX', 'pageY')
                );
            }
            hideMenu = false;
        }

    }
});
