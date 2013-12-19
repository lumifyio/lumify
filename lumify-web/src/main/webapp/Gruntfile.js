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
                sourceMapRootpath: '../'
            },
            files: {
                "css/lumify.css": "less/lumify.less"
            }
        }
    },

    watch: {
        scripts: {
            files: ['less/**/*.less', 'libs/**/*.css', 'libs/**/*.less'],
            tasks: ['less'],
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

  grunt.registerTask('deps', ['bower:install', 'bower:prune', 'exec']);
  grunt.registerTask('minify', ['less']);
  grunt.registerTask('default', ['deps', 'minify']);
};
