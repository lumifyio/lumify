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
                loading = $('<span>').addClass('badge loading').appendTo(this.$node.empty())

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
                .data(_.pairs(response.system))
                .call(function() {
                    this.enter()
                        .append('section').attr('class', 'collapsible has-badge-number expanded')
                        .call(function() {
                            this.append('h1').attr('class', 'collapsible-header')
                                .call(function() {
                                    this.append('span').attr('class', 'badge');
                                    this.append('strong');
                                })
                            this.append('div').append('ul').attr('class', 'nav-list');
                        })
                    this.exit().remove();

                    this.select('h1 strong').text(function(n) {
                        return n[0]
                    })

                    this.select('h1 .badge').text(function(n) {
                        return F.number.pretty(n[1].length);
                    })

                    this.select('ul.nav-list')
                        .selectAll('li')
                        .data(function(n) {
                            return n[1];
                        })
                        .call(function() {
                            this.enter()
                                .append('li').attr('class', 'highlight-on-hover')
                                .call(function() {
                                    this.append('button')
                                        .attr('class', 'show-on-hover btn btn-danger')
                                        .text('Delete')
                                    this.append('span').attr('class', 'nav-list-title')
                                    this.append('span').attr('class', 'nav-list-subtitle')
                                    this.append('dl')
                                })
                            this.exit().remove();

                            this.select('.nav-list-title').text(function(n) {
                                return n.title;
                            })

                            this.select('.nav-list-subtitle').text(function(n) {
                                return n.message;
                            })

                            this.select('.btn-danger').on('click', function(n) {
                                var btn = $(this)
                                    .addClass('loading').attr('disabled', true);
                                self.dataRequest('admin', 'systemNotificationDelete', n.id)
                                    .then(function() {
                                        //btn.closest('li').remove();
                                    })
                                    .finally(function() {
                                        btn.removeClass('loading').removeAttr('disabled');
                                    })
                            })

                            this.select('dl')
                                .selectAll('dt,dd')
                                .data(function(n) {
                                    return _.chain(n)
                                        .omit('message', 'title')
                                        .pairs()
                                        .sortBy(function(p) {
                                            return p[0].toLowerCase();
                                        })
                                        .flatten()
                                        .value();
                                })
                                .order()
                                .call(function() {
                                    this.enter()
                                        .append(function(v, i, j) {
                                            return document.createElement(i % 2 === 0 ? 'dt' : 'dd')
                                        })
                                    this.exit().remove();

                                    this.text(function(v, i) {
                                        return v;
                                    })
                                })
                        })
                })
        }
    }
});
