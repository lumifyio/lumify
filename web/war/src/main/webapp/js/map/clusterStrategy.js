
define(['openlayers'], function(OpenLayers) {

    return OpenLayers.Class(OpenLayers.Strategy.Cluster, {
        activate: function() {
            var activated = OpenLayers.Strategy.prototype.activate.call(this);
            if (activated) {
                this.selectedFeatures = {};
                this.layer.events.on({
                    beforefeaturesadded: this.cacheFeatures,
                    featuresremoved: this.removeFeatures,
                    featureselected: this.onFeaturesSelected,
                    featureunselected: this.onFeaturesUnselected,
                    moveend: this.cluster,
                    scope: this
                });
            }
            return activated;
        },

        onFeaturesSelected: function(event) {
            var feature = event.feature,
                sf = this.selectedFeatures = {};

            (feature.cluster || [feature]).forEach(function(f) {
                sf[f.id] = true;
            });
        },

        onFeaturesUnselected: function(event) {
            if (!this.selectedFeatures) return;

            var sf = this.selectedFeatures;

            if (event.feature) {
                if (event.feature.cluster) {
                    event.feature.cluster.forEach(function(f) {
                        delete sf[f.data.vertex.id];
                    });
                } else {
                    delete sf[event.feature.data.vertex.id];
                }
            } else {
                this.selectedFeatures = {};
            }
        },

        addToCluster: function(cluster, feature) {
            OpenLayers.Strategy.Cluster.prototype.addToCluster.apply(this, arguments);
            if (this.selectedFeatures[feature.data.vertex.id]) {
                cluster.renderIntent = 'select';
            }
        },

        cluster: function(event) {
            var i;

            if ((!event || event.zoomChanged) && this.features) {
                var resolution = this.layer.map.getResolution();
                if (resolution != this.resolution ||
                    !this.clustersExist() ||
                    this.previousCount != this.features.length
                ) {
                    this.previousCount = this.features.length;
                    this.resolution = resolution;
                    var clusters = [],
                        feature,
                        clustered,
                        cluster;

                    for (i = 0; i < this.features.length; ++i) {
                        feature = this.features[i];
                        if (feature.geometry) {
                            clustered = false;
                            for (var j = clusters.length - 1; j >= 0; --j) {
                                cluster = clusters[j];
                                if (this.shouldCluster(cluster, feature)) {
                                    this.addToCluster(cluster, feature);
                                    clustered = true;
                                    break;
                                }
                            }
                            if (!clustered) {
                                clusters.push(this.createCluster(this.features[i]));
                            }
                        }
                    }
                    this.clustering = true;
                    this.layer.removeAllFeatures();
                    this.clustering = false;
                    if (clusters.length > 0) {
                        if (this.threshold > 1) {
                            var clone = clusters.slice();
                            clusters = [];
                            var candidate;
                            for (i = 0, len = clone.length; i < len; ++i) {
                                candidate = clone[i];
                                if (candidate.attributes.count < this.threshold) {
                                    Array.prototype.push.apply(clusters, candidate.cluster);
                                } else {
                                    clusters.push(candidate);
                                }
                            }
                        }
                        this.clustering = true;
                        // A legitimate feature addition could occur during this
                        // addFeatures call.  For clustering to behave well, features
                        // should be removed from a layer before requesting a new batch.
                        this.layer.addFeatures(clusters);
                        this.clustering = false;
                    }
                    this.clusters = clusters;
                }
            }

            var needsRedraw = false;
            if (this.clusters) {
                var selectedIds = _.keys(this.selectedFeatures);
                this.clusters.forEach(function(feature) {
                    if (feature.cluster) {
                        var some = false, all = true;
                        feature.cluster.forEach(function(f) {
                            var selected = ~selectedIds.indexOf(f.data.vertex.id);
                            some = some || selected;
                            all = all && selected;
                        });

                        if (all) {
                            if (feature.renderIntent !== 'select') {
                                feature.renderIntent = 'select';
                                needsRedraw = true;
                            }
                        } else if (some) {
                            if (feature.renderIntent !== 'temporary') {
                                feature.renderIntent = 'temporary';
                                needsRedraw = true;
                            }
                        } else {
                            if (feature.renderIntent && feature.renderIntent !== 'default') {
                                feature.renderIntent = 'default';
                                needsRedraw = true;
                            }
                        }
                    }
                });
            }

            if (event && event.object) {
                var zoom = event.object.map.zoom;
                if (!this._lastZoom || zoom !== this._lastZoom) {
                    needsRedraw = true;
                    this._lastZoom = zoom;
                }
            }

            if (!this._throttledRedraw) {
                this._throttledRedraw = _.debounce(function() {
                    if (this.layer) {
                        this.layer.redraw();
                    }
                }.bind(this), 250);
            }

            if (needsRedraw) {
                this._throttledRedraw();
            }
        },

        createCluster: function(feature) {
            var cluster = OpenLayers.Strategy.Cluster.prototype.createCluster.apply(this, arguments);
            if (this.selectedFeatures[feature.id]) {
                cluster.renderIntent = 'select';
            } else cluster.renderIntent = 'default';
            return cluster;
        },

        cacheFeatures: function(event) {
            var propagate = true;
            if (!this.clustering) {
                this.features = this.features || [];
                var currentIds = [];
                this.features.forEach(function gatherId(feature) {
                    if (feature.cluster) {
                        feature.cluster.forEach(gatherId);
                        return;
                    }
                    currentIds.push(feature.id);
                });
                event.features.forEach(function(feature) {
                    if (!~currentIds.indexOf(feature.id)) {
                        this.features.push(feature);
                    }
                }.bind(this));
                this.cluster();
                propagate = false;
            }
            return propagate;
        },

        removeFeatures: function(event) {
            if (!this.clustering) {
                var existingIds = _.pluck(event.features, 'id');
                this.features = _.filter(this.features, function(feature) {
                    return !~existingIds.indexOf(feature.id);
                });
            }
        }
    });
});
