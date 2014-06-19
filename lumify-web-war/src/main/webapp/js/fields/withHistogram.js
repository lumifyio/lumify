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
            if (this.attr.predicates && this.attr.supportsHistogram) {
                this.$node.prepend(template({}));

                this.on('click', {
                    segmentedControlSelector: this.onChangeView
                });

                this.onUpdateHistogramExtent = _.throttle(this.onUpdateHistogramExtent.bind(this), 100);
                this.on('updateHistogramExtent', this.onUpdateHistogramExtent);

                Histogram.attachTo(this.$node.children('.histogram'), {
                    property: this.attr.property
                });
            }
        });

        this.onUpdateHistogramExtent = function(event, data) {
            this.$node.find('select.predicate')
                .val('range')
                .change();

            var val1 = data.extent && data.extent[0],
                val2 = data.extent && data.extent[1];

            this.setValues(val1, val2, {
                isScrubbing: true
            });
        };

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
