# Lumify Frontend

## Setup

    > npm install -g grunt-cli bower inherits

    # Install node build dependencies
    > npm install

    # Install JavaScript browser dependencies
    > grunt deps

    # Compile less, js and watch directory
    > grunt

## Helpful Global Functions

These are some developer helper functions. Run these commands in the browser console.

### LiveReload

Have the browser auto refresh when changes are made. This is remembered in local storage so, it only needs to be run once to enable. `grunt` must be watching.

        enableLiveReload(true); // to enable (refresh browser once to start)

        enableLiveReload(false); // to disable

### Switch Language

Test changing the language. Sets a localStorage token and reloads the page while loading appropriate resources. Useful for checking the UI with different size text.

        switchLanguage('de') // Accepts language or language and country with "_". Ex: en_us


### Component Highlighter

Overlays component name using mouseover events. Useful for checking what component is responsible for what on the page.

        enableComponentHighlighting(true); // Display component overlays

        enableComponentHighlighting(false); // Disable component overlays

### Gremlins

Randomly click in the UI for some period of time. Useful for checking to see if UI can break with errant and excessive clicking.

        gremlins();


## Extensibility

For more extensibility options, see README in the `js/configuration/plugins` directories.

### Localization

All strings are loaded from `MessageBundle.properties`. Extend / replace strings using a web plugin that defines / overrides strings in another bundle.

For example:
        
        visibility.label=Classification
        visibility.blank=Unclassified

Translate message keys to current locale value using `i18n` JavaScript function in global scope.

        i18n("visibility.label")
        // returns "Classification"

The translation function also supports interpolation

        // MessageBundle.properties
        my.property=The {0} brown fox {1} over the lazy dog

        // JavaScript plugin
        i18n("my.property", "quick", "jumps");
        // returns "The quick brown fox jumps over the lazy dog"

### Property Info Metadata

Configure properties in `/opt/lumify`


       properties.metadata.propertyNames: Lists metadata properties to display in popover
       properties.metadata.propertyNameDisplay: Lists metadata display name keys (MessageBundle.properties)
       properties.metadata.propertyNamesType: Lists metadata types to format values.

Metadata Types: `timezone`, `datetime`, `user`, `sandboxStatus`, `percent`

To add a new type:

1. Create a web plugin
2. Extend the formatter with custom type(s). For example, pluralize and translate. 

        require(['util/vertex/formatters'], function(F) {
            $.extend(F.vertex.metadata, {
                pluralize: function(el, value) {
                    el.textContent = value + 's';
                },

                // Suffix name with "Async" and return a promise
                translateAsync: function(el, value) {
                    var translationPromise = $.Deferred();
                    $.get('/translateService', { string:value })
                        .done(function(result) {
                            el.textContent = result;
                            translationPromise.resolve();
                        })

                    return translationPromise.promise();
                }
            })
        })


### Ontology Property Data Types

Allows custom DOM per ontology data type.

1. Create a web plugin and extend / override formatters.

        require(['util/vertex/formatters'], function(F) {
            $.extend(F.vertex.properties, {

                // Will be executed for properties that have dataType='link'
                link: function(domElement, property, vertexId) {
                    $('<a>')
                        .attr('href', property.value)
                        .text(i18n('properties.link.label'))
                        .appendTo(domElement);
                },

                visibility: function(el, property) {
                    $('<i>')
                        .text(property.value || i18n('visibility.blank'))
                        .appendTo(el);
                }


            })
        })

