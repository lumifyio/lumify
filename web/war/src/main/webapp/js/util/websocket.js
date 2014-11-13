define([], function() {

    return {
        generateSourceGuid: function() {
            var guid = [];
            for (i = 0; i < 4; i++) {
                guid.push(Math.floor((1 + Math.random()) * 0xFFFFFF).toString(16).substring(1));
            }
            return guid.join(':');
        },

        pushDataToSocket: function(socket, sourceGuid, data) {
            var string = JSON.stringify(_.extend({}, data, {
                sourceGuid: sourceGuid
            }));
            if (string.length > 1024 * 1024) {
                return console.warn('Unable to push data, too large: ', string.length, data)
            }
            return socket.push(string);
        }
    };

})
