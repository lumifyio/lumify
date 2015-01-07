
var Suite = Mocha.Suite;
var Test = Mocha.Test;

Mocha.interfaces['mocha-flight'] = (function () {
  'use strict';

  return function (suite) {
    var suites = [suite];

    suite.on('pre-require', function (context, file, mocha) {
      /**
       * Execute before running tests.
       */
      context.before = function (fn) {
        suites[0].beforeAll(fn);
      };

      /**
       * Execute after running tests.
       */
      context.after = function (fn) {
        suites[0].afterAll(fn);
      };

      /**
       * Execute before each test case.
       */
      context.beforeEach = function (fn) {
        suites[0].beforeEach(fn);
      };

      /**
       * Execute after each test case.
       */
      context.afterEach = function (fn) {
        suites[0].afterEach(fn);
      };

      /**
       * Describe a "suite" with the given `title` and callback `fn` containing
       * nested suites and/or tests.
       */
      context.describe = context.context = function (title, fn) {
        var suite = Suite.create(suites[0], title);
        suites.unshift(suite);
        fn.call(suite);
        suites.shift();
        return suite;
      };

      /**
       * Describe a "suite" with the given `title` and callback `fn` containing
       * nested suites and/or tests.
       *
       * Load the Flight component before each test.
       *
       * @param componentPath
       * @param fn
       */
      context.describeComponent = context.contextComponent = function (componentPath, fn) {
        var suite = Suite.create(suites[0], componentPath);
        suites.unshift(suite);

        context.beforeEach(function (done) {
          this.Component = this.component = this.$node = null;

          var requireCallback = function (registry, Component) {
            registry.reset();
            this.Component = Component;
            done();
          }.bind(this);

          require(['flight/lib/registry', componentPath], requireCallback);
        });

        context.afterEach(function (done) {
          if (this.$node) {
            this.$node.remove();
            this.$node = null;
          }

          var requireCallback = function (defineComponent) {
            if (this.component) {
              this.component = null;
            }

            this.Component = null;
            defineComponent.teardownAll();
            done();
          }.bind(this);

          require(['flight/lib/component'], requireCallback);
        });

        fn.call(suite);
        suites.shift();
        return suite;
      };

      /**
       * Describe a "suite" with the given `title` and callback `fn` containing
       * nested suites and/or tests.
       *
       * Load the Flight component before each test.
       *
       * @param componentPath
       * @param fn
       */
      context.describeMixin = context.contextMixin = function (mixinPath, fn) {
        var suite = Suite.create(suites[0], mixinPath);
        suites.unshift(suite);

        context.beforeEach(function (done) {
          this.Component = this.component = this.$node = null;

          var requireCallback = function (registry, defineComponent, Mixin) {
            registry.reset();
            this.Component = defineComponent(function () {}, Mixin);
            done();
          }.bind(this);

          require(['flight/lib/registry', 'flight/lib/component', mixinPath], requireCallback);
        });

        context.afterEach(function (done) {
          if (this.$node) {
            this.$node.remove();
            this.$node = null;
          }

          var requireCallback = function (defineComponent) {
            if (this.component) {
              this.component = null;
            }

            this.Component = null;
            defineComponent.teardownAll();
            done();
          }.bind(this);

          require(['flight/lib/component'], requireCallback);
        });

        fn.call(suite);
        suites.shift();
        return suite;
      };

      context.describeModule = context.contextModule = function (modulePath, fn) {
        var suite = Suite.create(suites[0], modulePath);
        suites.unshift(suite);

        context.beforeEach(function (done) {
          this.module = null;

          var requireCallback = function (module) {
            this.module = module;
            done();
          }.bind(this);

          require([modulePath], requireCallback);
        });

        fn.call(suite);
        suites.shift();
        return suite;
      };

      /**
       * Pending describe.
       */
      context.xdescribe = context.xcontext = context.describe.skip = function (title, fn) {
        var suite = Suite.create(suites[0], title);
        suite.pending = true;
        suites.unshift(suite);
        fn.call(suite);
        suites.shift();
      };

      /**
       * Exclusive suite.
       */
      context.describe.only = function (title, fn) {
        var suite = context.describe(title, fn);
        mocha.grep(suite.fullTitle());
      };

      /**
       * Describe a specification or test-case with the given `title` and
       * callback `fn` acting as a thunk.
       */
      context.it = context.specify = function (title, fn) {
        var suite = suites[0];
        if (suite.pending) {
          fn = null;
        }
        var test = new Test(title, fn);
        suite.addTest(test);
        return test;
      };

      /**
       * Exclusive test-case.
       */
      context.it.only = function (title, fn) {
        var test = context.it(title, fn);
        mocha.grep(test.fullTitle());
      };

      /**
       * Pending test case.
       */
      context.xit =
      context.xspecify =
      context.it.skip = function (title) {
        context.it(title);
      };

      /**
       * Create root node and initialize component. Fixture should be html
       * string or jQuery object.
       * @param fixture {String} (Optional)
       * @param options {Options} (Optional)
       */
      context.setupComponent = function (ctx, fixture, options) {
        if (ctx.component) {
          ctx.component.teardown();
          ctx.$node.remove();
        }

        if (fixture instanceof jQuery || typeof fixture === 'string') {
          ctx.$node = $(fixture);
        } else {
          ctx.$node = $('<div />');
          options = fixture;
          fixture = null;
        }
        ctx.$node.addClass('component-root');
        $('body').append(ctx.$node);

        options = typeof options === 'undefined' ? {} : options;

        ctx.component = (new ctx.Component()).initialize(ctx.$node, options);
      };
    });
  };
}).call(this);
