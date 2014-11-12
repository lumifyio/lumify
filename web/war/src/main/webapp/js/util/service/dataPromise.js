define(['util/promise'], function() {
    return new Promise(function(fulfill, reject) {
        if (typeof lumifyData !== 'undefined' && lumifyData.readyForDataRequests) {
            fulfill();
        } else {
            $(document).one('readyForDataRequests', function() {
                fulfill();
            })
            setTimeout(reject.bind(null, 'Data not ready, and timed out waiting'), 5000);
        }
    });
});
