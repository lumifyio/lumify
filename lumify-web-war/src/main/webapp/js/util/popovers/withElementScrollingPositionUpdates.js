
define([], function() {

    return withElementScrollingPositionUpdates;

    function withElementScrollingPositionUpdates() {

        this.before('teardown', function() {
            this.removePositionUpdating();
        });

        this.after('initialize', function() {
            this.on('registerForPositionChanges', this.onRegisterForPositionChanges);
            this.on('unregisterForPositionChanges', this.onUnregisterForPositionChanges);
        });

        this.onRegisterForPositionChanges = function(event, data) {

            event.stopPropagation();

            var self = this,
                $target = $(event.target),
                scroller = data && data.scrollSelector ?
                    $target.closest(data.scrollSelector) :
                    $target.scrollParent(),
                sendPositionChange = function() {
                    var position = $target.offset(),
                        width = $target.outerWidth(),
                        height = $target.outerHeight();

                    self.trigger(event.target, 'positionChanged', {
                        position: {
                            x: position.left + width / 2,
                            y: position.top + height / 2,
                            xMin: position.left,
                            xMax: position.left + width,
                            yMin: position.top,
                            yMax: position.top + height
                        }
                    })
                };

            this.positionChangeScroller = scroller;
            this.sendPositionChange = sendPositionChange;

            this.on(document, 'graphPaddingUpdated', sendPositionChange);
            scroller.on('scroll.positionchange', sendPositionChange);
            sendPositionChange();
        };

        this.onUnregisterForPositionChanges = function(event, data) {
            this.removePositionUpdating();
        };

        this.removePositionUpdating = function() {
            if (this.positionChangeScroller) {
                this.positionChangeScroller.off('.positionchange');
            }
            if (this.sendPositionChange) {
                this.off(document, 'graphPaddingUpdated', this.sendPositionChange);
            }
        }
    }
})
