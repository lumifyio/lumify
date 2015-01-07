
define([
    'flight/lib/component',
    './withPublicApi',
    './withBrokenWorkerConsole',
    './withDataRequestHandler',
    './withCurrentUser',
    './withCachedConceptIcons',
    './withDocumentCopyText',
    './withWebsocket',
    './withWebsocketLegacy',
    './withKeyboardRegistration',
    './withObjectSelection',
    './withObjectsUpdated',
    './withWorkspaces',
    './withWorkspaceFiltering',
    './withWorkspaceVertexDrop'
], function(
    defineComponent
    // mixins auto added in order (change index of slice)
) {
    'use strict';

    var PATH_TO_WORKER = 'jsc/data/web-worker/data-worker.js',
        mixins = Array.prototype.slice.call(arguments, 1);

    return defineComponent.apply(null, [Data].concat(mixins));

    function Data() {

        this.after('initialize', function() {
            var self = this;

            this.setupDataWorker();

            this.dataRequestPromise = new Promise(function(fulfill, reject) {
                    if (self.lumifyData.readyForDataRequests) {
                        fulfill();
                    } else {
                        var timer = _.delay(reject, 10000);
                        self.on('readyForDataRequests', function readyForDataRequests() {
                            if (timer) {
                                clearTimeout(timer);
                            }
                            fulfill();
                            self.off('readyForDataRequests', readyForDataRequests);
                        });
                    }
                }).then(function() {
                    return Promise.require('util/withDataRequest');
                }).then(function(withDataRequest) {
                    return withDataRequest.dataRequest;
                });

            this.messagesPromise = this.dataRequestPromise.then(function() {
                    return Promise.require('util/messages');
                }).then(this.setupMessages.bind(this));

            if (typeof DEBUG !== 'undefined') {
                DEBUG.logCacheStats = function() {
                    self.worker.postMessage({
                        type: 'postCacheStats'
                    });
                }
            }
        });

        this.setupMessages = function(i18n) {
            return (window.i18n = i18n);
        };

        this.setupDataWorker = function() {
            this.worker = new Worker(PATH_TO_WORKER);
            this.worker.onmessage = this.onDataWorkerMessage.bind(this);
            this.worker.onerror = this.onDataWorkerError.bind(this);
        };

        this.onDataWorkerError = function(event) {
            console.error('data-worker error', event);
        };

        this.onDataWorkerMessage = function(event) {
            var data = event.data;

            if (data.type && (data.type in this)) {
                this[data.type](event.data);
            } else {
                console.warn('Unhandled message from worker', event.data);
            }
        };

    }
});
