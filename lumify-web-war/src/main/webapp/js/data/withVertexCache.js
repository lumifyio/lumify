
define([
    'service/vertex',
], function(VertexService) {

    return withVertexCache;

    function withVertexCache() {

        this.cachedVertices = {};
        this.workspaceVertices = {};
        if (!this.vertexService) {
            this.vertexService = new VertexService();
        }

        this.resemblesVertices = function(val) {
            return val && val.length && val[0].id && val[0].properties;
        };

        this.verticesInWorkspace = function() {
            return _.values(_.pick(this.cachedVertices, _.keys(this.workspaceVertices)));
        };

        this.copy = function(obj) {
            return JSON.parse(JSON.stringify(obj));
        };

        this.workspaceOnlyVertexCopy = function(vertex) {
            return {
                id: vertex.id,
                workspace: this.copy(vertex.workspace || this.workspaceVertices[vertex.id] || {})
            };
        };

        this.workspaceOnlyVertex = function(id) {
            return {
                id: id,
                workspace: this.workspaceVertices[id] || {}
            };
        };

        this.inWorkspace = function(vertex) {
            return !!this.workspaceVertices[_.isString(vertex) ? vertex : vertex.id];
        };

        this.vertex = function(id) {
            return this.cachedVertices['' + id];
        };

        this.vertices = function(ids) {
            if (ids.toArray) {
                ids = ids.toArray();
            }

            ids = ids.map(function(id) {
                return id.id ? id.id : '' + id;
            });

            return _.values(_.pick(this.cachedVertices, ids));
        };

        this.refresh = function(vertex) {
            var self = this,
                deferred = null;

            if (_.isString(vertex) || _.isNumber(vertex)) {
                deferred = this.vertexService.getVertexProperties(vertex);
            } else {
                deferred = this.vertexService.getVertexProperties(vertex.id);
            }

            return deferred.then(function(v) {
                return self.vertex((v && v.id) || vertex.id);
            });
        };

        this.updateCacheWithVertex = function(vertex, options) {
            var id = vertex.id;
            var cache = this.cachedVertices[id] || (this.cachedVertices[id] = { id: id });

            if (options && options.deletedProperty && cache.properties) {
                delete cache.properties[options.deletedProperty]
            }

            $.extend(true, cache.properties || (cache.properties = {}), vertex.properties);
            $.extend(true, cache.workspace ||  (cache.workspace = {}),  vertex.workspace || {});
            $.extend(true, cache.detectedObjects || (cache.detectedObjects = []), vertex.detectedObjects || []);

            if (this.workspaceVertices[id]) {
                this.workspaceVertices[id] = cache.workspace;
            }

            cache.concept = this.cachedConcepts.byId[cache.properties._conceptType.value || cache.properties._conceptType]
            if (cache.concept) {
                setPreviewsForVertex(cache);
            } else console.error('Unable to attach concept to vertex', cache.properties._conceptType);

            return cache;
        };


        function setPreviewsForVertex(vertex) {
            var vId = encodeURIComponent(vertex.id),
                artifactUrl = _.template("/artifact/" + vId + "/<%= type %>");

            vertex.imageSrcIsFromConcept = false;

            if (vertex.properties._glyphIcon) {
                vertex.imageSrc = vertex.properties._glyphIcon.value;
            } else {
                switch (vertex.concept.displayType) {

                    case 'image': 
                        vertex.imageSrc = artifactUrl({ type: 'thumbnail' });
                        vertex.imageRawSrc = artifactUrl({ type: 'raw' });
                        break;

                    case 'video': 
                        vertex.imageSrc = artifactUrl({ type: 'poster-frame' });
                        vertex.imageRawSrc = artifactUrl({ type: 'raw' });
                        vertex.imageFramesSrc = artifactUrl({ type: 'video-preview' });
                        break;

                    default:
                        vertex.imageSrc = vertex.concept.glyphIconHref;
                        vertex.imageSrcIsFromConcept = true;
                }
            }
        }
    }
});
