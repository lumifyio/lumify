define(['service/config'], function(ConfigService) {
    'use strict';

    var configService = new ConfigService();

    return configService.getMessages();
});
