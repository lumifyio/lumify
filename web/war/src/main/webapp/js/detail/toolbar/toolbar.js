define([
    'flight/lib/component',
    'hbs!./template'
], function(
    defineComponent,
    template) {
    'use strict';

    var DIVIDER = {
            divider: true
        },
        ToolbarComponent = defineComponent(Toolbar);

    ToolbarComponent.ITEMS = {
        DIVIDER: DIVIDER,
        BACK: { title: '◀' },
        FORWARD: { title: '▶' },
        FULLSCREEN: {
            title: i18n('detail.toolbar.open.fullscreen'),
            cls: 'hide-in-fullscreen-details',
            subtitle: i18n('detail.toolbar.open.fullscreen.subtitle'), //'Open in New Window / Tab',
            event: 'openFullscreen'
        },
        ADD_PROPERTY: {
            title: i18n('detail.toolbar.add.property'),
            subtitle: i18n('detail.toolbar.add.property.subtitle'), // 'Add New Property to Entity',
            cls: 'requires-EDIT',
            event: 'addNewProperty'
        },
        ADD_IMAGE: {
            title: i18n('detail.toolbar.add.image'),
            subtitle: i18n('detail.toolbar.add.image.subtitle'), // 'Upload an Image for Entity',
            cls: 'requires-EDIT',
            event: 'addImage',
            options: {
                fileSelector: true
            }
        },
        ADD_COMMENT: {
            title: i18n('detail.toolbar.add.comment'),
            subtitle: i18n('detail.toolbar.add.comment.subtitle'), // 'Add New Comment to Entity',
            cls: 'requires-COMMENT',
            event: 'addNewComment'
        },
        AUDIT: {
            title: i18n('detail.toolbar.audit'),
            cls: 'audits',
            right: true,
            event: 'toggleAudit'
        }
    };

    return ToolbarComponent;

    function Toolbar() {
        this.defaultAttrs({
            toolbarItemSelector: 'li',
            toolbar: []
        })

        this.after('initialize', function() {
            this.on('click', {
                toolbarItemSelector: this.onToolbarItem
            })
            if (this.attr.toolbar.length) {
                this.$node.html(template(this.attr));
                this.$node.find('.has-submenu').each(function() {
                    var $this = $(this);
                        displayValues = $this.find('li').map(function() {
                            return $(this).css('display') === 'none';
                        }).toArray();

                    if (_.all(displayValues)) {
                        $this.hide();
                    }
                })
            } else {
                this.$node.hide();
            }
        });

        this.onToolbarItem = function(event) {
            var self = this,
                $target = $(event.target).closest('li'),
                eventName = $target.data('event'),
                eventData = $target.data('eventData');

            if (eventName && $(event.target).is('input[type=file]')) {
                $(event.target).one('change', function(e) {
                    if (e.target.files && e.target.files.length) {
                        self.trigger(eventName, $.extend({
                            files: e.target.files
                        }, eventData));
                    }
                })
                this.hideMenu();
                return;
            }

            if (eventName) {
                event.preventDefault();
                event.stopPropagation();

                _.defer(function() {
                    self.trigger(eventName, eventData);
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
