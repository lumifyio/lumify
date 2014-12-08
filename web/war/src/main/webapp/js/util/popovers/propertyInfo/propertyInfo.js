
define([
    'flight/lib/component',
    '../withPopover',
    'util/vertex/formatters',
    'util/withDataRequest',
    'd3'
], function(
    defineComponent,
    withPopover,
    F,
    withDataRequest,
    d3) {
    'use strict';

    return defineComponent(PropertyInfo, withPopover, withDataRequest);

    function PropertyInfo() {

        this.defaultAttrs({
            deleteButtonSelector: '.btn-danger',
            editButtonSelector: '.btn-edit',
            addButtonSelector: '.btn-add',
            replyButtonSelector: '.reply',
            justificationValueSelector: 'a'
        });

        this.before('initialize', function(node, config) {
            config.template = 'propertyInfo/template';
        });

        this.after('initialize', function() {
            var self = this;

            this.after('setupWithTemplate', function() {
                this.dataRequest('config', 'properties')
                    .done(function(config) {
                        var splitRegex = /\s*,\s*/,
                            metadataDisplay =
                                config['properties.metadata.propertyNamesDisplay'].split(splitRegex).map(i18n),
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
                            replyButtonSelector: self.onReply,
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
                isComment = property.name === 'http://lumify.io/comment#entry',
                isCommentCreator = isComment &&
                    property.metadata['http://lumify.io#modifiedBy'] === lumifyData.currentUser.id,
                canEdit = isComment ?
                    isCommentCreator :
                    (
                        F.vertex.sandboxStatus(property) ||
                        property.name === 'http://lumify.io#visibilityJson'
                    ),
                canDelete = canEdit && property.name !== 'http://lumify.io#visibilityJson',
                metadata = _.chain(this.metadataProperties || [])
                    .map(function(name) {
                        if ('metadata' in property) {
                            if (name in property.metadata) {
                                return [name, property.metadata[name]];
                            }
                        }
                        if (name in property) {
                            return [name, property[name]];
                        }
                    })
                    .compact()
                    .value()
                row = this.contentRoot.select('table')
                    .selectAll('tr')
                    .data(metadata)
                    .call(function() {
                        this.enter()
                            .append('tr')
                            .call(function() {
                                this.append('td').attr('class', 'property-name');
                                this.append('td').attr('class', 'property-value');
                            });
                    });

            this.contentRoot.select('.btn-danger')
                .style('display', canDelete ? 'inline' : 'none')
                .classed('requires-EDIT', !isComment)
                .classed('requires-COMMENT', isComment)
            this.contentRoot.select('.editadd')
                .style('display', isComment && !isCommentCreator ? 'none' : 'inline')
                .classed('btn-edit', canEdit)
                .classed('btn-add', !canEdit)
                .classed('requires-EDIT', !isComment)
                .classed('requires-COMMENT', isComment)
                .classed('nodelete', !canDelete)
                .text(canEdit ?
                  i18n('popovers.property_info.button.edit') :
                  i18n('popovers.property_info.button.add')
                );
            this.contentRoot.select('.reply').each(function() {
                var $this = $(this);
                if (isComment) {
                    $this.show();
                } else {
                    $this.hide();
                }
            })
            this.contentRoot.selectAll('tr')
                .call(function() {
                    var self = this;

                    this.select('td.property-name').text(function(d) {
                        return displayNames[d[0]];
                    });

                    var valueElement = self.select('td.property-value')
                        .each(function(d) {
                            var self = this,
                                $self = $(this),
                                typeName = displayTypes[d[0]],
                                formatter = F.vertex.metadata[typeName],
                                formatterAsync = F.vertex.metadata[typeName + 'Async'],
                                value = d[1];

                            if (formatter) {
                                formatter(this, value);
                            } else if (formatterAsync) {
                                formatterAsync(self, value, property, vertexId)
                                    .catch(function() {
                                        d3.select(self).text(i18n('popovers.property_info.error', value));
                                    })
                                    .finally(positionDialog);
                                d3.select(this).text(i18n('popovers.property_info.loading'));
                            } else {
                                console.warn('No metadata type formatter: ' + typeName);
                                d3.select(this).text(value);
                            }
                        });
                })

                // Hide blank metadata
                .each(function(d) {
                    $(this).toggle($(this).find('.property-value').text() !== '');
                });

            // Justification
            var justification = [];
            if (property.metadata &&
                (property.metadata._justificationMetadata || property.metadata._sourceMetadata)) {
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
                                        .text(i18n('popovers.property_info.justification'));
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
                                justificationMetadata: property.metadata._justificationMetadata,
                                sourceMetadata: property.metadata._sourceMetadata
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

        this.onReply = function() {
            var metadata = this.attr.property.metadata['http://lumify.io/comment#path'],
                path = (metadata ? (metadata + '/') : '') + this.attr.property.key;

            this.trigger('editProperty', {
                path: path
            });
            this.teardown();
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
            e.stopPropagation();
            var button = this.popover.find('.btn-danger').addClass('loading').attr('disabled', true);
            this.trigger('deleteProperty', {
                property: _.pick(this.attr.property, 'name', 'key')
            });
        };
    }
});
