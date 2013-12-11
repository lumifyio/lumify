
define(
[
    'service/serviceBase'
],
function(ServiceBase) {
    'use strict';

    function EntityService () {
        ServiceBase.call(this);
        return this;
    }

    EntityService.prototype = Object.create(ServiceBase.prototype);

	EntityService.prototype.createTerm = function(createRequest) {
		return this._ajaxPost({
			url: 'entity/createTerm',
			data: createRequest
		});
	};

	EntityService.prototype.updateTerm = function(updateRequest) {
        return this._ajaxPost({
            url: 'entity/updateTerm',
            data: updateRequest
        });
    };

	EntityService.prototype.resolveDetectedObject = function(createRequest) {
        return this._ajaxPost({
            url: 'entity/createResolvedDetectedObject',
            data: createRequest
        });
    };

    EntityService.prototype.updateDetectedObject = function(updateRequest) {
        return this._ajaxPost({
            url: 'entity/updateResolvedDetectedObject',
            data: updateRequest
        });
    };

    EntityService.prototype.deleteDetectedObject = function(deleteRequest) {
        return this._ajaxPost({
            url: 'entity/deleteResolvedDetectedObject',
            data: deleteRequest
        });
    };

    return EntityService;
});

