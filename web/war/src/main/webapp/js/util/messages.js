
define([
    'promise!./service/messagesPromise'
], function(messages) {

    return function(key) {
        if (key in messages) {
            return messages[key];
        }

        console.error('No message for key', key);
        return key;
    };
});
