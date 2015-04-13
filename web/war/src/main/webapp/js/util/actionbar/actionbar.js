
define([
    'flight/lib/component',
    'tpl!./actionbar'
], function(
    defineComponent,
    template
) {
    'use strict';

    var FPS = 1000 / 60,
        TOP_HIDE_THRESHOLD = 40,
        ALIGN_TO_TYPES = [
            'textselection',
            'node'
        ];

    return defineComponent(ActionBar);

    function ActionBar() {

        this.around('teardown', function(original) {
            this.$tip.hide();
            this[this.attr.alignTo + 'Teardown']();
            this.$node.tooltip('destroy');
            if (this.alreadyDisposed) {
                return;
            }
            original.call(this);
            this.alreadyDisposed = true;
        });

        this.after('initialize', function() {
            if (!this.attr.actions) {
                throw 'actions attribute required';
            }
            if (ALIGN_TO_TYPES.indexOf(this.attr.alignTo) === -1) {
                throw 'alignTo only supports ' + ALIGN_TO_TYPES.join(',');
            }

            this.$node.removeAttr('title');
            var tooltip = this.$node.tooltip({
                trigger: 'click',
                title: template(this.attr),
                placement: 'bottom',
                container: 'body',
                html: true
            });

            tooltip.tooltip('show');

            this.$tip = tooltip.data('tooltip').$tip
                .addClass('actionbar')
                .on('click', '.actionbarbutton', this.onActionClick.bind(this));
            this.updatePosition = _.throttle(this[this.attr.alignTo + 'UpdatePosition'].bind(this), FPS);

            this[this.attr.alignTo + 'Initializer']();
            this.updatePosition();
            this.on(document, 'graphPaddingUpdated', this.updatePosition);
            this.on(document, 'windowResize', this.updatePosition);

            var self = this;

            this.on(document, 'click', function() {
                _.defer(function() {
                    self.teardown();
                });
            });
        });

        this.onActionClick = function(event) {
            var $target = $(event.target).blur();

            this.trigger($target.data('event'));
            this.$tip.hide();
        };

        this.nodeUpdatePosition = function() {
            var offset = this.$node.offset(),
                width = this.$node.width(),
                height = this.$node.height();

            this.updateTipPositionWithDomElement(this.node, 'center');
        };

        this.nodeInitializer = function() {
            this.updatePositionOnScroll(this.node);
        };

        this.nodeTeardown = function() {
            this.scrollParent.off('.actionbar');
        };

        this.textselectionUpdatePosition = function() {
            var selection = getSelection(),
                range = selection.rangeCount > 0 && selection.getRangeAt(0);

            if (range) {
                this.updateTipPositionWithDomElement(range);
            } else this.teardown();
        };

        this.textselectionInitializer = function() {
            var selection = getSelection(),
                closest = selection.anchorNode.parentNode;

            this.updatePositionOnScroll(closest);
        };

        this.textselectionTeardown = function() {
            this.scrollParent.off('.actionbar');
        };

        this.updatePositionOnScroll = function(el) {

            // Reposition on scroll events
            this.scrollParent = $(el).scrollParent()
                .off('.actionbar')
                .on('scroll.actionbar', this.updatePosition);
        };

        this.updateTipPositionWithDomElement = function(el, alignment) {
            var box = null,
                rects = el.getClientRects();

            if (rects.length) {
                box = _.sortBy(rects, function(r) {
                    return r.top * -1;
                })[0];
            } else {
                box = el.getBoundingClientRect();
            }

            var windowScroll = $(window).scrollTop(), // for fullscreen view
                top = box.top + windowScroll,
                position = (alignment === 'center' && rects.length === 1) ?
                    box.left + box.width / 2 :
                    box.left + box.width,
                css = {
                    left: position - this.$tip.width() / 2,
                    top: top + box.height
                };

            if (this.attr.alignWithin) {
                var offset = this.attr.alignWithin.offset(),
                    width = this.attr.alignWithin.width(),
                    padding = parseInt(this.attr.alignWithin.css('padding-left')) * 2,
                    extraPadding = 3,
                    totalWidth = width + (isNaN(padding) ? 0 : padding),
                    tipWidth = this.$tip.width();

                css.left = Math.max(offset.left + extraPadding, css.left);
                css.left = Math.min(offset.left + totalWidth - tipWidth - extraPadding, css.left);
                var arrowPercent = (position - css.left) / tipWidth * 100;
                this.$tip.find('.tooltip-arrow').css('left', arrowPercent.toFixed(2) + '%');
            }

            css.opacity = top < TOP_HIDE_THRESHOLD ? '0' : '1';

            this.$tip.css(css);
        };
    }
});
