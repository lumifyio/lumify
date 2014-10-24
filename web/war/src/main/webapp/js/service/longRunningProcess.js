
define([
    'service/serviceBase'
], function(ServiceBase) {
    'use strict';

    function LongRunningService() {
        ServiceBase.call(this);

        this.serviceName = 'user';

        return this;
    }

    LongRunningService.prototype = Object.create(ServiceBase.prototype);

    LongRunningService.prototype.get = function(id) {
        return this._ajaxGet({
            url: 'long-running-process',
            data: {
                longRunningProcessId: id
            }
        })
    };

    LongRunningService.prototype['delete'] = function(id) {
        return this._ajaxDelete({
            url: 'long-running-process?' + $.param({
                longRunningProcessId: id
            })
        })
    };

    LongRunningService.prototype.cancel = function(id) {
        return this._ajaxGet({
            url: 'long-running-process/cancel',
            data: {
                longRunningProcessId: id
            }
        })
    };

    return LongRunningService;
});
