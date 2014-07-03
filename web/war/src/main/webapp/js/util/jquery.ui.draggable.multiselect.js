
define(['jquery', 'jqueryui'], function() {
    'use strict';

    $.ui.plugin.add('draggable', 'multi', {

        create: function(e, ui) {
            var inst = this.data('ui-draggable'),
                container = this.closest('ul');

            if (container.data('initialized') !== true) {
                container
                    .data('initialized', true)
                    .attr('tabindex', '0')
                    .addClass('multi-select')
                    .on({
                        click: function(evt) {
                            this.focus();

                            if ($(this).is(evt.target)) {
                                $(this).find('li').removeClass('active');
                                onSelection(evt, $());
                            }
                        },
                        keydown: function(evt) {
                            if ((evt.metaKey || evt.ctrlKey) && evt.which === 65) {
                                evt.preventDefault();
                                onSelection(evt, $(this).find('li').addClass('active'));
                            }
                        }
                    });
            }

            if (inst.options.revert === 'invalid') {

                inst.options.revert = function(dropped) {
                    var reverted = inst.reverted = !dropped;

                    if (reverted && inst.alsoDragging) {
                        inst.alsoDragging.each(function() {
                            this.animate(this.data('original').offset(),
                                parseInt(inst.options.revertDuration, 10),
                                function() {
                                    this.remove();
                                }
                            );
                        });
                    }

                    return reverted;
                };
            }

            this.on('click', function(evt) {
                evt.preventDefault();

                var $target = $(evt.target).parents('li'),
                    list = $target.parent(),
                    lastSelection = list.data('lastSelection'),
                    selected = list.find('.active');

                if (evt.shiftKey && lastSelection) {

                    // Handle contiguous selection
                    var targetIndex = $target.index(),
                        previousIndex = lastSelection.index();

                    if (targetIndex !== previousIndex) {
                        $target[targetIndex < previousIndex ? 'nextUntil' : 'prevUntil'](lastSelection)
                        .andSelf()
                        .addClass('active');
                    }

                } else if (evt.metaKey) {

                    // Just add this
                    $target.addClass('active');

                } else {

                    selected.not($target).removeClass('active');
                    $target.addClass('active');
                }

                // Keep track of last selected for shift-selection later
                list.data('lastSelection', $target);

                onSelection(evt, list.find('.active'));
            });

            function onSelection(evt, elements) {
                if (inst.options.selection) {
                    inst.options.selection.call(
                        inst.element,
                        evt,
                        $.extend(inst._uiHash(), {
                            selected: elements
                        }));
                }
            }
        },

        start: function(e, ui) {
            if (arguments.length > 2) {
                console.warn('Switch plugin to use [instance] argument instead of data');
            }

            var instance = this.data('ui-draggable'),
                helper = ui.helper,
                anchor = $(this),
                item = anchor.parent('li'),
                list = item.parent(),
                width = anchor.width();

            instance.reverted = false;

            // Make clone the same width
            helper.width(width);

            // Hovers while moving makes it slower as the browser
            // displays the url, so just make the link invalid
            helper.removeAttr('href');

            // If dragging a selected node, bring along other selected
            // items
            if (item.hasClass('active')) {
                var items = list.find('.active a')
                    .not(anchor)
                    .map(function() {
                        var $this = $(this),
                            cloned = $this.clone().removeAttr('id href').data('original', $this);

                        if (instance.options.otherDraggablesClass) {
                            cloned.addClass(instance.options.otherDraggablesClass);
                        }

                        return cloned.get(0);
                    });

                helper.append(items);
                instance.alsoDragging = items.map(function() {
                    return $(this);
                });

            } else {
                instance.alsoDragging = false;
            }
        },

        drag: function(ev, ui) {
            var instance = this.data('ui-draggable');
            if (!instance.alsoDragging || !instance.alsoDragging.length) {
                return;
            }
        },

        stop: function(ev, ui) {
            var inst = this.data('ui-draggable');
            if (!inst.alsoDragging || !inst.alsoDragging.length) {
                return;
            }

            if (!inst.reverted && inst.options.otherDraggables) {
                inst.options.otherDraggables.call(
                    inst.element,
                    ev,
                    $.extend(inst._uiHash(), {
                        otherDraggables: inst.alsoDragging
                    }));
            }

            if (!inst.reverted) {
                inst.alsoDragging.each(function() {
                    this.remove();
                });
            }

            inst.alsoDragging = false;
        }
    });

});
