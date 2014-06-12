define([
    'flight/lib/component',
    'hbs!./histogramTpl',
    'd3'
], function(
    defineComponent,
    template,
    d3) {
    'use strict';

    var MAX_ZOOM = 8;

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

            var domainRange = x.domain()[1] - x.domain()[0];
            if (domainRange < 0.5) {
                this.xAxis.tickFormat(d3.format(',.2f'))
            } else if (domainRange < 3.0) {
                this.xAxis.tickFormat(d3.format(',.1f'))
            } else this.xAxis.tickFormat(d3.format(',.0f'))

            this.brush.x(this.zoom.x())
            this.svg.selectAll('.bar').remove();
            this.createBars(data);
            this.svg.select('.brush').call(this.brush.x(x));
            this.svg.select('.x.axis').call(this.xAxis.orient('bottom'));
        };

        this.renderChart = function(optionalHeight) {

            var self = this,

                // Generate a Bates distribution of 10 random variables.
                values = this.values = d3.range(100).map(d3.random.bates(10))
                    .concat(d3.range(200).map(function() {
                        return d3.random.bates(10)() + 3;
                    })),

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
                    .ticks(5)
                    .tickFormat(d3.format(',.1f'))
                    .orient('bottom'),

                zoom = this.zoom = d3.behavior.zoom()
                    .x(x)
                    /*
                    .scale((function() {
                        var extent = x.(),
                            e = d3.extent(data),
                            dataDelta = (e[1].x - e[0].x),
                            scaleDelta = (extent[1] - extent[2]),
                            scale = dataDelta / scaleDelta;

                        console.log(dataDelta, scaleDelta, scale);

                        return scale;
                    })())
                    */
                    .scaleExtent([1 / MAX_ZOOM, MAX_ZOOM])
                    .on('zoom', zoomed),

                brush = d3.svg.brush()
                    .x(zoom.x())
                    .on('brush', brushed),

                svg = this.svg = d3.select(this.node).append('svg')
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
                    })
                    .append('g')
                    .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')'),

                barGroup = this.barGroup = svg.append('g').attr('class', 'barGroup'),

                bar = this.createBars(data),

                focus = svg.append('g')
                    .attr('class', 'focus')
                    .style('display', 'none');

                //gBrush = svg.append('g')
                    //.attr('class', 'brush')
                    //.call(brush);

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

            //gBrush.selectAll('rect')
                //.attr('height', height);

            var axis = svg.append('g')
                .attr('class', 'x axis')
                .attr('transform', 'translate(0,' + height + ')');
            axis.call(xAxis);
            //axis.call(zoom);

            function zoomed() {

                // TODO: this is the filter?
                //console.log(x.domain())
                /* Attempt at clamping
                var dom = x.domain(),
                    dt = dom[1] - dom[0],
                    dataExtent = d3.extent(data);

                if (dom[0] < dataExtent[0].x) dom[0] = dataExtent[0].x;
                else dom[1] = dom[0] + dt;
                if (dom[1] > dataExtent[1].x) dom[1] = dataExtent[1].x;
                else dom[0] = dom[1] - dt;
                x.domain(dom);
                */
                var scale = d3.event.scale,
                    scaleChange = scale !== self.previousScale;
                self.redraw(scaleChange);
                self.previousScale = scale;
            }

            function brushed() {
                //console.log(brush.extent());
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

            window.data = data;

            /*
            bars.enter()
                .append('g')
                    .attr('class', 'bar')
                    .attr('transform', function(d) {
                        return 'translate(' + x(d.x) + ',' + y(d.y) + ')';
                    })
                .append('rect')
                    .attr('x', 1)
                    .attr('width', x(data[0].dx))
                    .attr('height', function(d) {
                        return height - y(d.y);
                    });
            */

            bars.enter()
                .append('g')
                    .attr('class', 'bar')
                .append('rect')
                    .attr('x', 1);
            bars.attr('transform', function(d) {
                return 'translate(' + x(d.x) + ',' + y(d.y) + ')';
            }).select('rect')
                    .attr('width', 1)//x(data[0].dx))
                    .attr('height', function(d) {
                        return height - y(d.y);
                    });
            bars.exit().remove();

            return bars;
        }

    }
});
