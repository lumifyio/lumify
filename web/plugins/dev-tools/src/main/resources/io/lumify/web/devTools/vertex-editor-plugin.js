require([
    'configuration/admin/plugin',
    'hbs!io/lumify/web/devTools/templates/vertex-editor',
    'util/messages',
    'util/formatters',
    'd3',
    'util/withDataRequest',
    'less!io/lumify/web/devTools/less/vertex-editor'
], function(
    defineLumifyAdminPlugin,
    template,
    i18n,
    F,
    d3,
    withDataRequest,
    less) {
    'use strict';

    defineLumifyAdminPlugin(VertexEditor, {
        less: less,
        mixins: [withDataRequest],
        section: i18n('admin.vertex.editor.section'),
        name: i18n('admin.vertex.editor.name'),
        subtitle: i18n('admin.vertex.editor.subtitle')
    });

    function VertexEditor() {
        this.defaultAttrs({
            loadSelector: '.refresh',
            deleteVertexSelector: '.delete-vertex',
            workspaceInputSelector: '.workspaceId',
            vertexInputSelector: '.vertexId',
            collapsibleSelector: '.collapsible > h1',
            editSelector: '.prop-edit',
            deleteSelector: '.prop-delete',
            saveSelector: '.prop-save',
            cancelSelector: '.prop-cancel'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                loadSelector: this.onLoad,
                editSelector: this.onEdit,
                deleteVertexSelector: this.onVertexDelete,
                deleteSelector: this.onDelete,
                saveSelector: this.onSave,
                cancelSelector: this.onCancel,
                collapsibleSelector: this.onToggleExpand
            });

            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.$node.html(template({
                workspaceId: lumifyData.currentWorkspaceId,
                vertexId: ''
            }));
        });

        this.onEdit = function(event) {
            var button = $(event.target),
                li = button.closest('li').addClass('editing');
        };

        this.onDelete = function(event) {
            var self = this,
                button = $(event.target),
                li = button.closest('li').addClass('show-hover-items');

            this.handleSubmitButton(
                button,
                this.dataRequest('vertex', 'deleteProperty', {
                    vertexId: this.$node.find('.vertexId').val(),
                    property: li.data('property'),
                    workspaceId: this.$node.find('.workspaceId').val()
                })
                    .then(function() {
                        li.removeClass('show-hover-items');
                        self.onLoad();
                    })
                    .catch(function() {
                        self.showError();
                    })
            );
        };

        this.onSave = function(event) {
            var self = this,
                button = $(event.target),
                li = button.closest('li'),
                property = li.data('property');

            this.handleSubmitButton(
                button,

                this.dataRequest('vertex', 'setProperty', {
                    vertexId: this.$node.find('.vertexId').val(),
                    propertyKey: li.find('input[name=key]').val(),
                    propertyName: property.name || li.find('input[name=name]').val(),
                    value: li.find('input[name=value]').val(),
                    visibilitySource: li.find('textarea[name="http://lumify.io#visibilityJson"]').val(),
                    justificationText: 'admin graph vertex editor',
                    metadata: JSON.parse(li.find('textarea[name=metadata]').val()),
                }, this.$node.find('.workspaceId').val())
                    .then(function() {
                        if (li.closest('.collapsible').next('.collapsible').length) {
                            li.removeClass('editing');
                        }
                        this.onLoad();
                    })
                    .catch(function() {
                        self.showError();
                    })
            );
        };

        this.onCancel = function(event) {
            var li = $(event.target).closest('li'),
                section = li.closest('.collapsible');
            if (section.next('.collapsible').length === 0) {
                section.removeClass('expanded');
            } else {
                li.removeClass('editing');
            }
        };

        this.onToggleExpand = function(event) {
            var item = $(event.target).closest('.collapsible');

            if (event.altKey) {
                if (item.hasClass('expanded')) {
                    item.siblings('.collapsible').addBack().removeClass('expanded');
                } else {
                    item.siblings('.collapsible').addBack().addClass('expanded')
                        .last().find('.multivalue').addClass('editing');
                }
            } else {
                item.toggleClass('expanded');
                if (item.next('.collapsible').length === 0) {
                    item.find('.multivalue').addClass('editing');
                } else {
                    item.find('.editing').removeClass('editing');
                }
            }
        };

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

            this.dataRequest('vertex', 'store', {
                workspaceId: this.select('workspaceInputSelector').val(),
                vertexIds: this.select('vertexInputSelector').val()
            }).done(function(vertex) {
                self.update(vertex);
            });
        };

        this.onVertexDelete = function(event) {
            var self = this,
                button = $(event.target),
                graphVertexId = this.select('vertexInputSelector').val(),
                workspaceId = this.select('workspaceInputSelector').val();

            this.handleSubmitButton(
                button,
                this.adminService.vertexDelete(graphVertexId, workspaceId)
                    .fail(function() {
                        self.showError();
                    }).done(function() {
                        self.$node.find('section').remove();
                        self.$node.find('.vertexId').val('');
                    })
            );
        };

        this.update = function(vertex) {
            var newVertex = vertex.id !== this.currentVertexId,
                addNewText = i18n('admin.vertex.editor.addNewProperty.label');

            this.currentVertexId = vertex.id;
            d3.select(this.node)
                .selectAll('section')
                .data(
                    _.chain(vertex.properties)
                      .groupBy('name')
                      .pairs()
                      .sortBy(function(pair) {
                          return pair[0];
                      })
                      .tap(function(pairs) {
                          pairs.push([
                              addNewText,
                              [{
                                  sandboxStatus: '',
                                  'http://lumify.io#visibilityJson': {
                                      source: ''
                                  },
                                  name: '',
                                  value: '',
                                  key: ''
                              }]
                          ]);
                      })
                      .value()
                )
                .call(function() {
                    this.enter()
                        .append('section').attr('class', 'collapsible')
                        .call(function() {
                            this.append('h1')
                                .call(function() {
                                    this.append('span').attr('class', 'badge');
                                    this.append('strong');
                                })
                            this.append('div').append('ol').attr('class', 'props inner-list');
                        });

                    if (newVertex) {
                        this.attr('class', 'collapsible')
                    }
                    this.sort(function(pairA, pairB) {
                        var a = pairA[0], b = pairB[0];
                        if (a === addNewText) return 1;
                        if (b === addNewText) return 0;

                        return a.localeCompare(b);
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
                                .append('li').attr('class', 'multivalue')
                                    .call(function() {
                                        this.append('div').attr('class', 'show-on-hover')
                                            .call(function() {
                                                this.append('button')
                                                    .attr('class', 'btn btn-mini prop-edit')
                                                    .text('Edit');
                                                this.append('button')
                                                    .attr('class', 'btn btn-danger btn-mini prop-delete')
                                                    .text('Delete');
                                            })
                                        this.append('ul').attr('class', 'inner-list');
                                        this.append('button')
                                            .attr('class', 'btn btn-primary prop-save')
                                            .text('Save');
                                        this.append('button')
                                            .attr('class', 'btn btn-default prop-cancel')
                                            .text('Cancel');
                                    })

                            if (newVertex) {
                                this.attr('class', 'multivalue');
                            }
                            this.attr('data-property', function(d) {
                                return JSON.stringify(_.pick(d, 'name', 'key'));
                            });
                            this.select('ul').selectAll('li')
                                .data(function(d) {
                                    var notMetadata = ['name', 'key', 'value', 'sandboxStatus',
                                        'http://lumify.io#visibilityJson',
                                        '_sourceMetadata',
                                        '_justificationMetadata'
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

                                            return (pair[0] === 'name' && !(d.name === '' && d.key === '')) ||
                                                notMetadata.indexOf(pair[0]) === -1;
                                        })
                                        .sortBy(function(pair) {
                                            var order = (
                                                    'name key value sandboxStatus ' +
                                                    'http://lumify.io#visibilityJson'
                                                ).split(' '),
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
                                        var display = {
                                            'http://lumify.io#visibilityJson':
                                                i18n('admin.vertex.editor.visibility.label'),
                                            _justificationMetadata:
                                                i18n('admin.vertex.editor.justification.label'),
                                            _sourceMetadata:
                                                i18n('admin.vertex.editor.justification.label'),
                                            sandboxStatus:
                                                i18n('admin.vertex.editor.sandboxStatus.label')
                                        };
                                        this.textContent = (display[d[0]] || d[0]) + ' ';
                                        d3.select(this)
                                            .call(function() {
                                                var isJustification = display[d[0]] === display._justificationMetadata,
                                                    isMetadata = d[0] === 'metadata' || isJustification,
                                                    displayAsJson = _.isObject(d[1]),
                                                    value = d[0] === 'http://lumify.io#visibilityJson' ?
                                                        d[1].source :
                                                        displayAsJson ?  JSON.stringify(d[1], null, 4) : d[1];

                                                this.append('span')
                                                    .call(function() {
                                                        var cls = [];

                                                        if (isMetadata) {
                                                            cls.push('metadata');
                                                        }
                                                        if (isJustification) {
                                                            cls.push('justification');
                                                        }
                                                        if (displayAsJson) {
                                                            cls.push('display-as-json');
                                                        }
                                                        if (d[0] === 'key') {
                                                            cls.push('is-key');
                                                        }
                                                        if (cls.length) {
                                                            this.attr('class', cls.join(' '));
                                                        }

                                                        this.text(value);
                                                    });

                                                if (isMetadata || displayAsJson) {
                                                    this.append('textarea')
                                                        .attr('name', d[0])
                                                        .text(value)
                                                        .call(function() {
                                                            if (isJustification) {
                                                                this.attr('readonly', true);
                                                            }
                                                        })
                                                } else {
                                                    this.append('input')
                                                        .attr('name', d[0])
                                                        .call(function() {
                                                            if (d[0] === 'sandboxStatus') {
                                                                this.attr('readonly', true);
                                                            }
                                                        })
                                                        .attr('type', 'text')
                                                        .attr('value', value);
                                                }
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
