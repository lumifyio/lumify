define([
    'flight/lib/component',
    'd3'
], function(
    defineComponent,
    d3) {
    'use strict';

    return defineComponent(EdgeList);

    function EdgeList() {

        this.after('initialize', function() {
            console.log(this.attr)
            d3.select(this.node)
                .classed({
                    'vertices-list': true,
                    'vertex-list': true
                })
                .append('ul').attr('class', 'nav nav-list')
                .selectAll('li.edge')
                .data(this.attr.edges)
                .call(function() {
                    this.enter()
                        .append('li').attr('class', 'edge')
                        .append('a')

                    this.select('a').text(function(d) {
                        return d.properties[0].value.justificationText;
                    });

                    this.exit().remove();
                })
        });

    }
});
