define([
    'flight/lib/component',
    'hbs!./viewerTpl',
    'util/withDataRequest',
    'util/vertex/formatters'
], function(defineComponent, template, withDataRequest, F) {
    'use strict';

    return defineComponent(JustificationViewer, withDataRequest);

    function JustificationViewer() {

        this.defaultAttrs({
            sourceInfoTitleSelector: '.sourceInfoTitle',
        })

        this.after('initialize', function() {
            var self = this;

            this.$node.html(
                template(_.pick(this.attr, 'justificationMetadata', 'sourceMetadata'))
            );

            this.on('click', {
                sourceInfoTitleSelector: this.onSourceInfo
            });

            if (this.attr.sourceMetadata) {
                this.dataRequest('vertex', 'store', { vertexIds: this.attr.sourceMetadata.vertexId })
                    .done(function(vertex) {
                        self.select('sourceInfoTitleSelector').text(F.vertex.title(vertex));
                        self.trigger('positionDialog');
                    });
            }
        });

        this.onSourceInfo = function(e) {
            e.preventDefault();

            var metadata = this.attr.sourceMetadata,
                vertexId = metadata.vertexId,
                textPropertyKey = metadata.textPropertyKey,
                offsets = [metadata.startOffset, metadata.endOffset];

            this.trigger('selectObjects', {
                vertexIds: [vertexId],
                focus: {
                    vertexId: vertexId,
                    textPropertyKey: textPropertyKey,
                    offsets: offsets
                }
            })
        };
    }
});
