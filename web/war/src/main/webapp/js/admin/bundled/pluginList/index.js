require([
    'configuration/admin/plugin',
    'util/formatters',
    'util/withDataRequest',
    'util/withCollapsibleSections'
], function(
    defineLumifyAdminPlugin,
    F,
    withDataRequest,
    withCollapsibleSections) {
    'use strict';

    return defineLumifyAdminPlugin(PluginList, {
        mixins: [withDataRequest, withCollapsibleSections],
        section: 'Plugin',
        name: 'List',
        subtitle: 'Loaded plugins'
    });

    function PluginList() {

        this.after('initialize', function() {
            var self = this;

            this.$node.html(
                '<ul class="nav nav-list">' +
                  '<li class="nav-header">Plugins<span class="badge loading"></span></li>' +
                '</ul>'
            );

            this.dataRequest('admin', 'plugins')
                .then(this.renderPlugins.bind(this))
                .catch(this.showError.bind(this))
                .finally(function() {
                    self.$node.find('.badge').remove();
                });
        });

        this.renderPlugins = function(plugins) {
            var self = this,
                $list = this.$node.empty();

            require(['d3'], function(d3) {
                d3.select($list.get(0))
                    .selectAll('section.collapsible')
                    .data(
                        _.chain(plugins)
                        .pairs()
                        .map(function(pair) {
                            return [
                                pair[0].replace(/[A-Z]/g, function(cap) {
                                    return ' ' + cap;
                                }),
                                pair[1]
                            ];
                        })
                        .sortBy(function(pair) {
                            return pair[0].toLowerCase();
                        })
                        .value()
                    )
                    .call(function() {
                        this.enter()
                            .append('section').attr('class', 'collapsible has-badge-number')
                            .call(function() {
                                this.append('h1').attr('class', 'collapsible-header')
                                    .call(function() {
                                        this.append('span').attr('class', 'badge');
                                        this.append('strong');
                                    })
                                this.append('div').append('ol').attr('class', 'inner-list');
                            });

                        this.classed('expanded', function(d) {
                            return d[1].length > 0;
                        });
                        this.select('h1 strong').text(function(d) {
                            return d[0];
                        });
                        this.select('.badge').text(function(d) {
                            return F.number.pretty(d[1].length);
                        });
                        this.select('ol.inner-list')
                            .selectAll('li')
                            .data(function(d) {
                                return d[1];
                            })
                            .call(function() {
                                this.enter()
                                    .append('li')
                                    .append('a')
                                        .attr('title', 'Search in Github')
                                        .attr('target', 'github');

                                this.select('a')
                                    .attr('href', function(d) {
                                        var baseUrl = '';
                                        if ((/^io\.lumify\./).test(d.className)) {
                                            baseUrl = 'https://github.com/lumifyio/lumify/search?q=';
                                        } else {
                                            baseUrl = 'https://github.com/search?q=';
                                        }
                                        return baseUrl + d.className + '+language%3Ajava&type=Code';
                                    })
                                    .text(function(d) {
                                        return d.className;
                                    })
                            });

                    })
                    .exit().remove();
            });
        };

    }
});
