define([
    'flight/lib/component',
    'configuration/admin/plugin',
    'hbs!./template',
    'tpl!util/alert',
    'd3'
], function(
    defineComponent,
    lumifyAdminPlugins,
    template,
    alertTemplate,
    d3) {
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
            this.on('click', {
                pluginItemSelector: this.onClickPluginItem
            });
            this.$node.html(template({}));
            this.update();
        });

        this.onClickPluginItem = function(event) {
            event.preventDefault();
            this.trigger('showAdminPlugin', $(event.target).closest('li').data('component'));
        };

        this.onShowAdminPlugin = function(event, data) {
            var self = this;

            // TODO: toggleMenubar display if not visible
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
                    }).show().find('.content');
                component = _.findWhere(lumifyAdminPlugins.ALL_COMPONENTS, data);

            form.teardownAllComponents();
            component.Component.attachTo(form);

            this.trigger(document, 'paneResized');
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

            d3.select(this.select('listSelector').get(0))
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
                                d3.select(this).append('a').attr('href', '#');
                            }
                        });

                    this.each(function(component) {
                        if (_.isString(component)) {
                            this.textContent = component;
                            return;
                        }

                        d3.select(this)
                            .attr('data-component', JSON.stringify(_.pick(component, 'section', 'name')))
                            .select('a')
                            .text(component.name)
                            .append('div')
                                .attr('class', 'subtitle')
                                .attr('title', component.subtitle)
                                .text(component.subtitle)
                    });
                })
                .exit().remove();

            if (items.length === 0) {
                this.$node.prepend(alertTemplate({
                    warning: i18n('admin.plugins.none_available')
                }));
            } else {
                this.$node.children('.alert').remove();
            }
        }
    }
});
