# Lumify Frontend

## Setup

    > npm install -g grunt-cli bower inherits

    # Install node build dependencies
    > npm install

    # Install JavaScript browser dependencies
    > grunt deps

    # Compile less, js and watch directory
    > grunt

## Extensibility

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
                pluralize: function(value) {
                    return value + 's';
                },

                // Suffix name with "Async" and return a promise
                translateAsync: function(value) {
                    var translationPromise = $.Deferred();
                    $.get('/translateService', { string:value })
                        .done(function(result) {
                            translationPromise.resolve(result);
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

