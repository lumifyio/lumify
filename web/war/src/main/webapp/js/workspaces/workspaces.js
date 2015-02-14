
define([
    'flight/lib/component',
    './form/form',
    'tpl!./workspaces',
    'tpl!./list',
    'tpl!./item',
    'util/withDataRequest',
    'util/formatters'
], function(defineComponent,
    WorkspaceForm,
    workspacesTemplate,
    listTemplate,
    itemTemplate,
    withDataRequest,
    F) {
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

        this.after('initialize', function() {
            this.usersById = {};

            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'workspaceSaving', this.onWorkspaceSaving);
            this.on(document, 'workspaceSaved', this.onWorkspaceSaved);
            this.on(document, 'workspaceDeleted', this.onWorkspaceDeleted);
            this.on(document, 'workspaceUpdated', this.onWorkspaceUpdated);

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
            var workspace = _.findWhere(self.workspaces, { workspaceId: data.workspaceId })
            WorkspaceForm.attachTo(form, {
                data: workspace
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
            this.update(_.reject(this.workspaces, function(w) {
                return data.workspaceId === w.workspaceId;
            }));
        };

        this.onSwitchWorkspace = function(event, data) {
            var li = this.findWorkspaceRow(data.workspaceId);
            if (li.length) {
                this.switchActive(data.workspaceId);
                li.find('.badge').addClass('loading').show().next().hide();
            }
            this.collapseEditForm();
        };

        this.onWorkspaceLoaded = function(event, data) {
            if (this.$node.closest('.visible').length) {
                var li = this.findWorkspaceRow(data.workspaceId);
                li.find('.badge').removeClass('loading').hide().next().show();
                this.switchActive(data.workspaceId);
            }
        };

        this.findWorkspaceRow = function(workspaceId) {
            return this.$node.find('.wid-' + F.className.to(workspaceId), this.select('listSelector'));
        };

        this.onWorkspaceSaving = function(event, data) {
            var li = this.findWorkspaceRow(data.workspaceId);
            li.find('.badge').addClass('loading').show().next().hide();
        };

        this.onWorkspaceSaved = function(event, data) {
            var li = this.findWorkspaceRow(data.workspaceId);
            li.find('.badge').removeClass('loading').hide().next().show();
        };

        this.onWorkspaceUpdated = function(event, data) {
            var updatedWorkspace = data && data.workspace || data;
            this.update(
                _.chain(this.workspaces)
                .reject(function(w) {
                    return updatedWorkspace.workspaceId === w.workspaceId;
                })
                .tap(function(workspaces) {
                    if (_.findWhere(updatedWorkspace.users, { userId: lumifyData.currentUser.id })) {
                        workspaces.push(updatedWorkspace);
                    }
                })
                .value()
            );
        };

        this.switchActive = function(workspaceId) {
            this.workspaceId = workspaceId;

            var self = this,
                found = false,
                $row = this.findWorkspaceRow(workspaceId);

            this.select('workspaceListItemSelector')
                .not($row.addClass('active'))
                .removeClass('active');

            if ($row.length === 0) {
                this.loadWorkspaceList();
            }
        };

        this.update = function(workspaces) {
            var self = this,
                MINE = 'mine',
                SHARED = 'shared',
                workspacesGrouped = _.chain(workspaces)
                        .sortBy('workspaceId')
                        .sortBy('createdBy')
                        .sortBy(function(w) {
                            return w.title.toLowerCase()
                        })
                        .groupBy(function(w) {
                            return w.sharedToUser ? SHARED : MINE
                        })
                        .value(),
                renderRows = function(isShared) {
                    return function() {
                        var entering = isShared ?
                            this.enter().append('li').attr('class', 'w-shared') :
                            this.enter().insert('li', '.new-workspace').attr('class', 'w-mine');

                        entering
                            .append('a')
                            .call(function() {
                                this.append('span').attr('class', 'badge')
                                this.append('button').attr('class', 'disclosure btn btn-mini')
                                this.append('span').attr('class', 'nav-list-title')
                                this.append('span').attr('class', 'nav-list-subtitle')
                            })
                        this.exit().remove();

                        self.select('listSelector').find('.loading').remove();

                        this.each(function(w) {
                            $(this).removePrefixedClasses('wid-')
                                .addClass('wid-' + F.className.to(w.workspaceId));
                        })
                        this.attr('data-workspace-id', _.property('workspaceId'))
                        this.classed('active', function(w) {
                            return w.workspaceId === lumifyData.currentWorkspaceId;
                        })
                        this.select('a .nav-list-title').text(_.property('title'));
                        this.select('a .nav-list-subtitle').text(function(w) {
                            // TODO:
                            return i18n('workspaces.shared_with_me.subtitle.prefix')
                        });
                    };
                };

            this.workspaces = workspaces;

            return Promise.require('d3')
                .then(function(d3) {
                    return new Promise(function(fullfill, reject) {
                        var $list = self.select('listSelector'),
                            d3List = d3.select($list[0]);

                        if ($list.find('.new-workspace').length === 0) {
                            $list.html(listTemplate({}));
                        }

                        d3List
                            .selectAll('li.w-mine')
                            .data(workspacesGrouped[MINE] || [])
                            .order()
                            .call(renderRows(false));

                        d3List
                            .selectAll('li.w-shared')
                            .data(workspacesGrouped[SHARED] || [])
                            .order()
                            .call(renderRows(true));

                        fullfill();
                    });
                })
        };

        this.loadWorkspaceList = function(switchToFirst) {
            var self = this;

            return this.dataRequest('workspace', 'all')
                .then(function(workspaces) {
                    return self.update(workspaces || [])
                });
        };

        /*
        if (row.sharedToUser) {
            if (sharedByUser) {
                deferred.resolve(
                    i18n('workspaces.shared_with_me.subtitle.prefix', sharedByUser)
                );
            } else {
                this.dataRequest('user', 'getUserNames', [row.createdBy])
                    .then(function(users) {
                        var subtitle;
                        if (users && users[0]) {
                            subtitle = i18n('workspaces.shared_with_me.subtitle.prefix', users[0])
                        } else {
                            subtitle = i18n('workspaces.shared_with_me.subtitle.unknown_user');
                        }
                        deferred.resolve(subtitle);
                    })
                    .catch(function() {
                        deferred.resolve(i18n('workspaces.shared_with_me.subtitle.unknown_user'));
                    })
            }
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
        */

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
    }
});
