
define(['util/promise'], function() {
    'use strict';

    return ajax;

    function toQueryString(params) {
        var str = '', key;
        for (key in params) {
            if (typeof params[key] !== 'undefined') {

                if (_.isArray(params[key])) {
                    str += _.map(params[key], function(v) {
                        return key + '[]' + '=' + encodeURIComponent(v);
                    }).join('&') + '&';
                } else {
                    str += key + '=' + encodeURIComponent(params[key]) + '&';
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
                            ajaxPostfilter(null, r, json, method, url, parameters);
                        }
                        fulfill(json);
                    } catch(e) {
                        reject(e);
                    }
                } else {
                    reject(r.statusText);
                }
            };
            r.onerror = function() {
                reject();
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
