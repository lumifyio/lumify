define([
    'flight/lib/component',
    'flight/lib/registry',
    'tpl!./edges',
    '../withTypeContent',
    'util/edge/list',
    'util/withDataRequest'
], function(
    defineComponent,
    registry,
    template,
    withTypeContent,
    EdgeList,
    withDataRequest) {
    'use strict';

    return defineComponent(MultipleEdges, withTypeContent, withDataRequest);

    function MultipleEdges() {
        var d3;

        this.defaultAttrs({
            edgeListSelector: '.edges-list'
        });

        this.after('initialize', function() {
            var self = this,
                edges = this.attr.data.edges || [],
                edgeIds = _.pluck(edges, 'id');

            this.displayingIds = edgeIds;

            this.$node.html(template({
                edges: edges
            }));

            Promise.all([
                Promise.require('d3'),
                this.dataRequest('edge', 'store', { edgeIds: edgeIds }),
                this.dataRequest('ontology', 'concepts'),
                this.dataRequest('ontology', 'properties')
            ]).done(function(results) {
                var _d3 = results.shift(),
                    edges = results.shift(),
                    concepts = results.shift(),
                    properties = results.shift();

                d3 = _d3;

                EdgeList.attachTo(self.select('edgeListSelector'), {
                    edges: edges
                })
            });

        });

        this.onSelectObjects = function(event, data) {
            event.stopPropagation();
            this.$node.find('.edges-list').hide();
            this.$node.find('.multiple').addClass('viewing-vertex');
        };

    }
});
