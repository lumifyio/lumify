define([
    'service/serviceBase'
], function(ServiceBase) {
    'use strict';

    function RelationshipService() {
        ServiceBase.call(this);
        return this;
    }

    RelationshipService.prototype = Object.create(ServiceBase.prototype);

    RelationshipService.prototype.setProperty = function(propertyName, value, visibilitySource, justificationText,
        sourceInfo, sourceId, destId, id) {
        return this._ajaxPost({
            url: 'relationship/property/set',
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

    RelationshipService.prototype.setVisibility = function(edgeId, visibilitySource) {
        return this._ajaxPost({
            url: 'relationship/visibility/set',
            data: {
                graphEdgeId: edgeId,
                visibilitySource: visibilitySource
            }
        });
    };

    RelationshipService.prototype.deleteProperty = function(propertyName, sourceId, destId, edgeId) {
        return this._ajaxPost({
            url: 'relationship/property/delete',
            data: {
                propertyName: propertyName,
                source: sourceId,
                dest: destId,
                edgeId: edgeId
            }
        });
    };

    RelationshipService.prototype.createRelationship = function(createRequest) {
        return this._ajaxPost({
            url: 'relationship/create',
            data: createRequest
        });
    };

    RelationshipService.prototype.getRelationshipDetails = function(id) {
        return this._ajaxGet({
            url: 'relationship/properties',
            data: {
                graphEdgeId: id
            }
        });
    };

    return RelationshipService;
});
