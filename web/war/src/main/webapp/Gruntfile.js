module.exports = function(grunt) {

  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    clean: ['jsc', 'css'],

    bower: {
      install: {
          options: {
              targetDir: './libs',
              install: true,
              copy: false,
              quiet: true
          }
      },
      prune: {
          options: {
              targetDir: './libs',
              copy: false,
              offline: true,
              quiet: true
          }
      }
    },

    exec: {
        buildOpenlayers: {
            command: 'python build.py -c none full ../OpenLayers.debug.js',
            stdout: false,
            cwd: 'libs/openlayers/build'
        },
        buildCytoscape: {
            command: 'make minify',
            stdout: false,
            cwd: 'libs/cytoscape.js'
        },
        buildPathFinding: {
            command: 'npm install -q && make',
            stdout: false,
            cwd: 'libs/PathFinding.js'
        },
        rewritePromiseSourceMapUrl: {
            command: 'sed -i".bak" \'/# sourceMappingURL=/d\' promise-*.js',
            stdout: false,
            cwd: 'libs/promise-polyfill/polyfills/output'
        }
    },

    less: {
        options: {
            paths: ['less'],
            sourceMap: true,
            sourceMapFilename: 'css/lumify.css.map',
            sourceMapURL: 'lumify.css.map',
            sourceMapRootpath: '/',
            dumpLineNumbers: 'all'
        },
        development: {
            files: {
                'css/lumify.css': 'less/lumify.less'
            }
        },
        production: {
            files: {
                'css/lumify.css': 'less/lumify.less'
            },
            options: {
                compress: true
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

    jshint: {
        options: {
            jshintrc: true
        },
        development: {
            src: [
                'js/**/*\.js',
                '!js/plugin-development/**/libs/**/*.js'
            ]
        },
        ci: {
            src: [
                'js/**/*\.js',
                '!js/plugin-development/**/libs/**/*.js'
            ],
            options: {
                reporter: 'checkstyle',
                reporterOutput: 'build/jshint-checkstyle.xml'
            }
        }
    },

    jscs: {
        options: {
            config: '.jscs.json'
        },
        development: {
            src: [
                'js/**/*.js',
                'test/spec/**/*.js',
                '!js/**/three-plugins/*.js',
                '!js/graph/3d/3djs/3djs/graph/layout/force-directed.js',
                '!js/plugin-development/**/*.js',
                '!js/require.config.js'
            ],
        },
        ci: {
            src: [
                'js/**/*.js',
                'test/spec/**/*.js',
                '!js/**/three-plugins/*.js',
                '!js/graph/3d/3djs/3djs/graph/layout/force-directed.js',
                '!js/plugin-development/**/*.js',
                '!js/require.config.js'
            ],
            options: {
                force: true,
                reporter: 'checkstyle',
                reporterOutput: 'build/jscs-checkstyle.xml'
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
                    browser: true,
                    '-W033': true,
                    '-W040': true
                }
            }
        },
    },

    watch: {
        options: {
            dateFormat: function(time) {
                grunt.log.ok('The watch finished in ' + (time / 1000).toFixed(2) + 's. Waiting...');
            },
            spawn: false,
            interrupt: false
        },
        css: {
            files: ['less/**/*.less', 'libs/**/*.css', 'libs/**/*.less'],
            tasks: ['less:development', 'notify:css']
        },
        scripts: {
            files: [
                'js/**/*.js',
                'js/**/*.less',
                'js/**/*.ejs',
                'js/**/*.hbs',
                'js/**/*.vsh',
                'js/**/*.fsh'
            ],
            tasks: ['requirejs:development', 'notify:js'],
            options: {
                livereload: {
                    port: 35729,
                    key: grunt.file.read('test/localhost.key'),
                    cert: grunt.file.read('test/localhost.cert')
                }
            }
        },
        lint: {
            files: ['js/**/*.js'],
            tasks: ['jshint:development']
        },
        jscs: {
            files: [ 'js/**/*.js' ],
            tasks: ['jscs:development']
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

  grunt.loadNpmTasks('grunt-bower-task');
  grunt.loadNpmTasks('grunt-exec');
  grunt.loadNpmTasks('grunt-contrib-clean');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-requirejs');
  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-notify');
  grunt.loadNpmTasks('grunt-mocha-selenium');
  grunt.loadNpmTasks('grunt-karma');
  grunt.loadNpmTasks('grunt-jscs-checker');
  grunt.loadNpmTasks('grunt-plato');

  // Speed up jscs and jshint by only checking changed files
  // ensure we still ignore files though
  var initialJscsSrc = grunt.config('jscs.development.src'),
      initialHintSrc = grunt.config('jshint.development.src');
  grunt.event.on('watch', function(action, filepath) {
      var matchingHint = grunt.file.match(initialHintSrc, filepath),
          matchingJscs = grunt.file.match(initialJscsSrc, filepath);

      grunt.config('jshint.development.src', matchingHint);
      grunt.config('jscs.development.src', matchingJscs);
  });

  grunt.registerTask('deps', 'Install Webapp Dependencies',
     ['bower:install', 'bower:prune', 'exec']);

  grunt.registerTask('test:functional:chrome', 'Run JavaScript Functional Tests in Chrome',
     ['mochaSelenium:chrome']);
  grunt.registerTask('test:functional:firefox', 'Run JavaScript Functional Tests in Firefox',
     ['mochaSelenium:firefox']);
  grunt.registerTask('test:functional', 'Run JavaScript Functional Tests',
     ['test:functional:chrome', 'test:functional:firefox']);

  grunt.registerTask('test:unit', 'Run JavaScript Unit Tests',
     ['karma']);
  grunt.registerTask('test:style', 'Run JavaScript CodeStyle reports',
     ['jshint:ci', 'plato:ci', 'jscs:ci']);
  grunt.registerTask('style:development', 'Run JavaScript CodeStyle reports',
    ['jshint:development', 'jscs:development']);

  grunt.registerTask('development', 'Build js/less for development',
     ['less:development', 'requirejs:development']);
  grunt.registerTask('production', 'Build js/less for production',
     ['less:production', 'requirejs:production']);

  grunt.registerTask('default', ['clean', 'development', 'style:development', 'watch']);
};
