
define(['util/promise'], function() {
    'use strict';

    return ajax;

    function paramPair(key, value) {
        return key + '=' + encodeURIComponent(value);
    }

    function isFile(params) {
        return params && (
            params instanceof FormData ||
            (params instanceof File) ||
            _.isArray(params)
        )
    }

    function toFileUpload(params) {
        if (params instanceof FormData) {
            return params;
        }
        var formData = new FormData();
        if (!_.isArray(params)) {
            params = [params];
        }
        params.forEach(function(file) {
            formData.append(file.name, file);
        });

        return formData;
    }

    function toQueryString(params) {
        var str = '', key;
        for (key in params) {
            if (typeof params[key] !== 'undefined') {

                if (_.isArray(params[key])) {
                    str += _.map(params[key], _.partial(paramPair, key + '[]')).join('&') + '&';
                } else if (_.isObject(params[key])) {
                    str += paramPair(key, JSON.stringify(params[key])) + '&';
                } else {
                    str += paramPair(key, params[key]) + '&';
                }
            }
        }
        return str.slice(0, str.length - 1);
    }

    function ajax(method, url, parameters, debugOptions) {
        var isJson = true,
            methodRegex = /^(.*)->HTML$/;
        method = method.toUpperCase();

        var matches = method.match(methodRegex);
        if (matches && matches.length === 2) {
            isJson = false;
            method = matches[1];
        }

        var r = new XMLHttpRequest(),
            promise = new Promise(function(fulfill, reject) {
                var progressHandler,
                    params = isFile(parameters) ? toFileUpload(parameters) : toQueryString(parameters),
                    resolvedUrl = BASE_URL + url + ((/GET|DELETE/.test(method) && parameters) ?
                        ('?' + params) : ''),
                    formData;

                r.onload = function() {
                    if (r.upload) {
                        r.upload.removeEventListener('progress', progressHandler);
                    }
                    var text = r.status === 200 && r.responseText;

                    if (text) {
                        if (isJson) {
                            try {
                                var json = JSON.parse(text);
                                if (typeof ajaxPostfilter !== 'undefined') {
                                    ajaxPostfilter(r, json, {
                                        method: method,
                                        url: url,
                                        parameters: parameters
                                    });
                                }
                                fulfill(json);
                            } catch(e) {
                                reject(e && e.message);
                            }
                        } else {
                            fulfill(text)
                        }
                    } else {
                        if (r.statusText && /^\s*{/.test(r.statusText)) {
                            try {
                                var errorJson = JSON.parse(r.statusText);
                                reject(errorJson);
                                return;
                            } catch(e) {
                                // continue
                            }
                        }
                        reject({
                            status: r.status,
                            statusText: r.statusText
                        });
                    }
                };
                r.onerror = function() {
                    if (r.upload) {
                        r.upload.removeEventListener('progress', progressHandler);
                    }
                    reject(new Error('Network Error'));
                };
                r.open(method, resolvedUrl, true);

                if (r.upload) {
                    r.upload.addEventListener('progress', (progressHandler = function(event) {
                        if (event.lengthComputable) {
                            var complete = (event.loaded / event.total || 0);
                            if (complete < 1.0) {
                                fulfill.updateProgress(complete);
                            }
                        }
                    }), false);
                }

                if (method === 'POST' && parameters) {
                    formData = params;
                    if (!(params instanceof FormData)) {
                        r.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
                    }
                }

                if (debugOptions) {
                    console.warn('Request Debugging is set for ' + url)
                    if (debugOptions.error) {
                        r.setRequestHeader('Lumify-Request-Error', debugOptions.error);
                    }
                    if (debugOptions.delay) {
                        r.setRequestHeader('Lumify-Request-Delay-Millis', debugOptions.delay);
                    }
                }

                if (typeof ajaxPrefilter !== 'undefined') {
                    ajaxPrefilter.call(null, r, method, url, parameters);
                }

                r.send(formData);
            });

        promise.abort = function() {
            if (r) {
                r.abort();
            }
        }

        return promise;
    }
})
