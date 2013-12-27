define(
[
    'service/serviceBase'
],
function(ServiceBase) {
    'use strict';

    function Service() {
        ServiceBase.call(this);

        return this;
    }

    Service.prototype = Object.create(ServiceBase.prototype);

    Service.prototype.getRelationships = function(ids) {
        return this._ajaxPost({
            url: 'entity/relationships',
            data: {
                ids: ids
            }
        });
    };

    Service.prototype.deleteEdge = function(sourceId, targetId, label) {
        return this._ajaxPost({
            url: '/vertex/removeRelationship',
            data: {
                sourceId: sourceId,
                targetId: targetId,
                label: label
            }
        });
    };

    Service.prototype.getVertexToVertexRelationshipDetails = function (source, target, label) {
        return this._ajaxGet({
            url: 'vertex/relationship',
            data: {
                source: source,
                target: target,
                label: label
            }
        });
    };

    Service.prototype.locationSearch = function(lat, lon, radiuskm) {
        return this._ajaxGet({
            url: 'graph/vertex/geoLocationSearch',
            data: {
                lat: lat,
                lon: lon,
                radius: radiuskm
            }
        });
    };

    Service.prototype.getStatementByRowKey = function(statementRowKey) {
        return this._get("statement", statementRowKey);
    };

    Service.prototype.artifactSearch = function(query, filters, subType, paging) {
        if (typeof filters === 'function') {
            callback = filters;
            filters = [];
        }

        var parameters = {
            q: query.query || query,
            filter: JSON.stringify(filters || [])
        };

        if (subType) {
            parameters.subType = subType;
        }

        if (paging) {
            if (paging.offset) parameters.offset = paging.offset;
            if (paging.size) parameters.size = paging.size;
        }

        return this._ajaxGet({
            url: 'artifact/search',
            data: parameters
        });
    };

    Service.prototype.getArtifactById = function (id) {
        return this._get("artifact", id);
    };

    Service.prototype.getArtifactHighlightedTextById = function(graphVertexId) {
        return this._ajaxGet({
            dataType: 'html',
            url: "artifact/" + graphVertexId + "/highlightedText"
        });
    };

    Service.prototype.getRawArtifactById = function (id) {
        //maybe it's an object for future options stuff?
        var i = typeof id == "object" ? id.id : id;

        return this._ajaxGet({
            url: "artifact/" + i + "/raw",
        });
    };

    Service.prototype.graphVertexSearch = function (query, filters, subType, paging) {
        if (typeof filters === 'function') {
            callback = filters;
            filters = [];
        }

        var data = {};

        if (subType) data.subType = subType;
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

    Service.prototype.getRelatedVertices = function(data) {
        return this._ajaxGet({
            url: 'graph/' + encodeURIComponent(data.graphVertexId) + '/relatedVertices',
            data: {
                limitParentConceptId: data.limitParentConceptId
            }
        });
    };

    Service.prototype.getVertexRelationships = function(graphVertexId, paging) {
        return this._ajaxGet({
            url: 'vertex/' + graphVertexId + '/relationships',
            data: paging || {}
        });
    };

    Service.prototype.getVertexProperties = function(graphVertexId) {
        return this._ajaxGet({ url: 'vertex/' + graphVertexId + '/properties'});
    };

    Service.prototype._search = function (resource, query) {
        //maybe it's an object for future options stuff?
        var q = typeof query == "object" ? query.query : query;
        var url = resource + "/search";

        return this._ajaxGet({
            url: url,
            data: {
                'q' : q
            }
        });
    };

    Service.prototype._get = function (resource, id) {
        if(!id) {
            throw new Error("Invalid or no id specified for resource '" + resource + "'");
        }

        //maybe it's an object for future options stuff?
        var i = encodeURIComponent(typeof id == "object" ? id.id : id);
        return this._ajaxGet({
            url: resource + "/" + i,
        });
    };

    return Service;
});
