define([
    'flight/lib/component',
    'util/withDataRequest'
], function(defineComponent, withDataRequest) {
    'use strict';

    return defineComponent(Notifications, withDataRequest);

    function Notifications() {

        this.after('initialize', function() {
            var self = this;

            if ('localStorage' in window) {
                var previouslyDismissed = localStorage.getItem('notificationsDismissed');
                if (previouslyDismissed) {
                    this.userDismissed = JSON.parse(previouslyDismissed);
                }
            }
            if (!this.userDismissed) {
                this.userDismissed = {};
            }
            this.stack = [];

            this.on(document, 'notificationActive', this.onNotificationActive);
            this.on(document, 'notificationDeleted', this.onNotificationDeleted);
            this.dataRequest('notification', 'list')
                .done(function(notifications) {
                    self.displayNotifications(notifications.system.active.concat(notifications.user));
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
                shouldDisplay = notifications && _.filter(notifications, function(n) {
                    if (self.attr.showUserDismissed !== true &&
                        self.userDismissed[n.id] && self.userDismissed[n.id] === n.hash) {
                        return false;
                    }
                    if (n.type === 'user') {
                        return true;
                    }

                    return self.attr.showInformational === true || n.severity !== 'INFORMATIONAL';
                });

            if (shouldDisplay && shouldDisplay.length) {
                shouldDisplay.forEach(function(updated) {
                    var index = -1;
                    self.stack.forEach(function(n, i) {
                        if (n.id === updated.id) {
                            index = i;
                        }
                    });

                    if (index >= 0) {
                        self.stack.splice(index, 1, updated);
                    } else {
                        self.stack.push(updated);
                    }
                })
                this.update();
            }
            this.trigger('notificationCountUpdated', { count: this.stack.length });
        };

        this.setUserDismissed = function(notificationId, notificationHash) {
            this.userDismissed[notificationId] = notificationHash;
            try {
                if ('localStorage' in window) {
                    localStorage.setItem('notificationsDismissed', JSON.stringify(this.userDismissed));
                }
            } catch(e) { }
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
                                if (self.attr.allowDismiss !== false) {
                                    this.append('button')
                                }
                            })

                    if (self.attr.allowDismiss !== false) {
                        this.on('click', function(clicked) {
                            self.stack = _.reject(self.stack, function(n) {
                                return n.id === clicked.id;
                            });
                            self.setUserDismissed(clicked.id, clicked.hash);
                            self.update();
                        })
                    }
                    this.classed('critical', function(n) {
                        return (/CRITICAL/i).test(n.severity);
                    })
                    this.classed('warning', function(n) {
                        return (/WARNING/i).test(n.severity);
                    });
                    this.classed('info', function(n) {
                        return !n.severity || (/INFO/i).test(n.severity);
                    });
                    this.select('h1').text(function(n) {
                        return n.title
                    });
                    this.select('h2').text(function(n) {
                        return n.message
                    });

                    if (self.attr.animated !== false) {
                        newOnes = newOnes.transition()
                            .delay(function(d, i) {
                                return i / newOnes.size() * 100 + 100;
                            })
                            .duration(750)
                    }

                    newOnes
                        .style('left', '0px')
                        .style('opacity', 1)

                    var exiting = this.exit(),
                        exitingSize = exiting.size();

                    self.$container.css('min-width', self.$container.width() + 'px');

                    if (self.attr.animated !== false) {
                        exiting = exiting
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
                    }

                    exiting.remove()
                });

        };

    }
});
