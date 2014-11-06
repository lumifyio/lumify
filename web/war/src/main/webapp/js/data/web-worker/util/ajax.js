
define(['util/promise'], function() {
    'use strict';

    return ajax;

    function toQueryString(params) {
        var str = '', key;
        for (key in params) {
            str += key + '=' + encodeURIComponent(params[key]) + '&';
        }
        return str.slice(0, str.length - 1);
    }

    function ajax(method, url, parameters) {
        method = method.toUpperCase();

        return new Promise(function(fulfill, reject) {
            var r = new XMLHttpRequest(),
                resolvedUrl = BASE_URL +
                    url +
                    (parameters ?
                        '?' + toQueryString(parameters) :
                        ''
                    );

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
                    reject(r.status);
                }
            };
            r.onerror = function() {
                reject();
            };
            r.open(method || 'get', resolvedUrl, true);

            if (typeof ajaxPrefilter !== 'undefined') {
                ajaxPrefilter.call(null, r, method, url, parameters);
            }

            r.send();
        });
    }
})
