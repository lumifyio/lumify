require([
    'configuration/admin/plugin',
    'hbs!io/lumify/web/devTools/templates/vertex-editor',
    'util/messages',
    'util/formatters',
    'd3',
    'service/vertex',
    'less!io/lumify/web/devTools/less/vertex-editor'
], function(
    defineLumifyAdminPlugin,
    template,
    i18n,
    F,
    d3,
    VertexService,
    less) {
    'use strict';

    var vertexService = new VertexService();

    defineLumifyAdminPlugin(VertexEditor, {
        less: less,
        section: i18n('admin.vertex.editor.section'),
        name: i18n('admin.vertex.editor.name'),
        subtitle: i18n('admin.vertex.editor.subtitle')
    });

    function VertexEditor() {
        this.defaultAttrs({
            loadSelector: '.btn-primary',
            workspaceInputSelector: '.workspaceId',
            vertexInputSelector: '.vertexId'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                loadSelector: this.onLoad
            });

            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            require(['data'], function(appData) {
                self.$node.html(template({
                    workspaceId: appData.workspaceId,
                    vertexId: appData.selectedVertexIds[0] || ''
                }));
            });
        });

        this.onVerticesUpdated = function(event, data) {
            if (this.currentVertexId) {
                var vertex = _.findWhere(data && data.vertices, { id: this.currentVertexId })
                if (vertex) {
                    this.update(vertex);
                }
            }
        };

        this.onObjectsSelected = function(event, data) {
            var vertex = _.first(data && data.vertices);

            if (vertex) {
                this.select('vertexInputSelector').val(vertex.id);
                this.update(vertex);
            }
        };

        this.onLoad = function() {
            var self = this;

            vertexService.getVertexProperties(
                this.select('vertexInputSelector').val(),
                this.select('workspaceInputSelector').val()
            ).done(function(vertex) {
                self.update(vertex);
            })
        };

        this.update = function(vertex) {
            this.currentVertexId = vertex.id;
            d3.select(this.node)
                .selectAll('section')
                .data(
                    _.chain(vertex.properties)
                      .groupBy('name')
                      .pairs()
                      .value()
                )
                .call(function() {
                    this.enter()
                        .append('section').attr('class', 'collapsible expanded')
                        .call(function() {
                            this.append('h1')
                                .call(function() {
                                    this.append('span').attr('class', 'badge');
                                    this.append('strong');
                                })
                            this.append('div').append('ol').attr('class', 'props');
                        });

                    this.select('h1 strong').text(function(d) {
                        return d[0];
                    });
                    this.select('.badge').text(function(d) {
                        return F.number.pretty(d[1].length);
                    })
                    this.select('ol.props')
                        .selectAll('li.multivalue')
                        .data(function(d) {
                            return d[1];
                        })
                        .call(function() {
                            this.enter()
                                .append('li').attr('class', 'multivalue').append('ul');

                            this.select('ul').selectAll('li')
                                .data(function(d) {
                                    var notMetadata = ['name', 'key', 'value', 'sandboxStatus',
                                        'http://lumify.io#visibilityJson',
                                        'http://lumify.io#visibility'
                                    ];

                                    return _.chain(d)
                                        .clone()
                                        .tap(function(property) {
                                            property.metadata = _.omit(property, notMetadata);
                                        })
                                        .pairs()
                                        .reject(function(pair) {
                                            if (pair[0] === 'metadata') {
                                                return false;
                                            }

                                            return pair[0] === 'name' ||
                                                pair[0] === 'http://lumify.io#visibility' ||
                                                notMetadata.indexOf(pair[0]) === -1;
                                        })
                                        .sortBy(function(pair) {
                                            var order = 'name key value sandboxStatus'.split(' '),
                                                index = order.indexOf(pair[0]);

                                            if (index >= 0) {
                                                return '' + index;
                                            }

                                            return ('' + order.length) + pair[0];
                                        })
                                        .value()
                                })
                                .call(function() {
                                    this.enter()
                                        .append('li')
                                        .append('label')
                                        .attr('class', 'nav-header')

                                    this.select('label').each(function(d) {
                                        this.textContent = d[0] === 'http://lumify.io#visibilityJson' ?
                                            i18n('admin.vertex.editor.visibility.label') : d[0];
                                        d3.select(this)
                                            .call(function() {
                                                var value = d[0] === 'http://lumify.io#visibilityJson' ?
                                                    d[1].source :
                                                    _.isObject(d[1]) ?  JSON.stringify(d[1], null, 4) : d[1];

                                                this.append('span')
                                                    .style('display', function(d) {
                                                        if (d[0] === 'metadata') {
                                                            return 'block';
                                                        }
                                                    })
                                                    .style('white-space', function(d) {
                                                        if (d[0] === 'metadata') {
                                                            return 'pre';
                                                        }
                                                    })
                                                    .text(value);

                                                this.append('input')
                                                    .style('display', 'none')
                                                    .attr('type', 'text')
                                                    .attr('value', value);
                                            })
                                    });
                                })
                                .exit().remove();
                        })
                        .exit().remove();
                })
                .exit().remove();

        }

    }
});
