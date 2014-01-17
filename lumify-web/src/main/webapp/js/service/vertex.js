define(
    [
        'service/serviceBase'
    ],
    function (ServiceBase) {
        'use strict';

        function VertexService() {
            ServiceBase.call(this);
            return this;
        }

        VertexService.prototype = Object.create(ServiceBase.prototype);

        VertexService.prototype.setProperty = function (vertexId, propertyName, value) {
            return this._ajaxPost({
                url: 'vertex/' + vertexId + '/property/set',
                data: {
                    propertyName: propertyName,
                    value: value
                }
            });
        };

        VertexService.prototype.deleteProperty = function (vertexId, propertyName) {
            return this._ajaxPost({
                url: 'vertex/' + vertexId + '/property/delete',
                data: {
                    propertyName: propertyName
                }
            });
        };

        VertexService.prototype.getMultiple = function (vertexIds) {
            return this._ajaxGet({
                url: 'vertex/multiple',
                data: {
                    vertexIds: vertexIds
                }
            });
        };

        VertexService.prototype.getRelationships = function (ids) {
            return this._ajaxPost({
                url: 'entity/relationships',
                data: {
                    ids: ids
                }
            });
        };

        VertexService.prototype.deleteEdge = function (sourceId, targetId, label, edgeId) {
            return this._ajaxPost({
                url: '/vertex/removeRelationship',
                data: {
                    sourceId: sourceId,
                    targetId: targetId,
                    label: label,
                    edgeId: edgeId
                }
            });
        };

        VertexService.prototype.getVertexToVertexRelationshipDetails = function (source, target, label) {
            return this._ajaxGet({
                url: 'vertex/relationship',
                data: {
                    source: source,
                    target: target,
                    label: label
                }
            });
        };

        VertexService.prototype.findPath = function (data) {
            return this._ajaxGet({
                url: 'graph/findPath',
                data: data
            });
        };

        VertexService.prototype.locationSearch = function (lat, lon, radiuskm) {
            return this._ajaxGet({
                url: 'graph/vertex/geoLocationSearch',
                data: {
                    lat: lat,
                    lon: lon,
                    radius: radiuskm
                }
            });
        };

        VertexService.prototype.getStatementByRowKey = function (statementRowKey) {
            return this._get("statement", statementRowKey);
        };

        VertexService.prototype.graphVertexSearch = function (query, filters, conceptType, paging) {
            if (typeof filters === 'function') {
                callback = filters;
                filters = [];
            }

            var data = {};

            if (conceptType) data.conceptType = conceptType;
            if (paging) {
                if (paging.offset) data.offset = paging.offset;
                if (paging.size) data.size = paging.size;
            }

            data.q = query.query || query;
            data.filter = JSON.stringify(filters || []);

            return this._ajaxGet({
                url: 'graph/vertex/search',
                data: data
            });
        };

        VertexService.prototype.getArtifactHighlightedTextById = function (graphVertexId) {
            return this._ajaxGet({
                dataType: 'html',
                url: "artifact/" + graphVertexId + "/highlightedText"
            });
        };


        VertexService.prototype.getRelatedVertices = function (data) {
            return this._ajaxGet({
                url: 'graph/' + encodeURIComponent(data.graphVertexId) + '/relatedVertices',
                data: {
                    limitParentConceptId: data.limitParentConceptId
                }
            });
        };

        VertexService.prototype.getVertexRelationships = function (graphVertexId, paging) {
            return this._ajaxGet({
                url: 'vertex/' + graphVertexId + '/relationships',
                data: paging || {}
            });
        };

        VertexService.prototype.getVertexProperties = function (graphVertexId) {
            return this._ajaxGet({ url: 'vertex/' + graphVertexId + '/properties'});
        };

        VertexService.prototype._search = function (resource, query) {
            //maybe it's an object for future options stuff?
            var q = typeof query == "object" ? query.query : query;
            var url = resource + "/search";

            return this._ajaxGet({
                url: url,
                data: {
                    'q': q
                }
            });
        };

        VertexService.prototype._get = function (resource, id) {
            if (!id) {
                throw new Error("Invalid or no id specified for resource '" + resource + "'");
            }

            //maybe it's an object for future options stuff?
            var i = encodeURIComponent(typeof id == "object" ? id.id : id);
            return this._ajaxGet({
                url: resource + "/" + i,
            });
        };

        VertexService.prototype.createTerm = function (createRequest) {
            return this._ajaxPost({
                url: 'entity/createTerm',
                data: createRequest
            });
        };

        VertexService.prototype.updateTerm = function (updateRequest) {
            return this._ajaxPost({
                url: 'entity/updateTerm',
                data: updateRequest
            });
        };

        VertexService.prototype.resolveDetectedObject = function (createRequest) {
            return this._ajaxPost({
                url: 'entity/createResolvedDetectedObject',
                data: createRequest
            });
        };

        VertexService.prototype.updateDetectedObject = function (updateRequest) {
            return this._ajaxPost({
                url: 'entity/updateResolvedDetectedObject',
                data: updateRequest
            });
        };

        VertexService.prototype.deleteDetectedObject = function (deleteRequest) {
            return this._ajaxPost({
                url: 'entity/deleteResolvedDetectedObject',
                data: deleteRequest
            });
        };

        return VertexService;
    });

