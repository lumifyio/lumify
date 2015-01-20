
define([
    'util/requirejs/promise!./service/messagesPromise'
], function(messages) {

    return function(ignoreWarning, key/**, args **/) {
        var args = Array.prototype.slice.call(arguments);
        if (ignoreWarning === true) {
            args.shift();
        } else {
            ignoreWarning = false;
        }

        key = args[0];
        if (key in messages) {
            if (args.length === 1) {
                return messages[key];
            }

            args.shift();
            return messages[key].replace(/\{(\d+)\}/g, function(m) {
                var index = parseInt(m[1], 10);
                return args[index];
            });
        }

        if (ignoreWarning) {
            return;
        } else {
            console.error('No message for key', key);
        }
        return key;
    };
});
