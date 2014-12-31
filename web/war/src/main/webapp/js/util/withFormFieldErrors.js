define(['tpl!./alert'], function(alertTemplate) {
    'use strict';

    return withFormFieldErrors;

    function withFormFieldErrors() {

        this.clearFieldErrors = function(root) {
            $(root || this.$node).find('.errors').empty();
        };

        this.markFieldErrors = function(error, root) {
            var self = this,
                rootEl = root || this.$node,
                messages = [],
                cls = 'control-group error';

            rootEl.find('.control-group.error')
                .removeClass(cls);

            if (!error) {
                return;
            }

            try {
                if (_.isString(error)) {
                    error = JSON.parse(error);
                }
            } catch(e) { }

            if (_.isObject(error)) {
                _.keys(error).forEach(function(fieldName) {
                    switch (fieldName) {
                        case 'invalidValues': break;

                        case 'visibilitySource':
                            rootEl.find('.visibility')
                                .each(function() {
                                    var $this = $(this),
                                        vis = $this.data('visibility')

                                    if (error.invalidValues && vis) {
                                        $this.toggleClass(cls,
                                             _.any(error.invalidValues, function(v) {
                                                 return _.isEqual(v, vis.value);
                                             })
                                        );
                                    } else {
                                        $this.addClass(cls);
                                    }
                                });
                            messages.push(error[fieldName]);
                        break;

                        default:
                            messages.push(error[fieldName]);
                            break;
                    }
                });
            } else {
                messages.push(error || 'Unknown error');
            }

            var errorsContainer = rootEl.find('.errors');

            if (errorsContainer.length) {
                errorsContainer.html(
                    alertTemplate({
                        error: messages
                    })
                ).show();
            } else {
                console.warn(
                    'No <div class="errors"/> container found ' +
                    'to display error messages for component "' +
                    this.describe + '"'
                );
            }

            return messages;
        };
    }
});
