/**
 * Base plugin file that defines a lumify admin ui plugin.
 */
define([
    'flight/lib/component',
    'flight/lib/registry',
    'service/admin',
    'tpl!util/alert',
    'util/formatters',
    'util/handlebars/helpers'
], function(defineComponent,
    registry,
    AdminService,
    alertTemplate,
    F) {
    'use strict';

    var NODE_CLS_FOR_LESS_CONTAINMENT = 'admin_less_cls_',
        adminService = new AdminService(),
        componentInc = 0;

    defineLumifyAdminPlugin.ALL_COMPONENTS = [];

    return defineLumifyAdminPlugin;

    function defineLumifyAdminPlugin(Component, options) {

        var FlightComponent = defineComponent(Component),
            attachTo = FlightComponent.attachTo,
            cls = NODE_CLS_FOR_LESS_CONTAINMENT + componentInc;

        if (options && options.less) {
            options.less.applyStyleForClass(cls);
        }

        FlightComponent.attachTo = function attachToWithLessClass(selector) {
            attachTo.apply(this, arguments);

            $(selector).each(function() {
                var component = $(this).addClass(cls).lookupComponent(FlightComponent);

                component.adminService = adminService;

                component.showSuccess = function(message) {
                    this.$node.find('.alert').remove();
                    this.$node.prepend(alertTemplate({ info: message || i18n('admin.plugin.success') }));
                };
                component.showError = function(message) {
                    this.$node.find('.alert').remove();
                    this.$node.prepend(alertTemplate({ error: message || i18n('admin.plugin.error') }));
                };
                component.handleSubmitButton = function(button, promise) {
                    var text = button.text();

                    button.attr('disabled', true);

                    return promise
                        .progress(function(v) {
                            button.text(F.number.percent(v) + ' ' + text);
                        })
                        .always(function() {
                            button.removeAttr('disabled').text(text);
                        });
                };
            });
        }

        componentInc++;

        defineLumifyAdminPlugin.ALL_COMPONENTS.push(
            $.extend({},
                _.pick(options || {}, 'section', 'name', 'subtitle'),
                {
                    Component: FlightComponent
                }
            )
        );

        return FlightComponent;
    }
});
