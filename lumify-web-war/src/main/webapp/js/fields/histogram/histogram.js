define([
    'flight/lib/component',
    'hbs!./histogramTpl',
    'd3'
], function(
    defineComponent,
    template,
    d3) {
    'use strict';

    var BRUSH_PADDING = 0,
        BRUSH_TEXT_PADDING = 2,
        BRUSH_BACKGROUND_HEIGHT = 13,
        MAX_ZOOM = 20;

    return defineComponent(Histogram);

    function inDomain(d, xScale) {
        var domain = xScale.domain();
        return d > domain[0] && d < domain[1];
    }

    function Histogram() {

        var margin = {top: 10, right: 16, bottom: 40, left: 16};

        this.after('initialize', function() {
            this.$node.html(template({}));
            this.renderChart();

            this.onGraphPaddingUpdated = _.debounce(this.onGraphPaddingUpdated.bind(this), 500);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
        });

        this.onGraphPaddingUpdated = function(event, data) {
            var padding = data.padding,
                width = this.width = this.$node.scrollParent().width() - margin.left - margin.right,
                height = width / (16 / 9);

            this.$node.find('svg').attr('width', width + margin.left + margin.right);

            this.x.range([0, width]);
            this.focus.style('display', 'none');
            this.redraw();
        };

        this.redraw = function(rebin) {
            var x = this.x,
                y = this.y,
                data = this.data;

            if (rebin) {
                var valuesInDomain = _.filter(this.values, function(v) {
                        return true;//inDomain(v, x);
                    });

                data = d3.layout.histogram()
                        .bins(
                            Math.min(this.width, valuesInDomain.length)
                            //x.ticks(100)
                        )(valuesInDomain);
            }

                y.domain([0, d3.max(data, function(d) {
                    return d.y;
                })])

            this.brush.x(this.zoom.x())
            this.createBars(data);
            this.svg.select('.brush').call(this.brush.x(x));
            this.svg.select('.x.axis').call(this.xAxis.orient('bottom'));
        };

        this.renderChart = function(optionalHeight) {

            var self = this,

                // Generate a Bates distribution of 10 random variables.
                values = this.values =
                    //[95, 95, 3, 30],
                    ///*
                    d3.range(100).map(d3.random.bates(10))
                    .concat(d3.range(2000).map(function() {
                        return d3.random.bates(10)() * 10;
                    }))
                    .concat(d3.range(200).map(function() {
                        return d3.random.bates(10)() * 1.5 + 1;
                    })),
                    //*/

                // A formatter for counts.
                formatCount = d3.format(',.0f'),

                width = this.width = this.$node.scrollParent().width() - margin.left - margin.right,
                height = this.height = 100 - margin.top - margin.bottom,

                data = this.data = d3.layout.histogram()
                            .bins(
                                Math.min(width, values.length)
                                //x.ticks(100)
                            )(values),

                x = this.x = d3.scale.linear()
                    .domain(d3.extent(data, function(d) {
                        return d.x;
                    }))
                    .range([0, width]),

                y = this.y = d3.scale.linear()
                    .domain([0, d3.max(data, function(d) {
                        return d.y;
                    })])
                    .range([height, 0]),

                xAxis = d3.svg.axis()
                    .scale(x)
                    .ticks(4)
                    .tickSize(5, 0)
                    .tickFormat(function(d) {
                        var s = d3.format('s')(d)
                            f = s.replace(/^(-?)(\d+(\.\d{1,2})?).*$/, '$1$2')
                                .replace(/^(-?)0+/, '$1');

                        if (f.indexOf('.') >= 0) {
                            f = f.replace(/[0.]+$/g, '')
                        }

                        if (f === '.' || f === '') return '0';
                        return f;
                    })
                    .orient('bottom'),

                zoom = this.zoom = d3.behavior.zoom()
                    .x(x)
                    .scaleExtent([1 / MAX_ZOOM, MAX_ZOOM])
                    .on('zoom', zoomed),

                brush = d3.svg.brush()
                    .x(zoom.x())
                    .on('brush', brushed),

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

            function zoomed() {
                var scale = d3.event.scale,
                    scaleChange = scale !== self.previousScale;
                self.redraw(scaleChange);
                self.previousScale = scale;
                updateBrushInfo();
            }

            var brushedTextFormat = d3.format('0.2f');
            function brushed() {
                requestAnimationFrame(function() {
                    var extent = brush.extent(),
                        delta = extent[1] - extent[0];

                    gBrushText
                        .style('display', delta < 0.01 ? 'none' : '')
                        .attr('transform', 'translate(' + x(extent[0]) + ', 0)');

                    updateBrushInfo(extent, delta);
                });
            }

            function updateBrushInfo(brushExtent, brushExtentDelta) {
                var extent = brushExtent || brush.extent(),
                    delta = _.isUndefined(brushExtent) ?
                        (extent[1] - extent[0]) : brushExtentDelta,
                    width = Math.max(0, x(x.domain()[0] + delta) - 1);

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

            function mousemove() {
                var mouse = d3.mouse(this)[0];
                requestAnimationFrame(function() {
                    var x0 = x.invert(mouse - margin.left);
                    focus.attr('transform', 'translate(' + (x(x0)) + ', 0)');
                    focus.select('text').text(x0.toFixed(2));
                });
            }

            this.redraw();

        }

        this.createBars = function(data) {
            var height = this.height,
                x = this.x,
                y = this.y,
                bars = this.barGroup.selectAll('.bar').data(data);

            bars.enter()
                .append('g')
                    .attr('class', 'bar')
                .append('rect')
                    .attr('x', 1);
            bars.attr('transform', function(d) {
                return 'translate(' + x(d.x) + ',' + y(d.y) + ')';
            }).select('rect')
                    .attr('width', Math.max(1, x(x.domain()[0] + data[0].dx) - 1))
                    .attr('height', function(d) {
                        return height - y(d.y);
                    });
            bars.exit().remove();

            return bars;
        }

    }
});
