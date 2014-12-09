require([
    'configuration/admin/plugin',
    'hbs!io/lumify/web/devTools/templates/user',
    'hbs!io/lumify/web/devTools/templates/user-details',
    'util/formatters',
    'util/withDataRequest'
], function(
    defineLumifyAdminPlugin,
    template,
    userTemplate,
    F,
    withDataRequest,
    d3
    ) {
    'use strict';

    return defineLumifyAdminPlugin(UserAdmin, {
        mixins: [withDataRequest],
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
                user: lumifyData.currentUser
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

            this.loadUserDetails(lumifyData.currentUser.userName);
        });

        this.loadUserDetails = function(userName) {
            this.dataRequest('user', 'get', userName)
                .done(this.update.bind(this))
        };

        this.onDelete = function(e) {
            var self = this,
                button = $(e.target);

            this.handleSubmitButton(
                button,
                this.dataRequest('admin', 'userDelete', this.user.userName)
                    .then(function() {
                        self.showSuccess('Deleted User');
                        self.$node.find('.details').empty();
                    })
                    .catch(function() {
                        self.showError();
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
                this.dataRequest('admin', 'workspaceShare', workspaceId, this.user.userName)
                    .then(function() {
                        self.loadUserDetails(self.user.userName);
                    })
                    .catch(function() {
                        self.showError();
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
                    COMMENT: ['READ'],
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

            this.dataRequest('admin', 'userUpdatePrivileges', this.user.userName, this.user.privileges)
                .then(function(user) {
                    self.loadUserDetails(user.userName);
                })
                .finally(function() {
                    loading.hide();
                })
        };

        this.onAuthorizationKeyUp = function(event) {
            var self = this;

            if (event.which === 13) {
                var auth = $.trim(this.select('authorizationSelector').val());
                if (auth.length) {
                    this.dataRequest('admin', 'userAuthAdd', this.user.userName, auth)
                        .done(function(user) {
                            self.loadUserDetails(user.userName);
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
                this.dataRequest('admin', 'userAuthRemove', this.user.userName, auth)
                    .then(function(user) {
                        self.loadUserDetails(user.userName);
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
                            return ['READ', 'COMMENT', 'EDIT', 'PUBLISH', 'ADMIN'].indexOf(p);
                        })

                        var w = _.findWhere(user.workspaces, { workspaceId: user.currentWorkspaceId });
                        if (w) {
                            w.isCurrent = true;
                        }
                        user.authorizations = _.sortBy(user.authorizations, function(s) {
                            return s.toLowerCase();
                        });
                        user.priv = 'READ COMMENT EDIT PUBLISH ADMIN'.split(' ').map(function(p) {
                            return {
                                name: p,
                                lower: p.toLowerCase(),
                                disabled: user.displayName === lumifyData.currentUser.displayName,
                                has: user.privileges.indexOf(p) >= 0
                            };
                        });
                    })
                    .value()
                ));
        };

        this.setupTypeahead = function() {
            var self = this,
                groupedByDisplayName;

            this.select('userSearchSelector').typeahead({

                source: function(query, callback) {
                    self.dataRequest('user', 'search', query)
                        .done(function(users) {
                            groupedByDisplayName = _.indexBy(users, 'displayName');
                            callback(_.keys(groupedByDisplayName));
                        });
                },
                updater: function(displayName) {
                    self.loadUserDetails(groupedByDisplayName[displayName].userName);
                    return displayName;
                }
            });
        }

    }
});
