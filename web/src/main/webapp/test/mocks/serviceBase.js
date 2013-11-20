
define([], function() {
    'use strict';

    function ServiceBase(options) {
        options = options || {};

        //define deafault options
        var defaults = {

            // the base url to find the service
            serviceBaseUrl: "/",

            //the context of the service
            serviceContext: "",

            //to use jsonp, or not to use jsonp
            jsonp: false
        };

        this._ajaxRequests = {};
        this.options = $.extend({},defaults, options);
        return this; 
    }

    ServiceBase.prototype.getSocket = function () { };

    ServiceBase.prototype.socketPush = function(data) { };

    ServiceBase.prototype.subscribe = function (config) { };

    ServiceBase.prototype._ajaxPost = function(options) {
        return this._ajaxGet(options);
    };

    ServiceBase.prototype._ajaxDelete = function(options) {
        return this._ajaxGet(options);
    };

    ServiceBase.prototype._ajaxGet = function(options) {
        options.type = options.type || "GET";
        options.dataType = options.dataType || this._resolveDataType();
        options.resolvedUrl = options.resolvedUrl || this._resolveUrl(options.url);

        var deferred = $.Deferred();
        this._ajaxRequests[options.url] = deferred;
        return deferred;
        //return $.ajax(options);
    };

    ServiceBase.prototype._unsubscribe = function (url) { };


    ServiceBase.prototype._resolveUrl = function (urlSuffix) {
        return this.options.serviceBaseUrl + this.options.serviceContext + urlSuffix;
    };

    ServiceBase.prototype._resolveDataType = function () {
        return this.options.jsonp ? "jsonp" : "json";
    };

    return ServiceBase;
});
