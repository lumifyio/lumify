
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

        var timeout;
        this.saveWorkspace = function(immediate, changes) {
            var self = this,
                d = $.Deferred(),
                save = function() {
                    //self.trigger(document, 'workspaceSaving', self.attr.data);
                    return self.workspaceService.save(self.attr.data.workspaceId, changes)
                        .fail(function() {
                            d.reject();
                        })
                        .done(function(workspace) {
                            //self.trigger(document, 'workspaceSaved', self.attr.data);
                            d.resolve({ workspace: self.attr.data });
                        });
                }

            if (immediate) {
                save();
            } else {
                clearTimeout(timeout);
                timeout = setTimeout(function() {
                    save();
                }, 1000);
            }

            return d;
        };

        this.loadUserPermissionsList = function(response) {
            var self = this,
                workspace = this.attr.data,
                html = $();

            this.currentUsers = response.users;

            (workspace.users || (workspace.users = [])).forEach(function(userPermission) {
                if (userPermission.userId != window.currentUser.id) {
                    var data = self.shareRowDataForPermission(userPermission);
                    if (data) {
                        html = html.add(shareRowTemplate(data));
                    }
                }
            });
            this.select('shareHeader').after(html).find('.loading').remove();
            if (this.editable) {
                this.select('shareFormSelector').show();
                this.updatePopovers();
            }
        };

        this.shareRowDataForPermission = function(userPermission) {
            var user = _.findWhere(this.currentUsers, { id: userPermission.userId });
            if (user) {
                return {
                    user: {
                        access: userPermission.access,
                        permissionLabel: {read:'View', write:'Edit'}[userPermission.access.toLowerCase()],
                        userId: user.id,
                        userName: user.userName
                    },
                    editable: this.editable
                };
            } else console.warn('User ' + userPermission.userId + ' in permissions is not a current user');
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
                // FIXME this.saveWorkspace();
            }
        };

        this.onPermissionsChange = function(event) {
            var self = this,
                $target = $(event.target),
                newPermissions = $target.data('permissions'),
                list = $target.closest('.permissions-list'),
                userRow = list.data('userRow'),
                badge = userRow.find('.permissions'),
                userId = userRow.data('userId'),
                user = _.findWhere(this.attr.data.users, { userId: userId });

            if (user) {
                user.access = newPermissions;
                badge.popover('disable').addClass('loading');
                this.saveWorkspace(true, {
                    userUpdates: [user],
                    userDeletes: [userId]
                })
                    .done(function() {
                        var newUserRow = $(shareRowTemplate(self.shareRowDataForPermission(user)));
                        userRow.replaceWith(newUserRow);
                        self.makePopover(newUserRow.find('.badge'));
                    });
            } else console.warn('Unable to update permissions because user "' + userId + '" not found');
        };

        this.onDelete = function(event) {
            var self = this,
                workspaceId = this.attr.data.workspaceId,
                $target = $(event.target),
                previousText = $target.text();

            this.trigger(document, 'workspaceDeleting', { workspaceId: workspaceId });

            $target.text('Deleting...').attr('disabled', true);

            this.workspaceService['delete'](workspaceId)
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
                    self.trigger('workspaceDeleted', { workspaceId: workspaceId });
                });
        };

        this.onCopy = function (event) {
            var self = this,
                workspaceId = this.attr.data.workspaceId,
                $target = $(event.target),
                previousText = $target.text();

            $target.text('Copying...').attr('disabled', true);

            this.workspaceService.copy(workspaceId)
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
                    self.trigger(document, 'workspaceCopied', { workspaceId: workspace.workspaceId });
                });
        };

        this.onRevokeAccess = function(event) {
            var self = this,
                list = $(event.target).closest('.permissions-list'),
                row = list.data('userRow'),
                userId = row.data('userId'),
                badge = row.find('.permissions').popover('disable').addClass('loading');

            this.attr.data.users = _.reject(this.attr.data.users, function(user) {
                return user.user === userId;
            });
            this.saveWorkspace(true, {
                userDeletes: [userId]
            })
                .fail(function() {
                    var originalHtml = badge.html();
                    badge.removeClass('loading').addClass('badge-important').text('Error');
                    _.delay(function() {
                        badge.html(originalHtml).removeClass('badge-important').popover('enable');
                    }, 2000)
                })
                .done(function() {
                    row.remove();
                });
        };

        this.onShareWorkspaceWithUser = function(event, data) {

            var self = this,
                form = this.select('shareFormSelector'),
                user = {
                    userId: data.user.id,
                    access: 'READ'
                },
                row = $(shareRowTemplate(this.shareRowDataForPermission(user))).insertBefore(form),
                badge = row.find('.permissions');

            this.attr.data.users.push(user);

            badge.addClass('loading');
            this.saveWorkspace(true, {
                userUpdates: [user]
            })
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
