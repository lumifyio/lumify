
define([
    'flight/lib/component',
    'tpl!./image',
    'util/withAsyncQueue'
], function(defineComponent, template, withAsyncQueue) {
    'use strict';

    return defineComponent(ImageView, withAsyncQueue);

    function ImageView() {

        this.defaultAttrs({
            imageSelector: 'img',
            boxSelector: '.facebox',
            boxEditingSelector: '.facebox.editing'
        });

        this.after('initialize', function() {
            this.setupAsyncQueue('image');

            this.$node.html(template({ data: this.attr.data }));

            this.setupEditingFacebox();

            this.on('DetectedObjectEnter', this.onHover);
            this.on('DetectedObjectLeave', this.onHoverLeave);
            this.on('DetectedObjectEdit', this.onEdit);
            this.on('DetectedObjectDoneEditing', this.onDoneEditing);
        });

        this.setupEditingFacebox = function() {
            var self = this,
                image = this.select('imageSelector'),
                imageEl = image.get(0),
                naturalWidth = imageEl.naturalWidth,
                naturalHeight = imageEl.naturalHeight;

            if (naturalWidth === 0 || naturalHeight === 0) {
                image.on('load', this.setupEditingFacebox.bind(this));
                return;
            }

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
                        convertToPercentageAndTrigger(evt, { element:facebox });
                    });

                    return;
                }

                event.stopPropagation();
                event.preventDefault();

                var box = this.select('boxEditingSelector'),
                    offsetParent = this.$node.offset(),
                    offsetParentWidth = this.$node.width(),
                    offsetParentHeight = this.$node.height(),
                    startPosition = {
                        left: event.pageX - offsetParent.left,
                        top: event.pageY - offsetParent.top,
                        width: 1,
                        height: 1
                    };

                $(document).on('mousemove.facebox', function(evt) {
                        var currentPosition = {
                                left: Math.min(offsetParentWidth, Math.max(0, evt.pageX - offsetParent.left)),
                                top: Math.min(offsetParentHeight, Math.max(0, evt.pageY - offsetParent.top))
                            },
                            width = Math.abs(startPosition.left - currentPosition.left),
                            height = Math.abs(startPosition.top - currentPosition.top);

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
                        convertToPercentageAndTrigger(evt, { element:box });
                    });

                box.css(startPosition).hide();
            });

            this.select('boxEditingSelector')
                .resizable({
                    containment: this.$node,
                    handles: 'all',
                    minWidth: 5,
                    minHeight: 5,
                    start: function(event, ui) {
                        // Make fixed percentages during drag
                    },
                    stop: convertToPercentageAndTrigger
                }).draggable({ 
                    containment: this.$node,
                    cursor: "move",
                    stop: convertToPercentageAndTrigger
                });

            this.imageMarkReady(imageEl);

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
                    x1: (l * naturalWidth).toFixed(2) + '',
                    x2: ((l + w) * naturalWidth).toFixed(2) + '',
                    y1: (t * naturalHeight).toFixed(2) + '',
                    y2: ((t + h) * naturalHeight).toFixed(2) + ''
                });
            }
        };

        this.showFacebox = function(data, opts) {
            var self = this,
                options = $.extend({ editing:false, viewing:false }, opts || {});

            this.imageReady()
                .done(function() {
                    var box = (options.editing || options.viewing) ? 
                            self.select('boxEditingSelector') :
                            self.select('boxSelector').not('.editing'),
                        image = self.select('imageSelector'),
                        width = image.width(),
                        height = image.height(),
                        aspectWidth = width / image[0].naturalWidth,
                        aspectHeight = height / image[0].naturalHeight,
                        w = (data.x2 - data.x1) * aspectWidth / width * 100,
                        h = (data.y2 - data.y1) * aspectHeight / height * 100,
                        x = data.x1 * aspectWidth / width * 100,
                        y = data.y1 * aspectHeight / height * 100;

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
            });
        };

        this.showFaceboxForEdit = function(data) {
            this.showFacebox(data, { editing:true });
        };

        this.showFaceboxForView = function(data) {
            this.showFacebox(data, { viewing:true });
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

        this.onEdit = function(event, data) {
            if (data.entityVertex) {
                this.currentlyEditing = data.entityVertex.id;
                this.showFaceboxForView(data);
            } else if (data.isNew) {
                this.currentlyEditing = 'NEW';
                this.showFaceboxForEdit(data);
            } else {
                this.currentlyEditing = data._rowKey;
                this.showFaceboxForEdit(data);
            }
        };

        this.onDoneEditing = function(event) {
            this.currentlyEditing = null;
            this.select('boxSelector').hide();
        };
    }
});
