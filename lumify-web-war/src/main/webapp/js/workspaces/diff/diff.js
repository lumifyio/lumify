
define([
    'flight/lib/component',
    'tpl!./diff',
    'service/ontology',
    'service/workspace',
    'util/formatters',
    'data'
], function(defineComponent, template, OntologyService, WorkspaceService, formatters, appData) {
    'use strict';

    return defineComponent(Diff);

    function Diff() {

        var ontologyService = new OntologyService();
        var workspaceService = new WorkspaceService();

        this.defaultAttrs({
            buttonSelector: 'button',
            headerButtonSelector: '.header button.publish-all',
            rowSelector: 'tr',
        })

        this.after('initialize', function() {
            var self = this;

            $.when(
                ontologyService.properties(),
                ontologyService.relationships()
            ).done(function(properties, relationships) {
                self.ontologyProperties = properties;
                self.ontologyRelationships = relationships;
                self.setup();
            })
        });

        this.setup = function() {
            var self = this,
                formatLabel = function(name) {
                    return self.ontologyProperties.byTitle[name].displayName;
                },
                formatValue = function(name, change) {
                    var value = change.value;
                    switch (self.ontologyProperties.byTitle[name].dataType) {
                        case 'geoLocation':
                            value = [change.latitude, change.longitude].join(', ')
                            break;
                        case 'date':
                            value = formatters.date.dateString(value);
                            break;
                    }

                    return value;
                };

            self.processDiffs(self.attr.diffs).done(function(processDiffs) {
                self.$node.html(template({
                    diffs: processDiffs,
                    formatValue: formatValue,
                    formatLabel: formatLabel
                }));
            });

            // DEBUG: $('.workspace-overlay .badge').popover('show')

            self.on('click', {
                buttonSelector: self.onButtonClick,
                headerButtonSelector: self.onPublish,
                rowSelector: self.onRowClick,
            });
            self.on('diffsChanged', function(event, data) {
                var scroll = self.$node.find('.diffs-list'),
                    previousScroll = scroll.scrollTop(),
                    previousPublished = self.$node.find('.mark-publish').map(function() {
                        return '.' + self.classNameForVertex($(this).data('diffId'));
                    }).toArray(),
                    previousSelection  = self.$node.find('.active').map(function() {
                        return $(this).data('vertexId')
                    }).toArray(),
                    header = this.$node.find('.header').html();

                self.processDiffs(data.diffs).done(function(processDiffs) {
                    self.$node.html(template({
                        diffs: processDiffs,
                        formatValue: formatValue,
                        formatLabel: formatLabel
                    }));
                });

                self.selectVertices(previousSelection);
                self.$node.find(previousPublished.join(',')).each(function() {
                    $(this).addClass('mark-publish')
                        .find('.publish').addClass('btn-success')
                });
                self.$node.find('.header').html(header);
                self.$node.find('.diffs-list').scrollTop(previousScroll);
            })
            //self.on('mouseenter', { rowSelector: this.onRowHover });
            //self.on('mouseleave', { rowSelector: this.onRowHover });
            self.on('markPublishDiffItem', self.onMarkPublish);
            self.on('markUndoDiffItem', self.onMarkUndo);
            self.on(document, 'objectsSelected', self.onObjectsSelected);
        };

        this.onRowHover = function(event) {
            console.log(event.type, event.target)
        };

        this.processDiffs = function(diffs) {
            var self = this,
                referencedVertices = [],
                groupedByVertex = _.groupBy(diffs, function(diff) {
                    referencedVertices.push(diff.vertexId || diff.elementId || diff.outVertexId);
                    if (diff.inVertexId) referencedVertices.push(diff.inVertexId);
                    if (diff.vertexId) return diff.vertexId;
                    if (diff.elementId) return diff.elementId;
                    return diff.outVertexId;
                }),
                output = [];

            return appData.refresh(referencedVertices).then(function() {

                self.diffsForVertexId = {};
                self.diffsById = {};
                self.diffDependencies = {};

                _.keys(groupedByVertex).forEach(function(vertexId) {
                    var diffs = groupedByVertex[vertexId],
                        actionTypes = {
                            CREATE: { type:'create', display:'New' },
                            UPDATE: { type:'update', display:'Existing' },
                            DELETE: { type:'delete', display:'Deleted' }
                        },
                        outputItem = {
                            id: '',
                            vertexId: vertexId,
                            title: '',
                            properties: [],
                            edges: [],
                            action: {},
                            className: self.classNameForVertex(vertexId),
                            vertex: appData.vertex(vertexId)
                        };

                    diffs.forEach(function(diff) {
                        switch(diff.type) {
                            case 'VertexDiffItem': 
                                diff.id = outputItem.id = vertexId;
                                outputItem.action = actionTypes.CREATE;
                                self.diffsForVertexId[vertexId] = diff;
                                self.diffsById[vertexId] = diff;
                                break;

                            case 'PropertyDiffItem':
                                diff.id = vertexId + diff.name;
                                addDiffDependency(diff.elementId, diff);

                                if (diff.name === 'title' && self.diffsForVertexId[diff.elementId]) {
                                    outputItem.title = diff['new'].value;
                                } else {
                                    diff.className = self.classNameForVertex(diff.id);
                                    outputItem.properties.push(diff)
                                }
                                self.diffsById[diff.id] = diff;
                                break;

                            case 'EdgeDiffItem':
                                diff.id = diff.edgeId;
                                diff.inVertex = appData.vertex(diff.inVertexId);
                                diff.className = self.classNameForVertex(diff.edgeId);
                                diff.displayLabel = self.ontologyRelationships.byTitle[diff.label].displayName;
                                addDiffDependency(diff.inVertexId, diff);
                                addDiffDependency(diff.outVertexId, diff);
                                outputItem.edges.push(diff);
                                self.diffsById[diff.id] = diff;
                                break;

                            default:
                                console.warn('Unknown diff item type', diff.type)
                        }
                    });

                    if (!outputItem.title && outputItem.vertex) {
                        outputItem.action = actionTypes.UPDATE;
                        outputItem.title = outputItem.vertex.properties.title.value;
                        outputItem.id = outputItem.vertex.id;
                    }

                    output.push(outputItem);
                });

                return output;
            });


            function addDiffDependency(id, diff) {
                if (!self.diffDependencies[id]) {
                    self.diffDependencies[id] = [];
                }

                self.diffDependencies[id].push(diff.id);
            }
        }

        var classNameIndex = 0, 
            vertexIdMap = {}, clsIdMap = {};
        this.classNameForVertex = function(vertexId) {
            if (clsIdMap[vertexId]) {
                return clsIdMap[vertexId];
            }

            var clsName = 'vId-' + classNameIndex++;

            vertexIdMap[clsName] = vertexId;
            clsIdMap[vertexId] = clsName;

            return clsName;
        };
        this.vertexIdForClassName = function(clsName) {
            return vertexIdMap[clsName];
        };

        this.onObjectsSelected = function(event, data) {
            var self = this;

            if (data && data.vertices) {
                this.$node.find('.active').removeClass('active');
                this.selectVertices(data.vertices);
            } else if (data && data.edges) {
                // TODO
            }
        };

        this.selectVertices = function(vertices) {
            var self = this,
                cls = vertices.map(function(vertex) {
                    return '.' + self.classNameForVertex(_.isString(vertex) ? vertex : vertex.id);
                });
            this.$node.find(cls.join(',')).addClass('active')
        };

        this.onRowClick = function(event) {
            var $target = $(event.target).not('button').closest('tr'),
                vertexId = $target.data('vertexId'),
                vertex = vertexId && appData.vertex(vertexId);

            if (vertex) {
                this.trigger('selectObjects', {
                    vertices: [vertex]
                });
            }
        };

        this.onButtonClick = function(event) {
            var $target = $(event.target),
                $row = $target.closest('tr');

            if ($target.is('.header button')) return;

            this.trigger(
                'mark' + ($target.hasClass('publish') ? 'Publish' : 'Undo') + 'DiffItem',
                { 
                    diffId: $row.data('diffId'),
                    state: !($target.hasClass('btn-success') || $target.hasClass('btn-warning'))
                }
            );

            /*
            if ($target.closest('thead').length) {
                var allButtons = $target.closest('table').find('td button');
                if ($target.hasClass(cls)) {
                    allButtons.removeClass(cls);
                } else {
                    allButtons.addClass(cls);
                }
            }
            */
        };

        this.onPublish = function(event) {
            var self = this,
                diffsToPublish = this.$node.find('.mark-publish').map(function() {
                    var diff = self.diffsById[$(this).data('diffId')];
                    switch (diff.type) {
                        case 'PropertyDiffItem': return {
                            type: 'property',
                            vertexId: diff.elementId,
                            key: diff.key,
                            name: diff.name,
                            action: 'update',
                            status: diff.sandboxStatus
                        };

                        case 'VertexDiffItem': return {
                            type: 'vertex',
                            vertexId: diff.vertexId,
                            action: 'create', // TODO: ever delete?
                            status: diff.sandboxStatus
                        };

                        case 'EdgeDiffItem': return {
                            type: 'relationship',
                            edgeId: diff.edgeId,
                            sourceId: diff.outVertexId,
                            destId: diff.inVertexId,
                            action: 'create',
                            status: diff.sandboxStatus
                        };
                    }
                    console.error('Unknown diff type', diff);
                }).toArray();

            var button = $(event.target)
                .addClass('loading')
                .attr('disabled', true);

            workspaceService.publish(diffsToPublish)
                .always(function() {
                    button.removeAttr('disabled').removeClass('loading');
                })
                .fail(function(xhr, status, errorText) {
                    var error = $('<div>')
                        .addClass('alert alert-error')
                        .html(
                            '<button type="button" class="close" data-dismiss="alert">&times;</button>' +
                            'An error occured during publishing.<br>Reason: ' + errorText
                        )
                        .insertAfter(button);

                    _.delay(error.remove.bind(error), 5000)
                })
                .done(function(response) {
                    var failures = response.failures,
                        success = response.success;

                    self.updateHeader();

                    if (failures && failures.length) {
                        self.$node.find('.header .alert').remove();
                        var error = $('<div>')
                            .addClass('alert alert-error')
                            .html(
                                '<button type="button" class="close" data-dismiss="alert">&times;</button>' +
                                '<ul><li>' + _.pluck(failures, 'error_msg').join('</li><li>') + '</li></ul>'
                            )
                            .insertAfter(button);
                    }
                });
        };

        this.onMarkUndo = function(event, data) {
            var diff = data.diffId;

            switch(diff.type) {
                case 'VertexDiffItem': 

                    break;
                case 'PropertyDiffItem':

                    break;
                case 'EdgeDiffItem':

                    break;
                default: console.warn('Unknown diff item type', diff.type)
            }
        };

        this.updateHeader = function() {
            var markedAsPublish = this.$node.find('.mark-publish').length,
                markedAsUndo = this.$node.find('.mark-undo').length,
                buttonHtml = '<button class="btn btn-small">'

            this.$node.find('.header')
                .html(
                    markedAsPublish ? $(buttonHtml).addClass('publish-all btn-success').text('Publish ' + formatters.string.plural(markedAsPublish, 'change')) :
                    markedAsUndo ? $(buttonHtml).addClass('undo-all btn-warning').text('Undo ' + formatters.string.plural(markedAsUndo, 'change')) : 
                    'Unpublished Changes'
                );
        }

        this.onMarkPublish = function(event, data) {
            var self = this,
                diffId = data.diffId,
                diff = this.diffsById[diffId],
                state = data.state,
                stateBasedClassFunction = state ? 'addClass' : 'removeClass';

            if (!diff) {
                return;
            }

            this.$node.find('tr.' + clsIdMap[diff.id]).each(function() {
                $(this)[stateBasedClassFunction]('mark-publish')
                    .find('button.publish')[stateBasedClassFunction]('btn-success')
                    .siblings('button.undo').removeClass('btn-warning');
            });

            this.updateHeader();

            switch(diff.type) {
                case 'VertexDiffItem': 
                    if (!state) {
                        // Unpublish all dependents
                        var deps = this.diffDependencies[diff.id];
                        deps.forEach(function(diffId) {
                            self.trigger('markPublishDiffItem', { diffId: diffId, state:false });
                        });
                    }

                    break;

                case 'PropertyDiffItem':

                    if (state) {
                        var vertexDiff = this.diffsForVertexId[diff.elementId];
                        if (vertexDiff) {
                            this.trigger('markPublishDiffItem', { diffId: diff.elementId, state:true })
                        }
                    }

                    break;

                case 'EdgeDiffItem':

                    if (state) {
                        var inVertexDiff = this.diffsForVertexId[diff.inVertexId],
                            outVertexDiff = this.diffsForVertexId[diff.outVertexId];

                        if (inVertexDiff) {
                            this.trigger('markPublishDiffItem', { diffId: diff.inVertexId, state:true });
                        }
                        if (outVertexDiff) {
                            this.trigger('markPublishDiffItem', { diffId: diff.outVertexId, state:true });
                        }
                    }

                    break;

                default: console.warn('Unknown diff item type', diff.type)
            }
        };
    }
});

