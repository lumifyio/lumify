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
            command: 'python build.py -c minimize full ../OpenLayers.js && python build.py -c none full ../OpenLayers.debug.js',
            stdout: false,
            cwd: 'libs/openlayers/build'
        },
        build_cytoscape: {
            command: 'make',
            stdout: false,
            cwd: 'libs/cytoscape.js'
        }
    }
  });

  grunt.loadNpmTasks('grunt-bower-task');
  grunt.loadNpmTasks('grunt-exec');

  grunt.registerTask('deps', ['bower:install', 'bower:prune', 'exec']);
  grunt.registerTask('default', ['deps']);
};
