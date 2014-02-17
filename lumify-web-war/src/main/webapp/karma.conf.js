// Karma configuration
// Generated on Tue Aug 20 2013 12:03:01 GMT-0500 (CDT)

module.exports = function(config) {

  // karma start --coverage [coverageType]
  // http://karma-runner.github.io/0.8/config/coverage.html
  
  var karmaConfig = {

    // base path, that will be used to resolve files and exclude
    basePath: '',

    // frameworks to use
    frameworks: ['mocha', 'requirejs'],

    // list of files / patterns to load in the browser
    files: [

      // Source 
      {pattern: 'js/**/*.js', included: false},

      // Templates
      {pattern: 'js/**/*.ejs', included: false},

      // Included libs
      'libs/jquery/jquery.js',
      'libs/jquery-ui/ui/jquery-ui.js',
      'libs/bootstrap/docs/assets/js/bootstrap.js',

      // Libraries
      {pattern: 'libs/**/*.js', included: false},

      // Test Files
      {pattern: 'test/unit/spec/**/*.js', included: false},

      // Test Mocks
      {pattern: 'test/unit/mocks/**/*.js', included: false},
      {pattern: 'test/unit/utils/**/*.js', included: false},

      // Test runner
      'test/unit/runner/testRunner.js'
    ],


    // list of files to exclude
    exclude: [ ],

    // test results reporter to use
    // possible values: 'dots', 'progress', 'junit', 'growl', 'coverage'
    reporters: ['progress'],


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_WARN,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,


    // Start these browsers, currently available:
    // - Chrome
    // - ChromeCanary
    // - Firefox
    // - Opera
    // - Safari (only Mac)
    // - PhantomJS
    // - IE (only Windows)
    browsers: ['Chrome'],


    // If browser does not capture in given timeout [ms], kill it
    captureTimeout: 60000,


    // Continuous Integration mode
    // if true, it capture browsers, run tests and exit
    singleRun: false,

    plugins: [
      'karma-mocha',
      'karma-requirejs',
      'karma-coverage',
      'karma-chrome-launcher',
      'karma-safari-launcher',
      'karma-firefox-launcher',
      'karma-phantomjs-launcher'
    ]
  };

  var coverageType = 'html';
  var coverage = process.argv.filter(function(a, index) { 
    if (a == '--coverage') {
      if ((index + 1) < process.argv.length) {
        coverageType = process.argv[index+1];
      }
      return true;
    }
    return false;
  }).length;
  if (coverage) {
    karmaConfig.preprocessors = {
        'js/*.js': 'coverage',
        'js/**/*.js': 'coverage'
    };
    karmaConfig.reporters.push('coverage');
    karmaConfig.coverageReporter = {
      type: coverageType,
      dir: 'build/coverage/'
    };
  }

  config.set(karmaConfig);
};
