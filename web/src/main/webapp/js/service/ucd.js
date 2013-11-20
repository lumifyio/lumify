define(
[
    'service/serviceBase'
],
function(ServiceBase) {
    'use strict';

    function Ucd() {
        ServiceBase.call(this);

        return this;
    }

    Ucd.prototype = Object.create(ServiceBase.prototype);

    Ucd.prototype.getRelationships = function(ids) {
        return this._ajaxPost({
            url: 'entity/relationships',
            data: {
                ids: ids
            }
        });
    };

    Ucd.prototype.deleteEdge = function(sourceId, targetId, label) {
        return this._ajaxPost({
            url: '/vertex/removeRelationship',
            data: {
                sourceId: sourceId,
                targetId: targetId,
                label: label
            }
        });
    };

    Ucd.prototype.getVertexToVertexRelationshipDetails = function (source, target, label) {
        return this._ajaxGet({
            url: 'vertex/relationship',
            data: {
                source: source,
                target: target,
                label: label
            }
        });
    };

    Ucd.prototype.locationSearch = function(lat, lon, radiuskm) {
        return this._ajaxGet({
            url: 'graph/vertex/geoLocationSearch',
            data: {
                lat: lat,
                lon: lon,
                radius: radiuskm
            }
        });
    };

    Ucd.prototype.getStatementByRowKey = function(statementRowKey) {
        return this._get("statement", statementRowKey);
    };

    Ucd.prototype.artifactSearch = function(query, filters, subType, paging) {
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

    Ucd.prototype.getArtifactById = function (id) {
        return this._get("artifact", id);
    };

    Ucd.prototype.getArtifactHighlightedTextById = function(graphVertexId) {
        return this._ajaxGet({
            dataType: 'html',
            url: "artifact/" + graphVertexId + "/highlightedText"
        });
    };

    Ucd.prototype.getRawArtifactById = function (id) {
        //maybe it's an object for future options stuff?
        var i = typeof id == "object" ? id.id : id;

        return this._ajaxGet({
            url: "artifact/" + i + "/raw",
        });
    };

    Ucd.prototype.graphVertexSearch = function (query, filters, subType, paging) {
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

    Ucd.prototype.getRelatedVertices = function(data) {
        return this._ajaxGet({
            url: 'graph/' + encodeURIComponent(data.graphVertexId) + '/relatedVertices',
            data: {
                limitParentConceptId: data.limitParentConceptId
            }
        });
    };

    Ucd.prototype.getVertexRelationships = function(graphVertexId, paging) {
        return this._ajaxGet({
            url: 'vertex/' + graphVertexId + '/relationships',
            data: paging || {}
        });
    };

    Ucd.prototype.getVertexProperties = function(graphVertexId) {
        return this._ajaxGet({ url: 'vertex/' + graphVertexId + '/properties'});
    };

    Ucd.prototype._search = function (resource, query) {
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

    Ucd.prototype._get = function (resource, id) {
        if(!id) {
            throw new Error("Invalid or no id specified for resource '" + resource + "'");
        }

        //maybe it's an object for future options stuff?
        var i = encodeURIComponent(typeof id == "object" ? id.id : id);
        return this._ajaxGet({
            url: resource + "/" + i,
        });
    };

    return Ucd;
});
