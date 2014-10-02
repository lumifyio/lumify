define(['hbs!./withFaceboxTpl'], function(tpl) {
    'use strict';

    return withFacebox;

    function withFacebox() {

        this.defaultAttrs({
            boxSelector: '.facebox',
            boxEditingSelector: '.facebox.editing'
        })

        this.initializeFacebox = function(container) {
            this.container = container.append(tpl({}));
            this.setupEditingFacebox();

            this.on('DetectedObjectEnter', this.onHover);
            this.on('DetectedObjectLeave', this.onHoverLeave);
            this.on('DetectedObjectEdit', this.onEdit);
            this.on('DetectedObjectDoneEditing', this.onDoneEditing);
        }

        this.setupEditingFacebox = function() {
            var self = this;

            this.on('click', function(event) {
                if (self.preventClick) {
                    self.preventClick = false;
                    return;
                }
                var $target = $(event.target);
                if ($target.closest('.facebox').length) return;
                this.select('boxSelector').hide();
                this.currentlyEditing = null;
            });
            this.on('mousedown', function(event) {
                var $target = $(event.target),
                    facebox = $target.closest('.facebox')

                if (facebox.length) {
                    var position = facebox.position(),
                        width = facebox.width(),
                        height = facebox.height();

                    facebox.css({
                        top: position.top + 'px',
                        left: position.left + 'px',
                        width: width + 'px',
                        height: height + 'px'
                    });
                    $(document).on('mouseup.facebox', function(evt) {
                        $(document).off('.facebox');
                        convertToPercentageAndTrigger(evt, { element: facebox });
                    });

                    return;
                }

                event.stopPropagation();
                event.preventDefault();

                var box = this.select('boxEditingSelector'),
                    offsetParent = this.container.offset(),
                    offsetParentWidth = this.container.width(),
                    offsetParentHeight = this.container.height(),
                    startPosition = {
                        left: Math.min(
                            offsetParentWidth,
                            Math.max(0, event.pageX - offsetParent.left)
                        ),
                        top: event.pageY - offsetParent.top,
                        width: 1,
                        height: 1
                    };

                $(document).on('mousemove.facebox', function(evt) {
                        var currentPosition = {
                                left: Math.min(
                                    offsetParentWidth,
                                    Math.max(0, evt.pageX - offsetParent.left)
                                ),
                                top: Math.min(offsetParentHeight, Math.max(0, evt.pageY - offsetParent.top))
                            },
                            width = Math.min(offsetParentWidth, Math.abs(startPosition.left - currentPosition.left)),
                            height = Math.min(offsetParentWidth, Math.abs(startPosition.top - currentPosition.top));

                        if (width >= 5 && height >= 5) {
                            box.css({
                                left: Math.min(startPosition.left, currentPosition.left),
                                top: Math.min(startPosition.top, currentPosition.top),
                                width: width,
                                height: height
                            }).show();
                        } else {
                            box.hide();
                        }
                    })
                    .on('mouseup.facebox', function(evt) {
                        $(document).off('mouseup.facebox mousemove.facebox');
                        self.currentlyEditing = 'NEW';
                        self.preventClick = true;
                        convertToPercentageAndTrigger(evt, { element: box });
                    });

                box.css(startPosition).hide();
            });

            this.select('boxEditingSelector')
                .resizable({
                    containment: 'parent',
                    handles: 'all',
                    minWidth: 5,
                    minHeight: 5,
                    stop: convertToPercentageAndTrigger
                }).draggable({
                    containment: 'parent',
                    cursor: 'move',
                    stop: convertToPercentageAndTrigger
                });

            function convertToPercentageAndTrigger(event, ui) {
                // Make percentages for fluid
                var el = ui.element || ui.helper,
                    position = el.position(),
                    offsetParent = el.offsetParent(),
                    width = offsetParent.width(),
                    height = offsetParent.height(),
                    t = position.top / height,
                    l = position.left / width,
                    w = el.width() / width,
                    h = el.height() / height;

                el.css({
                    top: t * 100 + '%',
                    left: l * 100 + '%',
                    width: w * 100 + '%',
                    height: h * 100 + '%'
                });

                self.trigger('DetectedObjectCoordsChange', {
                    id: self.currentlyEditing,
                    x1: l.toFixed(2) + '',
                    x2: (l + w).toFixed(2) + '',
                    y1: t.toFixed(2) + '',
                    y2: (t + h).toFixed(2) + ''
                });
            }
        };

        this.showFacebox = function(property, opts) {
            var self = this,
                options = $.extend({ editing: false, viewing: false }, opts || {}),
                value = property.value,
                box = (options.editing || options.viewing) ?
                    self.select('boxEditingSelector') :
                    self.select('boxSelector').not('.editing'),
                w = (value.x2 - value.x1) * 100,
                h = (value.y2 - value.y1) * 100,
                x = value.x1 * 100,
                y = value.y1 * 100;

            if (options.viewing) {
                box.resizable('disable').draggable('disable')
            } else if (options.editing) {
                box.resizable('enable').draggable('enable')
            }

            box.css({
                    width: w + '%',
                    height: h + '%',
                    left: x + '%',
                    top: y + '%'
                })
                .show();
        };

        this.showFaceboxForEdit = function(property) {
            this.showFacebox(property, { editing: true });
        };

        this.showFaceboxForView = function(property) {
            this.showFacebox(property, { viewing: true });
        };

        this.onHover = function(event, data) {
            this.showFacebox(data);
        };

        this.onHoverLeave = function(event, data) {
            var toHide = this.select('boxSelector');

            if (this.currentlyEditing) {
                toHide = toHide.not('.editing');
            }

            toHide.hide();
        };

        this.onEdit = function(event, property) {
            if (property.value.resolvedVertexId) {
                this.currentlyEditing = property.resolvedVertexId;
                this.showFaceboxForView(data);
            } else if (property.isNew) {
                this.currentlyEditing = 'NEW';
                this.showFaceboxForEdit(property);
            } else {
                this.currentlyEditing = property.key;
                this.showFaceboxForEdit(property);
            }
        };

        this.onDoneEditing = function(event) {
            this.currentlyEditing = null;
            this.select('boxSelector').hide();
        };

    }
});
