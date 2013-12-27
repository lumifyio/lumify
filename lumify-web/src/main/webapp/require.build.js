{
  mainConfigFile: 'js/require.config.js',
  dir: 'jsc',
  baseUrl: 'js',
  //appDir: '../jsc',
  //baseUrl: './',
  //generateSourceMaps: true,
  preserveLicenseComments: false,
  optimize: 'none', //uglify2',
  keepBuildDir: false,
  modules: [
      { name: 'lumify' },
      { name: 'app' },
      { name: 'appFullscreenDetails' },
      { name: 'detail/artifact/artifact' },
      { name: 'detail/entity/entity' }
  ]
}
