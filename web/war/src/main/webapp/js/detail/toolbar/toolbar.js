define([
    'flight/lib/component',
    'hbs!./template'
], function(
    defineComponent,
    template) {
    'use strict';

    return defineComponent(Toolbar);

    function Toolbar() {
        this.defaultAttrs({
            toolbarItemSelector: 'li',
            toolbar: [
                {
                    title: 'Open',
                    submenu: [
                        {
                            title: 'Fullscreen',
                            subtitle: 'Open Entity in new Fullscreen Window',
                            event: 'openFullscreen'
                        }
                    ]
                },
                {
                    title: 'Add',
                    submenu: [
                        {
                            title: 'Property',
                            subtitle: 'Add New Property to Entity',
                            event: 'addNewProperty'
                        },
                        {
                            title: 'Image',
                            subtitle: 'Upload an Image for Entity',
                            event: 'addProperty'
                        }
                    ]
                },
                { title: 'Audit', right: true }
            ]
        })

        this.after('initialize', function() {
            this.on('click', {
                toolbarItemSelector: this.onToolbarItem
            })
            this.$node.html(template(this.attr));
        });

        this.onToolbarItem = function(event) {
            var self = this,
                $target = $(event.target).closest('li'),
                eventName = $target.data('event');

            if (eventName) {
                event.preventDefault();
                event.stopPropagation();

                var node = this.$node.addClass('hideSubmenus');
                _.defer(function() {
                    self.trigger(eventName);
                });
                _.delay(function() {
                    node.removeClass('hideSubmenus');
                }, 500);
            }
        };
    }
});
