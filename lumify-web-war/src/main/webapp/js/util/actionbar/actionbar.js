
define([
    'flight/lib/component',
    'tpl!./actionbar'
], function (
    defineComponent,
    template
) {
    'use strict';

    return defineComponent(ActionBar);

    function ActionBar() {

        this.before('teardown', function() {
            this.scrollParent.off('.actionbar');
            this.$node.tooltip('destroy');
            console.log('Destroy actionbar', this.identity);
        });

        this.after('initialize', function() {
            console.log('Creating actionbar', this.identity);
            if (!this.attr.actions) throw "actions attribute required";
            if (this.attr.alignTo !== 'textselection') throw "alignTo only supports textselection";

            var tooltip = this.$node.tooltip({
                trigger: 'click',
                title: template(this.attr),
                placement: 'bottom',
                container: 'body',
                html: true
            });

            tooltip.tooltip('show');
                    
            this.$tip = tooltip.data('tooltip').$tip.on('click', '.actionbarbutton', this.onActionClick.bind(this));
            this.updatePosition = _.throttle(this[this.attr.alignTo + 'UpdatePosition'].bind(this), 100);

            this[this.attr.alignTo + 'Initializer']();
            this.updatePosition();

            this.on('click', this.teardown);
        });


        this.onActionClick = function(event) {
            var $target = $(event.target).blur();

            this.trigger($target.data('event'));
            this.teardown();
        };

        this.textselectionUpdatePosition = function() {
            var selection = getSelection(),
                range = selection.rangeCount > 0 && selection.getRangeAt(0);

            if (range) {
                var boundingBox = range.getBoundingClientRect();
                this.$tip.css({
                    left: boundingBox.left + boundingBox.width - this.$tip.width() / 2,
                    top: boundingBox.top + boundingBox.height
                });
            } else this.teardown();
        };

        this.textselectionInitializer = function() {
            var selection = getSelection(),
                anchor = $(selection.anchorNode);

            // Reposition on scroll events
            this.scrollParent = anchor.scrollParent().off('.actionbar').on('scroll.actionbar', this.updatePosition);
        };
    }
});
