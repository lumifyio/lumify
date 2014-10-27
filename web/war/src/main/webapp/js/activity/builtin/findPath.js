define([
    'flight/lib/component',
    'service/longRunningProcess',
    'util/formatters'
], function(
    defineComponent,
    LongRunningProcessService,
    F) {
    'use strict';

    var longRunningProcessService = new LongRunningProcessService();

    return defineComponent(FindPath);

    function FindPath() {
        this.after('teardown', function() {
            this.$node.empty();
        });

        this.after('initialize', function() {
            var count = this.attr.process.resultsCount || 0;

            this.$node.html(
                '<button class="btn btn-mini btn-primary"' +
                (count === 0 ? ' disabled' : '') + '>' +
                (count === 0 ? 'No Entities' : ('Add ' + F.number.pretty(count) + ' Entities')) +
                '</button>'
            );
        });
    }
});
