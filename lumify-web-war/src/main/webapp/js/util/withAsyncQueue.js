
define([], function() {
    'use strict';

    return withAsyncQueue;

    function withAsyncQueue() {

        this.before('initialize', function() {
            var self = this,
                deferreds = {},
                stacks = {};

            this.setupAsyncQueue = function(name) {

                var stack = stacks[name] = [],
                    objectData;

                self[name + 'Ready'] = function(callback) {
                    if (callback) {
                        // Legacy non-promise
                        if (objectData) {
                            callback.call(self, objectData);
                        } else {
                            stack.push(callback);
                        }
                    } else {
                        return deferreds[name] || (deferreds[name] = $.Deferred());
                    }
                };

                self[name + 'IsReady'] = function() {
                    if (deferreds[name]) {
                        return deferreds[name].state() == 'resolved';
                    }

                    return objectData !== undefined;
                };

                self[name + 'MarkReady'] = function(data) {
                    if (!data) throw "No object passed to " + name + "MarkReady";

                    if (deferreds[name]) {
                        deferreds[name].resolve(data);
                    } else {
                        deferreds[name] = $.Deferred();
                        deferreds[name].resolve(data);
                    }

                    objectData = data;

                    stack.forEach(function(c) {
                        c.call(self, objectData);
                    });
                    stack.length = 0;
                };

                self[name + 'Unload'] = function() {
                    deferreds[name] = null;
                    objectData = null;
                };
            };
        });
    }
});
