
define(['util/promise'], function() {
    'use strict';

    return ajax;

    function paramPair(key, value) {
        return key + '=' + encodeURIComponent(v);
    }

    function toQueryString(params) {
        var str = '', key;
        for (key in params) {
            if (typeof params[key] !== 'undefined') {

                if (_.isArray(params[key])) {
                    str += _.map(params[key], _.partial(key + '[]', paramPair)).join('&') + '&';
                } else {
                    str += paramPair(key, params[key]) + '&';
                }
            }
        }
        return str.slice(0, str.length - 1);
    }

    function ajax(method, url, parameters) {
        method = method.toUpperCase();

        return new Promise(function(fulfill, reject) {
            var r = new XMLHttpRequest(),
                params = toQueryString(parameters),
                resolvedUrl = BASE_URL +
                    url +
                    ((method === 'GET' && parameters) ?
                        '?' + params :
                        ''
                    ),
                formData;

            r.onload = function() {
                var jsonStr = r.status === 200 && r.responseText;

                if (jsonStr) {
                    try {
                        var json = JSON.parse(jsonStr);
                        if (typeof ajaxPostfilter !== 'undefined') {
                            ajaxPostfilter(r, json, {
                                method: method,
                                url: url,
                                parameters: parameters
                            });
                        }
                        fulfill(json);
                    } catch(e) {
                        reject(new Error(e.message));
                    }
                } else {
                    reject(new Error(r.statusText));
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

            if (typeof ajaxPrefilter !== 'undefined') {
                ajaxPrefilter.call(null, r, method, url, parameters);
            }

            r.send(formData);
        });
    }
})
