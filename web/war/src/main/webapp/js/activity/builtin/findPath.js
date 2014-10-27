define([
    'flight/lib/component',
    'service/longRunningProcess'
], function(
    defineComponent,
    LongRunningProcessService) {
    'use strict';

    var longRunningProcessService = new LongRunningProcessService();

    return defineComponent(FindPath);

    function FindPath() {
        this.after('teardown', function() {
            this.$node.empty();
        });

        this.after('initialize', function() {
            if (_.isNumber(this.attr.process.resultsCount)) {
                this.$node.html('<button class="btn btn-mini btn-primary">Add ' +
                                this.attr.process.resultsCount +
                                ' Vertices</button>');
            }
        });
    }
});
