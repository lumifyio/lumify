var utils = require('../utils');

describe('Login', function () {

    it('Should be able to login using button', function () {
      return this.browser
          .get(utils.url)
          .title()
            .should.become('Lumify')
          .waitForElementByCss('.login button')
          .text().should.become('Log In')
          .elementByCss('.username').type(utils.username)
          .elementByCss('.password').type(utils.password)
          .elementByCss('.login button').click()
          .waitForElementByCss('.menubar-pane')
          .waitFor(this.asserters.jsCondition(utils.animations.menubarAnimationFinished) , 2000)
    })

    it('Should be able to logout with keyboard shortcut', function () {
      return this.browser
          .elementByTagName('body')
          .sendKeys(this.KEYS.Alt + ' L')
          .waitForElementByCss('.login button')
    })

    it('Should be able to login using [ENTER]', function () {
      return this.browser
          .elementByCss('.username').type(utils.username)
          .elementByCss('.password').type(utils.password)
          .sendKeys(this.KEYS.Return)
          .waitForElementByCss('.menubar-pane')
          .waitFor(this.asserters.jsCondition(utils.animations.menubarAnimationFinished) , 2000)
    })

    it('Should be able to logout with menubar', function () {
      return this.browser
          .elementByCss('.menubar-pane .logout a')
          .click()
          .waitForElementByCss('.login button')
    })
});
