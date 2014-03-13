
define([
    'flight/lib/component',
    'tpl!./diff',
    'service/ontology',
    'util/formatters',
    'data'
], function(defineComponent, template, OntologyService, formatters, appData) {
    'use strict';

    return defineComponent(Diff);

    function Diff() {

        var ontologyService = new OntologyService();

        this.defaultAttrs({
            buttonSelector: 'button',
            rowSelector: 'tr'
        })

        this.after('initialize', function() {
            var self = this;

            ontologyService.properties()
                .done(function(properties) {
                    var formatLabel = function(name) {
                            return properties.byTitle[name].displayName;
                        },
                        formatValue = function(name, change) {
                            var value = change.value;
                            switch (properties.byTitle[name].dataType) {
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
                        console.log(processDiffs);
                        self.$node.html(template({
                            diffs: processDiffs,
                            formatValue: formatValue,
                            formatLabel: formatLabel
                        }));
                    });

                    // DEBUG: $('.workspace-overlay .badge').popover('show')

                    self.on('click', {
                        buttonSelector: self.onButtonClick,
                        rowSelector: self.onRowClick
                    })
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
                });
        });

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

                                if (diff.name === 'title') {
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

            var markedAsPublish = this.$node.find('.mark-publish').length,
                markedAsUndo = this.$node.find('.mark-undo').length,
                buttonHtml = '<button class="btn btn-small">'

            this.$node.find('.header')
                .html(
                    markedAsPublish ? $(buttonHtml).addClass('btn-success').text('Publish ' + formatters.string.plural(markedAsPublish, 'change')) :
                    markedAsUndo ? $(buttonHtml).addClass('btn-warning').text('Undo ' + formatters.string.plural(markedAsUndo, 'change')) : 
                    'Unpublished Changes'
                );

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

