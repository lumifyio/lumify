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
            command: 'make',
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

    jshint: {
        development: {
            files: {
                src: ['js/**/*.js']
            },
            options: {
                browser: true,
                '-W033': true, // Semicolons
                '-W040': true, // Ignore Strict violations from flight idioms
            }
        }
    },

    watch: {
        css: {
            files: ['less/**/*.less', 'libs/**/*.css', 'libs/**/*.less'],
            tasks: ['less:development'],
            options: {
                spawn: true
            }
        },
        scripts: {
            files: ['Gruntfile.js', 'js/**/*.js', 'js/**/*.ejs'],
            tasks: ['requirejs:development'],
            options: {
                spawn: true
            }
        },
        lint: {
            files: ['Gruntfile.js', 'js/**/*.js'],
            tasks: ['jshint:development'],
            options: {
                spawn: true
            }
        }
    }
  });

  grunt.loadNpmTasks('grunt-bower-task');
  grunt.loadNpmTasks('grunt-exec');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-requirejs');
  grunt.loadNpmTasks('grunt-contrib-jshint');

  grunt.registerTask('deps', ['bower:install', 'bower:prune', 'exec']);

  grunt.registerTask('development', ['less:development', 'requirejs:development']);
  grunt.registerTask('production', ['less:production', 'requirejs:production']);

  grunt.registerTask('default', ['development']);

};
