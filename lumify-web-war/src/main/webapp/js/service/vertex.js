define([
    'service/serviceBase',
    'util/formatters'
], function(ServiceBase, formatters) {
    'use strict';

    function VertexService() {
        ServiceBase.call(this);
        return this;
    }

    VertexService.prototype = Object.create(ServiceBase.prototype);

    VertexService.prototype.setProperty = function(
        vertexId,
        propertyName,
        value,
        visibilitySource,
        justificationText,
        sourceInfo
    ) {
        return this._ajaxPost({
            url: 'vertex/property/set',
            data: {
                graphVertexId: vertexId,
                propertyName: propertyName,
                value: value,
                visibilitySource: visibilitySource,
                justificationText: justificationText,
                sourceInfo: JSON.stringify(sourceInfo)
            }
        });
    };

    VertexService.prototype.setVisibility = function(vertexId, visibilitySource) {
        return this._ajaxPost({
            url: 'vertex/visibility/set',
            data: {
                graphVertexId: vertexId,
                visibilitySource: visibilitySource
            }
        });
    };

    VertexService.prototype.deleteProperty = function(vertexId, propertyName) {
        return this._ajaxPost({
            url: 'vertex/property/delete',
            data: {
                graphVertexId: vertexId,
                propertyName: propertyName
            }
        });
    };

    VertexService.prototype.uploadImage = function(vertexId, imageFile) {
        var formData = new FormData();

        formData.append('file', imageFile);

        return this._ajaxUpload({
            url: 'graph/vertex/uploadImage?' + $.param({
                graphVertexId: vertexId
            }),
            data: formData
        });
    };

    VertexService.prototype.importFiles = function(files, visibilitySource) {
        var formData = new FormData(),
            pluralString = formatters.string.plural(files.length, 'file');

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
            url: 'artifact/import',
            data: formData
        });
    };

    VertexService.prototype.getMultiple = function(vertexIds) {
        return this._ajaxPost({
            url: 'vertex/multiple',
            data: {
                vertexIds: vertexIds
            }
        });
    };

    VertexService.prototype.deleteEdge = function(sourceId, targetId, label, edgeId) {
        return this._ajaxPost({
            url: 'vertex/removeRelationship',
            data: {
                sourceId: sourceId,
                targetId: targetId,
                label: label,
                edgeId: edgeId
            }
        });
    };

    VertexService.prototype.findPath = function(data) {
        return this._ajaxGet({
            url: 'graph/findPath',
            data: data
        });
    };

    VertexService.prototype.locationSearch = function(lat, lon, radiuskm) {
        return this._ajaxGet({
            url: 'graph/vertex/geoLocationSearch',
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

    VertexService.prototype.graphVertexSearch = function(query, filters, conceptType, paging) {
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
            url: 'graph/vertex/search',
            data: data
        });
    };

    VertexService.prototype.getArtifactHighlightedTextById = function(graphVertexId) {
        return this._ajaxGet({
            dataType: 'html',
            url: 'artifact/highlightedText',
            data: {
                graphVertexId: graphVertexId
            }
        });
    };

    VertexService.prototype.getRelatedVertices = function(data) {
        return this._ajaxGet({
            url: 'graph/relatedVertices',
            data: {
                graphVertexId: data.graphVertexId,
                limitParentConceptId: data.limitParentConceptId
            }
        });
    };

    VertexService.prototype.getVertexRelationships = function(graphVertexId, paging) {
        var data = paging || {};
        data.graphVertexId = graphVertexId;
        return this._ajaxGet({
            url: 'vertex/relationships',
            data: data
        });
    };

    VertexService.prototype.getVertexProperties = function(graphVertexId) {
        return this._ajaxGet({
            url: 'vertex/properties',
            data: {
                graphVertexId: graphVertexId
            }
        });
    };

    VertexService.prototype._search = function(resource, query) {
        //maybe it's an object for future options stuff?
        var q = typeof query == 'object' ? query.query : query,
            url = resource + '/search';

        return this._ajaxGet({
            url: url,
            data: {
                q: q
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
            url: 'entity/resolveTerm',
            data: resolveRequest
        });
    };

    VertexService.prototype.unresolveTerm = function(unresolveRequest) {
        return this._ajaxPost({
            url: 'entity/unresolveTerm',
            data: unresolveRequest
        });
    };

    VertexService.prototype.resolveDetectedObject = function(resolveRequest) {
        return this._ajaxPost({
            url: 'entity/resolveDetectedObject',
            data: resolveRequest
        });
    };

    VertexService.prototype.unresolveDetectedObject = function(unresolveRequest) {
        return this._ajaxPost({
            url: 'entity/unresolveDetectedObject',
            data: unresolveRequest
        });
    };

    return VertexService;
});
