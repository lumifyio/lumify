define([
    'flight/lib/component',
    'configuration/plugins/graphOptions/plugin'
], function(
    defineComponent,
    GraphOptionsPlugin) {
    'use strict';

    return defineComponent(GraphOptions);

    function GraphOptions() {

        this.after('initialize', function() {
            var self = this,
                $options = $(),
                components = GraphOptionsPlugin.options.map(function(option) {
                    return Promise.require(option.optionComponentPath);
                });

            if (!components.length) {
                return;
            }

            self.attr.cy.done(function(cy) {
                Promise.all(components).done(function(Components) {
                    Components.forEach(function(Component) {
                        var $node = $('<li>');
                        $options = $options.add($node);
                        Component.attachTo($node, {
                            cy: cy
                        });
                    });
                    $('<ul>')
                        .append($options)
                        .appendTo(
                            self.$node.empty()
                        )
                });
            });

        });

    }
});
