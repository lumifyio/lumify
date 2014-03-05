
define([
    'flight/lib/component',
    'tpl!./diff'
], function(defineComponent, template) {
    'use strict';

    return defineComponent(Diff);

    function Diff() {

        this.defaultAttrs({
            publishSelector: 'button'
        })

        this.after('initialize', function() {
            this.$node.html(template({diffs:this.attr.diffs}));

            this.on('click', {
                publishSelector: this.onPublish
            })
            this.on('diffsChanged', function(event, data) {
                this.$node.html(template({
                    diffs: data.diffs
                }));
            })
        });

        this.onPublish = function() {
            var $target = $(event.target),
                cls = 'btn-success';

            if ($target.closest('thead').length) {
                var allButtons = $target.closest('table').find('td button');
                if ($target.hasClass(cls)) {
                    allButtons.removeClass(cls);
                } else {
                    allButtons.addClass(cls);
                }
            }
            $target.toggleClass(cls)
        };
    }
});

