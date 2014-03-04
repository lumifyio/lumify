var tagChaiAssertionError = function(err) {
    err.retriable = true;
    throw err;
},
baseUsername = 'selenium',
browserNameInternal = 'unknown',
utils = {

    url: 'https://localhost:8443',

    get username() { return baseUsername + '-' + utils.browserName; },
    get usernameAlt() { return baseUsername + '-alt-' + utils.browserName; },
    get browserName() { return browserNameInternal; },
    password: 'password',

    pageLoadTimeout:  10000, // For initial page display
    animationTimeout: 2000,  // For animations to finish
    requestTimeout:   10000, // For things that require a server response

    animations: {
        menubarAnimationFinished:     "$('.menubar-pane').offset().left >= -1",
        openSearchAnimationFinished:  "$('.search-pane').offset().left >= ($('.menubar-pane').width() - 5)",
        openWorkspaceAnimationFinished:  "$('.workspaces-pane').offset().left >= ($('.menubar-pane').width() - 5)",
        closeSearchAnimationFinished: "$('.search-pane').offset().left < (-1 * $('.search-pane').width())"
    },

    lumifyReady: "$('.login button').length > 0 || ($('.menubar-pane').length > 0 && $('.loading-graph').length === 0)", 

    addMethods: {

        waitForApplicationLoad: function() {
            var browser = this.browser;

            return this.browser
                .waitForElementByCss('#app', utils.requestTimeout)
                .catch(function() {
                    console.log('Webserver is unreachable')
                    browser.quit()
                })
                .sessionCapabilities()
                .then(function(c) {
                    browserNameInternal = c.browserName;
                })
                .waitForElementByCss('#app:not(:empty),#login:not(:empty)', utils.pageLoadTimeout)
                    .should.eventually.exist
                .waitFor(this.asserters.jsCondition(utils.lumifyReady), utils.pageLoadTimeout)
        },

        openAddFilterMenu: function(nth) {
            if (!nth) nth = 2;
            var parentSelector = '.prop-filters > li:nth-child(' + nth + ')'; 

            return this.browser
                .elementByCss(parentSelector + ' .add-property input')
                .click()
                .waitForElementByCss(parentSelector + ' .dropdown-menu', this.asserters.isDisplayed)
        },

        waitForSearchFinished: function() {
            return this.browser
                  .waitFor(this.asserters.jsCondition(
                      "$('.search-results-summary .entities').text().toLowerCase()=='no entities'" + 
                      "||" +
                      "$('.search-results-summary li:visible').length > 1"), utils.requestTimeout)
        },

        clickMenubarIcon: function(menubarCls) {
            return this.browser
                    .waitForElementByCss('.menubar-pane .' + menubarCls)
                        .should.eventually.exist
                    // Click doesn't seem to work right in firefox
                    .moveTo()
                    .buttonDown()
                    .sleep(10)
                    .buttonUp()
        },

        searchForText: function(query) {
            return this.browser
                  .waitForElementByCss('.search-query')
                  .type('*')
                  .keys(this.KEYS.Return)
                  .waitForSearchFinished()
        },

        login: function(user, pass) {
            var browser = this.browser;

            return this.browser
              .get(utils.url)
              .waitForApplicationLoad()
              .then(function() {
                  return browser.execute(function(user, pass) {
                      if ($('.login button').length) {
                          $('.login .username').val(user);
                          $('.login .password').val(pass);
                          $('.login button').click();
                      }
                  }, [user || utils.username, pass || utils.password])
              })
              .waitFor(this.asserters.jsCondition("$('#login').length === 0"), utils.pageLoadTimeout)
              .waitForElementByCss('.menubar-pane', utils.animationTimeout)
                .should.eventually.exist
              .waitFor(this.asserters.jsCondition(utils.animations.menubarAnimationFinished), utils.animationTimeout)
              .waitFor(this.asserters.jsCondition("$('.loading-graph').length === 0"), utils.pageLoadTimeout)
        }

    },

    assertions: {
        includesText: function(text) {
            return this
                .text().should.eventually.include(text)
                .catch(tagChaiAssertionError);
        }
    },

    initializeMethods: function(wd) {
        for (var method in utils.addMethods) {
            wd.addPromiseChainMethod(method, utils.addMethods[method].bind(this));
        }

        for (var assertion in utils.assertions) {
            var a = utils.assertions[assertion]
            utils.assertions[assertion] = function() {
                var args = arguments;
                return new wd.Asserter(function(el) {
                    return a.apply(el, args);
                })
            }
        }
    },

    login: function() {
        utils.initializeMethods.call(this, this.wd);

        return this.browser.login();
    },

    logout: function() {
        utils.initializeMethods.call(this, this.wd);

        return this.browser
          .get(utils.url)
          .waitForApplicationLoad()
          .execute(function() {
              $('.menubar-pane .logout a').click()
          }, [])
          .waitForElementByCss('.login button', utils.pageLoadTimeout)
    }



}

module.exports = utils;
