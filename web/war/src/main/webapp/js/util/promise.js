/**
 * Add some promise helpers, done, finally, and progress
 *
 * Example:
 *
 *  var p = new Promise(function(f, r) {
 *      var duration = 4,
 *          startTime = Date.now(),
 *          t = setInterval(function() {
 *              var now = Date.now(),
 *                  dt = (now - startTime) / (duration * 1000);

 *              if (dt < 1.0) {
 *                  f.updateProgress(dt);
 *              } else {
 *                  f(true)
 *              }
 *          }, 16);
 *  }).progress(function(percent) {
 *      console.log('Updated', percent);
 *  }).then(function() {
 *      console.log('finished');
 *  }).finally(function() {
 *      console.log('finally')
 *  })
 */
define(['promise-polyfill'], function() {

    addProgress();

    addFinally();
    addTimeout();
    addRequire();
    fixThen();

    return Promise;

    function addFinally() {
        if (typeof Promise.prototype.finally !== 'function') {
            Promise.prototype.finally = function(onEither) {
                return this.then(function(result) {
                    onEither();
                    return result;
                }, function(error) {
                    onEither();
                    throw error;
                });
            };
        } else console.warn('Native implementation of finally');
    }

    function addProgress() {
        if (typeof Promise.prototype.progress !== 'function') {
            Promise.prototype.progress = function(progress) {
                this._progressCallbacks = this._progressCallbacks || [];
                this._progressCallbacks.push(progress);
                return this;
            };
        } else console.warn('Native implementation of progress');

        // Wrap Promise constructor to add progress support
        var OldPromise = Promise;
        Promise = function(callback) {

            var reject,
                self = new OldPromise(function(f, r) {
                // Update progress is a function on fulfill function
                f.updateProgress = updateProgress;

                callback(f, r);
            });

            self.then(function() {
                updateProgress(1);
            });

            return self;

            function updateProgress(percent) {
                if (self._progressCallbacks) {
                    self._progressCallbacks.forEach(function(c) {
                        c(percent || 0);
                    })
                }
            }
        }

        'all race reject resolve'.split(' ').forEach(function(key) {
            Promise[key] = OldPromise[key];
        })
        Promise.prototype = OldPromise.prototype;
    }

    function addTimeout() {
        if (typeof Promise.timeout !== 'function') {
            Promise.timeout = function(millis) {
                return new Promise(function(fulfill) {
                    setTimeout(fulfill, millis);
                });
            }
        } else console.warn('Native implementation of timeout');
    }

    function addRequire() {
        if (typeof Promise.prototype.require !== 'function') {
            Promise.require = function() {
                var deps = Array.prototype.slice.call(arguments, 0);

                return new Promise(function(fulfill, reject) {
                    require(deps, fulfill, reject);
                });
            };
        } else console.warn('Native implementation of require');
    }

    function fixThen() {
        var oldThen = Promise.prototype.then;
        Promise.prototype.then = function() {
            var p = oldThen.apply(this, Array.prototype.slice.call(arguments, 0));
            if (this.abort) {
                p.abort = this.abort;
            }
            return p;
        }
    }

});
