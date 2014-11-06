define([], function() {
    'use strict';

    return withObjectSelection;

    function withObjectSelection() {

        var selectedObjects = {},
            previousSelectedObjects = {};

        this.after('initialize', function() {
            this.on('selectObjects', this.onSelectObjects);
        });

        this.onSelectObjects = function(event, data) {
            if (data && data.remoteEvent) return;

            var self = this,
                vertices = data && data.vertices || [],
                needsLoading = _.chain(vertices)
                    .filter(function(v) {
                        return _.isEqual(v, { id: v.id }) && _.isUndefined(self.vertex(v.id));
                    })
                    .value(),
                deferred = $.Deferred();

            if (needsLoading.length) {
                this.vertexService.getMultiple(_.pluck(needsLoading, 'id'))
                    .done(function() {
                        deferred.resolve();
                    });
            } else {
                deferred.resolve();
            }

            deferred.done(function() {
                var selectedIds = _.pluck(vertices, 'id'),
                    loadedVertices = vertices.map(function(v) {
                        return self.vertex(v.id) || v;
                    }),
                    selected = _.groupBy(loadedVertices, function(v) {
                        return v.concept ? 'vertices' : 'edges';
                    });

                if ((!data || !data.options || data.options.forceSelectEvenIfSame !== true) &&
                    _.isArray(self.previousSelection) &&
                    _.isArray(selectedIds) &&
                    _.isEqual(self.previousSelection, selectedIds)) {
                    return;
                }
                self.previousSelection = selectedIds;

                selected.vertices = selected.vertices || [];
                selected.edges = selected.edges || [];

                if (window.DEBUG) {
                    DEBUG.selectedObjects = selected;
                }

                if (selected.vertices.length) {
                    self.trigger('clipboardSet', {
                        text: F.vertexUrl.url(selected.vertices, self.workspaceId)
                    });
                } else {
                    self.trigger('clipboardClear');
                }

                self.selectedVertices = selected.vertices;
                self.selectedVertexIds = _.pluck(selected.vertices, 'id');
                self.selectedEdges = selected.edges;

                _.keys(self.workspaceVertices).forEach(function(id) {
                    var info = self.workspaceVertices[id];
                    info.selected = selectedIds.indexOf(id) >= 0;
                });

                $.extend(selected, _.pick(data || {}, 'focus'));

                self.trigger('objectsSelected', selected);
            })
        };
    }
});
