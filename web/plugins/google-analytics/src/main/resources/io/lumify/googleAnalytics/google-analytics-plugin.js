require([
    'jquery',
    'underscore'
], function($, _) {
    'use strict';

    require(['util/withDataRequest'], function(withDataRequest) {
        withDataRequest.dataRequest('config', 'properties').done(function(config) {
            var key = config['google-analytics.key'],
                domain = config['google-analytics.domain'];

            if (key && domain) {
                console.log('Google Analytics key: ' + key + ', domain: ' + domain);

                (function(i,s,o,g,r,a,m) {
                    i.GoogleAnalyticsObject = r;
                    i[r]=i[r]||function(){
                    (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
                    m=s.getElementsByTagName(o)[0];
                    a.async = 1;
                    a.src = g;
                    m.parentNode.insertBefore(a,m)
                })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

                ga('create', key, domain);
                ga('send', 'pageview');

                var send = _.partial(ga, 'send', 'event');

                $(document)
                    .on('querysubmit', function(e, data) {
                        send('feature', 'querysubmit', data.value);
                    })
                    .on('filterWorkspace', function(e, data) {
                        send('feature', 'filterWorkspace', data.value);
                    })
                    .on('switchWorkspace', function(e, data) {
                        send('feature', 'switchWorkspace', data.workspaceId);
                     })
                    .on('toggleGraphDimensions', function(e, data) {
                        send('feature', 'toggleGraphDimensions');
                    })
                    .on('mapShow', function(e, data) {
                        send('feature', 'mapShow');
                    })
                    .on('fit', function(e, data) {
                        send('feature', 'fit');
                    })
                    .on('escape', function(e, data) {
                        send('feature', 'escape');
                    })
                    .on('showVertexContextMenu', function(e, data) {
                        send('feature', 'showVertexContextMenu');
                    })
                    .on('searchByEntity', function(e, data) {
                        send('feature', 'searchByEntity');
                    })
                    .on('searchByRelatedEntity', function(e, data) {
                        send('feature', 'searchByRelatedEntity');
                    })
                    .on('toggleAuditDisplay', function(e, data) {
                        send('feature', 'toggleAuditDisplay');
                    })
                    .on('addVertices', function(e, data) {
                        if (data && data.options && data.options.fileDropPosition) {
                            send('vertices', 'upload', data ? data.vertices.length : 0);
                        } else {
                            send('vertices', 'add', data ? data.vertices.length : 0);
                        }
                    })
                    .on('updateVertices', function(e, data) {
                        send('vertices', 'update', data ? data.vertices.length : 0);
                    })
                    .on('deleteVertices', function(e, data) {
                        send('vertices', 'delete', data ? data.vertices.length : 0);
                    })
                    .on('selectObjects', function(e, data) {
                        send('vertices', 'selectObjects', data ? data.vertices.length : 0);
                    });
            } else {
                console.log('required configuration properties for Google Analytics are not available');
            }
        });
    });
});
