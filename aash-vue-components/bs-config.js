
/*
 |--------------------------------------------------------------------------
 | Browser-sync config file
 |--------------------------------------------------------------------------
 |
 | For up-to-date information about the options:
 |   http://www.browsersync.io/docs/options/
 |
 | There are more options than you see here, these are just the ones that are
 | set internally. See the website for more info.
 |
 |
 */
module.exports = {
    "files": ["lib", "demo"],
    "server": {
        baseDir: "demo",
        routes: {
            "/lib": "lib",
            "/node_modules": "node_modules"
        }
    },
    middleware: [
        {
            route: "/lib",
            handle: function(req, res, next) {
                if (req.url === '/vue.esm.re-export.js') {
                    res.writeHead(301, {Location: '/vue.esm.re-export.js'});
                    res.end();
                }
                return next();
            }
        }
    ]
};