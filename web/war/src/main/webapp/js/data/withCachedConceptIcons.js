define([], function() {
    'use strict';

    return withCachedConceptIcons;

    function withCachedConceptIcons() {

        var cacheConceptIconsOnce = _.once(cacheConceptIcons);

        this.around('dataRequestCompleted', function(dataRequestCompleted, request) {
            if (isOntologyRequest(request)) {
                var self = this,
                    result = request.result,
                    concepts = request.originalRequest.method === 'ontology' ?
                        result.concepts : result;

                _.defer(cacheConceptIconsOnce, concepts);
            }

            return dataRequestCompleted.call(this, request);
        });

    }

    function isOntologyRequest(request) {
        return request &&
               request.success &&
               request.originalRequest.service === 'ontology' &&
               (
                   request.originalRequest.method === 'ontology' ||
                   request.originalRequest.method === 'concepts'
               ) &&
               request.result;
    }

    function cacheConceptIcons(concepts) {
        var urls = _.chain(concepts.byId)
            .values()
            .pluck('glyphIconHref')
            .compact()
            .unique()
            .value();

        require(['util/deferredImage'], function(deferredImage) {
            cacheNextImage(urls);

            function cacheNextImage(urls) {
                if (!urls.length) {
                    return;
                }

                var url = urls.shift();

                deferredImage(url).always(function() {
                    _.defer(cacheNextImage, urls);
                });
            }
        });
    }
});
