var BASE_URL = '../../..',
    self = this,
    publicData = {},
    WORKER_WEBSOCKETS = !!(this.WebSocket || this.MozWebSocket);

this.BASE_URL = BASE_URL;

if (WORKER_WEBSOCKETS) {
    this.window = this;
    window.addEventListener = function() {};
    importScripts('/libs/atmosphere/atmosphere.js')
    atmosphere.util.getAbsoluteURL = function() {
        return location.origin + '/messaging';
    }
} else {
    dispatchMain('websocketNotSupportedInWorker');
}

setupRequireJs();

onmessage = onMessageHandler;

function setupRequireJs() {
    importScripts(BASE_URL + '/jsc/require.config.js');
    require.baseUrl = BASE_URL + '/jsc/';
    importScripts(BASE_URL + '/libs/requirejs/require.js');
}

function onMessageHandler(event) {
    require(['underscore'], function() {
        var data = event.data;

        if (data.type) {
            require(['data/web-worker/handlers/' + data.type], function(handler) {
                handler(data);
            });
        } else console.warn('Unhandled message to worker', event);
    });
}

function dispatchMain(type, message) {
    if (!type) {
        throw new Error('dispatchMain requires type argument');
    }
    message = message || {};
    message.type = type;
    postMessage(message);
}

function ajaxPrefilter(xmlHttpRequest, method, url, parameters) {
    if (publicData) {
        var filters = [
                setWorkspaceHeader,
                setCsrfHeader
            ], invoke = function(f) {
                f();
            };

        filters.forEach(invoke);
    }

    function setWorkspaceHeader() {
        if (!parameters || !('workspaceId' in parameters)) {
            xmlHttpRequest.setRequestHeader('Lumify-Workspace-Id', publicData.currentWorkspaceId);
        }
    }
    function setCsrfHeader() {
        var eligibleForProtection = !(/get/i).test(method),
            user = publicData.currentUser,
            token = user && user.csrfToken;

        if (eligibleForProtection && token) {
            xmlHttpRequest.setRequestHeader('Lumify-CSRF-Token', token);
        }
    }
}

function ajaxPostfilter(xmlHttpRequest, jsonResponse, method, url, parameters) {
    if (method === 'GET') {
        require(['data/web-worker/util/cache'], function(cache) {
            //var changes = cache.cacheAjaxResult(jsonResponse, url, workspaceId);
            // TODO: broadcast these
        });
    }
}
