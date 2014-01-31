
define([], function() {
    'use strict';

    return withAsyncQueue;

    function withAsyncQueue() {

        this.before('initialize', function() {
            var self = this,
                stacks = {};

            this.setupAsyncQueue = function(name) {

                var stack = stacks[name] = [],
                    objectData;

                self[name + 'Ready'] = function(callback) {
                    if (objectData) {
                        callback.call(self, objectData);
                    } else {
                        stack.push(callback);
                    }
                };

                self[name + 'IsReady'] = function() {
                    return objectData !== undefined;
                };

                self[name + 'MarkReady'] = function(data) {
                    if (!data) throw "No object passed to " + name + "MarkReady";

                    objectData = data;

                    stack.forEach(function(c) {
                        c.call(self, objectData);
                    });
                    stack.length = 0;
                };

                self[name + 'Unload'] = function() {
                    objectData = null;
                };
            };
        });
    }
});
