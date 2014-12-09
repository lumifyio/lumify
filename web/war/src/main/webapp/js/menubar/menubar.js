
define([
    'flight/lib/component',
    'configuration/plugins/menubar/plugin',
    './activity/activity',
    'tpl!./menubar'
], function(defineComponent, menubarPlugin, Activity, template) {
    'use strict';

    // Add class name of <li> buttons here
    var BUTTONS = 'dashboard graph map search workspaces admin activity logout'.split(' '),
        TOOLTIPS = {
            dashboard: i18n('menubar.icons.dashboard.tooltip'),
            graph: { html: i18n('menubar.icons.graph') +
                ' <span class="subtitle">' +
                i18n('menubar.icons.graph.tooltip.suffix') + '</span>' },
            map: i18n('menubar.icons.map.tooltip'),
            search: i18n('menubar.icons.search.tooltip'),
            workspaces: i18n('menubar.icons.workspaces.tooltip'),
            admin: i18n('menubar.icons.admin.tooltip'),
            logout: i18n('menubar.icons.logout.tooltip')
        },

        // Which cannot both be active
        MUTALLY_EXCLUSIVE_SWITCHES = [
            { names: ['dashboard', 'graph','map'], options: { allowCollapse: false } },
            { names: ['workspaces', 'search', 'admin'], options: { } }
        ],

        ACTION_TYPES = {
            full: MUTALLY_EXCLUSIVE_SWITCHES[0],
            pane: MUTALLY_EXCLUSIVE_SWITCHES[1]
        },

        // Don't change state to highlighted on click
        DISABLE_ACTIVE_SWITCH = 'logout'.split(' '),

        DISABLE_HIDE_TOOLTIP_ON_CLICK = 'logout'.split(' ');

    return defineComponent(Menubar);

    function nameToI18N(name) {
        return i18n('menubar.icons.' + name);
    }

    function menubarItemHandler(name) {
        var sel = name + 'IconSelector';

        return function(e) {
            e.preventDefault();

            var self = this,
                isSwitch = false;

            if (DISABLE_ACTIVE_SWITCH.indexOf(name) === -1) {
                MUTALLY_EXCLUSIVE_SWITCHES.forEach(function(exclusive, i) {
                    if (exclusive.names.indexOf(name) !== -1 && exclusive.options.allowCollapse === false) {
                        isSwitch = true;
                    }
                });
            }
            var icon = this.select(sel);
            if (!_.contains(DISABLE_HIDE_TOOLTIP_ON_CLICK, name)) {
                icon.tooltip('hide');
            }
            if (isSwitch && icon.hasClass('active')) {

                icon.toggleClass('toggled');

                // Special case to toggle 2d/3d graph
                if (name === 'graph') {
                    requestAnimationFrame(function() {
                        self.trigger(document, 'toggleGraphDimensions');
                    });
                }
                return;
            } else {
                requestAnimationFrame(function() {
                    var data = { name: name };
                    if (name in self.extensions) {
                        data.action = self.extensions[name].action;
                    }
                    self.trigger(document, 'menubarToggleDisplay', data);
                });
            }
        };
    }

    function Menubar() {

        this.activities = 0;

        var attrs = {}, events = {};
        BUTTONS.forEach(function(name) {
            var sel = name + 'IconSelector';

            attrs[sel] = '.' + name;
            events[sel] = menubarItemHandler(name);
        });

        var self = this,
            extensionCount = 0,
            extensions = {};

        menubarPlugin.items.forEach(function(data) {
            var cls = 'extension-' + extensionCount++,
                type = data.action.type;

            if (type in ACTION_TYPES) {
                ACTION_TYPES[type].names.push(cls);
            }

            extensions[cls] = data;
            attrs[cls + 'IconSelector'] = '.' + cls;
            events[cls + 'IconSelector'] = menubarItemHandler(cls);
        });

        this.defaultAttrs(attrs);

        this.after('initialize', function() {
            var self = this;

            this.$node.html(template({}));
            this.extensions = extensions;

            _.each(this.extensions, function(item, cls) {
                var options = $.extend({
                        placementHint: 'top',
                        tooltip: item.title,
                        anchorCss: {}
                    }, item.options);

                $('<li>')
                    .addClass(cls)
                    .append(
                        $('<a>')
                        .text(item.title)
                        .css(
                            $.extend({
                                'background-image': item.icon
                            }, options.anchorCss)
                        )
                    )
                    .insertBefore(self.$node.find('.menu-' + options.placementHint + ' .divider:last-child'));
            })

            Object.keys(TOOLTIPS).forEach(function(selectorClass) {
                self.$node.find('.' + selectorClass).tooltip({
                    placement: 'right',
                    html: true,
                    title: (TOOLTIPS[selectorClass].html || TOOLTIPS[selectorClass]).replace(/\s+/g, '&nbsp;'),
                    delay: { show: 250, hide: 0 }
                });
            });

            this.on('click', events);

            Activity.attachTo(this.select('activityIconSelector'));

            this.on(document, 'menubarToggleDisplay', this.onMenubarToggle);
        });

        this.onMenubarToggle = function(e, data) {
            var self = this,
                icon = this.select(data.name + 'IconSelector'),
                active = icon.hasClass('active');

            if (DISABLE_ACTIVE_SWITCH.indexOf(data.name) === -1) {
                var isSwitch = false;

                if (!active) {
                    MUTALLY_EXCLUSIVE_SWITCHES.forEach(function(exclusive, i) {
                        if (exclusive.names.indexOf(data.name) !== -1) {
                            isSwitch = true;
                                exclusive.names.forEach(function(name) {
                                    if (name !== data.name) {
                                        var otherIcon = self.select(name + 'IconSelector');
                                        if (otherIcon.hasClass('active')) {
                                            self.trigger(document, 'menubarToggleDisplay', {
                                                name: name,
                                                isSwitchButCollapse: true
                                            });
                                        }
                                    } else icon.addClass('active');
                                });
                        }
                    });
                }

                if (!isSwitch || data.isSwitchButCollapse) {
                    icon.toggleClass('active');
                }

            } else {

                // Just highlight briefly to show click worked
                icon.addClass('active');
                setTimeout(function() {
                    icon.removeClass('active');
                }, 200);
            }
        };
    }
});
