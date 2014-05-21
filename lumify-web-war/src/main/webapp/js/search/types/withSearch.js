define([
    'hbs!./templates/type'
], function(
    template
) {
    'use strict';

    return withSearch;

    function withSearch() {

        this.after('initialize', function() {
            this.render();
        });

        this.render = function() {
            this.$node.html(template({}));
        };

    }
});
