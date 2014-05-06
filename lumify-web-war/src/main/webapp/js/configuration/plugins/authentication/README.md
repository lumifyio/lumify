
AUTHENTICATION Plugin
=====================

Plugin to configure the user interface for authentication

The visibility component requires 1 flightjs component:

    authentication.js


This plugin can reload the page after succesful login or trigger an event.

### Events

Authentication plugin event contract.

#### Optionally Fire:

* `loginSuccess`: When the login is succesful. Reloading the page will also work.

        this.trigger('loginSuccess');
