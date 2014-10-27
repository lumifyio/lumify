
define([
    'flight/lib/component',
    './withVertexPopover',
    'util/formatters',
    'util/withFormFieldErrors',
    'service/vertex',
    'data'
], function(
    defineComponent,
    withVertexPopover,
    F,
    withFormFieldErrors,
    VertexService,
    appData) {
    'use strict';

    return defineComponent(FindPathPopover, withVertexPopover, withFormFieldErrors);

    function FindPathPopover() {

        this.vertexService = new VertexService();

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
                        buttons = self.popover.find('button').attr('disabled', true);

                    self.clearFieldErrors(this.popover);
                    this.vertexService.findPath(parameters)
                        .done(function() {
                            self.teardown();
                            self.trigger('showActivityDisplay');
                        })
                        .fail(function() {
                            buttons.removeAttr('disabled');
                            $target.removeClass('loading');
                            self.markFieldErrors(i18n('popovers.find_path.error'), self.popover);
                        })
                }
            })
        };
    }
});
