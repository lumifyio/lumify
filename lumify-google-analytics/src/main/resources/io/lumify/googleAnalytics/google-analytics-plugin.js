require([
    'jquery',
    'service/config'
], function($, ConfigService) {
    'use strict';

    var configService = new ConfigService();
    configService.getProperties().done(function(config) {
        var key = config['google-analytics.key'],
            domain = config['google-analytics.domain'];

        if (key && key !== null && domain && domain !== null) {
            console.log("Google Analytics key: " + key + ", domain: " + domain);

            (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
                (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
                m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
            })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

            ga('create', key, domain);
            ga('send', 'pageview');
        } else {
            console.log("required configuration properties for Google Analytics are not available");
        }
    });

    $(document).on('querysubmit', function(e, data) {
        ga('send', 'event', 'feature', 'querysubmit', data.value);
    });

    $(document).on('switchWorkspace', function(e, data) {
        ga('send', 'event', 'feature', 'switchWorkspace', data.workspaceId);
    });

    $(document).on('toggleGraphDimensions', function(e, data) {
        ga('send', 'event', 'feature', 'toggleGraphDimensions');
    });

    $(document).on('mapShow', function(e, data) {
        ga('send', 'event', 'feature', 'mapShow');
    });
});
