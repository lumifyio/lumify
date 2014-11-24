
define([
    'flight/lib/component',
    'tpl!./template',
    'tpl!./instructions/regionCenter',
    'tpl!./instructions/regionRadius',
    'tpl!./instructions/regionLoading',
    'util/retina',
    'util/controls',
    'util/vertex/formatters',
    'util/withAsyncQueue',
    'util/withContextMenu',
    'util/withDataRequest'
], function(defineComponent,
    template,
    centerTemplate,
    radiusTemplate,
    loadingTemplate,
    retina,
    Controls,
    F,
    withAsyncQueue,
    withContextMenu,
    withDataRequest) {
    'use strict';

    var MODE_NORMAL = 0,
        MODE_REGION_SELECTION_MODE_POINT = 1,
        MODE_REGION_SELECTION_MODE_RADIUS = 2,
        MODE_REGION_SELECTION_MODE_LOADING = 3;

    return defineComponent(MapViewOpenLayers, withContextMenu, withAsyncQueue, withDataRequest);

    function MapViewOpenLayers() {

        var ol, latlon, point;

        this.mode = MODE_NORMAL;

        this.defaultAttrs({
            mapSelector: '#map',
            contextMenuSelector: '.contextmenu',
            contextMenuVertexSelector: '.contextmenuvertex',
            controlsSelector: '.controls'
        });

        this.before('teardown', function() {
            this.mapReady(function(map) {
                map.featuresLayer.destroyFeatures();
                this.clusterStrategy.deactivate();
                this.clusterStrategy.destroy();
                map.destroy();
                map = null;
                this.mapUnload();
                this.select('mapSelector').empty().removeClass('olMap');
            })
        })

        this.after('initialize', function() {
            this.initialized = false;
            this.setupAsyncQueue('map');
            this.$node.html(template({})).find('.shortcut').each(function() {
                var $this = $(this), command = $this.text();
                $this.text(F.string.shortcut($this.text()));
            });

            this.on(document, 'mapShow', this.onMapShow);
            this.on(document, 'mapCenter', this.onMapCenter);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'verticesAdded', this.onVerticesAdded);
            this.on(document, 'verticesDropped', this.onVerticesDropped);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on(document, 'verticesDeleted', this.onVerticesDeleted);
            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);

            this.padding = {l: 0,r: 0,b: 0,t: 0};

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: i18n('map.help.scope'),
                shortcuts: {
                    '-': { fire: 'zoomOut', desc: i18n('map.help.zoom_out') },
                    '=': { fire: 'zoomIn', desc: i18n('map.help.zoom_in') },
                    'alt-f': { fire: 'fit', desc: i18n('map.help.fit') },
                }
            });

            this.attachToZoomPanControls();

            // TODO: Fix
            /*
            var verticesInWorkspace = appData.verticesInWorkspace();
            if (verticesInWorkspace.length) {
                this.updateOrAddVertices(verticesInWorkspace, { adding: true, preventShake: true });
            }
            */
        });

        this.attachToZoomPanControls = function() {
            Controls.attachTo(this.select('controlsSelector'));

            this.mapReady(function(map) {

                // While panning add a div so map doesn't swallow mousemove
                // events
                this.on('startPan', function() {
                    this.$node.append('<div class="draggable-wrapper"/>');
                });
                this.on('endPan', function() {
                    this.$node.find('.draggable-wrapper').remove();
                    map.featuresLayer.redraw();
                });

                this.on('pan', function(e, data) {
                    e.stopPropagation();
                    map.pan(
                        data.pan.x * -1,
                        data.pan.y * -1,
                        { animate: false }
                    );
                });
                this.on('fit', function(e) {
                    e.stopPropagation();
                    this.fit(map);
                });

                var slowZoomIn = _.throttle(map.zoomIn.bind(map), 250, {trailing: false}),
                    slowZoomOut = _.throttle(map.zoomOut.bind(map), 250, {trailing: false});

                this.on('zoomIn', function() {
                    slowZoomIn();
                });
                this.on('zoomOut', function() {
                    slowZoomOut();
                });
            });
        };

        this.onMapShow = function() {
            if (this.mapIsReady()) {
                this.mapReady(function(map) {
                    _.defer(function() {
                        map.updateSize();
                    });
                });
            } else this.initializeMap();
        };

        this.onMapCenter = function(evt, data) {
            this.mapReady(function(map) {
                map.setCenter(latLon(data.latitude, data.longitude), 7);
            });
        };

        this.onGraphPaddingUpdated = function(event, data) {
            return (this.padding = $.extend({}, data.padding));
        };

        this.onWorkspaceLoaded = function(evt, workspaceData) {
            var self = this;
            this.isWorkspaceEditable = workspaceData.editable;
            this.mapReady(function(map) {

                map.featuresLayer.removeAllFeatures();

                if (this.clusterStrategy.features) {
                    this.clusterStrategy.features.length = 0;
                }

                if (this.clusterStrategy.clusters) {
                    this.clusterStrategy.clusters.length = 0;
                }

                map.featuresLayer.redraw();

                this.updateOrAddVertices(workspaceData.data.vertices, {
                    adding: true,
                    preventShake: true
                });
            });
        };

        this.onVerticesDropped = function(evt, data) {
            if (this.$node.is(':visible')) {
                this.trigger(document, 'addVertices', data);
            }
        };

        this.onVerticesAdded = function(evt, data) {
            this.updateOrAddVertices(data.vertices, _.extend({ adding: true }, data.options));
        };

        this.onVerticesUpdated = function(evt, data) {
            this.updateOrAddVertices(data.vertices);
        };

        this.onVerticesDeleted = function(evt, data) {
            this.mapReady(function(map) {
                var featuresLayer = map.featuresLayer,
                    toRemove = [],
                    ids = _.pluck(data.vertices, 'id');

                featuresLayer.features.forEach(function removeIfDeleted(feature) {
                    if (~ids.indexOf(feature.id)) {
                        toRemove.push(feature);
                    } else if (feature.cluster) {
                        feature.cluster.forEach(removeIfDeleted);
                    }
                });

                featuresLayer.removeFeatures(toRemove);
                this.clusterStrategy.cluster();
            });
        };

        this.onObjectsSelected = function(evt, data) {
            var self = this,
                vertices = data.vertices;

            this.mapReady(function(map) {
                var self = this,
                    featuresLayer = map.featuresLayer,
                    selectedIds = _.pluck(vertices, 'id'),
                    toRemove = [];

                // Remove features not selected and not in workspace
                featuresLayer.features.forEach(function unselectFeature(feature) {
                    if (feature.cluster) {
                        feature.cluster.forEach(unselectFeature);
                        return;
                    }

                    if (!~selectedIds.indexOf(feature.data.vertex.id)) {
                        if (feature.data.inWorkspace) {
                            feature.style.externalGraphic = feature.style.externalGraphic.replace(/&selected/, '');
                        } else {
                            toRemove.push(feature);
                        }
                    }
                });

                if (toRemove.length) {
                    featuresLayer.removeFeatures(toRemove);
                }

                // Create new features for new selections
                vertices.forEach(function(vertex) {
                    self.findOrCreateMarkers(map, vertex);
                });

                var sf = this.clusterStrategy.selectedFeatures = {};
                selectedIds.forEach(function(sId) {
                    sf[sId] = true;
                });

                featuresLayer.redraw();
            });
        };

        this.findOrCreateMarkers = function(map, vertex) {
            var self = this,
                geoLocationProperties = this.ontologyProperties.byDataType.geoLocation,
                geoLocations = geoLocationProperties &&
                    _.chain(geoLocationProperties)
                        .map(function(geoLocationProperty) {
                            return F.vertex.props(vertex, geoLocationProperty.title);
                        })
                        .compact()
                        .flatten()
                        .filter(function(g) {
                            return g.value && g.value.latitude && g.value.longitude;
                        })
                        .value(),
                conceptType = F.vertex.prop(vertex, 'conceptType'),
                selected = vertex.id in lumifyData.selectedObjects.vertexIds,
                iconUrl =  'map/marker/image?' + $.param({
                    type: conceptType,
                    scale: retina.devicePixelRatio > 1 ? '2' : '1'
                }),
                heading = F.vertex.heading(vertex);

            if (!geoLocations || geoLocations.length === 0) return;
            if (selected) iconUrl += '&selected';

            return geoLocations.map(function(geoLocation) {
                var featureId = vertex.id + geoLocation.key,
                    feature = map.featuresLayer.getFeatureById(featureId);

                if (!feature) {
                    map.featuresLayer.features.forEach(function(f) {
                        if (!feature && f.cluster) {
                            feature = _.findWhere(f.cluster, { id: featureId });
                        }
                    });
                }

                if (!feature) {
                    feature = new ol.Feature.Vector(
                        point(geoLocation.value.latitude, geoLocation.value.longitude),
                        { vertex: vertex },
                        {
                            graphic: true,
                            externalGraphic: iconUrl,
                            graphicWidth: 22,
                            graphicHeight: 40,
                            graphicXOffset: -11,
                            graphicYOffset: -40,
                            rotation: heading,
                            cursor: 'pointer'
                        }
                    );
                    feature.id = featureId;
                    map.featuresLayer.addFeatures(feature);
                } else {
                    if (feature.style.externalGraphic !== iconUrl) {
                        feature.style.externalGraphic = iconUrl;
                    }
                    feature.move(latLon(geoLocation.value.latitude, geoLocation.value.longitude));
                }

                return feature;
            })
        };

        this.updateOrAddVertices = function(vertices, options) {
            var self = this,
                adding = options && options.adding,
                preventShake = options && options.preventShake,
                validAddition = false;

            this.mapReady(function(map) {
                self.dataRequest('workspace', 'store')
                    .done(function(workspaceVertices) {
                        vertices.forEach(function(vertex) {
                            var inWorkspace = vertex.id in workspaceVertices,
                                markers = [];

                            if (!adding && !inWorkspace) {

                                // Only update marker if it exists
                                map.featuresLayer.features.forEach(function(f) {
                                    if (f.cluster) {
                                        markers.push(_.find(f.cluster, function(f) {
                                            return f.data.vertex.id === vertex.id;
                                        }))
                                    } else if (f.data.vertex.id === vertex.id) {
                                        markers.push(f);
                                    }
                                });

                                if (markers.length) {
                                    markers = self.findOrCreateMarkers(map, vertex);
                                }
                            } else {
                                markers = self.findOrCreateMarkers(map, vertex);
                            }

                            if (markers && markers.length) {
                                markers.forEach(function(m) {
                                    validAddition = true;
                                    m.data.inWorkspace = inWorkspace;
                                });
                            }
                        });

                        self.clusterStrategy.cluster();
                        map.featuresLayer.redraw();

                        if (adding && vertices.length && validAddition) {
                            self.fit(map);
                        }

                        if (adding && !validAddition && !preventShake) {
                            self.invalidMap();
                        }
                    })
            });

        };

        this.fit = function(map) {
            var self = this,
                dataExtent = map.featuresLayer.getDataExtent();

            if (dataExtent) {
                var screenPadding = 20,
                    padding = {
                        l: this.padding.l + screenPadding,
                        r: this.padding.r + this.select('controlsSelector').width() + screenPadding,
                        t: this.padding.t + screenPadding * 2,
                        b: this.padding.b + screenPadding * 2
                    },
                    viewportWidth = this.$node.width() - padding.l - padding.r,
                    viewportHeight = this.$node.height() - padding.t - padding.b,

                    // Figure out ideal resolution based on available realestate
                    idealResolution = Math.max(
                        dataExtent.getWidth()  / viewportWidth,
                        dataExtent.getHeight() / viewportHeight
                    ),
                    zoom = map.getZoomForResolution(idealResolution, false),
                    actualResolution = map.getResolutionForZoom(zoom),

                    // Center of markers...
                    centerLonLat = dataExtent.getCenterLonLat(),

                    // Adjust center based on pane paddings
                    offsetX = padding.l - padding.r,
                    offsetY = padding.t - padding.b,
                    lon = offsetX * actualResolution / 2,
                    lat = offsetY * actualResolution / 2;

                // If there is only one feature don't zoom in so close
                if (map.featuresLayer.features.length === 1) {
                    zoom = Math.min(5, zoom);
                }

                map.setCenter(new ol.LonLat(centerLonLat.lon - lon, centerLonLat.lat - lat), zoom);
            } else {
                map.zoomTo(2);
            }
        };

        this.invalidMap = function() {
            var map = this.select('mapSelector'),
                cls = 'invalid',
                animate = function() {
                    map.removeClass(cls);
                    _.defer(function() {
                        map.on(ANIMATION_END, function() {
                            map.off(ANIMATION_END);
                            map.removeClass(cls);
                        });
                        map.addClass(cls);
                    });
                };

            if (this.$node.closest('.visible').length === 0) {
                return;
            } else if (!this.preventShake) {
                animate();
            }
        };

        this.handleContextMenu = function(event) {
            event.originalEvent = event.originalEvent || event;

            this.mapReady(function(map) {
                var feature = map.featuresLayer.getFeatureFromEvent(event);
                if (feature) {

                    var vertices = (feature.cluster || [feature]).map(function(f) {
                        return f.data.vertex;
                    });

                    this.trigger('selectObjects', { vertices: vertices });

                    if (vertices.length === 1) {
                        this.trigger('showVertexContextMenu', {
                            vertexId: vertices[0].id,
                            position: {
                                x: event.pageX,
                                y: event.pageY
                            }
                        });
                    } else {
                        var menu = this.select('contextMenuVertexSelector');
                        menu.data('feature', feature);
                        this.toggleMenu({ positionUsingEvent: event }, menu);
                    }
                } else this.toggleMenu({ positionUsingEvent: event }, this.select('contextMenuSelector'));
            });
        };

        this.onContextMenuRemoveItem = function() {
            var menu = this.select('contextMenuVertexSelector'),
                feature = menu.data('feature'),
                vertices = (feature.cluster || [feature]).map(function(f) {
                    return f.data.vertex;
                });

            this.trigger('deleteVertices', { vertices: vertices });
        };

        this.onContextMenuLoadResultsWithinRadius = function() {
            var self = this;

            this.mode = MODE_REGION_SELECTION_MODE_POINT;
            this.$node.find('.instructions').remove();
            this.$node.append(centerTemplate({}));
            $(document).on('keydown.regionselection', function(e) {
                if (e.which === $.ui.keyCode.ESCAPE) {
                    self.endRegionSelection();
                }
            });
        };

        this.endRegionSelection = function() {
            this.mode = MODE_NORMAL;

            this.off('mousemove');
            $('#map_mouse_position_hack').remove();
            this.$node.find('.instructions').remove();

            if (this.regionLayer) {
                this.mapReady(function(map) {
                    map.removeLayer(this.regionLayer);
                    map.removeControl(this.modifyRegionControl);
                });
            }

            $(document).off('keydown.regionselection');
        };

        this.onMapClicked = function(evt, map) {
            var self = this;
            this.$node.find('.instructions').remove();

            switch (self.mode) {
                case MODE_NORMAL:
                    self.trigger('selectObjects');
                    map.featuresLayer.events.triggerEvent('featureunselected');
                    break;

                case MODE_REGION_SELECTION_MODE_POINT:

                    self.mode = MODE_REGION_SELECTION_MODE_RADIUS;

                    this.$node.append(radiusTemplate({}));

                    var offset = self.$node.offset();
                    self.regionCenterPoint = map.getLonLatFromViewPortPx({
                        x: evt.pageX - offset.left,
                        y: evt.pageY - offset.top
                    });
                    var centerPoint = new ol.Geometry.Point(self.regionCenterPoint.lon, self.regionCenterPoint.lat),
                        circleFeature = new ol.Feature.Vector(
                            OpenLayers.Geometry.Polygon.createRegularPolygon(
                                centerPoint,
                                // Default diameter is 10% of viewport
                                map.getExtent().getWidth() * 0.1 / 2,
                                30,
                                0
                            ),
                            {},
                            { fillOpacity: 0.8, fillColor: '#0070C3', strokeColor: '#08538B' }
                        ),
                        layer = new ol.Layer.Vector('SelectionLayer', {
                            /*
                             * TODO: change resize handle colors
                            styleMap: new ol.StyleMap({
                                'default': new ol.Style({ fillColor: '#ff0000'}),
                                'select': new ol.Style({ fillColor: '#ff0000'})
                            })
                            */
                        });

                    self.regionLayer = layer;
                    self.regionFeature = circleFeature;

                    layer.addFeatures(circleFeature);
                    map.addLayer(layer);

                    var modify = new ol.Control.ModifyFeature(layer);
                    modify.mode = ol.Control.ModifyFeature.RESIZE |
                                  ol.Control.ModifyFeature.DRAG;
                    map.addControl(modify);
                    modify.activate();
                    modify.selectFeature(circleFeature);
                    self.modifyRegionControl = modify;

                    break;

                case MODE_REGION_SELECTION_MODE_RADIUS:

                    self.mode = MODE_REGION_SELECTION_MODE_LOADING;

                    var area = self.regionFeature.geometry.getArea(),
                        radius = 0.565352 * Math.sqrt(area) / 1000,
                        lonlat = self.regionCenterPoint.transform(
                            map.getProjectionObject(),
                            new ol.Projection('EPSG:4326')
                        );

                    self.$node.find('.instructions').remove();
                    self.$node.append(loadingTemplate({}));

                    self.dataRequest('vertex', 'geo-search',
                        lonlat.lat,
                        lonlat.lon,
                        radius
                    ).done(
                        function(data) {
                            self.endRegionSelection();
                            self.trigger('updateWorkspace', {
                                entityUpdates: data.vertices.map(function(vertex) {
                                    return { vertexId: vertex.id };
                                })
                            });
                        }
                    );

                    break;

            }
        };

        this.initializeMap = function() {
            var self = this,
                openlayersDeferred = $.Deferred(),
                clusterStrategyDeferred = $.Deferred()
                mapProviderDeferred = $.Deferred();

            require(['openlayers'], openlayersDeferred.resolve);
            require(['map/clusterStrategy'], clusterStrategyDeferred.resolve);

            this.dataRequest('config', 'properties').done(function(configProperties) {
              if (configProperties['map.provider'] == 'google') {
                require(['goog!maps,3,other_params:sensor=false'], function() {
                  google.maps.visualRefresh = true;
                  mapProviderDeferred.resolve(configProperties);
                });
              } else {
                mapProviderDeferred.resolve(configProperties);
              }
            });

            $.when(
              openlayersDeferred,
              clusterStrategyDeferred,
              mapProviderDeferred
            ).done(function(openlayers, cluster, configProperties) {
              ol = openlayers;
              self.createMap(ol, cluster, configProperties);
            });
        };

        this.createMap = function(ol, ClusterStrategy, configProperties) {
            ol.ImgPath = '/libs/openlayers/img';

            var self = this,
                controls = new ol.Control.Navigation({
                    handleRightClicks: true,
                    dragPanOptions: {
                        enableKinetic: true
                    }
                }),
                map = new ol.Map({
                    zoomDuration: 0,
                    numZoomLevels: 18,
                    theme: null,
                    displayProjection: new ol.Projection('EPSG:4326'),
                    controls: [ controls ]
                }),
                cluster = new ClusterStrategy({
                    distance: 45,
                    threshold: 2,
                    animationMethod: ol.Easing.Expo.easeOut,
                    animationDuration: 100
                }),
                style = self.featureStyle(),
                selectedStyle = {
                    fillColor: '#0070C3', labelOutlineColor: '#08538B', strokeColor: '#08538B',
                },
                partialSelectionStyle = {
                    strokeColor: '#08538B'
                },
                base;

            map.featuresLayer = new ol.Layer.Vector('Markers', {
                strategies: [ cluster ],
                styleMap: new ol.StyleMap({
                    'default': new ol.Style(style.baseStyle, style.baseContext),
                    temporary: new ol.Style($.extend({}, style.baseStyle, partialSelectionStyle), style.baseContext),
                    select: new ol.Style($.extend({}, style.baseStyle, selectedStyle), style.baseContext)
                })
            });

            map.featuresLayer.getDataExtent = function() {
                var maxExtent = null,
                    features = this.features;

                if (features && (features.length > 0)) {
                    var geometry = null;
                    features.forEach(function(feature) {
                        (feature.cluster || [feature]).forEach(function(f) {
                            geometry = f.geometry;
                            if (geometry) {
                                if (maxExtent === null) {
                                    maxExtent = new OpenLayers.Bounds();
                                }
                                maxExtent.extend(geometry.getBounds());
                            }
                        });
                    });
                }
                return maxExtent;
            };

            // Feature Clustering
            cluster.activate();
            this.clusterStrategy = cluster;

            // Feature Selection
            var selectFeature = this.featuresLayerSelection = new ol.Control.SelectFeature(map.featuresLayer, {
                clickout: true
            });
            map.addControl(selectFeature);
            selectFeature.activate();
            map.featuresLayer.events.on({
                featureselected: function(featureEvents) {
                    var vertices = _.map(featureEvents.feature.cluster || [featureEvents.feature], function(feature) {
                            return feature.data.vertex;
                        });
                    self.trigger('selectObjects', {vertices: vertices});
                }
            });
            map.events.on({
                mouseup: function(event) {
                    if (event.button === 2 || event.ctrlKey) {
                        self.handleContextMenu(event);
                    }
                },
                click: function(event) {
                    self.closeMenu();
                    self.onMapClicked(event, map);
                }
            });

            if (configProperties['map.provider'] == 'google') {
              base = new ol.Layer.Google('Google Streets', {
                  numZoomLevels: 20,
                  wrapDateLine: false
              });
            } else if (configProperties['map.provider'] == 'osm') {
              var osmURL = configProperties['map.provider.osm.url'];
              if (osmURL) {
                  osmURL = $.map(osmURL.split(','), $.trim);
              }
              base = new ol.Layer.OSM('Open Street Map', osmURL, {
                tileOptions: { crossOriginKeyword: null }
              });
            } else if (configProperties['map.provider'] == 'ArcGIS93Rest') {
                var arcgisURL = configProperties['map.provider.ArcGIS93Rest.url'];
                base = new ol.Layer.ArcGIS93Rest('ArcGIS93Rest', arcgisURL, {
                    layers: '0,1,2',
                    format: 'png24'
                });
            } else {
              console.error('Unknown map provider type: ', configProperties['map.provider']);
            }

            map.addLayers([base, map.featuresLayer]);

            latLon = calcLatLon.bind(null, map.displayProjection, map.getProjectionObject());
            point = calcPoint.bind(null, map.displayProjection, map.getProjectionObject());

            map.zoomTo(2);
            map.render(this.select('mapSelector').get(0))

            // Prevent map shake on initialize while catching up with vertexAdd
            // events

            this.dataRequest('ontology', 'properties')
                .done(function(p) {
                    self.ontologyProperties = p;
                    self.preventShake = true;
                    self.mapMarkReady(map);
                    self.mapReady().done(function() {
                        self.preventShake = false;
                    });
                })
        };

        this.featureStyle = function() {
            return {
                baseStyle: {
                    pointRadius: '${radius}',
                    label: '${label}',
                    labelOutlineColor: '#AD2E2E',
                    labelOutlineWidth: '2',
                    fontWeight: 'bold',
                    fontSize: '16px',
                    fontColor: '#ffffff',
                    fillColor: '#F13B3C',
                    fillOpacity: 0.8,
                    strokeColor: '#AD2E2E',
                    strokeWidth: 3,
                    cursor: 'pointer'
                },
                baseContext: {
                    context: {
                        label: function(feature) {
                            return feature.attributes.count + '';
                        },
                        radius: function(feature) {
                            var count = Math.min(feature.attributes.count || 0, 10);
                            return count + 10;
                        }
                    }
                }
            };
        };

        function calcPoint(sourceProjection, destProjection, x, y) {
            if (arguments.length === 3 && _.isArray(x) && x.length === 2) {
                y = x[1];
                x = x[0];
            }

            return new ol.Geometry.Point(y, x).transform(sourceProjection, destProjection);
        }

        function calcLatLon(sourceProjection, destProjection, lat, lon) {
            if (arguments.length === 3 && _.isArray(lat) && lat.length === 2) {
                lon = lat[1];
                lat = lat[0];
            }

            return new ol.LonLat(lon, lat).transform(sourceProjection, destProjection);
        }
    }

});
