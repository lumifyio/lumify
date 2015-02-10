# Data

Responsible for all communication with server. All files in this directory run in the main browser thread, all files in `web-worker` directory run in a background thread web worker.

The data component is defined in `data.js` and along with all the mixins manages brokering requests from the UI to the worker thread.

The UI triggers `dataRequest` events that contain at least two parameters.

    * Service Name
    * Method Name
    * Array of parameters (optional)

For example:

        this.trigger('dataRequest', {
            service: 'config',
            method: 'properties'
        })

For convenience mixin `util/withDataRequest` to add a `dataRequest` function to your component. It returns a Promise.

        defineComponent(MyComponent, withDataRequest)
        ...
        this.dataRequest('config', 'properties')
            .always(function() { })
            .then(function(properties) { })
            .catch(function(error) { })
