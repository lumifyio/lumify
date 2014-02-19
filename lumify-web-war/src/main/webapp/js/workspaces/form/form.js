
define([
    'flight/lib/component',
    'tpl!./form',
    'tpl!./shareRow',
    'tpl!./permissions',
    'service/user',
    'service/workspace'
], function(
    defineComponent,
    template,
    shareRowTemplate,
    permissionsTemplate,
    UserService,
    WorkspaceService) {
    'use strict';

    return defineComponent(Form);

    function Form() {

        this.userService = new UserService();
        this.workspaceService = new WorkspaceService();

        this.defaultAttrs({
            titleSelector: '.workspace-title',
            shareListSelector: '.share-list',
            shareHeader: '.share-header',
            shareFormSelector: '.share-form',
            userSearchSelector: '.share-form input',
            permissionsSelector: '.permissions',
            permissionsRadioSelector: '.popover input',
            deleteSelector: '.delete',
            copySelector: '.copy',
            removeAccessSelector: '.remove-access'
        });

        this.after('teardown', function() {
            $(document).off('click.permPopover');
        });

        this.after('initialize', function() {
            var self = this;

            this.editable = this.attr.data.isEditable;

            this.$node.html(template({
                workspace: this.attr.data,
                editable: this.editable
            }));
            this.userService.getCurrentUsers().done(this.loadUserPermissionsList.bind(this));
            this.on('click', {
                copySelector: this.onCopy
            });

            if (this.editable) {
                this.setupTypeahead();
                $(document).on('click.permPopover', function(event) {
                    var $target = $(event.target);

                    if ($target.closest('.permissions').length === 0) {
                        self.$node.find('.permissions').popover('hide');
                    }
                });
                this.on('shareWorkspaceWithUser', this.onShareWorkspaceWithUser);
                this.on('click', {
                    deleteSelector: this.onDelete,
                    removeAccessSelector: this.onRevokeAccess
                });
                this.on('change', {
                    permissionsRadioSelector: this.onPermissionsChange
                });
                this.select('titleSelector').on('change keyup paste', this.onChangeTitle.bind(this));
            }
        });

        var timeout, deferred = [];
        this.saveWorkspace = function(immediate) {
            var self = this,
                d = $.Deferred();
            deferred.push(d);

            clearTimeout(timeout);
            timeout = setTimeout(function() {
                self.trigger(document, 'workspaceSaving', self.attr.data);
                self.workspaceService.save(self.attr.data._rowKey, self.attr.data)
                    .done(function(workspace) {
                        self.trigger(document, 'workspaceSaved', self.attr.data);
                        _.invoke(deferred, 'resolve', { workspace: self.attr.data });
                        deferred.length = 0;
                    });
            }, immediate ? 10 : 1000);

            return d;
        };

        this.loadUserPermissionsList = function(response) {
            var self = this,
                workspace = this.attr.data,
                html = $();

            this.currentUsers = response.users;

            (workspace.users || (workspace.users = [])).forEach(function(userPermission) {
                var data = self.shareRowDataForPermission(userPermission);
                if (data) {
                    html = html.add(shareRowTemplate(data));
                }
            });
            this.select('shareHeader').after(html).find('.loading').remove();
            if (this.editable) {
                this.select('shareFormSelector').show();
                this.updatePopovers();
            }
        };

        this.shareRowDataForPermission = function(userPermission) {
            var user = _.findWhere(this.currentUsers, { id: userPermission.user });
            if (user) {
                return {
                    user: {
                        userPermissions: userPermission.userPermissions,
                        permissionLabel: userPermission.userPermissions.edit ? 'edit' : 'view',
                        id: user.id,
                        userName: user.userName
                    },
                    editable: this.editable
                };
            } else console.warn('User ' + userPermission.user + ' in permissions is not a current user');
        };

        this.updatePopovers = function() {
            this.makePopover(this.select('permissionsSelector'));
        };

        this.makePopover = function(el) {
            el.popover({
                html: true,
                placement: 'bottom',
                container: this.$node,
                content: function() {
                    var row = $(this).closest('.user-row');
                    return $(permissionsTemplate($(this).data())).data('userRow', row);
                }
            });
        };

        this.onChangeTitle = function(event) {
            var $target = $(event.target),
                val = $target.val();

            if ($.trim(val).length === 0) {
                return;
            }

            if (val !== this.attr.data.title) {
                this.attr.data.title = val;
                this.saveWorkspace();
            }
        };

        this.onPermissionsChange = function(event) {
            var self = this,
                $target = $(event.target),
                newPermissions = $target.data('permissions'),
                list = $target.closest('.permissions-list'),
                userRow = list.data('userRow'),
                badge = userRow.find('.permissions'),
                id = userRow.data('userId'),
                user = _.findWhere(this.attr.data.users, { user: id });

            if (user) {
                user.userPermissions = newPermissions;
                badge.popover('disable').addClass('loading');
                this.saveWorkspace(true)
                    .done(function() {
                        var newUserRow = $(shareRowTemplate(self.shareRowDataForPermission(user)));
                        userRow.replaceWith(newUserRow);
                        self.makePopover(newUserRow.find('.badge'));
                    });
            } else console.warn('Unable to update permissions because user "' + id + '" not found');
        };

        this.onDelete = function(event) {
            var self = this,
                _rowKey = this.attr.data._rowKey,
                $target = $(event.target),
                previousText = $target.text();

            this.trigger(document, 'workspaceDeleting', { _rowKey:_rowKey });

            $target.text('Deleting...').attr('disabled', true);

            this.workspaceService['delete'](_rowKey)
                .fail(function(xhr) {
                    if (xhr.status === 403) {
                        // TODO: alert user with error:
                        // can't delete other users workspaces
                    }
                })
                .always(function() {
                    $target.text(previousText).removeAttr('disabled');
                })
                .done(function() {
                    self.trigger('workspaceDeleted', { _rowKey:_rowKey });
                });
        };

        this.onCopy = function (event) {
            var self = this,
                _rowKey = this.attr.data._rowKey,
                $target = $(event.target),
                previousText = $target.text();

            $target.text('Copying...').attr('disabled', true);

            this.workspaceService.copy(_rowKey)
                .fail(function(xhr) {
                    if (xhr.status === 403) {
                        // TODO: alert user with error:
                        // can't delete other users workspaces
                    }
                })
                .always(function () {
                    $target.text(previousText).removeAttr('disabled');
                })
                .done(function(workspace) {
                    self.trigger(document, 'workspaceCopied', { _rowKey:workspace._rowKey });
                });
        };

        this.onRevokeAccess = function(event) {
            var list = $(event.target).closest('.permissions-list'),
                row = list.data('userRow'),
                id = row.data('userId');
            
            row.find('.permissions').popover('disable').addClass('loading');
            this.attr.data.users = _.reject(this.attr.data.users, function(user) {
                return user.user === id;
            });
            this.saveWorkspace(true)
                .done(function() {
                    row.remove();
                });
        };

        this.onShareWorkspaceWithUser = function(event, data) {

            var self = this,
                form = this.select('shareFormSelector'),
                userPermission = {
                    user: data.user.id,
                    userPermissions: { view: true, edit: false }
                },
                row = $(shareRowTemplate(this.shareRowDataForPermission(userPermission))).insertBefore(form),
                badge = row.find('.permissions');

            this.attr.data.users.push(userPermission);

            badge.addClass('loading');
            this.saveWorkspace(true)
                .always(function() {
                    badge.removeClass('loading');
                    badge.popover('destroy');
                })
                .fail(function() {
                    badge.addClass('badge-important').text('Error');
                    _.delay(function() {
                        row.remove();
                    }, 2000);
                })
                .done(function() {
                    _.defer(function() {
                        self.makePopover(badge);
                    });
                });
        };

        this.setupTypeahead = function() {
            var self = this,
                userMap = {};

            this.select('userSearchSelector').typeahead({
                source: function(query, callback) {
                    // TODO: pass query to backend to scale
                    self.userService.getCurrentUsers()
                        .done(function(response) {
                            var users = response.users,
                                regex = new RegExp(query, 'i'),
                                search = users.filter(function(user) {
                                    userMap[user.userName] = user;

                                    // Can't share with oneself
                                    if (user.id === self.attr.data.createdBy) return false;

                                    return regex.test(user.userName);
                                }),
                                names = _.pluck(search, 'userName');
                                
                            self.currentUsers = users;
                            callback(names);
                        });
                },
                updater: function(userName) {
                    var user = userMap[userName];
                    if (user) {
                        self.trigger('shareWorkspaceWithUser', {
                            workspace: self.attr.data,
                            user: user
                        });
                    }
                    return '';
                }
            });
        };
    }
});
