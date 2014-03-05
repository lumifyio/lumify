
define([
    'flight/lib/component',
    'tpl!./diff'
], function(defineComponent, template) {
    'use strict';

    return defineComponent(Diff);

    function Diff() {

        this.after('initialize', function() {
            this.$node.html(template({diffs:this.attr.diffs}));

            this.on('diffsChanged', function(event, data) {
                this.$node.html(template({
                    diffs: data.diffs
                }));
            })
        });
    }
});

