
define([], function() {
    'use strict';

    var EVENT_NAME = 'componentInitialized';

    return withTeardown;

    /**
     * Mixin that broadcasts initialization to parent nodes components
     * and will teardown all child components
     */
    function withTeardown() {

        this.after('initialize', function() {
            this.trigger(EVENT_NAME);

            this.childComponents = [];
            this.on(EVENT_NAME, this.onComponentInitialized);
        });

        this.onComponentInitialized = function(event) {
            event.stopPropagation();

            if (event.target !== this.node) {
                this.childComponents.push(event.target);
            }
        };

        this.before('teardown', function() {
            this.childComponents.forEach(function(c) {
                $(c).teardownAllComponents();
            });
        });
    }

});
