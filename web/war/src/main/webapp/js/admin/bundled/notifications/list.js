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

            this.on(document, 'notificationAdded', this.onNotificationAdded);
            this.on(document, 'notificationDeleted', this.onNotificationDeleted);

            this.before('render', function() {
                if (loading) {
                    loading.remove();
                    loading = null;
                }
            })
            this.update();
        });

        this.onNotificationAdded = function(event, data) {
            this.update();
        };

        this.onNotificationDeleted = function(event, data) {
            this.update();
        };

        this.update = function() {
            var self = this;

            Promise.all([
                this.dataRequest('admin', 'systemNotificationList'),
                Promise.require('d3'),
                Promise.require('util/formatters')
            ]).done(function(results) {
                var response = results.shift(),
                    d3 = results.shift(),
                    F = results.shift();
                self.render(response, d3, F);
            });
        }

        this.render = function(response, d3, F) {
            var self = this;

            d3.select(this.node)
                .selectAll('section.collapsible')
                .data(_.chain(response.system)
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
