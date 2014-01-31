define(
    [
        'service/serviceBase'
    ],
    function (ServiceBase) {
        'use strict';

        function AuditService() {
            ServiceBase.call(this);
            return this;
        }

        AuditService.prototype = Object.create(ServiceBase.prototype);

        AuditService.prototype.getAudits = function (vertexId) {
            return this._ajaxGet({
                url: 'audit/' + vertexId
            });
        };

        return AuditService;
    });

