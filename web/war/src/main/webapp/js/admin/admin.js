define([
    'flight/lib/component',
    'configuration/admin/plugin',
    'hbs!./template',
    'tpl!util/alert',
    './bundled/pluginList/index',
    './bundled/notifications/index'
], function(
    defineComponent,
    lumifyAdminPlugins,
    template,
    alertTemplate) {
    'use strict';

    return defineComponent(AdminList);

    function AdminList() {

        this.defaultAttrs({
            listSelector: '.admin-list',
            pluginItemSelector: '.admin-list > li a',
            formSelector: '.admin-form'
        });

        this.after('initialize', function() {
            this.on(document, 'showAdminPlugin', this.onShowAdminPlugin);
            this.on(document, 'menubarToggleDisplay', this.onToggleDisplay);
            this.on('click', {
                pluginItemSelector: this.onClickPluginItem
            });
            this.$node.html(template({}));
            this.update();
        });

        this.onToggleDisplay = function(event, data) {
            if (data.name === 'admin' && this.$node.closest('.visible').length === 0) {
                this.$node.find('.admin-list .active').removeClass('active');
                this.select('formSelector')
                    .hide()
                    .find('.content')
                        .teardownAllComponents()
                        .removePrefixedClasses('admin_less_cls');
            }
        };

        this.onClickPluginItem = function(event) {
            event.preventDefault();
            this.trigger('showAdminPlugin', $(event.target).closest('li').data('component'));
        };

        this.onShowAdminPlugin = function(event, data) {
            var self = this;

            if (data && data.name && data.section) {
                data.name = data.name.toLowerCase();
                data.section = data.section.toLowerCase();
            }

            this.$node.find('li').filter(function() {
                return _.isEqual($(this).data('component'), data);
            }).addClass('active').siblings('.active').removeClass('active');

            var container = this.select('formSelector'),
                form = container.resizable({
                        handles: 'e',
                        minWidth: 120,
                        maxWidth: 500,
                        resize: function() {
                            self.trigger(document, 'paneResized');
                        }
                    }).show().find('.content'),
                component = _.find(lumifyAdminPlugins.ALL_COMPONENTS, function(c) {
                    return c.name.toLowerCase() === data.name &&
                        c.section.toLowerCase() === data.section;
                });

            form.teardownAllComponents()
                .removePrefixedClasses('admin_less_cls');
            component.Component.attachTo(form, data);

            this.trigger(container, 'paneResized');
        };

        this.update = function() {
            var self = this,
                items = [],
                lastSection;

            _.sortBy(lumifyAdminPlugins.ALL_COMPONENTS, function(component) {
                return component.section.toLowerCase() + component.name.toLowerCase();
            }).forEach(function(component) {
                if (lastSection !== component.section) {
                    items.push(component.section);
                    lastSection = component.section;
                }

                items.push(component);
            });

            require(['d3'], function(d3) {
                d3.select(self.select('listSelector').get(0))
                    .selectAll('li')
                    .data(items)
                    .call(function() {
                        this.enter().append('li')
                            .attr('class', function(component) {
                                if (_.isString(component)) {
                                    return 'nav-header';
                                }
                            }).each(function(component) {
                                if (!_.isString(component)) {
                                    d3.select(this).append('a');
                                }
                            });

                        this.each(function(component) {
                            if (_.isString(component)) {
                                this.textContent = component;
                                return;
                            }

                            d3.select(this)
                                .attr('data-component', JSON.stringify(
                                    _.chain(component)
                                    .pick('section', 'name')
                                    .tap(function(c) {
                                        c.name = c.name.toLowerCase();
                                        c.section = c.section.toLowerCase();
                                    }).value()
                                ))
                                .select('a')
                                .call(function() {
                                    this.append('div')
                                        .attr('class', 'nav-list-title')
                                        .text(component.name)

                                    this.append('div')
                                        .attr('class', 'nav-list-subtitle')
                                        .attr('title', component.subtitle)
                                        .text(component.subtitle)
                                });
                        });
                    })
                    .exit().remove();

                if (items.length === 0) {
                    self.$node.prepend(alertTemplate({
                        warning: i18n('admin.plugins.none_available')
                    }));
                } else {
                    self.$node.children('.alert').remove();
                }
            });
        }
    }
});
