
define([
    'flight/lib/component',
    'tpl!app',
    'data',
    'menubar/menubar',
    'dashboard/dashboard',
    'search/search',
    'workspaces/workspaces',
    'workspaces/overlay',
    'sync/sync',
    'users/users',
    'graph/graph',
    'detail/detail',
    'map/map',
    'help/help',
    'util/mouseOverlay'
], function(defineComponent, appTemplate, data, Menubar, Dashboard, Search, Workspaces, WorkspaceOverlay, Sync, Users, Graph, Detail, Map, Help, MouseOverlay) {
    'use strict';

    return defineComponent(App);

    function App() {
        var MAX_RESIZE_TRIGGER_INTERVAL = 250;
        var DATA_MENUBAR_NAME = 'menubar-name';


        this.onError = function(evt, err) {
            alert("Error: " + err.message); // TODO better error handling
        };

        this.defaultAttrs({
            menubarSelector: '.menubar-pane',
            dashboardSelector: '.dashboard-pane',
            searchSelector: '.search-pane',
            workspacesSelector: '.workspaces-pane',
            workspaceOverlaySelector: '.workspace-overlay',
            helpDialogSelector: '.help-dialog',
            usersSelector: '.users-pane',
            graphSelector: '.graph-pane',
            mapSelector: '.map-pane',
            detailPaneSelector: '.detail-pane'
        });


        this.after('initialize', function() {
            window.lumifyApp = this;

            this.triggerPaneResized = _.debounce(this.triggerPaneResized.bind(this), 10);

            this.on(document, 'error', this.onError);
            this.on(document, 'menubarToggleDisplay', this.toggleDisplay);
            this.on(document, 'chatMessage', this.onChatMessage);
            this.on(document, 'startChat', this.onChatMessage);
            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'syncStarted', this.onSyncStarted);
            this.on(document, 'paneResized', this.onInternalPaneResize);
            this.on(document, 'toggleGraphDimensions', this.onToggleGraphDimensions);
            this.on(document, 'resizestart', this.onResizeStart);
            this.on(document, 'resizestop', this.onResizeStop);
            this.on(document, 'windowResize', this.triggerPaneResized);
            this.on(document, 'mapCenter', this.onMapAction);
            this.on(document, 'changeView', this.onChangeView);

            this.on(document, 'toggleSearchPane', this.toggleSearchPane);
            this.on(document, 'escape', this.onEscapeKey);

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: ['Graph', 'Map'],
                shortcuts: {
                    'escape': { fire:'escape', desc:'Close all open panes and deselect objects' },
                }
            });

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: 'Search',
                shortcuts: {
                    '/': { fire:'toggleSearchPane', desc:'Show search pane' }
                }
            });


            // Prevent the fragment identifier from changing after an anchor
            // with href="#" not stopPropagation'ed
            $(document).on('click', 'a', this.trapAnchorClicks.bind(this));

            var content = $(appTemplate({})),
                menubarPane = content.filter('.menubar-pane'),
                dashboardPane = content.filter('.dashboard-pane').data(DATA_MENUBAR_NAME, 'dashboard'),
                searchPane = content.filter('.search-pane').data(DATA_MENUBAR_NAME, 'search'),
                workspacesPane = content.filter('.workspaces-pane').data(DATA_MENUBAR_NAME, 'workspaces'),
                usersPane = content.filter('.users-pane').data(DATA_MENUBAR_NAME, 'users'),
                graphPane = content.filter('.graph-pane').data(DATA_MENUBAR_NAME, 'graph'),
                detailPane = content.filter('.detail-pane'),
                mapPane = content.filter('.map-pane').data(DATA_MENUBAR_NAME, 'map'),
                helpDialog = content.filter('.help-dialog');


            WorkspaceOverlay.attachTo(content.filter('.workspace-overlay'));
            MouseOverlay.attachTo(document);
            Sync.attachTo(window);
            Menubar.attachTo(menubarPane.find('.content'));
            Dashboard.attachTo(dashboardPane);
            Search.attachTo(searchPane.find('.content'));
            Workspaces.attachTo(workspacesPane.find('.content'));
            Users.attachTo(usersPane.find('.content'));
            Graph.attachTo(graphPane.filter('.graph-pane-2d'));
            Map.attachTo(mapPane);
            Detail.attachTo(detailPane.find('.content'));

            Help.attachTo(helpDialog);

            // Configure splitpane resizing
            resizable(searchPane, 'e', 160, 200, this.onPaneResize.bind(this));
            resizable(workspacesPane, 'e', 190, 250, this.onPaneResize.bind(this));
            resizable(detailPane, 'w', 4, 500, this.onPaneResize.bind(this));

            this.$node.html(content);

            // Open Page to Dashboard
            this.trigger(document, 'menubarToggleDisplay', { name: graphPane.data(DATA_MENUBAR_NAME) });

            this.setupWindowResizeTrigger();

            data.loadActiveWorkspace();

            _.defer(this.triggerPaneResized.bind(this));

            this.trigger(document, 'applicationReady');
        });

        this.toggleSearchPane = function() {
            this.trigger(document, 'menubarToggleDisplay', { name:'search' });
        };

        this.onEscapeKey = function() {
            this.collapseAllPanes();
            this.trigger('selectObjects', { vertices:[] });
        };

        this.trapAnchorClicks = function(e) {
            var $target = $(e.target);

            if ($target.is('a') && $target.attr('href') === '#') {
                e.preventDefault();
            }
        };

        var resizeTimeout;
        this.setupWindowResizeTrigger = function() {
            var self = this;
            this.on(window, 'resize', function() {
                clearTimeout(resizeTimeout);
                resizeTimeout = setTimeout(function() {
                    self.trigger(document, 'windowResize');
                }, MAX_RESIZE_TRIGGER_INTERVAL);
            });
        };

        this.onMapAction = function(event, data) {
            this.trigger(document, 'changeView', { view: 'map', data:data });
        };

        this.onChangeView = function(event, data) {
            var view = data && data.view;

            var pane = view && this.select(view + 'Selector');
            if (pane && pane.hasClass('visible')) {
                return;
            } else if (pane) {
                this.trigger(document, 'menubarToggleDisplay', { name: pane.data(DATA_MENUBAR_NAME), data:data.data });
            } else {
                console.log("View " + data.view + " isn't supported");
            }
        };

        this.onToggleGraphDimensions = function(e) {
            var self = this,
                node2d = this.$node.find('.graph-pane-2d'),
                node3d = this.$node.find('.graph-pane-3d'),
                reloadWorkspace = !this._graphDimensions;

            // TODO: redraw graph

            require(['graph/3d/graph'], function(Graph3D) {
                if (!self._graphDimensions || self._graphDimensions === 2) {
                    node2d.removeClass('visible');
                    Graph3D.attachTo(node3d.addClass('visible'));
                    self._graphDimensions = 3;
                } else {
                    node3d.removeClass('visible');
                    node2d.addClass('visible');
                    self._graphDimensions = 2;
                    self.triggerPaneResized();
                }


                self.trigger('selectObjects');
                if (reloadWorkspace) self.trigger('reloadWorkspace');
            });
        };

        this.toggleDisplay = function(e, data) {
            var SLIDE_OUT = 'search workspaces',
                pane = this.select(data.name + 'Selector'),
                isVisible = pane.is('.visible');

            if (data.name === 'map' && !pane.hasClass('visible')) {
                this.trigger(document, 'mapShow', (data && data.data) || {});
            }

            if (SLIDE_OUT.indexOf(data.name) >= 0) {
                var self = this;

                pane.one('transitionend webkitTransitionEnd oTransitionEnd otransitionend', function() {
                    pane.off('transitionend webkitTransitionEnd oTransitionEnd otransitionend');
                    if (!isVisible) {
                        self.trigger(data.name + 'PaneVisible');
                    }
                    self.triggerPaneResized();
                });
            } else this.triggerPaneResized();

            // Can't toggleClass because if only one is visible we want to hide all
            if (isVisible) {
                pane.removeClass('visible');
            } else if (data.name === 'graph') {
                pane.filter('.graph-pane-' + (this._graphDimensions || 2) + 'd').addClass('visible');
            } else pane.addClass('visible');

            this.trigger('didToggleDisplay', {
                name: data.name,
                visible: isVisible
            })
        };

        this.onChatMessage = function(e, data) {
            if (!this.select('usersSelector').hasClass('visible')) {
                this.trigger(document, 'menubarToggleDisplay', { name: this.select('usersSelector').data(DATA_MENUBAR_NAME) });
            }
        };

        this.onObjectsSelected = function(e, data) {
            var detailPane = this.select('detailPaneSelector'),
                minWidth = 100,
                width = 0,
                vertices = data.vertices,
                edges = data.edges;

            if (vertices.length || edges.length) {
                if (detailPane.width() < minWidth) {
                    detailPane[0].style.width = null;
                }
                detailPane.removeClass('collapsed').addClass('visible');
                width = detailPane.width();
            } else {
                detailPane.removeClass('visible').addClass('collapsed');
            }

            this.triggerPaneResized();
        };

        this.onInternalPaneResize = function() {
            this.triggerPaneResized();
        };

        this.onPaneResize = function(e, ui) {
            var COLLAPSE_TOLERANCE = 50,
                width = ui.size.width,
                shouldCollapse = width < COLLAPSE_TOLERANCE;

            $(e.target).toggleClass('collapsed', shouldCollapse);
            $(e.target).toggleClass('visible', !shouldCollapse);

            this.triggerPaneResized();
        };

        this.triggerPaneResized = function() {
            var PANE_BORDER_WIDTH = 1,
                searchWidth = this.select('searchSelector')
                    .filter('.visible:not(.collapsed)')
                    .outerWidth(true) || 0,

                searchResultsWidth = searchWidth > 0 ? 
                    $('.search-results:visible:not(.collapsed)')
                        .outerWidth(true) || 0 : 0,

                searchFiltersWidth = searchWidth > 0 ? 
                    $('.search-filters:visible:not(.collapsed)')
                        .outerWidth(true) || 0 : 0,

                workspacesWidth = this.select('workspacesSelector')
                    .filter('.visible:not(.collapsed)')
                    .outerWidth(true) || 0,

                workspaceFormWidth = workspacesWidth > 0 ? 
                    $('.workspace-form:visible:not(.collapsed)')
                        .outerWidth(true) || 0 : 0,

                detailWidth = this.select('detailPaneSelector')
                    .filter('.visible:not(.collapsed)')
                    .outerWidth(true) || 0,

                padding = {
                    l:searchWidth + searchResultsWidth + searchFiltersWidth +
                      workspacesWidth + workspaceFormWidth, 
                    r:detailWidth,
                    t:0, 
                    b:0
                };

            if (padding.l) {
                padding.l += PANE_BORDER_WIDTH;
            }
            if (padding.r) {
                padding.r += PANE_BORDER_WIDTH;
            }

            this.trigger(document, 'graphPaddingUpdated', { padding: padding });
        };


        this.onSyncStarted = function() {
            this.collapseAllPanes();

            var graph = this.select('graphSelector');
            if ( ! graph.is('.visible') ) {
                self.trigger(document, 'menubarToggleDisplay', { name:graph.data(DATA_MENUBAR_NAME), syncToRemote:false });
            }
        };

        this.collapseAllPanes = function() {
            this.collapse([
                this.select('searchSelector'),
                this.select('workspacesSelector'),
                this.select('detailPaneSelector')
            ]);

            $('.search-results').hide();
        };

        this.collapse = function(panes) {
            var self = this,
                detailPane = this.select('detailPaneSelector');

            panes.forEach(function(pane) {
                if (pane.hasClass('visible')) {
                    var name = pane.data(DATA_MENUBAR_NAME),
                        isDetail = pane.is(detailPane);

                    if ( !name ) {
                        if ( isDetail ) {
                            return detailPane.addClass('collapsed').removeClass('visible');
                        }
                        return console.warn('No ' + DATA_MENUBAR_NAME + ' attribute, unable to collapse');
                    }

                    self.trigger(document, 'menubarToggleDisplay', { name:name, syncToRemote:false });
                }
            });
            this.triggerPaneResized();
        };

        this.onResizeStart = function() {
            var wrapper = $('.draggable-wrapper');

            // Prevent map from swallowing mousemove events by adding
            // this transparent full screen div
            if (wrapper.length === 0) {
                wrapper = $('<div class="draggable-wrapper"/>').appendTo(document.body);
            }
        };

        this.onResizeStop = function() {
            $('.draggable-wrapper').remove();
        };
    }


    function resizable( el, handles, minWidth, maxWidth, callback ) {
        return el.resizable({
            handles: handles,
            minWidth: minWidth || 150,
            maxWidth: maxWidth || 300,
            resize: callback 
        });
    }

});

