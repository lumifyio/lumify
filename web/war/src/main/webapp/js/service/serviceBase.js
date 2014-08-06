
define(['atmosphere'],
    function(atmosphere) {
        'use strict';

        var memoizedMap = {};

        // Add CSRF Header to all non-GET requests
        $.ajaxPrefilter(function(options) {
            var eligibleForProtection = !(/get/i).test(options.type),
                user = window.currentUser,
                token = user && user.csrfToken;

            if (eligibleForProtection && token) {
                options.headers = $.extend(options.headers || {}, {
                    'Lumify-CSRF-Token': token
                });
            }
        });

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
            var self = this;

            require(['util/offlineOverlay'], function(Overlay) {
                var req = {
                        url: 'messaging',
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
                            // so it only happens if server went down
                            _.delay(function() {
                                Overlay.attachTo(document);
                            }, 1000);
                        }
                    };
                document.$subSocket = self.getSocket().subscribe(req);
                document.subSocketId = $.atmosphere.guid();

                $(document).trigger('registerBeforeUnloadHandler', self.disconnect);
            });
        };

        ServiceBase.prototype.disconnect = function() {
            var socket = document.$subSocket,
                config = socket.request;

            config.maxReconnectOnClose = 0;
            socket.disconnect();
        };

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
                config = null,
                request = this._ajaxGet(config = $.extend({
                        type: 'POST',
                        wrappedPromise: deferred.promise(),
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
                        .fail(deferred.reject)
                        .done(deferred.resolve),
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

        ServiceBase.prototype.clearMemoizeCache = function() {
            if (!this.serviceName) {
                return console.error('Service doesn\'t register serviceName, skipping');
            }

            var serviceName = this.serviceName,
                toDelete = [];

            _.each(memoizedMap, function(value, key) {
                if (key.indexOf(serviceName) === 0) {
                    toDelete.push(key);
                }
            });

            toDelete.forEach(function(key) {
                console.log('deleting', key);
                delete memoizedMap[key];
            });
        }

        ServiceBase.prototype.memoizeFunctions = function(serviceName, toMemoize) {
            var self = this;

            toMemoize.forEach(function(f) {
                var cachedFunction = self[f],
                    hashFunction = cachedFunction.memoizeHashFunction;

                self[f] = function() {
                    var key = hashFunction && hashFunction.apply(self, arguments);
                    if (!key && arguments.length) key = arguments[0];
                    if (!key) key = '(noargs)';
                    key = f + key;

                    var result = memoizedMap[serviceName + key];

                    if (result && result.statusText != 'abort') {
                        return result;
                    }

                    memoizedMap[serviceName + key] = result = cachedFunction.apply(self, arguments);
                    if (result.fail) {
                        result.fail(function() {
                            delete memoizedMap[serviceName + key];
                        })
                    }
                    return result;
                }
            });
        }

        return ServiceBase;
    }
);
