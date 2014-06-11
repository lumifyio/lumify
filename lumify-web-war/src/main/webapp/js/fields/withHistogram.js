define([
    './histogram/histogram',
    'hbs!./withHistogramToggle'
], function(
    Histogram,
    template) {
    'use strict';

    return withHistogram;

    function withHistogram() {

        this.defaultAttrs({
            segmentedControlSelector: '.segmented-control button'
        });

        this.after('initialize', function() {
            if (this.attr.predicates) {
                this.$node.prepend(template({}));

                this.on('click', {
                    segmentedControlSelector: this.onChangeView
                });

                Histogram.attachTo(this.$node.children('.histogram'), {
                    property: this.attr.property
                });
            }
        });

        this.onChangeView = function(event) {
            var $target = $(event.target).closest('button');

            if ($target.hasClass('active')) {
                return;
            }

            $target.closest('button').addClass('active').find('span').addClass('icon-white').end()
                .siblings('.active').removeClass('active').find('span').removeClass('icon-white');

            this.$node.toggleClass('alternate');
        };

    }
});
