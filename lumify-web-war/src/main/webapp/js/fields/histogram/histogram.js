define([
    'flight/lib/component',
    'hbs!./histogramTpl',
    'd3'
], function(
    defineComponent,
    template,
    d3) {
    'use strict';

    return defineComponent(Histogram);

    function Histogram() {

        var margin = {top: 10, right: 16, bottom: 40, left: 16};

        this.after('initialize', function() {
            console.log(this.attr)
            this.$node.html(template({}));
            this.renderChart();

            this.onGraphPaddingUpdated = _.debounce(this.onGraphPaddingUpdated.bind(this), 500);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
        });

        this.onGraphPaddingUpdated = function(event, data) {
            var padding = data.padding,
                width = this.$node.scrollParent().width() - margin.left - margin.right,
                height = width / (16 / 9);

            this.$node.find('svg').attr('width', width + margin.left + margin.right);

            this.x.range([0, width]);
            this.focus.style('display', 'none');
            this.redraw();
        };

        this.redraw = function() {
            var x = this.x,
                y = this.y,
                data = d3.layout.histogram().bins(x.ticks(100))(this.values);

            this.svg.selectAll('.bar').remove();
            this.createBars(data);
            this.svg.select('.brush').call(this.brush.x(x));
            this.svg.select('.x.axis').call(this.xAxis.orient('bottom'));
        };

        this.renderChart = function(optionalHeight) {

            var self = this,

                // Generate a Bates distribution of 10 random variables.
                values = d3.range(1000).map(d3.random.bates(10)),

                // A formatter for counts.
                formatCount = d3.format(',.0f'),

                width = this.$node.scrollParent().width() - margin.left - margin.right,
                height = this.height = 100 - margin.top - margin.bottom,

                x = this.x = d3.scale.linear()
                    .domain([0, 1])
                    .range([0, width]),

                brush = d3.svg.brush()
                    .x(x)
                    .on('brush', brushed),

                data = d3.layout.histogram().bins(x.ticks(100))(values),

                y = this.y = d3.scale.linear()
                    .domain([0, d3.max(data, function(d) {
                        return d.y;
                    })])
                    .range([height, 0]),

                xAxis = d3.svg.axis()
                    .scale(x)
                    .ticks(5)
                    .orient('bottom'),

                zoom = d3.behavior.zoom()
                    .x(x)
                    .on('zoom', zoomed),

                svg = this.svg = d3.select(this.node).append('svg')
                    .attr('width', width + margin.left + margin.right)
                    .attr('height', height + margin.top + margin.bottom)
                    .on('mouseover', function() {
                        focus.style('display', null);
                    })
                    .on('mouseout', function() {
                        console.log(d3.event.toElement);
                        if (!d3.event.toElement || $(d3.event.toElement).closest('svg').length === 0) {
                            focus.style('display', 'none');
                        }
                    })
                    .append('g')
                    .on('mousemove', mousemove)
                    .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')')
                    .append('g')
                    .call(zoom),

                bar = this.createBars(data),

                focus = svg.append('g')
                    .attr('class', 'focus')
                    .style('display', 'none'),

                gBrush = svg.append('g')
                    .attr('class', 'brush')
                    .call(brush);

            this.values = values;
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

            gBrush.selectAll('rect')
                .attr('height', height);

            svg.append('g')
                .attr('class', 'x axis')
                .attr('transform', 'translate(0,' + height + ')')
                .call(xAxis);

            function zoomed() {
                //svg.attr('transform', 'translate(' + d3.event.translate + ')scale(' + d3.event.scale + ')');
                self.redraw();
            }

            function brushed() {
                //console.log(brush.extent());
            }

            function mousemove() {
                var mouse = d3.mouse(this)[0];
                requestAnimationFrame(function() {
                    var x0 = x.invert(mouse);
                    focus.attr('transform', 'translate(' + (x(x0)) + ', 0)');
                    focus.select('text').text(x0.toFixed(2));
                });
            }

        }

        this.createBars = function(data) {
            var height = this.height,
                x = this.x,
                y = this.y;

            return this.svg.selectAll('.bar')
                .data(data)
                .enter().append('g')
                .attr('class', 'bar')
                .attr('transform', function(d) {
                    return 'translate(' + x(d.x) + ',' + y(d.y) + ')';
                })
                .append('rect')
                .attr('x', 1)
                .attr('width', x(data[0].dx) - 1)
                .attr('height', function(d) {
                    return height - y(d.y);
                });

        }

    }
});
