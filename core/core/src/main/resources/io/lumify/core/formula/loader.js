require.config({
    baseUrl: "",
    paths: {
        // LIBS
        'promise': 'libs/promise',
        'sf': 'libs/sf',
        'timezone-js': 'libs/timezone-js',

        // MOCKS
        'service/serviceBase': 'mocks/serviceBase',

        // SRC
        'service/config': 'service_config',
        'service/ontology': 'service_ontology',
        'util/formatters': 'util_formatters',
        'util/messages': 'util_messages',
        'util/service/messagesPromise': 'util_service_messagesPromise',
        'util/service/ontologyPromise': 'util_service_ontologyPromise',
        'util/vertex/formatters': 'util_vertex_formatters',
        'util/vertex/formula': 'util_vertex_formula',
        'util/vertex/urlFormatters': 'util_vertex_urlFormatters'
    }
});

// Global Mocks
$ = { extend: _.extend };
window = this;
navigator = { userAgent: ''};
console = {
    log: print,
    info: print,
    debug: print,
    warn: consoleWarn,
    error: consoleError
};

// Fix service methods
require(['service/ontology', 'service/config'], function(OntologyService, ConfigService) {
    OntologyService.prototype._ajaxGet = staticAjaxGet(ONTOLOGY_JSON);
    ConfigService.prototype._ajaxGet = staticAjaxGet(CONFIG_JSON);

    function staticAjaxGet(json) {
        return function() {
            var val = JSON.parse(json),
            x = {
                done: function(call) {
                    call(val);
                    return x;
                },
                then: function(callback) {
                    val = callback(val);
                    return x;
                },
                fail: function(callback) {
                    return x;
                }
            }
            return x;
        }
    }
});

require(['util/vertex/formatters'], function(F) {
    var createFunction = function(name) {
            return function(json) {
                 return F.vertex[name](JSON.parse(json));
            }
        };

    window.evaluateTitleFormulaJson = createFunction('title');
    window.evaluateTimeFormulaJson = createFunction('time');
    window.evaluateSubtitleFormulaJson = createFunction('subtitle');


//    java.lang.System.out.println(
//        F.vertex.time({
//            properties:[
//                {name:'http://lumify.io/dev#birthDate',value:new Date()},
//                {name:'http://lumify.io#conceptType',value:'http://lumify.io/dev#person'}
//            ]
//        })
//    )
})
