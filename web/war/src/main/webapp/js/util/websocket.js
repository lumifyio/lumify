define([], function() {

    return pushDataToSocket;

    function pushDataToSocket(socket, data) {
        var string = JSON.stringify(data);
        if (string.length > 1024 * 1024) {
            return console.warn('Unable to push data, too large: ', string.length, data)
        }
        return socket.push(string);
    }
})
