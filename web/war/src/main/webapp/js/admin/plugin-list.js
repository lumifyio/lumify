require([
    'configuration/admin/plugin',
    'hbs!x-tpl',
    'util/formatters',
    'd3'
], function(
    defineLumifyAdminPlugin,
    template,
    F,
    d3
    ) {
    'use strict';

    return defineLumifyAdminPlugin(PluginList, {
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
            this.adminService.plugins()
                .always(function() {
                    self.$node.find('.badge').remove();
                })
                .done(this.renderPlugins.bind(this))
                .fail(this.showError.bind(this));
        });

        this.renderPlugins = function(plugins) {
            var self = this,
                $list = this.$node.empty();

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
                            this.append('h1')
                                .call(function() {
                                    this.append('span').attr('class', 'badge');
                                    this.append('strong');
                                })
                            this.append('div').append('ol').attr('class', 'plugins');
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
                    this.select('ol.plugins')
                        .selectAll('li.plugin')
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
                                    return 'https://github.com/lumifyio/lumify/search?q=' + d.className + '.java';
                                })
                                .text(function(d) {
                                    return d.className;
                                })
                        });

                })
                .exit().remove();
        };

    }
});
