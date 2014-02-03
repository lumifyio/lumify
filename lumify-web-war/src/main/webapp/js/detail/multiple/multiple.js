define([
    'flight/lib/component',
    'flight/lib/registry',
    'data',
    '../withTypeContent',
    'service/vertex',
    'service/ontology',
    'sf',
    'tpl!./multiple',
    'tpl!./histogram',
    'util/vertexList/list'
], function (defineComponent, registry, appData, withTypeContent, VertexService, OntologyService, sf, template, histogramTemplate, VertexList) {
    'use strict';

    var NO_HISTOGRAM_PROPERTIES = [

        // Would need to bin these intelligently to make useful
        'geoLocation', 'latitude', 'longitude', 

        // How aften would there actually be two with same title?
        'title',

        // TODO: should be displayed but needs custom binning
        'publishedDate'
    ];

    return defineComponent(Multiple, withTypeContent);

    function Multiple() {
        this.vertexService = new VertexService();
        this.ontologyService = new OntologyService();

        this.defaultAttrs({
            histogramSelector: '.multiple .histogram',
            histogramListSelector: '.multiple .nav-bar',
            vertexListSelector: '.multiple .vertices-list'
        });

        this.after('initialize', function () {
            var self = this,
                vertices = this.attr.data.filter(function (v) {
                    return v.properties._type.value != 'relationship';
                }),
                ids = _.pluck(vertices, 'id');

            this.$node.html(template({
                getClasses: this.classesForVertex,
                vertices: vertices,
                fullscreenButton: self.fullscreenButton(ids)
            }));

            this.on('selectObjects', this.onSelectObjects);

            var d3_deferred = $.Deferred();
            require(['d3'], d3_deferred.resolve);
            $.when(
                this.vertexService.getMultiple(ids),
                this.ontologyService.concepts(),
                this.ontologyService.properties(),
                d3_deferred
            ).done(function(verticesResponse, concepts, properties, d3) {
                var vertices = verticesResponse[0];

                VertexList.attachTo(self.select('vertexListSelector'), {
                    vertices: vertices
                });
                
                self.select('histogramSelector').remove();
                self.drawHistograms(vertices, concepts, properties);
            });

        });

        this.onSelectObjects = function(event, data) {
            event.stopPropagation();
            this.$node.find('.vertices-list').hide();
            this.$node.find('.multiple').addClass('viewing-vertex');

            var detailsContainer = this.$node.find('.details-container'),
                detailsContent = detailsContainer.find('.content'),
                instanceInfos = registry.findInstanceInfoByNode(detailsContent[0]);
            if (instanceInfos.length) {
                instanceInfos.forEach(function(info) {
                    info.instance.teardown();
                });
            }

            var vertices = data.vertices,
                first = vertices[0];

            if (this._selectedGraphId === first.id) {
                this.$node.find('.multiple').removeClass('viewing-vertex');
                this.$node.find('.vertices-list').show().find('.active').removeClass('active');
                this._selectedGraphId = null;
                return;
            }

            var self = this,
                moduleName = (
                    (first.properties._type.value != 'artifact' && first.properties._type.value != 'relationship') ? 'entity' :
                    (first.properties._type.value || 'entity')
                ).toLowerCase();

            this._selectedGraphId = first.id;
            require([
                'detail/' + moduleName + '/' + moduleName,
            ], function(Module) {
                Module.attachTo(detailsContent, { 
                    data: first
                });
                self.$node.find('.vertices-list').show();
            });
        };
        

        this.drawHistograms = function(vertices, concepts, properties) {
            var self = this,
                byProperty = calculateByProperty(vertices),
                fn = {
                    getPropertyDisplayName: getPropertyDisplayName.bind(null, properties),
                    getPropertyValueDisplay: getPropertyValueDisplay.bind(null, concepts, properties),
                    sortPropertyValues: sortPropertyValues.bind(null, properties, byProperty)
                };

            Object.keys(byProperty).filter(shouldDisplay).forEach(function(name) {

                var template = histogramTemplate({
                        displayName: fn.getPropertyDisplayName(name)
                    }),
                    container = self.select('histogramListSelector')
                        .append(template)
                        .find('.svg-container')
                        .last()
                        .on('mouseenter mouseleave', 'g', self.histogramHover.bind(self))
                        .on('click', 'g', self.histogramClick.bind(self)),
                    data = Object.keys(byProperty[name])
                        .map(function(key) { 
                            return { 
                                key:key, 
                                number:byProperty[name][key].length,
                                vertexIds: _.pluck(byProperty[name][key], 'id')
                            };
                        }),
                    width = container.width(),
                    height = data.length * 30,
                    x = d3.scale.linear()
                        .domain([0, d3.sum(data, function(d) { return d.number; })])
                        .range([0, 100]),
                    y = d3.scale.ordinal()
                        .domain(data.map(function(v) { return v.key; }))
                        .rangeRoundBands([0, height], 0.1),
                    svg = d3.select(container[0]).append('svg')
                        .attr("width", '100%')
                        .attr("height", height)
                        .selectAll(".bar")
                        .data(data)
                        .enter()
                        .append("g")
                        .attr('data-info', function(d) { return JSON.stringify($.extend({ property:name }, d)); });

                svg.append("rect")
                    .attr("class", "bar")
                    .attr("x", 0)
                    .attr("y", function(d, i) { return y(d.key); })
                    .attr("width", function(d) {return x(d.number) + '%'; })
                    .attr("height", function(d) { return y.rangeBand(); });

                svg.append("text")
                    .attr("class", "text")
                    .text(function(d) { return fn.getPropertyValueDisplay(name, d.key); })
                    .each(function() {
                        var height = this.getBBox().height;
                        this.setAttribute("dy", y.rangeBand() / 2 + height / 4 + 'px');
                    })
                .attr("x", 0)
                    .attr("y", function(d, i) { return y(d.key); })
                    .attr("dx", "10px")
                    .attr("fill", "#fff")
                    .attr("text-anchor", "start");

                svg.append("text")
                    .attr("class", "text-number")
                    .attr("text-anchor", "end")
                    .attr("x", function(d) { return x(d.number) + '%'; })
                    .attr("y", function(d, i) { return y(d.key); })
                    .text(function(d) { return d.number; })
                    .each(function() {
                        var height = this.getBBox().height;
                        this.setAttribute("dy", y.rangeBand() / 2 + height / 4 + 'px');

                        var text = this.previousSibling.getBBox();
                        var minX = text.width + text.x + 10;
                        var x = this.getBBox().x;
                        if (x < minX) {
                            this.setAttribute("dx", minX - x);
                        } else this.setAttribute("dx", "-10px");
                    });
            });

            function shouldDisplay(propertyName) {
                if (propertyName == '_conceptType') {
                    return true;
                } else if (/^[_]/.test(propertyName)) {
                    return false;
                } else if (NO_HISTOGRAM_PROPERTIES.indexOf(propertyName) >= 0) {
                    return false;
                }
                return true;
            }

            function sortPropertyValues(properties, byProperty, propertyName, a, b) {
                return genericCompare(byProperty[propertyName][a].length, byProperty[propertyName][b].length);
            }

            function genericCompare(a, b) {
                if (a == b) {
                    return 0;
                }
                return a > b ? -1 : 1;
            }

            function getPropertyDisplayName(properties, propertyName) {
                var propertyNameDisplay = propertyName;
                if (properties.byTitle[propertyNameDisplay]) {
                    propertyNameDisplay = properties.byTitle[propertyNameDisplay].displayName;
                }
                return propertyNameDisplay;
            }

            function getPropertyValueDisplay(concepts, properties, propertyName, propertyValue) {
                var propertyValueDisplay = propertyValue;
                if (propertyName == '_conceptType' && concepts.byId[propertyValue]) {
                    propertyValueDisplay = concepts.byId[propertyValue].title;
                } else if (properties.byTitle[propertyName]) {
                    switch (properties.byTitle[propertyName].dataType) {
                        case 'date':
                            propertyValueDisplay = sf("{0:yyyy/MM/dd}", new Date(parseInt(propertyValue)));
                            break;
                    }
                }
                if (propertyValueDisplay === '') return '[ blank ]';
                return propertyValueDisplay;
            }

            function calculateByProperty(vertices) {
                var byProperty = {};
                vertices.forEach(function (v) {
                    Object.keys(v.properties).forEach(function (propertyName) {
                        if (!byProperty[propertyName]) {
                            byProperty[propertyName] = {};
                        }
                        appendBin(byProperty[propertyName], v.properties[propertyName], v);
                    });
                });
                return byProperty;
            }

            function appendBin(hash, binName, val) {
                if (!hash[binName]) {
                    hash[binName] = [];
                }
                hash[binName].push(val);
            }
        };


        this.histogramHover = function(event, object) {
            var data = $(event.target).closest('g').data('info'),
                eventName = event.type === 'mouseenter' ? 'focus' : 'defocus';

            this.trigger(document, eventName + 'Vertices', { vertexIds:data.vertexIds });
        };

        this.histogramClick = function(event, object) {
            var data = $(event.target).closest('g').data('info');

            this.trigger(document, 'selectObjects', { vertices:appData.vertices(data.vertexIds) });
            this.trigger(document, 'defocusVertices', { vertexIds:data.vertexIds });
        };
    }
});
