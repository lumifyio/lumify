
define([
    'util/requirejs/promise!./service/messagesPromise'
], function(messages) {

    return function(key/**, args **/) {
        if (key in messages) {
            if (arguments.length === 1) {
                return messages[key];
            }

            var args = Array.prototype.slice.call(arguments);
            args.shift();
            return messages[key].replace(/\{(\d+)\}/g, function(m) {
                var index = parseInt(m[1], 10);
                return args[index];
            });
        }

        console.error('No message for key', key);
        return key;
    };
});
