require([
    'configuration/admin/plugin',
    'util/withDataRequest',
    'util/withCollapsibleSections'
], function(
    defineLumifyAdminPlugin,
    withDataRequest,
    withCollapsibleSections) {
    'use strict';

    return defineLumifyAdminPlugin(NotificationList, {
        mixins: [withDataRequest, withCollapsibleSections],
        section: 'System Notifications',
        name: 'List',
        subtitle: 'View all Notifications'
    });

    function NotificationList() {

        this.after('initialize', function() {
            var self = this,
                loading = $('<span>')
                    .addClass('badge loading')
                    .appendTo(this.$node.empty().addClass('notificationList'));

            this.notifications = [];

            this.on(document, 'notificationActive', this.onNotificationUpdated);
            this.on(document, 'notificationUpdated', this.onNotificationUpdated);
            this.on(document, 'notificationDeleted', this.onNotificationDeleted);

            this.before('render', function() {
                if (loading) {
                    loading.remove();
                    loading = null;
                }
            })
            this.update();
        });

        this.onNotificationUpdated = function(event, data) {
            var currentIndex = -1;
            _.each(this.notifications, function(n, i) {
                if (n.id === data.notification.id) {
                    currentIndex = i;
                }
            });

            if (data.notification.type !== 'system') {
                return;
            }

            if (event.type === 'notificationActive') {
                data.notification.active = true;
            }

            if (currentIndex >= 0) {
                this.notifications.splice(currentIndex, 1, data.notification);
            } else {
                this.notifications.push(data.notification);
            }
            this.render();
        };

        this.onNotificationDeleted = function(event, data) {
            this.update();
        };

        this.update = function() {
            var self = this;

            this.dataRequest('notification', 'systemNotificationList')
                .done(function(response) {
                    self.notifications = response.system.active.concat(response.system.future);
                    self.render();
                });
        }

        this.render = function(d3, F) {
            var self = this,
                now = Date.now();

            if (!d3 || !F) {
                return require(['d3', 'util/formatters'], function(d3, F) {
                    self.render(d3, F);
                })
            }

            if (this.notifications.length) {
                this.$node.find('.none').remove();
            } else {
                $('<div>').addClass('none')
                    .text('No Notications')
                    .appendTo(this.node);
            }

            d3.select(this.node)
                .selectAll('section.collapsible')
                .data(_.chain(this.notifications)
                      .groupBy(function(n) {
                          return n.active || n.startDate <= now ? 'Active' : 'Future';
                      })
                      .pairs()
                      .sortBy(function(p) {
                          return p[0];
                      })
                      .value()
                )
                .order()
                .call(function() {
                    this.enter()
                        .append('section').attr('class', 'collapsible has-badge-number expanded')
                        .call(function() {
                            this.append('h1').attr('class', 'collapsible-header')
                                .call(function() {
                                    this.append('span').attr('class', 'badge');
                                    this.append('strong');
                                })
                            this.append('div').append('ol').attr('class', 'nav-list nav');
                        })
                    this.exit().remove();

                    this.select('h1 strong').text(function(n) {
                        return n[0]
                    })

                    this.select('h1 .badge').text(function(n) {
                        return F.number.pretty(n[1].length);
                    })

                    this.select('.nav-list')
                        .selectAll('li')
                        .data(function(n) {
                            return n[1];
                        })
                        .call(function() {
                            this.enter()
                                .append('li').attr('class', 'highlight-on-hover')
                                .call(function() {
                                    this.append('div').attr('class', 'show-on-hover')
                                        .call(function() {
                                            this.append('button')
                                                .attr('class', 'btn btn-default btn-mini')
                                                .text('Edit');
                                            this.append('button')
                                                .attr('class', 'btn btn-danger btn-mini')
                                                .text('Delete');
                                        })
                                    this.append('span').attr('class', 'nav-list-title')
                                    this.append('span').attr('class', 'nav-list-subtitle')
                                        .call(function() {
                                            this.append('span').attr('class', 'title')
                                            this.append('span').attr('class', 'dates')
                                        })
                                })
                            this.exit().remove();

                            this.each(function() {
                                var d = d3.select(this).datum();
                                $(this)
                                    .removeClass('INFORMATIONAL CRITICAL WARNING')
                                    .addClass(d.severity.toUpperCase());
                            });

                            this.select('.nav-list-title').text(function(n) {
                                return n.title;
                            })

                            this.select('.nav-list-subtitle .title').text(function(n) {
                                return n.message;
                            })
                            this.select('.nav-list-subtitle .dates').text(function(n) {
                                if (n.endDate) {
                                    return F.date.dateTimeString(n.startDate) +
                                        ' – ' +
                                        F.date.dateTimeString(n.endDate);
                                }
                                return F.date.dateTimeString(n.startDate);
                            })

                            this.select('.btn-default').on('click', function(n) {
                                self.trigger('showAdminPlugin', {
                                    section: 'System Notifications',
                                    name: 'Create',
                                    notification: n
                                });
                            })
                            this.select('.btn-danger').on('click', function(n) {
                                var btn = $(this)
                                    .addClass('loading').attr('disabled', true);
                                self.dataRequest('admin', 'systemNotificationDelete', n.id)
                                    .finally(function() {
                                        btn.removeClass('loading').removeAttr('disabled');
                                    })
                            })
                        })
                })
        }
    }
});
