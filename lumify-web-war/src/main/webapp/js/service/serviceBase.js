
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
                serviceBaseUrl: '/',

                //the context of the service
                serviceContext: '',

                //to use jsonp, or not to use jsonp
                jsonp: false
            };

            this.options = $.extend({},defaults, options);

            return this;
        }

		ServiceBase.prototype.getSocket = function() {
			return document.$socket;
		};

        ServiceBase.prototype.socketPush = function(data) {
            data.sourceId = document.subSocketId;
            var string = JSON.stringify(data);
            if (string.length > 1024 * 1024) {
                return console.warn('Unable to push data, too large: ', string.length, data)
            }
            return document.$subSocket.push(string);
        };

        ServiceBase.prototype.subscribe = function(config) {
            var self = this,
                req = {
                    url: location.href.replace(/\/#?$/, '') + '/messaging/',
                    transport: 'websocket',
                    fallbackTransport: 'long-polling',
                    contentType: 'application/json',
                    trackMessageLength: true,
                    suspend: false,
                    shared: false,
                    connectTimeout: -1,
                    enableProtocol: true,
                    maxReconnectOnClose: 2,
                    maxStreamingLength: 2000,
                    logLevel: 'debug',
                    onOpen: function(response) {
                        if (config.onOpen) config.onOpen.apply(null, arguments);
                    },
                    onClientTimeout: function() {
                        console.error('timeout');
                        self.subscribe(config);
                    },
                    onClose: function(req) {
                        console.error('closed', req.reasonPhrase, req.error);
                    },
                    onMessage: function(response) {
                        var body = response.responseBody,
                            data = JSON.parse(body);

                        if (data && data.sourceId == document.subSocketId) {
                            return;
                        }

                        if (config.onMessage) config.onMessage(null, data);
                    },
                    onError: function(response) {
                        console.error('subscribe error:', response);
                        if (config.onMessage) config.onMessage(response.error, null);

                        // Might be closing because of browser refresh, delay
                        // logout so it only happens if server went down
                        _.delay(function() {
                            $(document).trigger('logout');
                        }, 1000);
                    }
                };
            document.$subSocket = this.getSocket().subscribe(req);
            document.subSocketId = req.uuid;

            $(window).off('beforeunload.serviceBaseSubscribe');
            $(window).on('beforeunload.serviceBaseSubscribe', function() {
                self.disconnect();
            });
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
            options.type = options.type || 'POST';
            return this._ajaxGet(options);
        };

        ServiceBase.prototype._ajaxUpload = function(options) {
            // We must use this xhr
            delete options.xhr;

            var xhr = null,
                progressHandler = null,
                deferred = $.Deferred(),
                request = this._ajaxGet($.extend({
                        type: 'POST',
                        xhr: function() {
                            xhr = $.ajaxSettings.xhr();
                            if (xhr.upload) {
                                xhr.upload.addEventListener('progress', (progressHandler = function(event) {
                                    if (event.lengthComputable) {
                                        var complete = (event.loaded / event.total || 0);
                                        if (complete < 1.0) {
                                            deferred.notify(complete);
                                        }
                                    }
                                }), false);
                            }
                            return xhr;
                        },
                        cache: false,
                        contentType: false,
                        processData: false
                    }, options))
                        .always(function() {
                            if (xhr && progressHandler) {
                                xhr.removeEventListener('progress', progressHandler);
                            }
                        })
                        .fail(function(xhr, m, error) {
                            deferred.reject(xhr, m, error);
                        })
                        .done(function(result) {
                            deferred.resolve(result);
                        }),
                promise = deferred.promise();
                
            promise.abort = function() {
                request.abort();
            };
            return promise;
        };

        ServiceBase.prototype._ajaxDelete = function(options) {
            options.type = options.type || 'DELETE';
            return this._ajaxGet(options);
        };

        ServiceBase.prototype._ajaxGet = function(options) {
            options.type = options.type || 'GET';
            options.dataType = options.dataType || this._resolveDataType();
            options.resolvedUrl = options.resolvedUrl || this._resolveUrl(options.url);

            return $.ajax(options);
        };

		ServiceBase.prototype._unsubscribe = function(url) {
			this.getSocket().unsubscribeUrl(url);
		};

        ServiceBase.prototype._resolveUrl = function(urlSuffix) {
            return this.options.serviceBaseUrl + this.options.serviceContext + urlSuffix;
        };

        ServiceBase.prototype._resolveDataType = function() {
            return this.options.jsonp ? 'jsonp' : 'json';
        };

        var memoizedMap = {};
        ServiceBase.prototype.memoizeFunctions = function(toMemoize) {
            var self = this;
            toMemoize.forEach(function(f) {
                var cachedFunction = self[f],
                hashFunction = self[f].memoizeHashFunction;
                self[f] = function() {
                    var key = hashFunction && hashFunction.apply(self, arguments);
                    if (!key && arguments.length) key = arguments[0];
                    if (!key) key = '(noargs)';
                    key = f + key;

                    var result = memoizedMap[key];

                    if (result && result.statusText != 'abort') {
                        return result;
                    }
                    memoizedMap[key] = result = cachedFunction.apply(self, arguments);
                    if (result.fail) {
                        result.fail(function() {
                            delete memoizedMap[key];
                        })
                    }
                    return result;
                }
            });
        }

        return ServiceBase;
    }
);
