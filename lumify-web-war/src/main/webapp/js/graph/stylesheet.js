
define([
    'cytoscape',
    'service/ontology',
    'util/retina'
], function(cytoscape, OntologyService, retina) {
    'use strict';

    var ontologyService = new OntologyService(),
        style = cytoscape.stylesheet();

    return load;

    function apply(concept) {
        style.selector('.concept-' + concept.id)
             .css({
                 'background-image': concept.glyphIconHref
             });

        if (concept.children) {
            concept.children.forEach(apply);
        }
    }

    function defaultStyle() {
        style
            .selector('node')
            .css({
                'width': 30 * retina.devicePixelRatio,
                'height': 30 * retina.devicePixelRatio,
                'content': 'data(truncatedTitle)',
                'font-family': 'helvetica',
                'font-size': 18 * retina.devicePixelRatio,
                'text-outline-width': 2,
                'text-outline-color': 'white',
                'text-valign': 'bottom',
                'color': '#999',
                'shape': 'roundrectangle'
            })

            // TODO: make shape movieStrip if video type

            .selector('node.hasCustomGlyph')
            .css({
                'width': 60 * retina.devicePixelRatio,
                'height': 60 * retina.devicePixelRatio,
                'background-image': 'data(_glyphIconUri)'
            })

            .selector('node.hover')
            .css({
                'opacity': 0.6
            })

            .selector('node.focus')
            .css({
                'border-width': 5 * retina.devicePixelRatio,
                'border-color': '#a5e1ff',
                'color': '#00547e',
                'font-weight': 'bold',
                'font-size': 20 * retina.devicePixelRatio
            })

            .selector('node.temp')
            .css({
                'background-color': 'rgba(255,255,255,0.0)',
                'width': '1',
                'height': '1'
            })

            .selector('node.controlDragSelection')
            .css({
                'border-width': 5 * retina.devicePixelRatio,
                'border-color': '#a5e1ff'
            })

            .selector('node:selected')
            .css({
                'background-color': '#0088cc',
                'border-color': '#0088cc',
                'color': '#0088cc'
            })

            .selector('edge:selected')
            .css({
                'line-color': '#0088cc',
                'color': '#0088cc',
                'target-arrow-color': '#0088cc',
                'source-arrow-color': '#0088cc',
                'width': 4 * retina.devicePixelRatio
            })

            .selector('edge')
            .css({
                'width': 1.5 * retina.devicePixelRatio,
                'target-arrow-shape': 'triangle'
            })

            .selector('edge.label')
                .css({
                'content': 'data(label)',
                'font-size': 12 * retina.devicePixelRatio,
                'color': '#0088cc',
                'text-outline-color': 'white',
                'text-outline-width': 4,
            })

            .selector('edge.path-hidden-verts')
            .css({
                'line-style': 'dashed',
                'content': 'data(label)',
                'font-size': 16 * retina.devicePixelRatio,
                'color': 'data(pathColor)',
                'text-outline-color': 'white',
                'text-outline-width': 4,
            })

            .selector('edge.path-edge')
            .css({
                'line-color': 'data(pathColor)',
                'target-arrow-color': 'data(pathColor)',
                'source-arrow-color': 'data(pathColor)',
                'width': 4 * retina.devicePixelRatio,
            })

            .selector('edge.temp')
            .css({
                'width': 4,
                'line-color': '#0088cc',
                'line-style': 'dotted',
                'target-arrow-color': '#0088cc'
            });
    }

    function load(styleReady) {
        ontologyService.concepts(function(err, concepts) {
            if(concepts.entityConcept.children) {
                concepts.entityConcept.children.forEach(apply);
            }

            defaultStyle();

            styleReady(style);
        });
    }

});
