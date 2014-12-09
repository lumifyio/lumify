define(['util/promise'], function(Promise) {
    return function ajax(method, url) {
        if (url === '/configuration') {
            return Promise.resolve(JSON.parse(CONFIG_JSON));
        } else if (url === '/ontology') {
            return Promise.resolve(JSON.parse(ONTOLOGY_JSON));
        }

        throw new Error('Unknown url called', method, url);
    };
})