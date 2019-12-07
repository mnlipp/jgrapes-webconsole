var path = require('path');
var webpack = require('webpack')
var ROOT = path.resolve(__dirname, 'portalapp');
var SRC = path.resolve(ROOT, 'javascript');
var DEST = path.resolve(__dirname, 'portalapp/');

module.exports = {
  mode: 'development',
  devtool: 'source-map',
  entry: {
    app: SRC + '/functions.js',
  },
  output: {
    path: DEST,
    filename: 'bundle.js',
    publicPath: '/dist/'
  },
  module: {
  }
};
