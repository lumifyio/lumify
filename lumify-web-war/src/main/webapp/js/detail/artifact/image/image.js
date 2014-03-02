
define([
    'flight/lib/component',
    'tpl!./image',
    'tpl!util/blur/blur-svg'
], function(defineComponent, template, blur, Jcrop) {
    'use strict';

    return defineComponent(ImageView);

    function ImageView() {

        this.defaultAttrs({
            imageSelector: 'img',
            boxSelector: '.facebox',
            boxEditingSelector: '.facebox.editing',
            svgPrefix: 'detail-pane'
        });

        this.after('initialize', function() {
            var html = template({ data: this.attr.data });

            this.$node.css({
                backgroundImage: this.attr.src
            }).html(html);

            this.setupEditingFacebox();

            this.$node.closest('.detail-pane').on('DetectedObjectEnter', this.onHover.bind(this));
            this.$node.closest('.detail-pane').on('DetectedObjectLeave', this.onHoverLeave.bind(this));
            this.$node.closest('.detail-pane').on('DetectedObjectEdit', this.onEdit.bind(this));
            this.$node.closest('.detail-pane').on('DetectedObjectDoneEditing', this.onDoneEditing.bind(this));
            this.before('teardown',function () {
                this.$node.closest('.detail-pane').off('DetectedObjectEnter');
                this.$node.closest('.detail-pane').off('DetectedObjectLeave');
                this.$node.closest('.detail-pane').off('DetectedObjectDoneEditing');
            });
        });

        this.setupEditingFacebox = function() {
            var self = this,
                image = this.select('imageSelector'),
                naturalWidth = image[0].naturalWidth,
                naturalHeight = image[0].naturalHeight;

            if (naturalWidth === 0 || naturalHeight === 0) {
                image.on('load', this.setupEditingFacebox.bind(this));
                return;
            }

            this.on('click', function(event) {
                var $target = $(event.target);
                if ($target.closest('.facebox').length) return;
                this.select('boxSelector').hide();
                this.currentlyEditing = null;
            });
            this.on('mousedown', function(event) {
                var $target = $(event.target);
                if ($target.closest('.facebox').length) return;

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
                        ui.element.css(
                            $.extend({}, ui.element.position(), {
                                width:ui.element.width(),
                                height:ui.element.height()
                            })
                        );
                    },
                    stop: convertToPercentageAndTrigger
                }).draggable({ 
                    containment: this.$node,
                    cursor: "move",
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
                    x1: (l * naturalWidth).toFixed(2) + '',
                    x2: ((l + w) * naturalWidth).toFixed(2) + '',
                    y1: (t * naturalHeight).toFixed(2) + '',
                    y2: ((t + h) * naturalHeight).toFixed(2) + ''
                });
            }
        };

        this.showFacebox = function(data, toEdit) {
            var box = toEdit ? this.select('boxEditingSelector') :
                        this.select('boxSelector').not('.editing');
            var image = this.select('imageSelector');

            var width = image.width(),
                height = image.height(),
                aspectWidth = width / image[0].naturalWidth,
                aspectHeight = height / image[0].naturalHeight,
                w = (data.x2 - data.x1) * aspectWidth / width * 100,
                h = (data.y2 - data.y1) * aspectHeight / height * 100,
                x = data.x1 * aspectWidth / width * 100,
                y = data.y1 * aspectHeight / height * 100;
            box.css({
                    width: w + '%',
                    height: h + '%',
                    left: x + '%',
                    top: y + '%'
                })
                .show();
        };

        this.showFaceboxForEdit = function(data) {
            this.showFacebox(data, true);
        };

        this.onHover = function(event, data) {
            this.showFacebox(data);
        };

        this.onHoverLeave = function(event, data) {
            var toHide = this.select('boxSelector');

            if (this.currentlyEditing) {
                toHide = toHide.not('.editing');
            }

            if (data.id !== this.currentlyEditing){
                toHide.hide();
            }
        };

        this.onEdit = function(event, data) {
            if (data.id) {
                this.currentlyEditing = data.id;
                this.showFacebox(data);
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
