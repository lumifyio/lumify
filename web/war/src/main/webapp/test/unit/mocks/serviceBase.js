
define(['text!../../test/unit/mocks/ontology.json'], function(ontologyJson) {
    'use strict';

    var _ajaxRequests = {},
        jsonTransformers = [
            function removePersonColor(json) {
                var person = _.findWhere(json.concepts, { title: 'http://lumify.io/dev#person' });
                delete person.color;
            },
            function addVideoSub(json) {
                json.concepts.push({
                    id: 'http://lumify.io/dev#videoSub',
                    title: 'http://lumify.io/dev#videoSub',
                    color: 'rgb(149, 138, 218)',
                    pluralDisplayName: 'Video Subs',
                    parentConcept: 'http://lumify.io/dev#video',
                    displayName: 'Video Sub'
                });
            }
        ],
        defaultDevOntology = transformForTesting(JSON.parse(ontologyJson));

    function ServiceBase(options) {
        options = options || {};

        //define deafault options
        var defaults = {

            // the base url to find the service
            serviceBaseUrl: '/',

            //the context of the service
            serviceContext: '',

            //to use jsonp, or not to use jsonp
            jsonp: false
        };

        this.options = $.extend({},defaults, options);
        return this;
    }

    function transformForTesting(json) {
        _.reduce(jsonTransformers, function(ignored, transformer) {
            return transformer(json);
        }, json);
        return json;
    }

    ServiceBase.prototype.memoizeFunctions = function()  { }

    ServiceBase.prototype._ajaxRequests = function(url) {
        return _ajaxRequests[url];
    }

    ServiceBase.prototype.getSocket = function() { };

    ServiceBase.prototype.socketPush = function(data) { };

    ServiceBase.prototype.subscribe = function(config) { };

    ServiceBase.prototype._ajaxPost = function(options) {
        return this._ajaxGet(options);
    };

    ServiceBase.prototype._ajaxDelete = function(options) {
        return this._ajaxGet(options);
    };

    ServiceBase.prototype._ajaxGet = function(options) {
        options.type = options.type || 'GET';
        options.dataType = options.dataType || this._resolveDataType();
        options.resolvedUrl = options.resolvedUrl || this._resolveUrl(options.url);

        if (_ajaxRequests[options.url]) {
            return _ajaxRequests[options.url];
        }

        var deferred = $.Deferred();
        _ajaxRequests[options.url] = deferred;
        if (options.url === 'ontology') {
            deferred.resolve(defaultDevOntology);
        }
        return deferred;
        //return $.ajax(options);
    };

    ServiceBase.prototype._unsubscribe = function(url) { };

    ServiceBase.prototype._resolveUrl = function(urlSuffix) {
        return this.options.serviceBaseUrl + this.options.serviceContext + urlSuffix;
    };

    ServiceBase.prototype._resolveDataType = function() {
        return this.options.jsonp ? 'jsonp' : 'json';
    };

    return ServiceBase;
});
