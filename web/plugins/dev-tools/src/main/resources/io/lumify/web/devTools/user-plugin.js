require([
    'configuration/admin/plugin',
    'hbs!io/lumify/web/devTools/templates/user',
    'hbs!io/lumify/web/devTools/templates/user-details',
    'service/user',
    'util/formatters'
], function(
    defineLumifyAdminPlugin,
    template,
    userTemplate,
    UserService,
    F,
    d3
    ) {
    'use strict';

    var userService = new UserService();

    return defineLumifyAdminPlugin(UserAdmin, {
        section: 'User',
        name: 'Admin',
        subtitle: 'Modify users and permissions'
    });

    function UserAdmin() {

        this.defaultAttrs({
            userSearchSelector: 'input.user',
            authorizationSelector: 'input.auth',
            removeAuthSelector: '.auth-item button',
            privilegesSelector: 'input.priv',
            shareSelector: 'button.share',
            deleteUserSelector: 'button.delete-user'
        });

        this.after('initialize', function() {
            var self = this;

            this.updatePrivileges = _.debounce(this.updatePrivileges.bind(this), 1000);

            this.$node.html(template({
                user: window.currentUser
            }));

            this.on('keyup', {
                authorizationSelector: this.onAuthorizationKeyUp
            });
            this.on('change', {
                privilegesSelector: this.onPrivilegeChange
            });
            this.on('click', {
                userSearchSelector: function(e) {
                    e.target.select();
                },
                removeAuthSelector: this.onRemoveAuth,
                shareSelector: this.onShare,
                deleteUserSelector: this.onDelete
            });

            this.setupTypeahead();

            this.loadUserDetails(window.currentUser.displayName);
        });

        this.loadUserDetails = function(userName) {
            userService.getUser(userName).done(this.update.bind(this));
        };

        this.onDelete = function(e) {
            var self = this,
                button = $(e.target);

            this.handleSubmitButton(
                button,
                this.adminService.userDelete(this.user.displayName)
                    .fail(function() {
                        self.showError();
                    })
                    .done(function() {
                        self.showSuccess('Deleted User');
                        self.$node.find('.details').empty();
                    })
            );
        };

        this.onShare = function(e) {
            var self = this,
                button = $(e.target),
                li = button.closest('li'),
                workspaceId = li.data('workspaceId');

            li.addClass('show-hover-items');

            this.handleSubmitButton(
                button,
                this.adminService.workspaceShare(this.user.displayName, workspaceId)
                    .fail(function() {
                        self.showError();
                    })
                    .done(function() {
                        self.loadUserDetails(self.user.displayName);
                    })
            );
        };

        this.onPrivilegeChange = function(event) {
            var self = this,
                privilege = event.target.value,
                user = this.user,
                adding = $(event.target).is(':checked'),
                deps = {
                    READ: [],
                    EDIT: ['READ'],
                    PUBLISH: ['READ', 'EDIT'],
                    ADMIN: ['READ', 'EDIT', 'PUBLISH']
                };

            if (adding) {
                user.privileges.push(privilege);
                deps[privilege].forEach(function(p) {
                    self.$node.find('.priv-' + p).attr('checked', true);
                    user.privileges.push(p);
                });
            } else {
                var index = user.privileges.indexOf(privilege);
                if (~index) {
                    user.privileges.splice(index, 1);
                }
                _.each(deps, function(value, key) {
                    if (~value.reverse().indexOf(privilege)) {
                        var index = user.privileges.indexOf(key);
                        if (~index) {
                            user.privileges.splice(index, 1);
                            self.$node.find('.priv-' + key).removeAttr('checked');
                        }
                    }
                });
            }
            user.privileges = _.uniq(user.privileges);
            this.updatePrivileges();
        };

        this.updatePrivileges = function() {
            var self = this,
                loading = this.$node.find('.priv-header .loading').show();

            this.adminService.userUpdatePrivileges(
                    this.user.displayName,
                    this.user.privileges
                ).always(function() {
                    loading.hide();
                })
                .done(function(user) {
                    self.loadUserDetails(user.displayName);
                });
        };

        this.onAuthorizationKeyUp = function(event) {
            var self = this;

            if (event.which === 13) {
                var auth = $.trim(this.select('authorizationSelector').val());
                if (auth.length) {
                    this.adminService.userAuthAdd(this.user.displayName, auth)
                        .done(function(user) {
                            self.loadUserDetails(user.displayName);
                        });
                }
            }
        };

        this.onRemoveAuth = function(e) {
            var self = this,
                button = $(e.target),
                li = button.closest('li'),
                auth = li.data('auth');

            li.addClass('show-hover-items');

            this.handleSubmitButton(
                button,
                this.adminService.userAuthRemove(this.user.displayName, auth)
                    .done(function(user) {
                        self.loadUserDetails(user.displayName);
                    })
            );
        };

        this.update = function(user) {
            this.user = user;
            this.select('userSearchSelector').val(user.displayName).get(0).select();
            this.$node.find('.details')
                .html(userTemplate(
                    _.chain(user)
                    .clone()
                    .tap(function(user) {
                        user.privileges = _.sortBy(user.privileges, function(p) {
                            return ['READ', 'EDIT', 'PUBLISH', 'ADMIN'].indexOf(p);
                        })

                        var w = _.findWhere(user.workspaces, { workspaceId: user.currentWorkspaceId });
                        if (w) {
                            w.isCurrent = true;
                        }
                        user.authorizations = _.sortBy(user.authorizations, function(s) {
                            return s.toLowerCase();
                        });
                        user.priv = 'READ EDIT PUBLISH ADMIN'.split(' ').map(function(p) {
                            return {
                                name: p,
                                lower: p.toLowerCase(),
                                disabled: user.displayName === window.currentUser.displayName,
                                has: user.privileges.indexOf(p) >= 0
                            };
                        });
                    })
                    .value()
                ));
        };

        this.setupTypeahead = function() {
            var self = this;

            this.select('userSearchSelector').typeahead({

                source: function(query, callback) {
                    userService.search(query)
                        .done(function(response) {
                            var users = response.users,
                                names = _.pluck(users, 'displayName');

                            callback(names);
                        });
                },
                updater: function(displayName) {
                    self.loadUserDetails(displayName);
                    return displayName;
                }
            });
        }

    }
});