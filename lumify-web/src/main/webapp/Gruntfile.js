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
            options: {
                paths: ["less"],
                compress: true,
                sourceMap: true,
                sourceMapFilename: 'css/lumify.css.map',
                sourceMapURL: 'lumify.css.map',
                sourceMapRootpath: '/',
                dumpLineNumbers: 'all'
            },
            files: {
                "css/lumify.css": "less/lumify.less"
            }
        }
    },

    requirejs: {
        compile: {
            options: {
                mainConfigFile: 'js/require.config.js',
                dir: 'jsc',
                baseUrl: 'js',
                preserveLicenseComments: false,
                optimize: 'uglify2',
                generateSourceMaps: true,
                logLevel: 2,
                modules: [
                    { name: 'lumify' },
                    { name: 'app' },
                    { name: 'appFullscreenDetails' },
                    { name: 'detail/artifact/artifact' },
                    { name: 'detail/entity/entity' }
                ]
            }
        }
    },

    uglify: {
        development: {
            options: {
                sourceMap: 'path/to/source-map.js',
                sourceMapRoot: 'http://example.com/path/to/src/', // the location to find your original source
                sourceMapIn: 'example/coffeescript-sourcemap.js', // input sourcemap from a previous compilation
            },
            files: {
                'dest/output.min.js': ['src/input.js'],
            },
        },
    },

    watch: {
        css: {
            files: ['less/**/*.less', 'libs/**/*.css', 'libs/**/*.less'],
            tasks: ['less'],
            options: {
                spawn: false
            }
        },
        scripts: {
            files: ['js/**/*.js'],
            tasks: ['requirejs'],
            options: {
                spawn: false
            }
        }
    }
  });

  grunt.loadNpmTasks('grunt-bower-task');
  grunt.loadNpmTasks('grunt-exec');
  grunt.loadNpmTasks('grunt-contrib-less');
  grunt.loadNpmTasks('grunt-contrib-watch');
  grunt.loadNpmTasks('grunt-contrib-uglify');
  grunt.loadNpmTasks('grunt-contrib-requirejs');

  grunt.registerTask('deps', ['bower:install', 'bower:prune', 'exec']);
  grunt.registerTask('minify', ['less', 'requirejs']);
  grunt.registerTask('default', ['deps', 'minify']);
};
