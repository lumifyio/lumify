
define([
    'flight/lib/component',
    './withVertexPopover',
    'util/formatters',
    'util/withFormFieldErrors',
    'util/withDataRequest'
], function(
    defineComponent,
    withVertexPopover,
    F,
    withFormFieldErrors,
    withDataRequest) {
    'use strict';

    return defineComponent(FindPathPopover, withVertexPopover, withFormFieldErrors);

    function FindPathPopover() {

        this.defaultAttrs({
            buttonSelector: 'button',
        });

        this.after('teardown', function() {
            this.trigger('finishedVertexConnection');
        });

        this.before('initialize', function(node, config) {
            config.hops = config.connectionData && config.connectionData.hops || 2;
            config.template = 'findPathPopover';
        });

        this.popoverInitialize = function() {
            this.trigger('defocusPaths');

            var self = this;

            this.positionDialog();

            this.on(this.popover, 'click', {
                buttonSelector: function(event) {
                    var $target = $(event.target),
                        parameters = {
                            sourceGraphVertexId: this.attr.sourceVertexId,
                            destGraphVertexId: this.attr.targetVertexId,
                            depth: 5,
                            hops: $target.addClass('loading').data('hops')
                        },
                        buttons = this.popover.find('button').attr('disabled', true);

                    this.clearFieldErrors(this.popover);
                    this.dataRequest('vertex', 'findPath', parameters)
                        .then(function() {
                            self.teardown();
                            self.trigger('showActivityDisplay');
                        })
                        .catch(function() {
                            buttons.removeAttr('disabled');
                            $target.removeClass('loading');
                            self.markFieldErrors(i18n('popovers.find_path.error'), self.popover);
                        })
                }
            })
        };
    }
});
