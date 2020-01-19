var path = require('path');
var webpack = require('webpack')
var ROOT = path.resolve(__dirname, 'consoleapp');
var SRC = path.resolve(ROOT, 'javascript');
var DEST = path.resolve(__dirname, 'consoleapp/');

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
