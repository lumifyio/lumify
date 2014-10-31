/*! see LICENCE for Simplified BSD Licence */
/*jslint browser:true, indent:2*/
/*global define, require*/ // Require.JS

define(function () {


  return {
    /**
     * @param {String} name This is the name of the desired resource module.
     * @param {Function} req Provides a "require" to load other modules.
     * @param {Function} load Pass the module's result to this function.
     * @param {Object} config Provides the optimizer's configuration.
     */
    load: function (name, req, load) { // , config
      // TODO: check config.isBuild\
      // TODO: call load.fromText() if necessary to eval JavaScript text
      req([name], function (result) {
        if (result && typeof result === 'object' &&
            typeof result.then === 'function') {
          result.fail(function () {
            load.error.apply(this, arguments);
          });
          // If the promise supports "done" (not all do), we want to use that to
          // terminate the promise chain and expose any exceptions.
          var complete = result.done || result.then;
          complete.call(result, function () {
            load.apply(this, arguments);
          });
        } else {
          load(result);
        }
      });
    }/*,
    write: function () {
      // TODO: what needs to be done for write() ??
    }, */
/*        pluginBuilder: function () {
      // TODO: what needs to be done for pluginBuilder() ??
    } */
    /*
     * Note: we explicitly do NOT implement normalize(), as the simpler
     * default implementation is sufficient for current use cases.
     */
  };
});
