
define([
    'flight/lib/component',
    'tpl!./diff',
    'service/ontology',
    'service/workspace',
    'util/formatters',
    'data'
], function(defineComponent, template, OntologyService, WorkspaceService, formatters, appData) {
    'use strict';

    var SHOW_CHANGES_TEXT_SECONDS = 3;

    return defineComponent(Diff);

    function Diff() {

        var ontologyService = new OntologyService(),
            workspaceService = new WorkspaceService();

        this.defaultAttrs({
            buttonSelector: 'button',
            headerButtonSelector: '.header button',
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
                            value = [change.value.latitude, change.value.longitude].join(', ');
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
                    formatLabel: formatLabel,
                    formatters: formatters
                }));
                self.updateHeader();
            });

            // DEBUG $('.workspace-overlay .badge').popover('show')

            self.on('click', {
                buttonSelector: self.onButtonClick,
                headerButtonSelector: self.onApplyAll,
                rowSelector: self.onRowClick,
            });
            self.on('diffsChanged', function(event, data) {
                self.processDiffs(data.diffs).done(function(processDiffs) {

                    var scroll = self.$node.find('.diffs-list'),
                        previousScroll = scroll.scrollTop(),
                        previousPublished = self.$node.find('.mark-publish').map(function() {
                            return '.' + formatters.className.to($(this).data('diffId'));
                        }).toArray(),
                        previousUndo = self.$node.find('.mark-undo').map(function() {
                            return '.' + formatters.className.to($(this).data('diffId'));
                        }).toArray(),
                        previousSelection  = _.compact(self.$node.find('.active').map(function() {
                            return $(this).data('diffId');
                        }).toArray());

                    self.$node.html(template({
                        diffs: processDiffs,
                        formatValue: formatValue,
                        formatLabel: formatLabel,
                        formatters: formatters
                    }));

                    self.selectVertices(previousSelection);
                    self.$node.find(previousPublished.join(',')).each(function() {
                        var $this = $(this);

                        if ($this.find('.actions').length) {
                            $(this).addClass('mark-publish')
                                .find('.publish').addClass('btn-success')
                        }
                    });
                    self.$node.find(previousUndo.join(',')).each(function() {
                        var $this = $(this);

                        if ($this.find('.actions').length) {
                            $(this).addClass('mark-undo')
                                .find('.undo').addClass('btn-danger')
                        }
                    });
                    self.updateHeader(self.$node.closest('.popover:visible').length > 0);
                    self.$node.find('.diffs-list').scrollTop(previousScroll);
                });
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
                self.undoDiffDependencies = {};

                _.keys(groupedByVertex).forEach(function(vertexId) {
                    var diffs = groupedByVertex[vertexId],
                        actionTypes = {
                            CREATE: { type: 'create', display: 'New' },
                            UPDATE: { type: 'update', display: 'Existing' },
                            DELETE: { type: 'delete', display: 'Deleted' }
                        },
                        outputItem = {
                            id: '',
                            vertexId: vertexId,
                            title: '',
                            properties: [],
                            edges: [],
                            action: {},
                            className: formatters.className.to(vertexId),
                            vertex: appData.vertex(vertexId)
                        };

                    diffs.forEach(function(diff) {
                        switch(diff.type) {
                            case 'VertexDiffItem': 
                                diff.id = outputItem.id = vertexId;
                                if (outputItem.vertex) {
                                    outputItem.title = formatters.vertex.prop(outputItem.vertex, 'title');
                                }
                                outputItem.action = actionTypes.CREATE;
                                self.diffsForVertexId[vertexId] = diff;
                                self.diffsById[vertexId] = diff;
                                break;

                            case 'PropertyDiffItem':

                                var ontologyProperty = self.ontologyProperties.byTitle[diff.name];
                                if (ontologyProperty && ontologyProperty.userVisible) {
                                    diff.id = vertexId + diff.name;
                                    addDiffDependency(diff.elementId, diff);

                                    if (diff.name === 'title' && self.diffsForVertexId[diff.elementId]) {
                                        outputItem.title = diff['new'].value;
                                    } else {
                                        diff.className = formatters.className.to(diff.id);
                                        outputItem.properties.push(diff)
                                    }
                                    self.diffsById[diff.id] = diff;
                                }
                                break;

                            case 'EdgeDiffItem':
                                diff.id = diff.edgeId;
                                diff.inVertex = appData.vertex(diff.inVertexId);
                                diff.className = formatters.className.to(diff.edgeId);
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
                        outputItem.title = formatters.vertex.prop(outputItem.vertex, 'title')
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

                // Undo dependencies are inverse
                if (!self.undoDiffDependencies[diff.id]) {
                    self.undoDiffDependencies[diff.id] = [];
                }
                self.undoDiffDependencies[diff.id].push(id);
            }
        }

        this.onObjectsSelected = function(event, data) {
            var self = this,
                toSelect = data && data.vertices.concat(data.edges || []) || [];

            this.$node.find('.active').removeClass('active');
            this.selectVertices(toSelect);
        };

        this.selectVertices = function(vertices) {
            var self = this,
                cls = vertices.map(function(vertex) {
                    return '.' + formatters.className.to(_.isString(vertex) ? vertex : vertex.id);
                });
            this.$node.find(cls.join(',')).addClass('active');
        };

        this.onRowClick = function(event) {
            var $target = $(event.target).not('button').closest('tr'),
                vertexRow = $target.is('.vertex-row') ? $target : $target.prevAll('.vertex-row'),
                vertexId = vertexRow.data('vertexId'),
                vertex = vertexId && appData.vertex(vertexId),
                alreadySelected = vertexRow.is('.active');

            this.trigger('selectObjects', {
                vertices: (!alreadySelected && vertex) ? [vertex] : []
            });
        };

        this.onButtonClick = function(event) {
            var $target = $(event.target),
                $row = $target.closest('tr');

            if ($target.is('.header button')) return;

            this.trigger(
                'mark' + ($target.hasClass('publish') ? 'Publish' : 'Undo') + 'DiffItem',
                { 
                    diffId: $row.data('diffId'),
                    state: !($target.hasClass('btn-success') || $target.hasClass('btn-danger'))
                }
            );
        };

        this.onApplyAll = function(event) {
            var self = this,
                button = $(event.target).addClass('loading').attr('disabled', true),
                otherButton = button.siblings('button').attr('disabled', true),
                bothButtons = button.add(otherButton),
                header = this.$node.find('.header'),
                type = button.hasClass('publish-all') ? 'publish' : 'undo',
                diffsToSend = this.$node.find('.mark-' + type).map(function() {
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

            workspaceService[type](diffsToSend)
                .always(function() {
                    bothButtons.hide().removeAttr('disabled').removeClass('loading');
                })
                .fail(function(xhr, status, errorText) {
                    var error = $('<div>')
                        .addClass('alert alert-error')
                        .html(
                            '<button type="button" class="close" data-dismiss="alert">&times;</button>' +
                            'An error occured during ' + type + '.<br>Reason: ' + errorText
                        )
                        .appendTo(header);

                    button.show();

                    _.delay(error.remove.bind(error), 5000)
                })
                .done(function(response) {
                    var failures = response.failures,
                        success = response.success;

                    self.$node.find('.header .alert').remove();

                    if (failures && failures.length) {
                        var error = $('<div>')
                            .addClass('alert alert-error')
                            .html(
                                '<button type="button" class="close" data-dismiss="alert">&times;</button>' +
                                '<ul><li>' + _.pluck(failures, 'error_msg').join('</li><li>') + '</li></ul>'
                            )
                            .appendTo(header);
                    }
                });
        };

        this.updateHeader = function(showSuccess) {
            var self = this,
                markedAsPublish = this.$node.find('.mark-publish').length,
                markedAsUndo = this.$node.find('.mark-undo').length,
                header = this.$node.find('.header span'),
                headerText = header.text(),
                publish = this.$node.find('.publish-all'),
                undo = this.$node.find('.undo-all');

            if (this.updateHeaderDelay) {
                clearTimeout(this.updateHeaderDelay);
                this.updateHeaderDelay = null;
            }

            if (showSuccess) {
                publish.hide();
                undo.hide();
                header.show().text(headerText + ' (updated)');
                this.updateHeaderDelay = _.delay(function() {
                    header.text(headerText);
                    self.updateHeader();
                }, SHOW_CHANGES_TEXT_SECONDS * 1000);
            } else {
                header.toggle(markedAsPublish === 0 && markedAsUndo === 0);

                publish.toggle(markedAsPublish > 0)
                       .text('Publish ' + formatters.string.plural(markedAsPublish, 'change'));
                
                undo.toggle(markedAsUndo > 0)
                    .text('Undo ' + formatters.string.plural(markedAsUndo, 'change'));
            }
        }

        this.onMarkUndo = function(event, data) {
            var self = this,
                diffId = data.diffId,
                diff = this.diffsById[diffId],
                deps = this.diffDependencies[diffId] || [],
                state = data.state,
                stateBasedClassFunction = state ? 'addClass' : 'removeClass',
                inverseStateBasedClassFunction = !state ? 'addClass' : 'removeClass';

            if (!diff) {
                return;
            }

            this.$node.find('tr.' + formatters.className.to(diff.id)).each(function() {
                $(this)
                    .removePrefixedClasses('mark-')
                    [stateBasedClassFunction]('mark-undo')
                    .find('button.undo')[stateBasedClassFunction]('btn-danger')
                    .siblings('button.publish').removeClass('btn-success');
            });

            this.updateHeader();

            switch (diff.type) {
                case 'VertexDiffItem':

                    deps.forEach(function(diffId) {
                        self.trigger('markUndoDiffItem', { diffId: diffId, state: state });
                    })
                    
                    break;

                case 'PropertyDiffItem':

                    //self.trigger('markUndoDiffItem', { diffId: diffId, state: false });

                    break;

                case 'EdgeDiffItem':

                    break;

                default: console.warn('Unknown diff item type', diff.type)
            }
        };

        this.onMarkPublish = function(event, data) {
            var self = this,
                diffId = data.diffId,
                diff = this.diffsById[diffId],
                state = data.state,
                stateBasedClassFunction = state ? 'addClass' : 'removeClass',
                inverseStateBasedClassFunction = !state ? 'addClass' : 'removeClass';

            if (!diff) {
                return;
            }

            this.$node.find('tr.' + formatters.className.to(diff.id)).each(function() {
                $(this)
                    .removePrefixedClasses('mark-')
                    [stateBasedClassFunction]('mark-publish')
                    .find('button.publish')[stateBasedClassFunction]('btn-success')
                    .siblings('button.undo').removeClass('btn-danger');
            });

            this.updateHeader();

            switch (diff.type) {

                case 'VertexDiffItem':

                    if (!state) {
                        // Unpublish all dependents
                        var deps = this.diffDependencies[diff.id];
                        deps.forEach(function(diffId) {
                            self.trigger('markPublishDiffItem', { diffId: diffId, state: false });
                        });
                    }

                    break;

                case 'PropertyDiffItem':

                    if (state) {
                        var vertexDiff = this.diffsForVertexId[diff.elementId];
                        if (vertexDiff) {
                            this.trigger('markPublishDiffItem', { diffId: diff.elementId, state: true })
                        }
                    }

                    break;

                case 'EdgeDiffItem':

                    if (state) {
                        var inVertexDiff = this.diffsForVertexId[diff.inVertexId],
                            outVertexDiff = this.diffsForVertexId[diff.outVertexId];

                        if (inVertexDiff) {
                            this.trigger('markPublishDiffItem', { diffId: diff.inVertexId, state: true });
                        }
                        if (outVertexDiff) {
                            this.trigger('markPublishDiffItem', { diffId: diff.outVertexId, state: true });
                        }
                    }

                    break;

                default: console.warn('Unknown diff item type', diff.type)
            }
        };
    }
});
