
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
        });

        this.after('initialize', function() {
            var t = this.attr.template || 'noTemplate',
                path = 'hbs!util/popovers/' + t;

            require([path], this.setupWithTemplate.bind(this));
        });

        this.setupWithTemplate = function(tpl) {
            var self = this;

            this.dialog = $('<div class="dialog-popover">')
                .css({position: 'absolute'})
                .html(tpl(this.attr))
                .appendTo(document.body);

            this.popover = this.dialog.find('.popover');

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

            this.positionDialog = _.throttle(this.positionDialog.bind(this), 1000 / 30);
            this.after('onPositionChange', this.positionDialog);
            this.on('positionChanged', this.onPositionChange);
            this.trigger('registerForPositionChanges', this.attr.anchorTo);

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
        };

        this.positionDialog = function() {
            if (this.dialogPosition) {
                var width = this.popover.outerWidth(),
                    height = this.popover.outerHeight(),
                    windowWidth = $(window).width(),
                    windowHeight = $(window).height(),
                    maxLeft = windowWidth - width,
                    maxTop = windowHeight - height,
                    calcLeft = this.dialogPosition.x - (width / 2),
                    calcTop = this.dialogPosition.y - height,
                    proposed = { 
                        left: Math.max(0, Math.min(maxLeft, calcLeft)), 
                        top: Math.max(0, Math.min(maxTop, calcTop)),  
                    };
                
                if (this.dialogPosition.y < height) {
                    proposed.top = Math.min(maxTop, this.dialogPosition.y);
                    this.popover.removeClass('top').addClass('bottom');
                } else {
                    this.popover.removeClass('bottom').addClass('top');
                }
                
                this.dialog.css(proposed);
                this.popover.show();
            }
        }
    }
});
