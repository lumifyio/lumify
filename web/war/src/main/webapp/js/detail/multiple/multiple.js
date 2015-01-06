define([
    'flight/lib/component',
    'flight/lib/registry',
    '../withTypeContent',
    '../toolbar/toolbar',
    'tpl!./multiple',
    'tpl!./histogram',
    'util/vertex/list',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    registry,
    withTypeContent,
    Toolbar,
    template,
    histogramTemplate,
    VertexList,
    F,
    withDataRequest) {
    'use strict';

    var HISTOGRAM_STYLE = 'max', // max or sum
        BAR_HEIGHT = 25,
        PADDING = 5,
        ANIMATION_DURATION = 400,
        BINABLE_TYPES = 'double date currency integer number'.split(' '), // TODO: heading
        SCALE_COLOR_BASED_ON_WIDTH = false,
        SCALE_COLOR_BASED_ON_WIDTH_RANGE = ['#00A1F8', '#0088cc'],
        SCALE_OPACITY_BASED_ON_WIDTH = true,
        SCALE_OPACITY_BASED_ON_WIDTH_RANGE = [50, 90],
        NO_HISTOGRAM_DATATYPES = [
            'geoLocation'
        ],
        MAX_BINS_FOR_NON_HISTOGRAM_TYPES = 5,
        OTHER_PLACEHOLDER = '${OTHER-CATEGORY}';

    return defineComponent(Multiple, withTypeContent, withDataRequest);

    function propertyDisplayName(properties, pair) {
        var o = properties.byTitle[pair[0]];

        return o && o.displayName || o;
    }

    function propertyValueDisplay(concepts, properties, bin) {
        var propertyValue = bin[0],
            propertyName = bin.name,
            display = propertyValue;

        if (propertyName === 'http://lumify.io#conceptType' && concepts.byId[propertyValue]) {
            display = concepts.byId[propertyValue].displayName;
        } else if (display === OTHER_PLACEHOLDER) {
            display = i18n('detail.multiple.histogram.other');
        } else if (properties.byTitle[propertyName]) {
            if ('dx' in bin) {
                display =
                    F.vertex.displayProp({
                        name: propertyName,
                        value: bin.x
                    }) + ' – ' +
                    F.vertex.displayProp({
                        name: propertyName,
                        value: bin.x + bin.dx
                    });
            } else {
                display = F.vertex.displayProp({
                    name: propertyName,
                    value: propertyValue
                });
            }
        }
        if (display === '') return '[ blank ]';
        return display;
    }

    function shouldDisplayProperty(properties, property) {
        var propertyName = property.name;

        if (propertyName == 'http://lumify.io#conceptType') {
            return true;
        } else {
            var ontology = properties.byTitle[propertyName];
            if (ontology && ~NO_HISTOGRAM_DATATYPES.indexOf(ontology.dataType)) {
                return false;
            }
            return !!(ontology && ontology.userVisible);
        }
    }

    function positionTextNumber() {
        var self = this,
            t = this.previousSibling,
            tX = t.x.baseVal[0].value,
            getWidthOfNodeByClass = function(cls) {
                var node = self.parentNode;
                while ((node = node.nextSibling)) {
                    if (node.classList.contains(cls)) {
                        return node.getBBox().width;
                    }
                }
                return 0;
            },
            textWidth = getWidthOfNodeByClass('on-bar-text'),
            textNumberWidth = getWidthOfNodeByClass('on-number-bar-text'),
            barRect = this.parentNode.nextSibling,
            barWidthVal = barRect.width.baseVal,
            barWidth = barWidthVal.value,
            remainingBarWidth = barWidth - textWidth - tX - PADDING;

        if (remainingBarWidth <= textNumberWidth) {
            this.setAttribute('x', Math.max(barWidth, tX + textWidth) + PADDING);
            this.setAttribute('text-anchor', 'start');
            this.setAttribute('dx', 0);
        } else {
            this.setAttribute('x', barWidthVal.valueAsString);
            this.setAttribute('dx', -PADDING);
            this.setAttribute('text-anchor', 'end');
        }
    }

    function Multiple() {
        var d3;

        this.defaultAttrs({
            histogramSelector: '.multiple .histogram',
            histogramListSelector: '.multiple .histograms',
            vertexListSelector: '.multiple .vertices-list',
            histogramBarSelector: 'g.histogram-bar',
            toolbarSelector: '.comp-toolbar'
        });

        this.before('teardown', function() {
            this.$node.off('mouseenter mouseleave');
        });

        this.after('initialize', function() {
            var self = this,
                vertices = this.attr.data.vertices || [],
                edges = this.attr.data.edges || [],
                vertexIds = _.pluck(vertices, 'id'),
                edgeIds = _.pluck(edges, 'id');

            this.displayingIds = vertexIds.concat(edgeIds);

            this.$node.html(template({
                getClasses: this.classesForVertex,
                vertices: vertices,
                edges: edges
            }));

            this.on('click', {
                histogramBarSelector: this.histogramClick
            })
            this.$node.on('mouseenter mouseleave', '.histogram-bar', this.histogramHover.bind(this));

            this.on('selectObjects', this.onSelectObjects);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.onGraphPaddingUpdated = _.debounce(this.onGraphPaddingUpdated.bind(this), 100);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);

            Promise.all([
                Promise.require('d3'),
                vertexIds.length ?
                    this.dataRequest('vertex', 'store', { vertexIds: vertexIds }) :
                    this.dataRequest('edge', 'store', { edgeIds: edgeIds }),
                this.dataRequest('ontology', 'concepts'),
                this.dataRequest('ontology', 'properties')
            ]).done(function(results) {
                var _d3 = results.shift(),
                    verticesOrEdges = results.shift(),
                    concepts = results.shift(),
                    properties = results.shift();

                d3 = _d3;

                if (vertexIds.length) {
                    VertexList.attachTo(self.select('vertexListSelector'), {
                        vertices: vertices
                    });
                }

                Toolbar.attachTo(self.select('toolbarSelector'), {
                    toolbar: [
                        {
                            title: i18n('detail.toolbar.open'),
                            submenu: [
                                Toolbar.ITEMS.FULLSCREEN
                            ]
                        }
                    ]
                });

                self.drawHistograms = _.partial(self.renderHistograms, _, concepts, properties);
                self.select('histogramSelector').remove();
                if (vertexIds.length) {
                    self.drawHistograms(vertices, { duration: 0 });
                }
            });

        });

        this.onVerticesUpdated = function(event, data) {
            var self = this;
            if (data && data.vertices) {
                var intersection = _.intersection(this.displayingIds, _.pluck(data.vertices, 'id'));
                if (intersection.length) {
                    this.dataRequest('vertex', 'store', { vertexIds: this.displayingIds })
                        .done(function(vertices) {
                            self.drawHistograms(vertices);
                        });
                }
            }
        };

        this.onGraphPaddingUpdated = function(event, data) {
            if (d3 && !this.previousPaddingRight ||
                data.padding.r != this.previousPaddingRight) {
                this.previousPaddingRight = data.padding.r;

                d3.selectAll('defs .text-number')
                    .each(positionTextNumber);
            }
        };

        this.onSelectObjects = function(event, data) {
            event.stopPropagation();
            this.$node.find('.vertices-list').hide();
            this.$node.find('.multiple').addClass('viewing-vertex');

            var self = this,
                detailsContainer = this.$node.find('.details-container'),
                detailsContent = detailsContainer.find('.content'),
                instanceInfos = registry.findInstanceInfoByNode(detailsContent[0]);
            if (instanceInfos.length) {
                instanceInfos.forEach(function(info) {
                    info.instance.teardown();
                });
            }

            if (data && data.vertexIds) {
                if (!_.isArray(data.vertexIds)) {
                    data.vertexIds = [data.vertexIds];
                }
                promise = this.dataRequest('vertex', 'store', { vertexIds: data.vertexIds });
            } else {
                promise = Promise.resolve(data && data.vertices || []);
            }
            promise.done(function(vertices) {
                if (vertices.length === 0) {
                    return;
                }

                var first = vertices[0];
                if (this._selectedGraphId === first.id) {
                    this.$node.find('.multiple').removeClass('viewing-vertex');
                    this.$node.find('.vertices-list').show().find('.active').removeClass('active');
                    this._selectedGraphId = null;
                    return;
                }

                var type = F.vertex.concept(first).displayType;

                if (type === 'relationship') {
                    moduleName = type;
                } else {
                    moduleName = (((type != 'document' &&
                                    type != 'image' &&
                                    type != 'video') ? 'entity' : 'artifact'
                    ) || 'entity').toLowerCase();
                }

                this._selectedGraphId = first.id;
                require([
                    'detail/' + moduleName + '/' + moduleName,
                ], function(Module) {
                    Module.attachTo(detailsContent, {
                        data: first
                    });
                    self.$node.find('.vertices-list').show();
                });
            });
        };

        this.renderHistograms = function(vertices, concepts, properties, options) {
            var self = this,
                opacityScale = d3.scale.linear().domain([0, 100]).range(SCALE_OPACITY_BASED_ON_WIDTH_RANGE),
                colorScale = d3.scale.linear().domain([0, 100]).range(SCALE_COLOR_BASED_ON_WIDTH_RANGE),
                animationDuration = (options && _.isNumber(options.duration)) || ANIMATION_DURATION;

            if (!concepts || !properties) {
                return;
            }

            var propertySections = _.chain(vertices)
                    .map(function(vertex) {
                        return vertex.properties.map(function(p) {
                            return $.extend({ vertexId: vertex.id }, p);
                        });
                    })
                    .flatten()
                    .filter(_.partial(shouldDisplayProperty, properties))
                    .groupBy('name')
                    .pairs()
                    .filter(function(pair) {
                        var ontologyProperty = properties.byTitle[pair[0]];
                        if (ontologyProperty && ~BINABLE_TYPES.indexOf(ontologyProperty.dataType)) {
                            return true;
                        }
                        if (ontologyProperty && ontologyProperty.possibleValues) {
                            return true;
                        }

                        var valueCounts = _.groupBy(pair[1], 'value'),
                            values = _.map(valueCounts, function(value, key) {
                                return value.length;
                            }),
                            len = values.length;

                        if (len <= MAX_BINS_FOR_NON_HISTOGRAM_TYPES) {
                            return true;
                        }

                        var orderedCounts = _.unique(values)
                                .sort(function(a, b) {
                                    return a - b;
                                }),
                            collapseSmallest = function(orderedCounts, valueCounts, len) {
                                if (orderedCounts.length === 0 || len <= MAX_BINS_FOR_NON_HISTOGRAM_TYPES) {
                                    return;
                                }

                                var moveToOtherWithCount = orderedCounts.shift(),
                                    toMove = _.chain(valueCounts)
                                        .pairs()
                                        .filter(function(p) {
                                            return p[1].length === moveToOtherWithCount;
                                        })
                                        .value(),
                                    toMoveNames = _.map(toMove, function(p) {
                                            return p[0];
                                        });

                                pair[1] = _.reject(pair[1], function(p) {
                                    return ~toMoveNames.indexOf(p.value);
                                });

                                for (var i = 0; i < toMove.length; i++) {
                                    for (var j = 0; j < toMove[i][1].length; j++) {
                                        pair[1].push({
                                            value: OTHER_PLACEHOLDER,
                                            vertexId: toMove[i][1][j].vertexId
                                        });
                                    }
                                }

                                valueCounts = _.groupBy(pair[1], 'value');
                                values = _.map(valueCounts, function(value, key) {
                                    return value.length;
                                });
                                len = values.length;

                                collapseSmallest(orderedCounts, valueCounts, len);
                            };

                        collapseSmallest(orderedCounts, valueCounts, len);

                        return true;
                    })
                    .sortBy(function(pair) {
                        var ontologyProperty = properties.byTitle[pair[0]],
                            value = pair[0];

                        if (value === 'http://lumify.io#conceptType') {
                            return '0';
                        }

                        if (ontologyProperty && ontologyProperty.displayName) {
                            value = ontologyProperty.displayName;
                        }

                        return '1' + value.toLowerCase();
                    })
                    .value(),
                container = this.select('histogramListSelector');

                width = container.width();

            d3.select(container.get(0))
                    .selectAll('li.property-section')
                    .data(propertySections, function(d) {
                        return d[0];
                    })
                    .call(function() {
                        this.enter()
                            .append('li').attr('class', 'property-section')
                            .call(function() {
                                this.append('div').attr('class', 'nav-header')
                                this.append('svg').attr('width', '100%')
                            });
                        this.exit().remove();

                        this.order()
                            .call(function() {
                                this.select('.nav-header')
                                    .text(_.partial(propertyDisplayName, properties));
                                this.select('svg')
                                    .call(function() {
                                        this.transition()
                                            .duration(animationDuration)
                                            .attr('height', function(d) {
                                                var ontologyProperty = properties.byTitle[d[0]],
                                                    values = _.pluck(d[1], 'value');

                                                d.values = values;

                                                if (ontologyProperty &&
                                                    ~BINABLE_TYPES.indexOf(ontologyProperty.dataType)) {

                                                    var bins = _.reject(
                                                        d3.layout.histogram().value(_.property('value'))
                                                        (d[1]), function(bin) {
                                                        return bin.length === 0;
                                                    });

                                                    d.bins = bins;
                                                    return bins.length * BAR_HEIGHT;
                                                }

                                                if ('bins' in d) {
                                                    delete d.bins;
                                                }

                                                return _.unique(_.pluck(d[1], 'value')).length * BAR_HEIGHT;
                                            })
                                    })
                                    .selectAll('.histogram-bar')
                                    .data(function(d) {
                                        var xScale, yScale,
                                            bins,
                                            values = d.values;

                                        if ('bins' in d) {
                                            bins = d.bins;

                                            xScale = d3.scale.linear().range([0, 100]);
                                            yScale = d3.scale.ordinal();

                                            bins = bins.map(function(bin) {
                                                    bin.xScale = xScale;
                                                    bin.yScale = yScale;
                                                    bin.name = d[0];
                                                    bin.vertexIds = _.pluck(bin, 'vertexId');
                                                    for (var i = 0; i < bin.length; i++) {
                                                        bin[i] = bin[i].value;
                                                    }
                                                    return bin;
                                                });
                                            yScale.domain(_.compact(_.pluck(bins, '0')));
                                            yScale.rangeRoundBands([0, bins.length * BAR_HEIGHT], 0.1);
                                            xScale.domain([0, d3[HISTOGRAM_STYLE](bins, function(d) {
                                                return d.length
                                            })]);

                                            return bins;
                                        }

                                        xScale = d3.scale.linear()
                                            .range([0, 100]);

                                        var groupedByValue = _.groupBy(d[1], 'value'),
                                            displayValue = _.partial(propertyValueDisplay, concepts, properties);
                                        yScale = d3.scale.ordinal()
                                            .domain(
                                                _.chain(values)
                                                .unique()
                                                .sortBy(function(f) {
                                                    if (f === OTHER_PLACEHOLDER) {
                                                        return 999;
                                                    }

                                                    var bin = [f];
                                                    bin.name = d[0];
                                                    return (100 - groupedByValue[f].length) +
                                                        displayValue(bin);
                                                })
                                                .value()
                                            );

                                        bins = _.chain(groupedByValue)
                                                .pairs()
                                                .map(function(bin) {
                                                    bin.xScale = xScale;
                                                    bin.yScale = yScale;
                                                    bin.name = d[0];
                                                    bin.vertexIds = _.pluck(bin[1], 'vertexId');
                                                    bin[1] = bin[1].length;
                                                    return bin;
                                                })
                                                .value();

                                        yScale.rangeRoundBands([0, bins.length * BAR_HEIGHT], 0.1)
                                        xScale.domain([0, d3[HISTOGRAM_STYLE](bins, function(d) {
                                            return d[1];
                                        })]);

                                        return bins;
                                    })
                                    .call(function() {
                                        this.enter()
                                            .append('g')
                                                .attr('class', 'histogram-bar')
                                                .call(function() {
                                                    this.append('defs')
                                                        .call(function() {
                                                            this.call(createMask);
                                                            this.call(createMask);
                                                            this.append('text')
                                                                .attr('class', 'text')
                                                                .attr('x', PADDING)
                                                                .attr('text-anchor', 'start');
                                                            this.append('text')
                                                                .attr('class', 'text-number');

                                                            function createMask() {
                                                                this.append('mask')
                                                                    .attr('maskUnits', 'userSpaceOnUse')
                                                                    .attr('y', 0)
                                                                    .append('rect')
                                                                        .attr('x', 0)
                                                                        .attr('y', 0)
                                                                        .attr('fill', 'white')
                                                            }
                                                        })
                                                    this.append('rect').attr('class', 'bar-background');
                                                    this.append('rect')
                                                        .attr('width', '100%')
                                                        .attr('class', 'click-target')
                                                        .attr('height', barHeight)
                                                    this.append('use').attr('class', 'on-bar-text');
                                                    this.append('use').attr('class', 'off-bar-text');
                                                    this.append('use').attr('class', 'on-number-bar-text');
                                                    this.append('use').attr('class', 'off-number-bar-text');
                                                })
                                        this.exit()
                                            .transition()
                                            .duration(animationDuration)
                                            .style('opacity', 0)
                                            .remove();

                                        this.order()
                                            .attr('data-vertex-ids', function(d) {
                                                return JSON.stringify(d.vertexIds || []);
                                            })
                                            .transition()
                                            .duration(animationDuration)
                                            .attr('transform', function(d) {
                                                return 'translate(0,' + d.yScale(d[0]) + ')';
                                            })

                                        this.select('rect.bar-background')
                                            .style('fill-opacity', _.compose(toPercent, barOpacity, barWidth))
                                            .style('fill', _.compose(barColor, barWidth))
                                            .attr('height', barHeight)
                                            .attr('width', _.compose(toPercent, barWidth));

                                        this.select('defs')
                                            .call(function() {
                                                this.select('mask:first-child')
                                                    .attr('id', _.compose(append('_0'), maskId))
                                                    .attr('height', barHeight)
                                                    .attr('width', _.compose(toPercent, maskWidth(0)))
                                                    .attr('x', _.compose(toPercent, maskX(0)))
                                                    .select('rect')
                                                        .attr('width', '500%')
                                                        .attr('height', barHeight)
                                                this.select('mask:nth-child(2)')
                                                    .attr('id', _.compose(append('_1'), maskId))
                                                    .attr('height', barHeight)
                                                    .attr('width', _.compose(toPercent, maskWidth(1)))
                                                    .attr('x', _.compose(toPercent, maskX(1)))
                                                    .select('rect')
                                                        .attr('width', '500%')
                                                        .attr('height', barHeight)

                                                this.select('.text')
                                                    .attr('id', textId)
                                                    .text(_.partial(propertyValueDisplay, concepts, properties))
                                                    .each(setTextY(0.15));

                                                this.select('.text-number')
                                                    .attr('id', textNumberId)
                                                    .text(textNumberValue)
                                                    .attr('x', textNumberX)
                                                    .attr('dx', PADDING * -1)
                                                    .attr('text-anchor', 'end')
                                                    .each(positionTextNumber)
                                                    .each(setTextY(0.2));
                                            });

                                        this.select('use.on-bar-text')
                                            .call(markOther)
                                            .attr('xlink:href', _.compose(toRefId, textId))
                                            .attr('mask', _.compose(toUrlId, append('_0'), maskId));
                                        this.select('use.off-bar-text')
                                            .call(markOther)
                                            .attr('xlink:href', _.compose(toRefId, textId))
                                            .attr('mask', _.compose(toUrlId, append('_1'), maskId));
                                        this.select('use.on-number-bar-text')
                                            .attr('xlink:href', _.compose(toRefId, textNumberId))
                                            .attr('mask', _.compose(toUrlId, append('_0'), maskId));
                                        this.select('use.off-number-bar-text')
                                            .attr('xlink:href', _.compose(toRefId, textNumberId))
                                            .attr('mask', _.compose(toUrlId, append('_1'), maskId));

                                        d3.selectAll('defs .text-number')
                                            .each(positionTextNumber);

                                    })

                                function markOther(useTag) {
                                    useTag.classed('other', function(pair) {
                                        return pair[0] === OTHER_PLACEHOLDER;
                                    })
                                }

                                function append(toAppend) {
                                    return function(str) {
                                        return str + toAppend;
                                    }
                                }

                                function setTextY(k) {
                                    return function() {
                                        // Firefox exception here
                                        var height = 22;
                                        try {
                                            height = this.getBBox().height;
                                        } catch(e) { }
                                        this.setAttribute('y', (BAR_HEIGHT / 2 + height * k) + 'px');
                                    };
                                }
                                function maskId(d, i, barIndex) {
                                    return 'section_' + barIndex + '_bar_' + i + '_mask';
                                }
                                function textId(d, i, barIndex) {
                                    return 'section_' + barIndex + '_bar_' + i + '_text';
                                }
                                function textNumberId(d, i, barIndex) {
                                    return 'section_' + barIndex + '_bar_' + i + '_textnumber';
                                }
                                function textNumberValue(d, i, barIndex) {
                                    return barNumber(d);
                                }
                                function textNumberX(d, i, barIndex) {
                                    return toPercent(barWidth(d, i, barIndex));
                                }
                                function toRefId(id) {
                                    return '#' + id;
                                }
                                function toUrlId(id) {
                                    return 'url(#' + id + ')';
                                }
                                function maskX(i) {
                                    return function(d, ignored, barIndex) {
                                        if (i === 0) {
                                            return '0';
                                        }

                                        return barWidth(d, i, barIndex);
                                    }
                                }
                                function maskWidth(i) {
                                    return function(d, ignored, barIndex) {
                                        var width = barWidth(d, i, barIndex);
                                        if (i === 0) {
                                            return width;
                                        }

                                        return 100 - width;
                                    }
                                }
                                function barHeight(d) {
                                    return d.yScale.rangeBand();
                                }
                                function barNumber(d) {
                                    if ('dx' in d) {
                                        return d.y;
                                    }
                                    return d[1];
                                }
                                function barWidth(d) {
                                    return d.xScale(barNumber(d));
                                }
                                function barColor(percent) {
                                    if (SCALE_COLOR_BASED_ON_WIDTH) {
                                        return colorScale(percent);
                                    }
                                    return undefined;
                                }
                                function barOpacity(percent) {
                                    if (SCALE_OPACITY_BASED_ON_WIDTH) {
                                        return opacityScale(percent);
                                    }
                                    return 100;
                                }
                                function toPercent(number) {
                                    return number + '%';
                                }
                            });
                    })
        };

        this.histogramHover = function(event, object) {
            var vertexIds = $(event.target).closest('g').data('vertexIds'),
                eventName = event.type === 'mouseenter' ? 'focus' : 'defocus';

            this.trigger(document, eventName + 'Vertices', { vertexIds: vertexIds });
        };

        this.histogramClick = function(event, object) {
            var vertexIds = $(event.target).closest('g').data('vertexIds');

            this.trigger(document, 'selectObjects', { vertexIds: vertexIds });
            this.trigger(document, 'defocusVertices', { vertexIds: vertexIds });
        };

    }
});
