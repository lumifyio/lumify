
define([
    'service/vertex',
    'tpl!./previews'
], 
/**
 * Generate preview screenshots of artifact rendering (with highlighting)
 */
function(VertexService, template) {
    'use strict';

    var PREVIEW_CACHE = {};

    function Preview(_rowKey, options, callback) {
        this.options = options || {};
        this._rowKey = _rowKey;
        this.callback = this._cacheResult(callback);
    }

    Preview.prototype._cacheResult = function(callback) {
        var self = this;
        return function() {
            var args = jQuery.makeArray(arguments);
            if (args.length) {
                PREVIEW_CACHE[self._rowKey] = args;
            }
            callback.apply(undefined, args);

            if (self.taskFinished) {
                self.taskFinished();
            }
        };
    };

    Preview.prototype.start = function() {
        var self = this;
        // TODO
        self.callback();
        /*
        new VertexService().getProperties(this._rowKey)
            .fail(function() {
                self.callback();
            })
            .done(function(artifact) {
                if (artifact.type == 'image') {
                    var thumbnailUrl = artifact.thumbnailUrl;
                    if(thumbnailUrl) {
                        if(self.options.width) {
                            thumbnailUrl += '?width=' + self.options.width;
                        }
                    } else {
                        thumbnailUrl = artifact.rawUrl;
                    }
                    self.callback(thumbnailUrl, thumbnailUrl);
                } else if (artifact.type == 'video') {
                    var posterFrameUrl = artifact.posterFrameUrl;
                    var videoPreviewImageUrl = artifact.videoPreviewImageUrl;
                    if(self.options.width) {
                        posterFrameUrl += '?width=' + self.options.width;
                        videoPreviewImageUrl += '?width=' + self.options.width;
                    }
                    self.callback(posterFrameUrl, videoPreviewImageUrl);
                } else {
                    // TODO: Generate artifact preview on server
                    self.callback();
                }
            });
            */
    };


    function PreviewQueue(name, opts) {
        this.name = name;
        this.items = [];
        this.executing = [];
        this.options = $.extend({
            maxConcurrent: 1
        }, opts);
    }
    PreviewQueue.prototype.addTask = function(task) {
        this.items.push( task );
        this.take();
    };
    PreviewQueue.prototype.take = function() {
        if (this.items.length === 0) return;

        if (this.executing.length < this.options.maxConcurrent) {
            var task = this.items.shift();
            var cache = PREVIEW_CACHE[task._rowKey];
            if (cache) {
                task.callback.apply(null, cache);
                setTimeout(function() {
                    this.take();
                }.bind(this), 50);
            } else {
                this.executing.push( task );

                task.taskFinished = function() {
                    this.executing.splice(this.executing.indexOf(task), 1);
                    setTimeout(function() {
                        this.take();
                    }.bind(this), 50);
                }.bind(this);

                task.start();
            }
        }
    };
    PreviewQueue.prototype.cancel = function() {
        this.executing.forEach(function(task) {
            task.cancel();
        });
        this.executing.length = 0;
        this.items.length = 0;
    };

    var queues = {},
        defaultQueue = queues['default'] = new PreviewQueue('default', { maxConcurrent: 1 });

    return {

        cancelQueue: function(queueName) {
            queues[queueName || 'default'].cancel();
        },

        /**
         * Create a queue for preview processing
         *
         * @param queueName Name of the queue
         * @param options Options for configuring the queue
         * @param options.maxConcurrent Maximum concurrent operations on the queue
         */
        createQueue: function(queueName, options) {
            var queue = queues[queueName] = new PreviewQueue(queueName, options);
            return queue;
        },

        /**
         * Add a preview generation task to the queue
         *
         * @param _rowKey The artifact _rowKey
         * @param opts Options for preview generation
         * @param opts.width Width of the preview image preferred
         * @param opts.queueName Optional queue name to use
         * @param callback Task completion notification callback
         */
        generatePreview: function(_rowKey, opts, callback) {
            var options = $.extend({
                width: 200,
                queueName: 'default'
            }, opts || {});

            var queue = queues[options.queueName];
            if ( !queue) {
                queue = queues[options.queueName] = new PreviewQueue(queueName, options.queueOptions);
            }

            delete options.queueOptions;
            delete options.queueName;

            queue.addTask( new Preview(_rowKey, options, callback) );
        }
    };
});
