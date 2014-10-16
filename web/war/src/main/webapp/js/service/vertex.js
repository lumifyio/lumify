define([
    'service/serviceBase',
    'util/formatters'
], function(ServiceBase, F) {
    'use strict';

    function VertexService() {
        ServiceBase.call(this);
        return this;
    }

    VertexService.prototype = Object.create(ServiceBase.prototype);

    VertexService.prototype.getAudits = function(vertexId) {
        return this._ajaxGet({
            url: 'vertex/audit',
            data: {
                graphVertexId: vertexId
            }
        });
    };

    VertexService.prototype.setProperty = function(
        vertexId,
        propertyKey,
        propertyName,
        value,
        visibilitySource,
        justificationText,
        sourceInfo,
        metadata,
        workspaceId
    ) {
        return this._ajaxPost({
            url: 'vertex/property',
            data: _.tap({
                graphVertexId: vertexId,
                propertyName: propertyName,
                value: value,
                visibilitySource: visibilitySource,
                justificationText: justificationText
            }, function(o) {
                if (sourceInfo) {
                    o.sourceInfo = JSON.stringify(sourceInfo);
                }
                if (propertyKey) {
                    o.propertyKey = propertyKey
                }
                if (metadata) {
                    o.metadata = JSON.stringify(metadata)
                }
                if (workspaceId) {
                    o.workspaceId = workspaceId;
                }
            })
        });
    };

    VertexService.prototype.setVisibility = function(vertexId, visibilitySource) {
        return this._ajaxPost({
            url: 'vertex/visibility',
            data: {
                graphVertexId: vertexId,
                visibilitySource: visibilitySource
            }
        });
    };

    VertexService.prototype.deleteProperty = function(vertexId, property, workspaceId) {
        return this._ajaxDelete({
            url: 'vertex/property?' + $.param(_.tap({
                    graphVertexId: vertexId,
                    propertyName: property.name,
                    propertyKey: property.key
                }, function(data) {
                    if (workspaceId) {
                        data.workspaceId = workspaceId;
                    }
                }))
        });
    };

    VertexService.prototype.uploadImage = function(vertexId, imageFile) {
        var formData = new FormData();

        formData.append('file', imageFile);

        return this._ajaxUpload({
            url: 'vertex/upload-image?' + $.param({
                graphVertexId: vertexId
            }),
            data: formData
        });
    };

    VertexService.prototype.createVertex = function(conceptType, visibilitySource) {
        return this._ajaxPost({
            url: 'vertex/new',
            data: {
                conceptType: conceptType,
                visibilitySource: visibilitySource
            }
        })
    };

    VertexService.prototype.importFiles = function(files, visibilitySource) {
        var formData = new FormData(),
            pluralString = F.string.plural(files.length, 'file');

        _.forEach(files, function(f) {
            formData.append('file', f);
            if (_.isString(visibilitySource)) {
                formData.append('visibilitySource', visibilitySource);
            }
        });

        if (_.isArray(visibilitySource)) {
            _.forEach(visibilitySource, function(v) {
                formData.append('visibilitySource', v);
            });
        }

        return this._ajaxUpload({
            activityMessages: [
                'Importing ' + pluralString,
                'Imported ' + pluralString
            ],
            url: 'vertex/import',
            data: formData
        });
    };

    VertexService.prototype.getMultiple = function(vertexIds, fallbackToPublic) {
        return this._ajaxPost({
            url: 'vertex/multiple',
            data: {
                vertexIds: vertexIds,
                fallbackToPublic: fallbackToPublic
            }
        });
    };

    // Move to edgeService
    VertexService.prototype.deleteEdge = function(sourceId, targetId, label, edgeId) {
        return this._ajaxDelete({
            url: 'vertex/edge?' + $.param({
                sourceId: sourceId,
                targetId: targetId,
                label: label,
                edgeId: edgeId
            })
        });
    };

    VertexService.prototype.findPath = function(data) {
        return this._ajaxGet({
            url: 'vertex/find-path',
            data: data
        });
    };

    VertexService.prototype.geoSearch = function(lat, lon, radiuskm) {
        return this._ajaxGet({
            url: 'vertex/geo-search',
            data: {
                lat: lat,
                lon: lon,
                radius: radiuskm
            }
        });
    };

    VertexService.prototype.getStatementByRowKey = function(statementRowKey) {
        return this._get('statement', statementRowKey);
    };

    VertexService.prototype.search = function(query, filters, conceptType, paging) {
        var data = {};

        if (conceptType) data.conceptType = conceptType;
        if (paging) {
            if (paging.offset) data.offset = paging.offset;
            if (paging.size) data.size = paging.size;
        }

        var q = _.isUndefined(query.query) ? query : query.query;
        if (q) {
            data.q = q;
        }
        if (query && query.relatedToVertexId) {
            data.relatedToVertexId = query.relatedToVertexId;
        }
        data.filter = JSON.stringify(filters || []);

        return this._ajaxGet({
            url: 'vertex/search',
            data: data
        });
    };

    VertexService.prototype.getArtifactHighlightedTextById = function(graphVertexId, propertyKey) {
        return this._ajaxGet({
            dataType: 'html',
            url: 'vertex/highlighted-text',
            data: {
                graphVertexId: graphVertexId,
                propertyKey: propertyKey
            }
        });
    };

    VertexService.prototype.getRelatedVertices = function(data) {
        return this._ajaxGet({
            url: 'vertex/find-related',
            data: {
                graphVertexId: data.graphVertexId,
                limitParentConceptId: data.limitParentConceptId
            }
        });
    };

    VertexService.prototype.getVertexRelationships = function(graphVertexId, paging, edgeLabel) {
        var data = paging || {};
        if (edgeLabel) {
            data.edgeLabel = edgeLabel;
        }
        data.graphVertexId = graphVertexId;
        return this._ajaxGet({
            url: 'vertex/edges',
            data: data
        });
    };

    VertexService.prototype.getVertexProperties = function(graphVertexId, workspaceId) {
        return this._ajaxGet({
            url: 'vertex/properties',
            data: {
                graphVertexId: graphVertexId,
                workspaceId: workspaceId
            }
        });
    };

    VertexService.prototype._get = function(resource, id) {
        if (!id) {
            throw new Error("Invalid or no id specified for resource '" + resource + "'");
        }

        //maybe it's an object for future options stuff?
        var graphVertexId = (typeof id == 'object' ? id.id : id);
        return this._ajaxGet({
            url: resource,
            data: {
                graphVertexId: graphVertexId
            }
        });
    };

    VertexService.prototype.resolveTerm = function(resolveRequest) {
        return this._ajaxPost({
            url: 'vertex/resolve-term',
            data: resolveRequest
        });
    };

    VertexService.prototype.unresolveTerm = function(unresolveRequest) {
        return this._ajaxPost({
            url: 'vertex/unresolve-term',
            data: unresolveRequest
        });
    };

    VertexService.prototype.resolveDetectedObject = function(resolveRequest) {
        return this._ajaxPost({
            url: 'vertex/resolve-detected-object',
            data: resolveRequest
        });
    };

    VertexService.prototype.unresolveDetectedObject = function(unresolveRequest) {
        return this._ajaxPost({
            url: 'vertex/unresolve-detected-object',
            data: unresolveRequest
        });
    };

    return VertexService;
});
