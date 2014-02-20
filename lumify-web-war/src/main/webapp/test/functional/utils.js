var utils = {

    url: 'https://localhost:8443',
    username: 'selenium',
    password: 'password',

    animations: {
        menubarAnimationFinished: "$('.menubar-pane').offset().left >= -1",
        openSearchAnimationFinished: "$('.search-pane').offset().left >= ($('.menubar-pane').width() - 5)",
        closeSearchAnimationFinished: "$('.search-pane').offset().left < (-1 * $('.search-pane').width())"
    },

    login: function() {
        return this.browser
          .get(utils.url)
          .waitFor(this.asserters.jsCondition("$('.login button').length || $('.menubar-pane').length") , 2000)
          .execute(function(user, pass) {
              if ($('.login button').length) {
                  $('.login .username').val(user);
                  $('.login .password').val(pass);
                  $('.login button').click();
              }
          }, [utils.username, utils.password])
          .waitForElementByCss('.menubar-pane')
          .waitFor(this.asserters.jsCondition(utils.animations.menubarAnimationFinished) , 2000)
    }
}

module.exports = utils;
