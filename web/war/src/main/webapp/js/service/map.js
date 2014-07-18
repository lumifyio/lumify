define([
    'service/serviceBase'
], function(ServiceBase) {
    'use strict';

    function MapService() {
        ServiceBase.call(this);

        var toMemoize = [
            'geocode'
        ];

        this.memoizeFunctions('map', toMemoize);

        return this;
    }

    MapService.prototype = Object.create(ServiceBase.prototype);

    MapService.prototype.geocode = function(query) {
        return this._ajaxGet({
            url: 'map/geocode',
            data: {
                q: query
            }
        });
    };

    return MapService;
});
