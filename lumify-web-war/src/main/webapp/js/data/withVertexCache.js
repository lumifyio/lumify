
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

            cache.detectedObjects = vertex.detectedObjects;
            if (this.workspaceVertices[id]) {
                this.workspaceVertices[id] = cache.workspace;
            }

            /*
            if (_.isString(cache.properties.geoLocation && cache.properties.geoLocation.value)) {
                var m = cache.properties.geoLocation.value.match(/point\[(.*?),(.*?)\]/);
                if (m) {
                    var latitude = m[1];
                    var longitude = m[2];
                    cache.properties.geoLocation.value = {
                        latitude: latitude,
                        longitude: longitude,
                        title: cache.properties._geoLocationDescription.value
                    };
                }
            } 
            */

            cache.concept = this.cachedConcepts.byId[cache.properties._conceptType.value || cache.properties._conceptType]
            if (!cache.concept) {
                console.error('Unable to attach concept to vertex', cache.concept, cache.properties._conceptType);
            }

            /*
            if ((cache.properties.latitude && cache.properties.latitude.value) || 
                (cache.properties.longitude && cache.properties.longitude.value)) {
                $.extend(cache.properties.geoLocation.value || (cache.properties.geoLocation.value = {}), {
                    latitude: cache.properties.latitude.value,
                    longitude: cache.properties.longitude.value,
                    title: cache.properties._geoLocationDescription.value
                });
                delete cache.properties.latitude.value;
                delete cache.properties.longitude.value;
                delete cache.properties._geoLocationDescription.value;
            }
            */

            return cache;
        };

    }
});
