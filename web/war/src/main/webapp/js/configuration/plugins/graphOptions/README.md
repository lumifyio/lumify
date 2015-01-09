Graph Options Plugin
=================

Plugin to add custom options components which display in the graph options menu (next to Fit).

To register an option:

        require(['configuration/plugins/graphOptions/plugin'], function(GraphOptionsPlugin) {

            // Define a custom Flight component
            define('myplugins/hello_world', ['flight/lib/component'], function(defineComponent) {
                return defineComponent(HelloWorld);

                function HelloWorld() {
                    this.after('initialize', function() {
                        this.$node.html('Hello World!!');
                    })
                }
            });

            // Register the component path,
            GraphViewOption.registerGraphOption('myplugins/hello_world', 'hello-world-example');
        });

Graph options can access the `cy` (cytoscape) object using `this.attr.cy`
