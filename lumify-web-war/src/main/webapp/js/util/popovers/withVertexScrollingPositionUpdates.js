
define([], function() {

    return withVertexScrollingPositionUpdates;

    function withVertexScrollingPositionUpdates() {

        this.before('teardown', function() {
            this.removePositionUpdating();
        });

        this.after('initialize', function() {
            this.on('registerForPositionChanges', this.onRegisterForPositionChanges);
            this.on('unregisterForPositionChanges', this.onUnregisterForPositionChanges);
        });

        this.onRegisterForPositionChanges = function(event, data) {

            if (!data || !data.vertexId) {
                return console.error('Registering for position events requires a vertexId');
            }

            event.stopPropagation();

            var self = this,
                $target = $(event.target),
                scroller = $target.scrollParent(),
                sendPositionChange = function() {
                    var position = $target.offset();
                    self.trigger(event.target, 'positionChanged', {
                        position: {
                            x: position.left + $target.outerWidth(true) / 2,
                            y: position.top + $target.outerHeight(true) / 2
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
