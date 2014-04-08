
define([
    'util/retina'
], function(retina) {
    'use strict';

    return withVertexPopover;

    function withVertexPopover() {

        this.after('teardown', function() {
            this.dialog.remove();
            this.attr.cy.off('pan zoom position', this.onViewportChanges);
            this.attr.cy.off('tap', this.onTap);
        })

        this.after('initialize', function() {
            if (!this.attr.cy) throw 'cy attr required';
            if (!this.attr.cyNode || this.attr.cyNode.length !== 1) throw 'cyNode attr required: ' + this.attr.cyNode;

            this.onViewportChanges = _.throttle(this.onViewportChanges.bind(this), 1000 / 30);
            this.onTap = this.onTap.bind(this);

            require(['tpl!graph/popovers/' + (this.attr.template || 'noTemplate')], 
                    this.setupWithTemplate.bind(this));
        });

        this.setupWithTemplate = function(tpl) {
            this.dialog = $('<div class="dialog-popover">')
                .css({position: 'absolute'})
                .html(tpl(this.attr))
                .appendTo(this.$node);

            this.popover = this.dialog.find('.popover');

            this.attr.cy.on('pan zoom position', this.onViewportChanges);
            if (this.attr.teardownOnTap !== false) {
                this.attr.cy.on('tap', this.onTap);
            }
            this.onViewportChanges();

            this.positionDialog();
            this.trigger('popoverInitialize');
        };

        this.onTap = function() {
            this.teardown();
        };

        this.onViewportChanges = function() {
            this.dialogPosition = retina.pixelsToPoints(this.attr.cyNode.renderedPosition());
            this.dialogPosition.y -= this.attr.cyNode.height() / 2 * this.attr.cy.zoom();
            this.positionDialog();
        };

        this.positionDialog = function() {
            var width = this.popover.outerWidth(true),
                height = this.popover.outerHeight(true),
                proposed = {
                    left: Math.max(0, Math.min(this.$node.width() - width, this.dialogPosition.x - (width / 2))),
                    top: Math.max(0, Math.min(this.$node.height() - height, this.dialogPosition.y - height))
                };

            this.dialog.css(proposed);
            this.popover.show();
        }
    }
});
