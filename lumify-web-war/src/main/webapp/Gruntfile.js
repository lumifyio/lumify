module.exports = function(grunt) {

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    bower: {
      install: {
          options: {
              targetDir: './libs',
              install: true,
              copy: false,
          }
      },
      prune: {
          options: {
              targetDir: './libs',
              copy: false,
              offline: true
          }
      }
    },

    exec: {
        build_openlayers: {
            command: 'python build.py -c none full ../OpenLayers.debug.js',
            stdout: false,
            cwd: 'libs/openlayers/build'
        },
        build_cytoscape: {
            command: 'make minify',
            stdout: false,
            cwd: 'libs/cytoscape.js'
        }
    },

    less: {
        development: {
            files: {
                "css/lumify.css": "less/lumify.less"
            },
            options: {
                paths: ["less"]
            }
        },
        production: {
            files: {
                "css/lumify.css": "less/lumify.less"
            },
            options: {
                paths: ["less"],
                compress: true,
                sourceMap: true,
                sourceMapFilename: 'css/lumify.css.map',
                sourceMapURL: 'lumify.css.map',
                sourceMapRootpath: '/',
                dumpLineNumbers: 'all'
            }
        }
    },

    requirejs: {
        options: {
            mainConfigFile: 'js/require.config.js',
            dir: 'jsc',
            baseUrl: 'js',
            preserveLicenseComments: false,
            removeCombined: false,
            /*
            modules: [
                { name: 'lumify' },
                { name: 'app' },
                { name: 'appFullscreenDetails' },
                { name: 'detail/artifact/artifact' },
                { name: 'detail/entity/entity' }
            ]
                */
        },
        development: {
            options: {
                logLevel: 2,
                optimize: 'none',
                keepBuildDir: true,
            }
        },
        production: {
            options: {
                logLevel: 0,
                optimize: 'uglify2',
                generateSourceMaps: true,
            }
        }
    },

    concurrent: {
        development: ['requirejs:development', 'less:development'],
        selenium: ['mochaSelenium:chrome', 'mochaSelenium:firefox'],
        tests: ['karma', 'jshint', 'mochaSelenium:chrome', 'mochaSelenium:firefox']
    },

    jshint: {
        options: {
            jshintrc: true
        },
        development: {
            files: {
                src: ['js/**/*.js']
            },
            options: {
                reporter: require('jshint-stylish')
            }
        },
        ci: {
            files: {
                src: ['js/**/*.js']
            },
            options: {
                reporter: 'checkstyle',
                reporterOutput: 'build/jshint-checkstyle.xml'
            }
        }
    },

    jscs: {
        options: {
            config: ".jscs.json"
        },
        all: { 
            src: ["js/**/*.js", "!js/**/three-plugins/*.js"],
        },
        passing: { 
            src: [
                "js/lumify.js",
                "js/detail/properties.js",
                "js/detail/dropdowns/propertyForm/propForm.js",
                "js/workspaces/diff/*.js"
            ]
        },
        ci: {
            src: ["js/**/*.js", "!js/**/three-plugins/*.js"],
            options: {
                force: true,
                reporter: "checkstyle",
                reporterOutput: "build/jscs-checkstyle.xml"
            }
        }
    },

    plato: {
        ci: {
            files: {
                'build/plato': ['js/**/*.js'],
            },
            options: {
                jshint: {
                    "browser": true,
                    "-W033": true,
                    "-W040": true
                }
            }
        },
    },

    watch: {
        options: {
            dateFormat: function(time) {
                grunt.log.ok('The watch finished in ' + (time/1000).toFixed(2) + 's. Waiting...');
            },
            spawn: false,
            interrupt: true
        },
        css: {
            files: ['less/**/*.less', 'libs/**/*.css', 'libs/**/*.less'],
            tasks: ['less:development', 'notify:css'],
            options: { livereload: true }
        },
        scripts: {
            files: ['js/**/*.js', 'js/**/*.ejs'],
            tasks: ['requirejs:development', 'notify:js'],
            options: { livereload: true }
        },
        lint: {
            files: ['js/**/*.js'],
            tasks: ['jshint:development']
        }
    },

    notify: {
        js: {
            options: {
                title: 'Lumify',
                message: 'RequireJS finished',
            }
        },
        css: {
            options: {
                title: 'Lumify',
                message: 'Less finished',
            }
        }
    },

    mochaSelenium: {
        options: { 
            screenshotAfterEach: true,
            screenshotDir: 'test/reports',
            reporter: 'spec', // doc for html
            viewport: { width: 900, height: 700 },
            timeout: 30e3,
            slow: 10e3,
            implicitWaitTimeout: 100,
            asyncScriptTimeout: 5000,
            usePromises: true,
            useChaining: true,
            ignoreLeaks: false
        },
        firefox: { src: ['test/functional/spec/**/*.js' ], options: { browserName: 'firefox' } },
        chrome:  { src: ['test/functional/spec/**/*.js' ], options: { browserName: 'chrome' } }
    },

    karma: {
        unit: {
            configFile: 'karma.conf.js',
            runnerPort: 9999,
            singleRun: true,
            //browsers: ['PhantomJS']
        }
    }
  });

  // on watch events configure jshint:all to only run on changed file
  grunt.event.on('watch', function(action, filepath) {
      grunt.config('jshint.development.src', filepath);
  });

  grunt.loadNpmTasks('grunt-bower-task');
  grunt.loadNpmTasks('grunt-exec');
  grunt.loadNpmTasks('grunt-concurrent');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-requirejs');
  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-notify');
  grunt.loadNpmTasks('grunt-mocha-selenium');
  grunt.loadNpmTasks('grunt-karma');
  grunt.loadNpmTasks("grunt-jscs-checker");
  grunt.loadNpmTasks('grunt-plato');

  grunt.registerTask('deps', 'Install Webapp Dependencies', ['bower:install', 'bower:prune', 'exec']);


  grunt.registerTask('test:functional:chrome', 'Run JavaScript Functional Tests in Chrome', ['mochaSelenium:chrome']);
  grunt.registerTask('test:functional:firefox', 'Run JavaScript Functional Tests in Firefox', ['mochaSelenium:firefox']);
  grunt.registerTask('test:functional', 'Run JavaScript Functional Tests', ['test:functional:chrome', 'test:functional:firefox']);
  grunt.registerTask('test:unit', 'Run JavaScript Unit Tests', ['karma']);
  grunt.registerTask('test:style', 'Run JavaScript CodeStyle reports', ['jshint:ci', 'plato:ci', 'jscs:ci']);
  grunt.registerTask('test', 'Run unit and functional tests', ['concurrent:tests'])

  grunt.registerTask('watch:style', function() {
      var config = {
          options: { interrupt: true },
          jscs: {
              files: [ 'js/**/*.js' ],
              tasks: ['jscs:passing']
          }
      };

      grunt.config('watch', config);
      grunt.task.run('watch');
  });

  grunt.registerTask('development', 'Build js/less for development', ['less:development', 'requirejs:development']);
  grunt.registerTask('production', 'Build js/less for production', ['less:production', 'requirejs:production']);

  grunt.registerTask('default', ['development']);
};
