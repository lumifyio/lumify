
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
    'chat/chat',
    'graph/graph',
    'detail/detail',
    'map/map',
    'help/help',
    'util/mouseOverlay',
    'util/withFileDrop',
    'service/user',
    'service/vertex'
], function(
    defineComponent,
    appTemplate,
    data,
    Menubar,
    Dashboard,
    Search,
    Workspaces,
    WorkspaceOverlay,
    Sync,
    Chat,
    Graph,
    Detail,
    Map,
    Help,
    MouseOverlay,
    withFileDrop,
    UserService,
    VertexService) {
    'use strict';

    return defineComponent(App, withFileDrop);

    function App() {
        var Graph3D,
            MAX_RESIZE_TRIGGER_INTERVAL = 250,
            DATA_MENUBAR_NAME = 'menubar-name',
            userService = new UserService(),
            vertexService = new VertexService();

        this.onError = function(evt, err) {
            alert('Error: ' + err.message); // TODO better error handling
        };

        this.defaultAttrs({
            menubarSelector: '.menubar-pane',
            dashboardSelector: '.dashboard-pane',
            searchSelector: '.search-pane',
            workspacesSelector: '.workspaces-pane',
            workspaceOverlaySelector: '.workspace-overlay',
            helpDialogSelector: '.help-dialog',
            chatSelector: '.chat-pane',
            graphSelector: '.graph-pane',
            mapSelector: '.map-pane',
            detailPaneSelector: '.detail-pane'
        });

        this.before('teardown', function() {

            _.invoke([
                WorkspaceOverlay,
                MouseOverlay,
                Sync,
                Menubar,
                Dashboard,
                Search,
                Workspaces,
                Chat,
                Graph,
                Map,
                Detail,
                Help
            ], 'teardownAll');

            if (Graph3D) {
                Graph3D.teardownAll();
            }

            data.teardown();

            this.$node.empty();
        });

        this.after('initialize', function() {
            var self = this;

            window.lumifyApp = this;

            this.triggerPaneResized = _.debounce(this.triggerPaneResized.bind(this), 10);

            this.on('registerForPositionChanges', this.onRegisterForPositionChanges);

            this.on(document, 'error', this.onError);
            this.on(document, 'menubarToggleDisplay', this.toggleDisplay);
            this.on(document, 'chatMessage', this.onChatMessage);
            this.on(document, 'selectUser', this.onChatMessage);
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
            this.on(document, 'logout', this.logout);
            this.on(document, 'showVertexContextMenu', this.onShowVertexContextMenu);

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: ['graph.help.scope', 'map.help.scope'].map(i18n),
                shortcuts: {
                    escape: { fire: 'escape', desc: i18n('lumify.help.escape') },
                }
            });

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: i18n('search.help.scope'),
                shortcuts: {
                    '/': { fire: 'toggleSearchPane', desc: i18n('search.help.toggle') }
                }
            });

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: i18n('lumify.help.scope'),
                shortcuts: {
                    'alt-l': { fire: 'logout', desc: i18n('lumify.help.logout') }
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
                chatPane = content.filter('.chat-pane').data(DATA_MENUBAR_NAME, 'chat'),
                graphPane = content.filter('.graph-pane').data(DATA_MENUBAR_NAME, 'graph'),
                detailPane = content.filter('.detail-pane'),
                mapPane = content.filter('.map-pane').data(DATA_MENUBAR_NAME, 'map'),
                helpDialog = content.filter('.help-dialog');

            // Configure splitpane resizing
            resizable(searchPane, 'e', 190, 300, this.onPaneResize.bind(this));
            resizable(workspacesPane, 'e', 190, 250, this.onPaneResize.bind(this));
            resizable(detailPane, 'w', 4, 500, this.onPaneResize.bind(this));

            WorkspaceOverlay.attachTo(content.filter('.workspace-overlay'));
            MouseOverlay.attachTo(document);
            Sync.attachTo(window);
            Menubar.attachTo(menubarPane.find('.content'));
            Dashboard.attachTo(dashboardPane);
            Search.attachTo(searchPane.find('.content'));
            Workspaces.attachTo(workspacesPane.find('.content'));
            Chat.attachTo(chatPane.find('.content'));
            Graph.attachTo(graphPane.filter('.graph-pane-2d'));
            Map.attachTo(mapPane);
            Detail.attachTo(detailPane.find('.content'));
            Help.attachTo(helpDialog);

            this.$node.html(content);

            $(document.body).toggleClass('animatelogin', !!this.attr.animateFromLogin)

            // Open Page to Dashboard
            this.trigger(document, 'menubarToggleDisplay', { name: graphPane.data(DATA_MENUBAR_NAME) });

            this.setupWindowResizeTrigger();

            this.triggerPaneResized();
            if (self.attr.animateFromLogin) {
                $(document.body).on(TRANSITION_END, function(e) {
                    var oe = e.originalEvent;
                    if (oe.propertyName === 'opacity' && $(oe.target).is(graphPane)) {
                        $(document.body).off(TRANSITION_END);
                        data.loadActiveWorkspace();
                        self.trigger(document, 'applicationReady');
                        graphPane.focus();
                    }
                });
                _.defer(function() {
                    $(document.body).addClass('animateloginstart');
                })
            } else {
                data.loadActiveWorkspace();
                self.trigger(document, 'applicationReady');
            }

            _.delay(function() {
                if (self.attr.addVertexIds) {
                    self.handleAddToWorkspace(self.attr.addVertexIds);
                }
            }, 500);
        });

        this.onRegisterForPositionChanges = function(event, data) {
            var self = this;

            if (data && data.anchorTo && data.anchorTo.page) {
                reposition(data.anchorTo.page);
                this.on(document, 'windowResize', function() {
                    reposition(data.anchorTo.page);
                });
            }

            function reposition(position) {
                if (position === 'center') {
                    position = {
                        x: $(window).width() / 2 + $('.menubar-pane').width() / 2,
                        y: $(window).height() / 2
                    };
                }
                self.trigger(event.target, 'positionChanged', {
                    position: position
                });
            }
        };

        this.handleAddToWorkspace = function(addVertexIds) {
            var self = this;

            require(['util/popovers/addToWorkspace/addToWorkspace'], function(AddToWorkspace) {
                AddToWorkspace.attachTo(self.node, {
                    addVertexIds: addVertexIds,
                    overlay: true,
                    teardownOnTap: false,
                    anchorTo: {
                        page: 'center'
                    }
                });
            });
        };

        this.handleFilesDropped = function(files, event) {
            var self = this;

            require(['util/popovers/fileImport/fileImport'], function(FileImport) {
                FileImport.attachTo(event.target, {
                    files: files,
                    anchorTo: {
                        page: {
                            x: event.pageX,
                            y: event.pageY
                        }
                    }
                });
            });
        };

        this.toggleSearchPane = function() {
            this.trigger(document, 'menubarToggleDisplay', { name: 'search' });
        };

        this.onEscapeKey = function() {
            var self = this;

            // Close any context menus first
            require(['util/vertex/menu'], function(VertexMenu) {
                var contextMenu = $(document.body).lookupComponent(VertexMenu);
                if (contextMenu) {
                    contextMenu.teardown();
                } else {
                    self.collapseAllPanes();
                    self.trigger('selectObjects');
                }
            });
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
            this.on(window, 'resize', function(event) {
                if (event.target !== window) return;
                clearTimeout(resizeTimeout);
                resizeTimeout = setTimeout(function() {
                    self.trigger(document, 'windowResize');
                }, MAX_RESIZE_TRIGGER_INTERVAL);
            });
        };

        this.onShowVertexContextMenu = function(event, data) {
            data.element = event.target;

            require(['util/vertex/menu'], function(VertexMenu) {
                VertexMenu.teardownAll();
                VertexMenu.attachTo(document.body, data);
            })
        };

        this.onMapAction = function(event, data) {
            this.trigger(document, 'changeView', { view: 'map', data: data });
        };

        this.onChangeView = function(event, data) {
            var view = data && data.view,
                pane = view && this.select(view + 'Selector');

            if (pane && pane.hasClass('visible')) {
                return;
            } else if (pane) {
                this.trigger(document, 'menubarToggleDisplay', { name: pane.data(DATA_MENUBAR_NAME), data: data.data });
            } else {
                console.log('View ' + data.view + " isn't supported");
            }
        };

        this.onToggleGraphDimensions = function(e) {
            var self = this,
                node2d = this.$node.find('.graph-pane-2d'),
                node3d = this.$node.find('.graph-pane-3d'),
                reloadWorkspace = !this._graphDimensions;

            require(['graph/3d/graph'], function(graph3d) {
                Graph3D = graph3d;

                if (!self._graphDimensions || self._graphDimensions === 2) {
                    node2d.removeClass('visible').trigger('hidePanel');
                    Graph3D.attachTo(node3d.addClass('visible').trigger('showPanel'));
                    self._graphDimensions = 3;
                } else {
                    node3d.removeClass('visible').trigger('hidePanel');
                    node2d.addClass('visible').trigger('showPanel');
                    self._graphDimensions = 2;
                    self.triggerPaneResized();
                }

                self.trigger('selectObjects');
                if (reloadWorkspace) self.trigger('reloadWorkspace');
            });
        };

        this.logout = function(event, data) {
            var self = this;

            this.trigger('willLogout');

            userService.logout()
                .fail(function() {
                    require(['login'], function(Login) {
                        $(document.body)
                            .removeClass('animatelogin animateloginstart')
                            .append('<div id="login"/>');
                        Login.teardownAll();
                        Login.attachTo('#login', {
                            errorMessage: data && data.message || i18n('lumify.server.not_found')
                        });
                        _.defer(function() {
                            self.teardown();
                        });
                    });
                })
                .done(function() {
                    window.location.reload();
                });
        };

        this.toggleDisplay = function(e, data) {
            var SLIDE_OUT = 'search workspaces',
                pane = this.select(data.name + 'Selector'),
                isVisible = pane.is('.visible');

            if (data.name === 'logout') {
                return this.logout();
            }

            if (data.name === 'map' && !pane.hasClass('visible')) {
                this.trigger(document, 'mapShow', (data && data.data) || {});
            }

            if (SLIDE_OUT.indexOf(data.name) >= 0) {
                var self = this;

                pane.one(TRANSITION_END, function() {
                    pane.off(TRANSITION_END);
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
            if (!this.select('chatSelector').hasClass('visible')) {
                this.trigger(document, 'menubarToggleDisplay', {
                    name: this.select('chatSelector').data(DATA_MENUBAR_NAME)
                });
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
            this.triggerPaneResized();
        };

        this.triggerPaneResized = function() {
            var PANE_BORDER_WIDTH = 1,
                searchWidth = this.select('searchSelector')
                    .filter('.visible:not(.collapsed)')
                    .outerWidth(true) || 0,

                searchResultsWidth = searchWidth > 0 ?
                    $('.active .search-results:visible:not(.collapsed)')
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
                    l: searchWidth + searchResultsWidth + workspacesWidth + workspaceFormWidth,
                    r: detailWidth,
                    t: 0,
                    b: 0
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
            if (!graph.is('.visible')) {
                self.trigger(document, 'menubarToggleDisplay', {
                    name: graph.data(DATA_MENUBAR_NAME),
                    syncToRemote: false
                });
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

                    if (!name) {
                        if (isDetail) {
                            return detailPane.addClass('collapsed').removeClass('visible');
                        }
                        return console.warn('No ' + DATA_MENUBAR_NAME + ' attribute, unable to collapse');
                    }

                    self.trigger(document, 'menubarToggleDisplay', {
                        name: name,
                        syncToRemote: false
                    });
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

    function resizable(el, handles, minWidth, maxWidth, callback) {
        return el.resizable({
            handles: handles,
            minWidth: minWidth || 150,
            maxWidth: maxWidth || 300,
            resize: callback
        });
    }

});
