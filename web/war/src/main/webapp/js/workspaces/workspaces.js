
define([
    'flight/lib/component',
    './form/form',
    'tpl!./workspaces',
    'tpl!./list',
    'tpl!./item',
    'util/withDataRequest'
], function(defineComponent,
    WorkspaceForm,
    workspacesTemplate,
    listTemplate,
    itemTemplate,
    withDataRequest) {
    'use strict';

    return defineComponent(Workspaces, withDataRequest);

    function Workspaces() {

        this.defaultAttrs({
            listSelector: 'ul.nav-list',
            workspaceListItemSelector: 'ul.nav-list li',
            addNewInputSelector: 'input.new',
            addNewSelector: 'button.new',
            disclosureSelector: 'button.disclosure',
            formSelector: '.workspace-form'
        });

        this.onWorkspaceItemClick = function(event) {
            var $target = $(event.target);

            if ($target.is('input') || $target.is('button') || $target.is('.new-workspace')) return;
            if ($target.closest('.workspace-form').length) return;

            var li = $(event.target).closest('li'),
                workspaceId = li.data('workspaceId');

            if (workspaceId) {
                this.switchToWorkspace(workspaceId.toString());
            }
        };

        this.switchToWorkspace = function(workspaceId) {
            if (workspaceId !== lumifyData.currentWorkspaceId) {
                this.trigger('switchWorkspace', { workspaceId: workspaceId });
            }
        };

        this.onAddNew = function(event) {
            var self = this,
                $input = $(event.target).parents('li').find('input'),
                title = $.trim($input.val());

            if (!title) return;

            this.dataRequest('workspace', 'create', { title: title })
                .then(function(workspace) {
                    $input.val('')
                    self.trigger('switchWorkspace', { workspaceId: workspace.workspaceId });
                })
                .catch(function() {
                    $input.focus();
                })
        };

        this.onInputKeyUp = function(event) {
            switch (event.which) {
                case $.ui.keyCode.ENTER:
                    this.onAddNew(event);
            }
        };

        this.onDisclosure = function(event) {
            var self = this,
                $target = $(event.target),
                data = $target.closest('li').data();

            event.preventDefault();

            this.switchToWorkspace(data.workspaceId);

            var container = this.select('formSelector'),
                form = container.resizable({
                        handles: 'e',
                        minWidth: 120,
                        maxWidth: 250,
                        resize: function() {
                            self.trigger(document, 'paneResized');
                        }
                    }).show().find('.content'),
                instance = form.lookupComponent(WorkspaceForm);

            if (instance && instance.attr.data.workspaceId === data.workspaceId) {
                container.hide();
                instance.teardown();
                return self.trigger(document, 'paneResized');
            }

            WorkspaceForm.teardownAll();
            WorkspaceForm.attachTo(form, {
                data: data
            });

            self.trigger(container, 'paneResized');
        };

        this.collapseEditForm = function() {
            if (this.select('formSelector').is(':visible')) {
                WorkspaceForm.teardownAll();
                this.select('formSelector').hide();
                this.trigger(document, 'paneResized');
            }
        };

        this.onWorkspaceDeleting = function(event, data) {
            this.deletingWorkspace = $.extend({}, data);
        };

        this.onWorkspaceDeleted = function(event, data) {
            this.collapseEditForm();
            this.findWorkspaceRow(data.workspaceId).remove();
        };

        this.onSwitchWorkspace = function(event, data) {
            var li = this.findWorkspaceRow(data.workspaceId);
            if (li.length) {
                this.switchActive(data.workspaceId);
                li.find('.badge').addClass('loading').show().next().hide();
            }
            this.collapseEditForm();
        };

        this.onWorkspaceLoad = function(event, data) {
            if (this.$node.closest('.visible').length) {
                this.updateListItemWithData(data);
                this.switchActive(data.workspaceId);
            }
        };

        this.findWorkspaceRow = function(workspaceId) {
            return this.select('workspaceListItemSelector').filter(function() {
                return $(this).data('workspaceId') == workspaceId;
            });
        };

        this.onWorkspaceSaving = function(event, data) {
            var li = this.findWorkspaceRow(data.workspaceId);
            li.find('.badge').addClass('loading').show().next().hide();
        };

        this.onWorkspaceSaved = function(event, data) {
            var li = this.findWorkspaceRow(data.workspaceId);
            li.find('.badge').removeClass('loading').hide().next().show();
        };

        this.updateListItemWithData = function(data, timestamp) {
            var self = this,
                currentUser = lumifyData.currentUser,
                li = this.findWorkspaceRow(data.workspaceId);

            if (data.createdBy === currentUser.id ||
                _.contains(_.pluck(data.users, 'userId'), currentUser.id)
            ) {
                li.find('.badge').removeClass('loading').hide().next().show();
                this.workspaceDataForItemRow(data)
                    .done(function(data) {
                        var content = $(itemTemplate({ workspace: data, selected: self.workspaceId }));
                        if (li.length === 0) {
                            self.$node.find('li.nav-header').eq(data.sharedToUser ? 1 : 0).after(content);
                        } else {
                            li.replaceWith(content);
                        }

                        // Sort section because title might be renamed
                        var lis = self.getWorkspaceListItemsInSection(data.sharedToUser),
                            titleGetter = function() {
                                return $(this).data('title');
                            },
                            lowerCase = function(s) {
                                return s.toLowerCase();
                            },
                            titles = _.sortBy(lis.map(titleGetter).get(), lowerCase),
                            insertIndex = _.indexOf(titles, data.title),
                            currentIndex = lis.index(content);

                        if (currentIndex < insertIndex) {
                            content.insertAfter(lis.eq(insertIndex));
                        } else if (currentIndex > insertIndex) {
                            content.insertBefore(lis.eq(insertIndex));
                        }
                    });
            } else {
                li.remove();
            }
        };

        this.getWorkspaceListItemsInSection = function(shared) {
            if (shared) {
                return this.select('listSelector')
                    .children('.nav-header').eq(1)
                    .nextUntil();
            } else {
                return this.select('listSelector')
                    .children('.nav-header').eq(0)
                    .nextUntil('.new-workspace');
            }
        };

        this.onWorkspaceUpdated = function(event, data) {
            var currentUser = lumifyData.currentUser,
                workspace = data.workspace,
                userAccess = _.findWhere(workspace.users, { userId: currentUser.id });
            workspace.editable = (/write/i).test(userAccess && userAccess.access);
            workspace.sharedToUser = workspace.createdBy !== currentUser.id;

            this.updateListItemWithData(workspace);
        };

        this.onWorkspaceNotAvailable = function(event, data) {
            this.loadWorkspaceList();
            this.trigger('displayInformation', { message: i18n('workspaces.not_found') });
        };

        this.switchActive = function(workspaceId) {
            var self = this;
            this.workspaceId = workspaceId;

            var found = false;
            this.select('workspaceListItemSelector')
                .removeClass('active')
                .each(function() {
                    if ($(this).data('workspaceId') == workspaceId) {
                        found = true;
                        $(this).addClass('active');
                        return false;
                    }
                });

            if (!found) {
                this.loadWorkspaceList();
            }
        };

        this.loadWorkspaceList = function(switchToFirst) {
            var self = this;

            // TODO: convert to d3
            return this.dataRequest('workspace', 'all')
                .then(function(workspacesResponse) {
                    var workspaces = workspacesResponse || [],
                        users = _.chain(workspaces)
                            .map(function(workspace) {
                                return _.pluck(workspace.users, 'userId');
                            })
                            .flatten()
                            .uniq()
                            .value(),
                        updateHtml = function() {
                            $.when.apply($, _.chain(workspaces)
                                .reject(function(workspace) {
                                    return _.isUndefined(workspace.createdBy);
                                })
                                .map(self.workspaceDataForItemRow.bind(self))
                                .value()
                            ).done(function() {
                                var rows = _.chain(arguments)
                                        .sortBy(function(w) {
                                            return w.title.toLowerCase()
                                        })
                                        .groupBy(function(w) {
                                            return w.sharedToUser ? 'shared' : 'mine';
                                        })
                                        .value();

                                self.select('listSelector').html(
                                    listTemplate({
                                        results: rows,
                                        selected: self.workspaceId
                                    })
                                );
                            });
                            self.trigger(document, 'paneResized');
                            if (switchToFirst) {
                                self.switchToWorkspace(workspaces[0].workspaceId);
                            }
                        };

                    if (users.length) {
                        return self.dataRequest('user', 'search', { userIds: users })
                            .done(function(result) {
                                self.usersById = $.extend(self.usersById, _.indexBy(users, 'id'));
                                updateHtml();
                            })
                    } else {
                        updateHtml();
                    }
                });
        };

        this.workspaceDataForItemRow = function(w) {
            var deferred = $.Deferred(),
                row = $.extend({}, w),
                usersNotCurrent = row.users.filter(function(u) {
                    return u.userId != lumifyData.currentUser.id;
                }),
                people = usersNotCurrent.length;

            if (row.sharedToUser) {
                this.dataRequest('user', 'search', { userIds: row.createdBy })
                    .done(function(createdBy) {
                        var name = createdBy && createdBy.displayName ||
                                i18n('workspaces.shared_with_me.subtitle.unknown_user');

                        deferred.resolve(i18n('workspaces.shared_with_me.subtitle.prefix', name));
                    });
            } else {
                deferred.resolve(i18n('workspaces.sharing.subtitle.prefix'));
            }

            return deferred.promise().then(function(text) {
                if (people === 1 && row.sharedToUser) {
                    row.sharingSubtitle = text + ' ' + i18n('workspaces.sharing.subtitle.suffix.only_you');
                } else if (people === 1) {
                    row.sharingSubtitle = text + ' ' + i18n('workspaces.sharing.subtitle.suffix.one_other');
                } else if (people) {
                    row.sharingSubtitle = text + ' ' + i18n('workspaces.sharing.subtitle.suffix.others', people);
                } else {
                    row.sharingSubtitle = null;
                }
                return row;
            });
        };

        this.onToggleMenu = function(event, data) {
            var self = this;

            if (data.name === 'workspaces') {
                if (this.$node.closest('.visible').length) {
                    this.loadWorkspaceList()
                        .then(function() {
                            self.switchActive(lumifyData.currentWorkspaceId);
                        })
                } else {
                    this.collapseEditForm();
                }
            }
        };

        this.after('initialize', function() {

            this.on(document, 'workspaceLoaded', this.onWorkspaceLoad);
            this.on(document, 'workspaceSaving', this.onWorkspaceSaving);
            this.on(document, 'workspaceSaved', this.onWorkspaceSaved);
            this.on(document, 'workspaceDeleted', this.onWorkspaceDeleted);
            this.on(document, 'workspaceUpdated', this.onWorkspaceUpdated);
            this.on(document, 'workspaceNotAvailable', this.onWorkspaceNotAvailable);

            this.on(document, 'menubarToggleDisplay', this.onToggleMenu);
            this.on(document, 'switchWorkspace', this.onSwitchWorkspace);

            this.on('workspaceDeleting', this.onWorkspaceDeleting);

            this.on('click', {
                workspaceListItemSelector: this.onWorkspaceItemClick,
                addNewSelector: this.onAddNew,
                disclosureSelector: this.onDisclosure
            });

            this.on('keyup', {
                addNewInputSelector: this.onInputKeyUp
            });

            this.$node.html(workspacesTemplate({}));
        });
    }

});
