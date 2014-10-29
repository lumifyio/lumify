
$ = {
    ajaxPrefilter: function() {},
    extend: function(o, a) {
        for (var i in a) {
            o[i] = a[i];
        }
        return o;
    }
};

document = { };

navigator = { userAgent: '' };

postfixes = {
    'service/ontology': function(dep) {
        dep.prototype._ajaxGet = function() {
            var val = JSON.parse(TitleFormulaEvaluator.getOntologyJson()),
                x = {
                    done: function(call) {
                        call(val);
                        return x;
                    },
                    then: function(callback) {
                        val = callback(val);
                        return x;
                    }
                }
            return x;
        }
        return dep;
    }
};

function define(array, func) {
    var deps = [],
        jsFile,
        dep,
        path;


try {

    for (var i = 0; i < array.length; i++) {
        path = array[i];
        println('// ' + path);

        jsFile = TitleFormulaEvaluator.getJavaScriptFile(path);

        if (jsFile) {
            dep = TitleFormulaEvaluator.evaluate(jsFile, path);
            if (dep && path in postfixes) {
                dep = postfixes[path](dep);
            }
        } else println('No File: ' + path)
        deps.push(dep);
    }
    return func.apply(this, deps);
    } catch(e) {
    println(e);
    }
}
