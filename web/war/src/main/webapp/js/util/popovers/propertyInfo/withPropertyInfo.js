define([], function() {
    'use strict';

    return withPropertyInfo;

    function withPropertyInfo() {

        this.showPropertyInfo = function(button, vertexId, property) {
            var $target = $(button),
                shouldOpen = $target.lookupAllComponents().length === 0;

            require(['util/popovers/propertyInfo/propertyInfo'], function(PropertyInfo) {
                if (shouldOpen) {
                    PropertyInfo.teardownAll();
                    PropertyInfo.attachTo($target, {
                        property: property,
                        vertexId: vertexId
                    });
                } else {
                    $target.teardownComponent(PropertyInfo);
                }
            });
        };

    }
});
