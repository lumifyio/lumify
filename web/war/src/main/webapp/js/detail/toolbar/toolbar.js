define([
    'flight/lib/component',
    'hbs!./template'
], function(
    defineComponent,
    template) {
    'use strict';

    var DIVIDER = {
            divider: true
        };

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
                            subtitle: 'Open in New Window / Tab',
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
                            event: 'addImage',
                            options: {
                                fileSelector: true
                            }
                        }/*, TODO: implement add to graph
                        DIVIDER,
                        {
                            title: 'To Workspace',
                            subtitle: 'Add Entity to Workspace',
                            event: 'addToGraph'
                        }
                        */
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

            if (eventName && $(event.target).is('input[type=file')) {
                $(event.target).one('change', function(e) {
                    if (e.target.files && e.target.files.length) {
                        self.trigger(eventName, {
                            files: e.target.files
                        })
                    }
                })
                this.hideMenu();
                return;
            }

            if (eventName) {
                event.preventDefault();
                event.stopPropagation();

                _.defer(function() {
                    self.trigger(eventName);
                });

                this.hideMenu();
            }
        };

        this.hideMenu = function() {
            var node = this.$node.addClass('hideSubmenus');
            _.delay(function() {
                node.removeClass('hideSubmenus');
            }, 500);
        };
    }
});
