define([], function() {
    'use strict';

    return withCollapsibleSections;

    function withCollapsibleSections() {

        this.defaultAttrs({
            collapsibleSectionSelector: '.collapsible .collapsible-header'
        });

        this.after('initialize', function() {
            this.on('click', {
                collapsibleSectionSelector: this.onToggleCollapsibleSection
            });
        });

        this.onToggleCollapsibleSection = function(event) {
            $(event.target).closest('.collapsible').toggleClass('expanded');
        }
    }
});
