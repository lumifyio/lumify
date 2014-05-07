/*
 * https://github.com/guybedford/require-less
 *
 * Modified to lazily add less css to page after call to `applyStyleForClass`
 * that wraps the less in a class to componetize it. -Jason Harwig
 *
MIT License
-----------

Copyright (C) 2013 Guy Bedford

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in the
Software without restriction, including without limitation the rights to use, copy,
modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to the
following conditions:

The above copyright notice and this permission notice shall be included in all copies
or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

define([], function() {
    'use strict';

    var lessAPI = {};

    lessAPI.normalize = function(name, normalize) {
        if (name.substr(name.length - 5, 5) == '.less') {
            name = name.substr(0, name.length - 5);
        }

        name = normalize(name);

        return name;
    }

    var head = document.getElementsByTagName('head')[0],
        base = document.getElementsByTagName('base');

    base = base && base[0] && base[0] && base[0].href;

    var pagePath = (base || window.location.href.split('#')[0].split('?')[0]).split('/');
    pagePath[pagePath.length - 1] = '';
    pagePath = pagePath.join('/');

    var styleCnt = 0,
        curStyle;

    lessAPI.inject = function(css) {
        if (styleCnt < 31) {
            curStyle = document.createElement('style');
            curStyle.type = 'text/css';
            head.appendChild(curStyle);
            styleCnt++;
        }
        if (curStyle.styleSheet) {
            curStyle.styleSheet.cssText += css;
        } else {
            curStyle.appendChild(document.createTextNode(css));
        }
    }

    lessAPI.load = function(lessId, req, load, config) {
        window.less = config.less || {};
        window.less.env = 'development';

        require(['lessc', 'normalize'], function(lessc, normalize) {

            var fileUrl = req.toUrl(lessId + '.less');
            fileUrl = normalize.absoluteURI(fileUrl, pagePath);

            require(['text!' + fileUrl], function(lessText) {
                load({
                    applyStyleForClass: function(cls) {
                        var parser = new lessc.Parser(window.less);

                        parser.parse('.' + cls + ' { ' + lessText + ' };', function(err, tree) {
                            if (err)
                                return load.error(err);

                            lessAPI.inject(normalize(tree.toCSS(config.less), fileUrl, pagePath));
                        });
                    }
                });
            });

        });
    }

    return lessAPI;
});
