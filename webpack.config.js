const webpack = require('@nativescript/webpack');

module.exports = (env) => {
  webpack.init(env);

  // force the vue config, since we don't have nativescript-vue in package.json
  webpack.useConfig('vue');

  webpack.chainWebpack((config) => {
    // resolve nativescript-toasty to the updated version scoped under @triniwiz
    config.resolve.alias
        .set('nativescript-toasty', '@triniwiz/nativescript-toasty');
  });

  return webpack.resolveConfig();
};
