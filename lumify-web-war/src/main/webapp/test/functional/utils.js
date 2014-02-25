var utils = {

    url: 'https://localhost:8443',
    username: 'selenium',
    password: 'password',

    pageLoadTimeout: 10000, // For initial page display
    animationTimeout: 2000, // For animations to finish
    requestTimeout: 10000,   // For things that require a server response

    animations: {
        menubarAnimationFinished:     "$('.menubar-pane').offset().left >= -1",
        openSearchAnimationFinished:  "$('.search-pane').offset().left >= ($('.menubar-pane').width() - 5)",
        closeSearchAnimationFinished: "$('.search-pane').offset().left < (-1 * $('.search-pane').width())"
    },

    lumifyNotReady: "(typeof window.$ !== 'undefined') && $('.login button').length > 0 || ($('.menubar-pane').length > 0 && $('.loading-graph').length > 0)", 
    lumifyReady: "(typeof window.$ !== 'undefined') && $('.login button').length > 0 || ($('.menubar-pane').length > 0 && $('.loading-graph').length === 0)", 

    login: function() {
        return this.browser
          .get(utils.url)
          .waitForElementByCss('#app')
          .waitFor(this.asserters.jsCondition(utils.lumifyReady), utils.pageLoadTimeout)
          .execute(function(user, pass) {
              if ($('.login button').length) {
                  $('.login .username').val(user);
                  $('.login .password').val(pass);
                  $('.login button').click();
              }
          }, [utils.username, utils.password])
          .waitForElementByCss('.menubar-pane')
          .waitFor(this.asserters.jsCondition(utils.animations.menubarAnimationFinished), utils.animationTimeout)
          .waitFor(this.asserters.jsCondition("$('.loading-graph').length === 0"), utils.pageLoadTimeout)
    },

    logout: function() {
        return this.browser
          .get(utils.url)
          .waitForElementByCss('#app')
          .waitFor(this.asserters.jsCondition(utils.lumifyNotReady), utils.pageLoadTimeout)
          .waitFor(this.asserters.jsCondition(utils.lumifyReady), utils.pageLoadTimeout)
          .execute(function() {
              window.$('.menubar-pane .logout a').click()
          }, [])
          .waitForElementByCss('.login button', utils.pageLoadTimeout)
    }

}

module.exports = utils;
