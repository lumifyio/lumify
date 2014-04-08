
define([], function() {
    'use strict';

    function withDropdown() {

        this.defaultAttrs({
            canceButtonSelector: '.btn.cancel'
        });

        this.open = function() {
            var self = this,
                node = this.$node;

            if (node.outerWidth() <= 0) {
                // Fix issue where dropdown is zero width/height 
                // when opening dropdown later in detail pane when
                // dropdown is already open earlier in detail pane
                node.css({position: 'relative'});
                return _.defer(this.open.bind(this));
            }

            node.on(TRANSITION_END, function(e) {
                var oe = e.originalEvent || e;

                if (oe.target === self.node && oe.propertyName === 'height') {
                    node.off(TRANSITION_END);
                    node.css({
                        transition: 'none',
                        height: 'auto',
                        width: '100%',
                        overflow: 'visible'
                    });
                    self.trigger('opened');
                }
            });
            var form = node.find('.form');
            node.css({ height: form.outerHeight(true) + 'px' });
        };

        this.after('teardown', function() {
            this.trigger('dropdownClosed');
            this.$node.closest('.text').removeClass('dropdown');
            this.$node.remove();
        });

        this.buttonLoading = function(selector) {
            selector = selector || '.btn-primary';
            this.$node.find(selector).addClass('loading').attr('disabled', true);
        };

        this.clearLoading = function() {
            this.$node.find('.btn:disabled').removeClass('loading').removeAttr('disabled');
        };

        this.markFieldErrors = function(error) {
            var self = this,
                messages = [],
                cls = 'control-group error';

            this.$node.find('.control-group.error')
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
                    switch(fieldName) {
                        case 'visibilitySource':
                            self.$node.find('.visibility')
                                .addClass(cls);
                            messages.push(error[fieldName]);
                            break;
                    }
                });
            } else {
                messages.push(error || 'Unknown error');
            }

            return messages;
        };

        this.manualOpen = function() {
            if (this.attr.manualOpen) {
                _.defer(this.open.bind(this));
                this.attr.manualOpen = false;
            }
        }

        this.after('initialize', function() {
            this.$node.closest('.text').addClass('dropdown');
            this.on('click', {
                canceButtonSelector: this.teardown
            });
            if (!this.attr.manualOpen) {
                _.defer(this.open.bind(this));
            }
        });
    }

    return withDropdown;
});
