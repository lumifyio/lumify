
define(['util/promise'], function() {
    'use strict';

    return ajax;

    function paramPair(key, value) {
        return key + '=' + encodeURIComponent(value);
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

        return new Promise(function(fulfill, reject) {
            var r = new XMLHttpRequest(),
                params = toQueryString(parameters),
                resolvedUrl = BASE_URL +
                    url +
                    ((/GET|DELETE/.test(method) && parameters) ?
                        '?' + params :
                        ''
                    ),
                formData;

            r.onload = function() {
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
                            console.error(e);
                            reject(new Error(e && e.message));
                        }
                    } else {
                        fulfill(text)
                    }
                } else {
                    reject(r);
                }
            };
            r.onerror = function() {
                reject(new Error('Network Error'));
            };
            r.open(method || 'get', resolvedUrl, true);

            if (method === 'POST' && parameters) {
                formData = params;
                r.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
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
    }
})
