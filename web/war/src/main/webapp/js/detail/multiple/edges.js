define([
    'flight/lib/component',
    'flight/lib/registry',
    'tpl!./edges',
    '../withTypeContent',
    'util/edge/list',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    registry,
    template,
    withTypeContent,
    EdgeList,
    F,
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

            Promise.all([
                Promise.require('d3'),
                this.dataRequest('edge', 'store', { edgeIds: edgeIds }),
                this.dataRequest('ontology', 'ontology')
            ]).done(function(results) {
                var _d3 = results.shift(),
                    edges = results.shift(),
                    ontology = results.shift(),
                    concepts = ontology.concepts,
                    properties = ontology.properties,
                    relationships = ontology.relationships;
                    ontologyRelation = relationships.byTitle[edges[0].label];

                d3 = _d3;

                self.$node.html(template({
                    F: F,
                    label: ontologyRelation && ontologyRelation.displayName || '',
                    edges: edges
                }));

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
