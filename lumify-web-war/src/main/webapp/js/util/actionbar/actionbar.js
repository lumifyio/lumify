
define([
    'flight/lib/component',
    'tpl!./actionbar',
    'util/withTeardown'
], function(
    defineComponent,
    template,
    withTeardown
) {
    'use strict';

    var FPS = 1000 / 60,
        TOP_HIDE_THRESHOLD = 40,
        ALIGN_TO_TYPES = [
            'textselection',
            'node'
        ];

    return defineComponent(ActionBar, withTeardown);

    function ActionBar() {

        this.before('teardown', function() {
            $(document).off('.actionbar');
            this[this.attr.alignTo + 'Teardown']();
            this.$node.tooltip('destroy');
            this.$tip.hide();
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

            var self = this;
            $(document).off('.actionbar').on('click.actionbar', function() {
                $(document).off('.actionbar');
                self.teardown();
            });
        });

        this.onActionClick = function(event) {
            var self = this,
                $target = $(event.target).blur();
                
            this.$tip.hide();

            _.defer(function() {
                self.trigger($target.data('event'));
            });
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
            var rects = el.getClientRects();

            if (rects.length) {
                box = _.sortBy(rects, function(r) {
                    return r.top * -1;
                })[0];
            } else {
                box = el.getBoundingClientRect();
            }

            this.$tip.css({
                left: (alignment === 'center' && rects.length === 1) ?
                    box.left + box.width / 2 - this.$tip.width() / 2 : 
                    box.left + box.width - this.$tip.width() / 2,
                top: box.top + box.height,
                opacity: box.top < TOP_HIDE_THRESHOLD ? '0' : '1'
            });
        };
    }
});
