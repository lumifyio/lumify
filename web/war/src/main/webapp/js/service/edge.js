define([
    'service/serviceBase',
    'util/formatters'
], function(ServiceBase, F) {
    'use strict';

    function EdgeService() {
        ServiceBase.call(this);
        return this;
    }

    EdgeService.prototype = Object.create(ServiceBase.prototype);

    EdgeService.prototype.getAudits = function(edgeId) {
        return this._ajaxGet({
            url: 'edge/audit',
            data: {
                edgeId: edgeId
            }
        });
    };

    EdgeService.prototype.setProperty = function(propertyName, value, visibilitySource, justificationText,
        sourceInfo, sourceId, destId, id) {
        return this._ajaxPost({
            url: 'edge/property',
            data: {
                propertyName: propertyName,
                value: value,
                visibilitySource: visibilitySource,
                justificationText: justificationText,
                sourceInfo: sourceInfo,
                source: sourceId,
                dest: destId,
                edgeId: id
            }
        });
    };

    EdgeService.prototype.setVisibility = function(edgeId, visibilitySource) {
        return this._ajaxPost({
            url: 'edge/visibility',
            data: {
                graphEdgeId: edgeId,
                visibilitySource: visibilitySource
            }
        });
    };

    EdgeService.prototype.deleteProperty = function(propertyName, sourceId, destId, edgeId) {
        return this._ajaxDelete({
            url: 'edge/property?' + $.param({
                propertyName: propertyName,
                source: sourceId,
                dest: destId,
                edgeId: edgeId
            })
        });
    };

    EdgeService.prototype.create = function(createRequest) {
        return this._ajaxPost({
            url: 'edge/create',
            data: createRequest
        });
    };

    EdgeService.prototype.properties = function(id) {
        return this._ajaxGet({
            url: 'edge/properties',
            data: {
                graphEdgeId: id
            }
        });
    };

    return EdgeService;
});
