define([
    'flight/lib/component',
    'util/withDataRequest'
], function(defineComponent, withDataRequest) {
    'use strict';

    return defineComponent(Notifications, withDataRequest);

    function Notifications() {

        this.after('initialize', function() {
            var self = this;

            this.userDismissed = {};
            this.stack = [];

            this.on(document, 'notificationActive', this.onNotificationActive);
            this.on(document, 'notificationDeleted', this.onNotificationDeleted);
            this.dataRequest('notification', 'systemNotificationList')
                .done(function(notifications) {
                    self.displayNotifications(notifications.system.active);
                });

            this.$container = $('<div>')
                .addClass('notifications')
                .appendTo(this.$node);
        });

        this.onNotificationActive = function(event, data) {
            this.displayNotifications([data.notification]);
        };

        this.onNotificationDeleted = function(event, data) {
            this.stack = _.reject(this.stack, function(n) {
                return data.notificationId === n.id;
            });
            this.update();
        };

        this.displayNotifications = function(notifications) {
            var self = this,
                now = Date.now(),
                shouldDisplay = notifications && _.filter(notifications, function(n) {
                    if (self.userDismissed[n.id] && self.userDismissed[n.id] === n.hash) {
                        return false;
                    }
                    return n.severity !== 'INFORMATIONAL';
                });

            if (shouldDisplay && shouldDisplay.length) {
                this.stack = this.stack.concat(shouldDisplay);
                this.update();
            }
        };

        this.update = function() {
            var self = this;

            d3.select(this.$container[0])
                .selectAll('.notification')
                .data(this.stack, function(n) {
                    return n.id;
                })
                .call(function() {
                    var newOnes = this.enter()
                        .append('li')
                            .attr('class', 'notification')
                            .style('opacity', 0)
                            .style('left', '-50px')
                            .call(function() {
                                this.append('h1')
                                this.append('h2')
                                this.append('button')
                            })

                    this.on('click', function(clicked) {
                        self.stack = _.reject(self.stack, function(n) {
                            return n.id === clicked.id;
                        });
                        self.userDismissed[clicked.id] = clicked.hash;
                        self.update();
                    })
                    this.classed('critical', function(n) {
                        return (/CRITICAL/i).test(n.severity);
                    })
                    this.classed('warning', function(n) {
                        return (/WARNING/i).test(n.severity);
                    });
                    this.select('h1').text(function(n) {
                        return n.title
                    });
                    this.select('h2').text(function(n) {
                        return n.message
                    });

                    newOnes.transition()
                        .delay(function(d, i) {
                            return i / newOnes.size() * 100 + 100;
                        })
                        .duration(750)
                        .style('left', '0px')
                        .style('opacity', 1)

                    var exiting = this.exit(),
                        exitingSize = exiting.size();

                    self.$container.css('min-width', self.$container.width() + 'px');

                    exiting
                        .style('left', '0px')
                        .transition()
                        .delay(function(d, i) {
                            if (exitingSize === 1) {
                                return 0;
                            } else {
                                return (exitingSize - 1 - i) / exitingSize * 100 + 100;
                            }
                        })
                        .duration(500)
                        .style('left', '-50px')
                        .style('opacity', 0)
                        .remove()
                });

        };

    }
});
