define(
    [
        'service/serviceBase'
    ],
    function (ServiceBase) {
        'use strict';

        function RelationshipService() {
            ServiceBase.call(this);
            return this;
        }

        RelationshipService.prototype = Object.create(ServiceBase.prototype);

        RelationshipService.prototype.setProperty = function (propertyName, value, sourceId, destId, edgeId) {
            return this._ajaxPost({
                url: 'relationship/property/set',
                data: {
                    propertyName: propertyName,
                    value: value,
                    source: sourceId,
                    dest: destId,
                    edgeId: edgeId
                }
            });
        };

        RelationshipService.prototype.deleteProperty = function (propertyName, sourceId, destId, edgeId) {
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

        RelationshipService.prototype.createRelationship = function (createRequest) {
            return this._ajaxPost({
                url: 'relationship/create',
                data: createRequest
            });
        };

        return RelationshipService;
    });

