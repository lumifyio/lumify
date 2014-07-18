
define([
    'flight/lib/component',
    '../withPopover',
    'service/config',
    'util/vertex/formatters',
    'd3'
], function(
    defineComponent,
    withPopover,
    ConfigService,
    F,
    d3) {
    'use strict';

    var configService = new ConfigService();

    return defineComponent(PropertyInfo, withPopover);

    function PropertyInfo() {

        this.defaultAttrs({
            deleteButtonSelector: '.btn-danger',
            editButtonSelector: '.btn-edit',
            addButtonSelector: '.btn-add',
            justificationValueSelector: 'a'
        });

        this.before('initialize', function(node, config) {
            config.template = 'propertyInfo/template';
            config.hideUntilEvent = 'popoverRendered';
        });

        this.after('initialize', function() {
            var self = this;

            this.after('setupWithTemplate', function() {
                configService.getProperties().done(function(config) {
                    var splitRegex = /\s*,\s*/,
                        metadataDisplay =
                            config['properties.metadata.propertyNamesDisplay'].split(splitRegex),
                        metadataType =
                            config['properties.metadata.propertyNamesType'].split(splitRegex);

                    self.metadataProperties =
                        config['properties.metadata.propertyNames'].split(splitRegex);

                    if (self.metadataProperties.length !== metadataDisplay.length ||
                        self.metadataProperties.length !== metadataType.length) {
                        throw new Error('Metadata properties must have display names and types');
                    }
                    self.metadataPropertiesDisplayMap = _.object(self.metadataProperties, metadataDisplay);
                    self.metadataPropertiesTypeMap = _.object(self.metadataProperties, metadataType);

                    self.on(self.popover, 'click', {
                        deleteButtonSelector: self.onDelete,
                        editButtonSelector: self.onEdit,
                        addButtonSelector: self.onAdd,
                        justificationValueSelector: self.teardown
                    });

                    self.contentRoot = d3.select(self.popover.get(0))
                        .select('.popover-content');
                    self.update(self.attr.property);

                    self.on(document, 'verticesUpdated', self.onVerticesUpdated);
                });
            });
        });

        this.update = function(property) {
            var vertexId = this.attr.vertexId,
                positionDialog = this.positionDialog.bind(this),
                displayNames = this.metadataPropertiesDisplayMap,
                displayTypes = this.metadataPropertiesTypeMap,
                canEdit = F.vertex.sandboxStatus(property) ||
                    property.name === 'http://lumify.io#visibilityJson',
                metadata = _.pick.apply(_, [property].concat(this.metadataProperties)),
                transformed = _.chain(metadata)
                    .pairs()
                    .map(function(pair) {
                        var name = pair[0],
                            value = pair[1],
                            typeName = displayTypes[name],
                            formatter = F.vertex.metadata[typeName],
                            formatterAsync = F.vertex.metadata[typeName + 'Async'];

                        if (!formatter && !formatterAsync) {
                            console.warn('No metadata type formatter: ' + typeName);
                            return pair;
                        }

                        if (formatter) {
                            return [name, formatter(value, property, vertexId)];
                        }

                        return pair;
                    })
                    .reject(function(pair) {
                        return _.isUndefined(pair[1]);
                    })
                    .value(),
                row = this.contentRoot.select('table')
                    .selectAll('tr')
                    .data(transformed)
                    .call(function() {
                        this.enter()
                            .append('tr')
                            .call(function() {
                                this.append('td').attr('class', 'property-name');
                                this.append('td').attr('class', 'property-value');
                            });
                    });

            this.contentRoot.select('.btn-danger')
                .style('display', canEdit ? 'inline' : 'none');
            this.contentRoot.select('.editadd')
                .classed('btn-edit', canEdit)
                .classed('btn-add', !canEdit)
                .classed('nodelete', !canEdit)
                .text(canEdit ? 'Edit' : 'Add');
            this.contentRoot.selectAll('tr')
                .call(function() {
                    var self = this;

                    self.select('td.property-name').text(function(d) {
                        return displayNames[d[0]];
                    });

                    var valueElement = self.select('td.property-value')
                        .text(function(d) {
                            var self = this,
                                typeName = displayTypes[d[0]],
                                formatterAsync = F.vertex.metadata[typeName + 'Async'],
                                value = d[1];

                            if (formatterAsync) {
                                formatterAsync(value, property, vertexId)
                                    .fail(function() {
                                        d3.select(self).text('Error: ' + value);
                                    })
                                    .done(function(value) {
                                        d3.select(self).text(value);
                                    })
                                    .always(positionDialog);
                                return 'Loading...';
                            }

                            return value;
                        });
                });

            // Justification
            var justification = [];
            if (property._justificationMetadata || property._sourceMetadata) {
                justification.push(true);
            }

            var table = this.contentRoot.select('table'),
                justificationRow = this.contentRoot.selectAll('.justification')

            justificationRow
                .data(justification)
                .call(function() {
                    this.enter()
                        .call(function() {
                            this.insert('div', 'button').attr('class', 'justification')
                                .call(function() {
                                    this.append('div')
                                        .attr('class', 'property-name property-justification')
                                        .text('Justification');
                                    this.append('div')
                                        .attr('class', 'justificationValue');
                                });
                        });
                    this.exit().remove();

                    var node = this.select('.justificationValue').node();
                    if (node) {
                        require(['util/vertex/justification/viewer'], function(JustificationViewer) {
                            $(node).teardownAllComponents();
                            JustificationViewer.attachTo(node, {
                                justificationMetadata: property._justificationMetadata,
                                sourceMetadata: property._sourceMetadata
                            });
                            positionDialog();
                        });
                    }
                })

            row.exit().remove();

            positionDialog();
        };

        this.onVerticesUpdated = function(event, data) {
            var vertex = _.findWhere(data.vertices, {
                    id: this.attr.vertexId
                }),
                property = vertex && _.findWhere(vertex.properties, {
                    name: this.attr.property.name,
                    key: this.attr.property.key
                });
            if (vertex && !property) {
                this.teardown();
            } else if (property) {
                this.attr.property = property;
                this.update(property);
            }
        };

        this.onAdd = function() {
            this.trigger('editProperty', {
                property: _.omit(this.attr.property, 'key')
            });
            this.teardown();
        };

        this.onEdit = function() {
            this.trigger('editProperty', {
                property: this.attr.property
            });
            this.teardown();
        };

        this.onDelete = function(e) {
            var button = this.select('deleteButtonSelector').addClass('loading').attr('disabled', true);
            this.trigger('deleteProperty', {
                property: _.pick(this.attr.property, 'name', 'key')
            });
        };
    }
});
