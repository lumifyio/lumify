define([
    'flight/lib/component',
    'data',
    'hbs!./viewerTpl'
], function(defineComponent, appData, template) {
    'use strict';

    return defineComponent(JustificationViewer);

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
                appData.getVertexTitle(this.attr.sourceMetadata.vertexId)
                    .done(function(title) {
                        self.select('sourceInfoTitleSelector').text(title);
                    });
            }
        });

        this.onSourceInfo = function(e) {
            e.preventDefault();
            e.stopPropagation();

            var metadata = this.attr.sourceMetadata,
                vertexId = metadata.vertexId,
                offsets = [metadata.startOffset, metadata.endOffset];

            this.trigger('selectObjects', {
                vertices: [ { id: vertexId } ],
                focus: {
                    vertexId: vertexId,
                    offsets: offsets
                }
            })
        };
    };
});
