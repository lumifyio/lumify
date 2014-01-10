
define([
    'service/vertex'
], 
/**
 * Generate preview screenshots of artifact rendering (with highlighting)
 */
function(VertexService, template) {
    'use strict';

    var PREVIEW_CACHE = {};

    function Preview(vertex, options, callback) {
        this.options = options || {};
        this.vertex = vertex;
        this.callback = this._cacheResult(callback);
    }

    Preview.prototype._cacheResult = function(callback) {
        var self = this;
        return function() {
            var args = jQuery.makeArray(arguments);
            if (args.length) {
                PREVIEW_CACHE[self.vertex.id] = args;
            }
            callback.apply(undefined, args);

            if (self.taskFinished) {
                self.taskFinished();
            }
        };
    };

    Preview.prototype.start = function() {
        var self = this,
            vertex = this.vertex;

        if (!vertex || !vertex.concept) return self.callback();


        switch (vertex.concept.displayType) {
            case "image":
                var url = '/artifact/' + vertex.id + '/thumbnail'
                self.callback(url, url);
                break;
            case "video":
                self.callback(
                    '/artifact/' + vertex.id + '/poster-frame',
                    '/artifact/' + vertex.id + '/video-preview'
                );
                break;
            default:
                self.callback();
        }
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
            var cache = PREVIEW_CACHE[task.vertex.id];
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
         * @param vertex The vertex to generate
         * @param opts Options for preview generation
         * @param opts.width Width of the preview image preferred
         * @param opts.queueName Optional queue name to use
         * @param callback Task completion notification callback
         */
        generatePreview: function(vertex, opts, callback) {
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

            queue.addTask( new Preview(vertex, options, callback) );
        }
    };
});
