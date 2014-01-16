
define(['atmosphere'],
    function() {
        'use strict';

        function ServiceBase(options) {
            options = options || {};

			if (!document.$socket) {
				document.$socket = $.atmosphere;
			}

            //define deafault options
            var defaults = {

                // the base url to find the service
                serviceBaseUrl: "/",

                //the context of the service
                serviceContext: "",

                //to use jsonp, or not to use jsonp
                jsonp: false
            };

            this.options = $.extend({},defaults, options);

            return this;
        }

		ServiceBase.prototype.getSocket = function () {
			return document.$socket;
		};

        ServiceBase.prototype.socketPush = function(data) {
            data.sourceId = document.subSocketId;
            return document.$subSocket.push(JSON.stringify(data));
        };

        ServiceBase.prototype.subscribe = function (config) {
            var self = this,
                req = {
                    url: location.href.replace(/\/#?$/, '') + "/messaging/",
                    transport: 'websocket',
                    fallbackTransport: 'long-polling',
                    contentType: "application/json",
                    trackMessageLength : true,
                    suspend: false,
                    shared: false,
                    uuid: this.getSocket().guid(),
                    connectTimeout: -1,
                    maxStreamingLength: 100,
                    enableProtocol: true,
                    maxReconnectOnClose: -1,
                    logLevel: 'debug',
                    onOpen: function(response) {
                        if (config.onOpen) config.onOpen.apply(null, arguments);
                    },
                    onClientTimeout: function() {
                        console.error('timeout', arguments);
                    },
                    onClose: function() {
                        console.error('closed', arguments);

                        // Might be closing because of browser refresh, delay
                        // logout so it only happens if server went down
                        _.delay(function() {
                            $(document).trigger('logout');
                        }, 1000);
                    },
                    onMessage: function (response) {
                        var body = response.responseBody,
                            data = JSON.parse(body);

                        if(data && data.sourceId == document.subSocketId) {
                            return;
                        }

                        if (config.onMessage) config.onMessage(null, data);
                    },
                    onError: function (response) {
                        console.error('subscribe error:', response);
                        if (config.onMessage) config.onMessage(response.error, null);
                    }
                };
            document.$subSocket = this.getSocket().subscribe(req);
            document.subSocketId = req.uuid;

            $(window).off('unload.serviceBaseSubscribe');
            $(window).on('unload.serviceBaseSubscribe', this.disconnect.bind(this));
        };

        ServiceBase.prototype.disconnect = function() {
            var req = document.$subSocket.request,
                url = req.url + '?X-Atmosphere-Transport=close&X-Atmosphere-tracking-id=' + req.uuid;

            return $.ajax(url, {
                async: false,
                type: 'POST'
            });
        }

        ServiceBase.prototype._ajaxPost = function(options) {
            options.type = options.type || "POST";
            return this._ajaxGet(options);
        };

        ServiceBase.prototype._ajaxDelete = function(options) {
            options.type = options.type || "DELETE";
            return this._ajaxGet(options);
        };

        ServiceBase.prototype._ajaxGet = function(options) {
            options.type = options.type || "GET";
            options.dataType = options.dataType || this._resolveDataType();
            options.resolvedUrl = options.resolvedUrl || this._resolveUrl(options.url);

            return $.ajax(options);
        };

		ServiceBase.prototype._unsubscribe = function (url) {
			this.getSocket().unsubscribeUrl(url);
		};


        ServiceBase.prototype._resolveUrl = function (urlSuffix) {
            return this.options.serviceBaseUrl + this.options.serviceContext + urlSuffix;
        };

        ServiceBase.prototype._resolveDataType = function () {
            return this.options.jsonp ? "jsonp" : "json";
        };

        return ServiceBase;
    }
);
