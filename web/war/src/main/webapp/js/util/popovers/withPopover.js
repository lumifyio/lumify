
define([
], function() {
    'use strict';

    var ERROR_NO_POSITION_RESPONSE =
            'Unable to attach popover, ' +
            'nothing responded to registerForPositionChanges';

    return withPopover;

    function withPopover() {

        this.before('teardown', function() {
            clearTimeout(this.positionChangeErrorCheck);
            $(document).off('.popoverclose')
            this.trigger('unregisterForPositionChanges')
        });

        this.after('teardown', function() {
            this.dialog.remove();
            $('.popover-bg-overlay').remove();
        });

        this.after('initialize', function() {
            var t = this.attr.template || 'noTemplate',
                path = 'hbs!util/popovers/' + t;

            require([path], this.setupWithTemplate.bind(this));
        });

        this.setupWithTemplate = function(tpl) {
            var self = this;

            if (this.attr.overlay) {
                $(document.body).append('<div class="popover-bg-overlay">')
            }

            this.dialog = $('<div class="dialog-popover">')
                .css({
                    position: 'absolute',
                    display: this.attr.hideDialog ? 'none' : 'block'
                })
                .html(tpl(this.attr))
                .appendTo(document.body);

            this.popover = this.dialog.find('.popover');

            this.on(this.popover, 'positionDialog', this.positionDialog);
            this.on(this.popover, 'closePopover', this.teardown);

            $(document).off('.popoverclose').on('click.popoverclose', function(e) {
                if (self.attr.teardownOnTap !== false) {
                    if ($(e.target).closest(self.popover).length) {
                        return;
                    }
                    self.teardown();
                }
            })

            this.registerAnchorTo();
        };

        this.registerAnchorTo = function() {
            var self = this;

            this.on('positionChanged', this.onPositionChange);
            this.trigger('registerForPositionChanges', this.attr);

            this.positionChangeErrorCheck = _.delay(function() {
                if (!self.dialogPosition) {
                    console.error(ERROR_NO_POSITION_RESPONSE);
                    self.dialogPosition = { x: 0, y: 0 };
                    self.positionDialog();
                }
            }, 500)
        };

        this.onPositionChange = function(event, data) {
            clearTimeout(this.positionChangeErrorCheck);
            this.dialogPosition = data.position;
            this.positionDialog();

            if (!this.throttledPositionDialog) {
                this.throttledPositionDialog = true;
                _.delay(function() {
                    this.positionDialog = _.throttle(this.positionDialog.bind(this), 1000 / 60);
                }.bind(this), 500);
            }
        };

        this.positionDialog = function() {
            if (this.dialogPosition) {
                var padding = 10,
                    width = this.popover.outerWidth(),
                    height = this.popover.outerHeight(),
                    windowWidth = $(window).width(),
                    windowHeight = $(window).height(),
                    maxLeft = windowWidth - width,
                    maxTop = windowHeight - height,
                    calcLeft = this.dialogPosition.x - (width / 2),
                    calcTop = (this.dialogPosition.yMin || this.dialogPosition.y) - height,
                    proposed = {
                        left: Math.max(padding, Math.min(maxLeft - padding, calcLeft)),
                        top: Math.max(padding, Math.min(maxTop - padding, calcTop)),
                    };

                if (this.dialogPosition.y < (windowHeight / 2)) {
                    proposed.top = Math.min(maxTop, this.dialogPosition.yMax || this.dialogPosition.y);
                    if (!~this.popover[0].className.indexOf('bottom')) {
                        this.popover.removeClass('top').addClass('bottom');
                    }
                } else if (!~this.popover[0].className.indexOf('top')) {
                    this.popover.removeClass('bottom').addClass('top');
                }

                var arrowLeft = this.dialogPosition.x - proposed.left,
                    maxLeftAllowed = width - padding * 1.5,
                    percent = (Math.min(maxLeftAllowed, arrowLeft) / width * 100) + '%';

                this.dialog.find('.arrow').css('left', percent);

                this.dialog.css(proposed);
                this.popover.show();
            }
        }
    }
});
