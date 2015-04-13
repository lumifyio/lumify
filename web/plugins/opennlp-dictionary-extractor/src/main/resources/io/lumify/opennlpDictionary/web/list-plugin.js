require([
    'configuration/admin/plugin',
    'util/formatters',
    'd3',
    'util/withDataRequest'
], function(
    defineLumifyAdminPlugin,
    F,
    d3,
    withDataRequest
    ) {
    'use strict';

    return defineLumifyAdminPlugin(DictionaryList, {
        mixins: [withDataRequest],
        section: 'Dictionary',
        name: 'List',
        subtitle: 'Current dictionary list'
    });

    function DictionaryList() {

        this.defaultAttrs({
            deleteSelector: '.btn-danger'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                deleteSelector: this.onDelete
            });

            this.$node.html(
                '<ul class="nav nav-list">' +
                    '<li class="nav-header">Entries<span class="badge loading"></span></li>' +
                '</ul>'
            );

            this.loadEntries();
        });

        this.loadEntries = function() {
            var self = this;

            this.dataRequest('admin', 'dictionary')
                .then(this.renderEntries.bind(this))
                .catch(this.showError.bind(this, 'Error loading entries'))
                .finally(function() {
                    self.$node.find('.badge').remove();
                })
        }

        this.onDelete = function(event) {
            var button = $(event.target),
                rowKey = button.data('rowKey');

            button.closest('li').addClass('show-hover-items');

            this.handleSubmitButton(button,
                this.dataRequest('admin', 'dictionaryDelete', rowKey)
                    .then(this.loadEntries.bind(this))
                    .catch(this.showError.bind(this, 'Error deleting entry'))
            );
        };

        this.renderEntries = function(result) {
            var $list = this.$node.find('ul');

            if (result.entries.length === 0) {
                return $list.empty().html('<li class="nav-header">No Entries</li>');
            }

            d3.select($list.empty().get(0))
                .selectAll('li.entry')
                .data(
                    _.chain(result.entries)
                    .groupBy('concept')
                    .pairs()
                    .sortBy(function(pair) {
                        return pair[0].toLowerCase();
                    })
                    .value()
                )
                .call(function() {
                    this.enter()
                        .append('section').attr('class', 'collapsible expanded has-badge-number')
                        .call(function() {
                            this.append('h1')
                                .call(function() {
                                    this.append('span').attr('class', 'badge');
                                    this.append('strong');
                                })
                            this.append('div').append('ol').attr('class', 'inner-list');
                        });

                    this.select('h1 strong').text(function(d) {
                        return d[0];
                    });
                    this.select('.badge').text(function(d) {
                        return F.number.pretty(d[1].length);
                    });
                    this.select('div > ol.inner-list')
                        .selectAll('li.entry')
                        .data(function(d) {
                            return d[1];
                        })
                        .call(function() {
                            this.enter()
                                .append('li')
                                    .attr('class', 'entry')
                                    .call(function() {
                                        this.append('button')
                                            .attr('class', 'btn btn-danger show-on-hover btn-mini')
                                            .text('Delete')
                                        this.append('table')
                                    })

                            this.select('button.btn-danger').attr('data-row-key', function(d) {
                                return d['rowKey'];
                            });
                            this.select('table')
                                .selectAll('tr')
                                .data(function(d) {
                                    return _.sortBy(_.pairs(_.omit(d, 'concept')), function(pair) {
                                        var list = 'tokens resolvedName'.split(' '),
                                            index = list.indexOf(pair[0]);

                                        if (index >= 0) {
                                            return '' + index;
                                        }

                                        return '' + list.length + pair[0];
                                    });
                                })
                                .call(function() {
                                    this.enter().append('tr')
                                        .call(function() {
                                            this.append('th');
                                            this.append('td');
                                        })

                                    this.select('th').text(function(d) {
                                        if (d[0] === 'http://lumify.io#rowKey') {
                                            return 'Rowkey';
                                        }
                                        if (d[0] === 'resolvedName') {
                                            return 'Resolved';
                                        }
                                        if (d[0] === 'tokens') {
                                            return 'Tokens';
                                        }

                                        return d[0];
                                    })
                                    this.select('td').text(function(d) {
                                        return d[1];
                                    })
                                })
                                .exit().remove();
                        })
                        .exit().remove();
                })
                .exit().remove();
        };

    }
});
