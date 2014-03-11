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

        VertexService.prototype.setProperty = function (vertexId, propertyName, value, visibilitySource, justificationText, sourceInfo) {
            return this._ajaxPost({
                url: 'vertex/' + encodeURIComponent(vertexId) + '/property/set',
                data: {
                    propertyName: propertyName,
                    value: value,
                    visibilitySource: visibilitySource,
                    justificationText: justificationText,
                    sourceInfo: JSON.stringify(sourceInfo)
                }
            });
        };

        VertexService.prototype.deleteProperty = function (vertexId, propertyName) {
            return this._ajaxPost({
                url: 'vertex/' + encodeURIComponent(vertexId) + '/property/delete',
                data: {
                    propertyName: propertyName
                }
            });
        };

        VertexService.prototype.getMultiple = function (vertexIds) {
            return this._ajaxPost({
                url: 'vertex/multiple',
                data: {
                    vertexIds: vertexIds
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

        VertexService.prototype.getVertexToVertexRelationshipDetails = function (source, target, id) {
            return this._ajaxGet({
                url: 'vertex/relationship',
                data: {
                    source: source,
                    target: target,
                    id: id
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

        VertexService.prototype.getArtifactHighlightedTextById = function (graphVertexId) {
            return this._ajaxGet({
                dataType: 'html',
                url: "artifact/" + encodeURIComponent(graphVertexId) + "/highlightedText"
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
                url: 'vertex/' + encodeURIComponent(graphVertexId) + '/relationships',
                data: paging || {}
            });
        };

        VertexService.prototype.getVertexProperties = function (graphVertexId) {
            return this._ajaxGet({ url: 'vertex/' + encodeURIComponent(graphVertexId) + '/properties'});
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
                url: resource + "/" + i
            });
        };

        VertexService.prototype.resolveTerm = function (resolveRequest) {
            return this._ajaxPost({
                url: 'entity/resolveTerm',
                data: resolveRequest
            });
        };

        VertexService.prototype.unresolveTerm = function (unresolveRequest) {
            return this._ajaxPost({
                url: 'entity/unresolveTerm',
                data: unresolveRequest
            });
        };

        VertexService.prototype.resolveDetectedObject = function (resolveRequest) {
            return this._ajaxPost({
                url: 'entity/resolveDetectedObject',
                data: resolveRequest
            });
        };

        VertexService.prototype.unresolveDetectedObject = function (unresolveRequest) {
            return this._ajaxPost({
                url: 'entity/unresolveDetectedObject',
                data: unresolveRequest
            });
        };

        return VertexService;
    });

