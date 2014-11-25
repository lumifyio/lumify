define([
    'flight/lib/component',
    'hbs!./histogramTpl',
    'd3',
    'util/withDataRequest'
], function(
    defineComponent,
    template,
    d3,
    withDataRequest) {
    'use strict';

    var HEIGHT = 100,
        BRUSH_PADDING = 0,
        BRUSH_TEXT_PADDING = 2,
        BRUSH_BACKGROUND_HEIGHT = 13,
        MAX_ZOOM_IN = 1000,
        MAX_ZOOM_OUT = 100,
        DAY = 24 * 60 * 60 * 1000;

    return defineComponent(Histogram, withDataRequest);

    function inDomain(d, xScale) {
        var domain = xScale.domain();
        return d > domain[0] && d < domain[1];
    }

    function Histogram() {

        var margin = {top: 10, right: 16, bottom: 40, left: 16};

        this.after('initialize', function() {
            this.$node.html(template({}));

            this.onGraphPaddingUpdated = _.debounce(this.onGraphPaddingUpdated.bind(this), 500);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);

            this.dataRequest('workspace', 'histogramValues', this.attr.property)
                .done(this.renderChart.bind(this));

            this.redraw = _.throttle(this.redraw.bind(this), 16);
        });

        this.onGraphPaddingUpdated = function(event, data) {
            if (this.xScale) {
                var padding = data.padding,
                    width = this.width = this.$node.scrollParent().width() - margin.left - margin.right,
                    height = width / (16 / 9);

                this.$node.find('svg').attr('width', width + margin.left + margin.right);

                this.xScale.range([0, width]);
                this.focus.style('display', 'none');
                this.redraw();
            }
        };

        this.redraw = function(rebin) {
            var self = this,
                xScale = this.xScale,
                yScale = this.yScale,
                data = this.data,
                updateElements = function(animate) {
                    yScale.domain([0, d3.max(data, function(d) {
                        return d.y;
                    })])

                    self.brush.x(self.zoom.x())
                    self.createBars(data, animate);
                    self.svg.select('.brush').call(self.brush.x(xScale));
                    self.svg.select('.x.axis').call(self.xAxis.orient('bottom'));
                };

            if (rebin) {
                if (!this.debouncedBin) {
                    this.debouncedBin = _.debounce(function() {
                        self.binCount = null;
                        data = self.data = self.binValues();
                        updateElements(true);
                    }, 250);
                }
                this.debouncedBin();
            }
            updateElements();
        };

        this.binValues = function() {
            var isDate = this.attr.property.dataType === 'date',
                xScale = this.xScale;

            if (!this.binCount) {
                this.binCount = isDate ? xScale.ticks(25) : 25;//this.width;
            }

            var count = this.values.length === 0 ? 0 : this.binCount;
            return d3.layout.histogram().bins(count)(_.filter(this.values, function(v) {
                return inDomain(v, xScale);
            }));
        }

        this.renderChart = function(vals) {

            var self = this,

                isDate = this.attr.property.dataType === 'date',

                isDateTime = this.attr.property.displayType !== 'dateOnly',

                values = this.values = (
                    isDate && !isDateTime ?
                        _.map(vals, function(v) {
                            return v + (new Date(v).getTimezoneOffset() * 60000);
                        }) :
                        vals
                ),

                width = this.width = this.$node.scrollParent().width() - margin.left - margin.right,
                height = this.height = HEIGHT - margin.top - margin.bottom,

                valuesExtent = window.valuesExtent = calculateValuesExtent(),

                xScale = this.xScale = createXScale(),

                data = this.data = this.binValues(),

                yScale = this.yScale = d3.scale.linear()
                    .domain([0, d3.max(data, function(d) {
                        return d.y;
                    })])
                    .range([height, 0]),

                xAxis = d3.svg.axis()
                    .scale(xScale)
                    .ticks(isDate ? 3 : 4)
                    .tickSize(5, 0)
                    .orient('bottom'),

                onZoomedUpdate = _.throttle(function() {
                    updateBrushInfo();
                    updateFocusInfo();
                }, 1000 / 30),

                onZoomed = function() {
                    var scale = d3.event.scale,
                        scaleChange = scale !== self.previousScale,
                        translate = d3.event.translate[0],
                        translateChange = translate !== self.previousTranslate;

                    self.redraw(scaleChange || translateChange);
                    self.previousScale = scale;
                    self.previousTranslate = translate;
                    onZoomedUpdate();
                },

                zoom = this.zoom = createZoomBehavior(),

                onBrushed = _.throttle(function() {
                    var extent = brush.extent(),
                    delta = extent[1] - extent[0];

                    gBrushText
                        .style('display', delta < 0.01 ? 'none' : '')
                        .attr('transform', 'translate(' + xScale(extent[0]) + ', 0)');

                    updateBrushInfo(extent, delta);
                }, 1000 / 30),

                brush = d3.svg.brush()
                    .x(zoom.x())
                    .on('brush', onBrushed),

                svgOuter = this.svg = d3.select(this.node).append('svg')
                    .attr('width', width + margin.left + margin.right)
                    .attr('height', height + margin.top + margin.bottom)
                    .call(zoom)
                    .on('mousemove', mousemove)
                    .on('mouseover', function() {
                        focus.style('display', null);
                    })
                    .on('mouseout', function() {
                        if (!d3.event.toElement || $(d3.event.toElement).closest('svg').length === 0) {
                            focus.style('display', 'none');
                        }
                    }),

                preventDragOverGraph = svgOuter.append('rect')
                    .attr({
                        class: 'preventDrag',
                        x: 0,
                        y: 0,
                        width: '100%',
                        height: height + margin.top
                    })
                    .on('mousedown', function() {
                        d3.event.stopPropagation();
                    }),

                axisOverlay = svgOuter.append('rect')
                    .attr({
                        class: 'axis-overlay',
                        x: 0,
                        y: height + margin.top,
                        width: '100%',
                        height: margin.bottom
                    }),

                svg = svgOuter
                    .append('g')
                    .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')'),

                barGroup = this.barGroup = svg.append('g')
                    .attr('class', 'barGroup'),

                bar = this.createBars(data),

                focus = svg.append('g')
                    .attr('class', 'focus')
                    .style('display', 'none'),

                gBrush = svgOuter.append('g')
                    .attr('class', 'brush')
                    .attr(
                        'transform',
                        'translate(' + margin.left + ',' + Math.max(0, margin.top - BRUSH_PADDING - 1) + ')'
                    )
                    .on('mousedown', function() {
                        d3.event.stopPropagation();
                    })
                    .call(brush),

                gBrushRects = gBrush.selectAll('rect')
                    .attr('height', height + BRUSH_PADDING * 2),

                gBrushText = gBrush.append('g')
                    .style('display', 'none')
                    .attr('class', 'brushText'),

                gBrushTextStartBackground = gBrushText.append('rect')
                    .attr({
                        x: 0.5,
                        y: 0.5,
                        height: BRUSH_BACKGROUND_HEIGHT
                    }),

                gBrushTextEndBackground = gBrushText.append('rect')
                    .attr({
                        x: 0.5,
                        y: height - BRUSH_BACKGROUND_HEIGHT,
                        height: BRUSH_BACKGROUND_HEIGHT
                    }),

                gBrushTextStart = gBrushText.append('text')
                    .attr({
                        x: BRUSH_TEXT_PADDING,
                        y: Math.max(0, margin.top - BRUSH_PADDING + BRUSH_TEXT_PADDING)
                    }),

                gBrushTextEnd = gBrushText.append('text')
                    .attr({
                        y: height + BRUSH_PADDING - BRUSH_TEXT_PADDING,
                        'text-anchor': 'end'
                    });

            if (isDate) {
                xAxis.tickFormat(d3.time.format.utc.multi([
                    ['.%L', function(d) {
                        return d.getMilliseconds();
                    }],
                    [':%S', function(d) {
                        return d.getSeconds();
                    }],
                    ['%I:%M', function(d) {
                        return d.getMinutes();
                    }],
                    ['%I %p', function(d) {
                        return d.getHours();
                    }],
                    ['%a %d', function(d) {
                        return d.getDay() && d.getDate() != 1;
                    }],
                    ['%b %d', function(d) {
                        return d.getDate() != 1;
                    }],
                    ['%b', function(d) {
                        return d.getMonth();
                    }],
                    ['%Y', function() {
                        return true;
                    }]
                ]));
            } else {
                xAxis.tickFormat(function(d) {
                    return d3.format('s')(d)
                        .replace(/(\.\d{2})\d+/, '$1');
                });
            }

            this.xAxis = xAxis;
            this.brush = brush;
            this.focus = focus;

            focus.append('text')
                .attr('y', height + margin.bottom)
                .attr('text-anchor', 'middle');

            focus.append('rect')
                .attr('class', 'scrub')
                .attr('y', height)
                .attr('width', 1)
                .attr('height', margin.bottom * 0.6);

            var axis = svg.append('g')
                .attr('class', 'x axis')
                .attr('transform', 'translate(0,' + height + ')');
            axis.call(xAxis);

            function calculateValuesExtent() {
                var min, max, delta;

                if (isDate) {
                    if (values.length === 0) {
                        return [
                            d3.time.day.offset(new Date(), -1),
                            d3.time.day.offset(new Date(), 2)
                        ];
                    } else if (values.length === 1) {
                        return [
                            d3.time.day.offset(values[0], -1),
                            d3.time.day.offset(values[0], 2)
                        ];
                    } else {
                        min = d3.min(values);
                        max = d3.max(values);
                        delta = max - min;

                        var days = Math.max(1,
                            parseInt(Math.round(delta / 1000 / 60 / 60 / 24 * 0.1), 10)
                        );

                        return [
                            d3.time.day.offset(min, days * -1),
                            d3.time.day.offset(max, days)
                        ];
                    }
                } else if (values.length === 0) {
                    return [0, 100];
                } else if (values.length === 1) {
                    return [values[0] - 1, values[0] + 1];
                }

                min = d3.min(values);
                max = d3.max(values);
                delta = max - min;

                return [
                    min - delta * 0.1,
                    max + delta * 0.1
                ];
            }

            function createZoomBehavior() {
                var delta = valuesExtent[1] - valuesExtent[0],
                    maxZoomIn = delta / (isDateTime ? 36e5 : isDate ? 36e5 * 48 : 10),
                    maxZoomOut = delta / (isDate ? (50 * 365 * 24 * 36e5) : 1);

                return d3.behavior.zoom()
                    .x(xScale)
                    .scaleExtent([1 / 2, maxZoomIn])
                    .on('zoom', onZoomed);
            }

            function createXScale() {
                if (isDateTime || isDate) {
                    return d3.time.scale()
                        .domain(valuesExtent)
                        .range([0, width]);
                }

                return d3.scale.linear()
                    .domain(valuesExtent)
                    .range([0, width]);
            }

            var brushedTextFormat = xAxis.tickFormat();//d3.format('0.2f');

            function updateBrushInfo(brushExtent, brushExtentDelta) {
                var extent = brushExtent || brush.extent(),
                    delta = _.isUndefined(brushExtent) ?
                        (extent[1] - extent[0]) : brushExtentDelta,
                    width = Math.max(0, xScale(
                             isDate ?
                             new Date(xScale.domain()[0].getTime() + delta) :
                             (xScale.domain()[0] + delta)
                        ) - 1);

                self.trigger('updateHistogramExtent', {
                    extent: delta < 0.00001 ? null : extent
                });

                gBrushTextStartBackground.attr('width', width);
                gBrushTextEndBackground.attr('width', width);
                gBrushTextStart.text(brushedTextFormat(extent[0]));

                gBrushTextEnd
                    .text(brushedTextFormat(extent[1]))
                    .attr(
                        'transform',
                        'translate(' + (width - BRUSH_TEXT_PADDING) + ', 0)'
                    );
            }

            var mouse = null;
            function mousemove() {
                mouse = d3.mouse(this)[0];
                updateFocusInfo();
            }

            var format = isDate && '%Y-%m-%d';
            if (isDateTime) {
                format += ' %I %p';
            }
            format = format && d3.time.format(format);
            if (format) {
                brushedTextFormat = format;
            }
            function updateFocusInfo() {
                if (mouse !== null) {
                    var x0 = xScale.invert(mouse - margin.left);
                    focus.attr('transform', 'translate(' + xScale(x0) + ', 0)');
                    if (isDate) {
                        focus.select('text').text(format(x0));
                    } else {
                        focus.select('text').text(xAxis.tickFormat()(x0));
                    }
                }
            }

            this.redraw();

        }

        this.createBars = function(data, animate) {
            var height = this.height,
                xScale = this.xScale,
                yScale = this.yScale,
                dx = data.length > 0 ? data[0].dx : 0,
                keys = {},
                bars = this.barGroup.selectAll('.bar').data(data),
                isDate = this.attr.property.dataType === 'date',
                isDateTime = this.attr.property.displayType !== 'dateOnly';

            bars.enter()
                .append('g').attr('class', 'bar')
                .append('rect');

            bars
                .attr('transform', function(d) {
                    var dX = isDate && !isDateTime ? d3.time.day.floor(d.x) : d.x;
                    return 'translate(' + xScale(dX) + ',' + yScale(d.y) + ')';
                }).select('rect')
                    .attr('width',
                        Math.max(1,
                            (isDate ?
                             xScale(xScale.domain()[0].getTime() + dx) :
                             xScale(xScale.domain()[0] + dx)
                            ) - 1
                        )
                    )
                    .attr('height', function(d) {
                        return height - yScale(d.y);
                    });

            bars.exit().remove();
            this.barGroup.selectAll('.bar').filter(function(d) {
                return d.y === 0;
            }).remove();

            return bars;
        }

    }
});
