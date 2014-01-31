Plugins
=======

Override any plugins in this directory in `/opt/lumify/config/*.properties`

    web.plugins.[plugin name]=/path/to/plugin/directory

Plugin directory must have at least one file
  
    [plugin name].js

For example, to override visibility:

    # /opt/lumify/config/application.properties
    web.plugins.visibility=/opt/lumify/plugins/visibility


    > ls /opt/lumify/plugins/visibility
  
    visibility.js
    form.ejs
    style.css

Root JavaScript file must use requirejs, and flightjs. Any other `html, js, css, ejs` files in plugin directory will be served at path:

    /jsc/configuration/plugins/[plugin name]/*

For example, 

    /jsc/configuration/plugins/visibility/form.ejs



JavaScript Requirements
-----------------------

Minimal plugin scaffold.

    define(['flight/lib/component'], function(defineComponent) {

        return defineComponent(Plugin);

        function Plugin() {
        }
    });
