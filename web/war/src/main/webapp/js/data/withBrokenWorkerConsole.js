// Some browser don't provide console in worker threads
define([], function() {
    'use strict';

    return withBrokenWorkerConsole;

    function withBrokenWorkerConsole() {

        this.brokenWorkerConsole = function(message) {
            console[message.logType].apply(console, message.messages);
        };

    }
});
