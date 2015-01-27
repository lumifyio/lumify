define([], function() {
    'use strict';

    return withPropertyInfo;

    function withPropertyInfo() {

        this.showPropertyInfo = function(button, data, property) {
            var $target = $(button),
                shouldOpen = $target.lookupAllComponents().length === 0;

            require(['util/popovers/propertyInfo/propertyInfo'], function(PropertyInfo) {
                if (shouldOpen) {
                    PropertyInfo.teardownAll();
                    PropertyInfo.attachTo($target, {
                        data: data,
                        property: property
                    });
                } else {
                    $target.teardownComponent(PropertyInfo);
                }
            });
        };

        this.hidePropertyInfo = function(button) {
            var $target = $(button);

            require(['util/popovers/propertyInfo/propertyInfo'], function(PropertyInfo) {
                $target.teardownComponent(PropertyInfo);
            });
        }

    }
});
