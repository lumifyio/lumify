var USER = 'selenium',
    PASS = 'password',
    menubarAnimationFinished = "$('.menubar-pane').offset().left >= -1";

describe('Login', function () {

    it('Should be able to login using button', function () {
      return this.browser
          .get("https://localhost:8443")
          .title()
            .should.become('Lumify')
          .waitForElementByCss('.login button')
          .text().should.become('Log In')
          .elementByCss('.username').type(USER)
          .elementByCss('.password').type(PASS)
          .elementByCss('.login button').click()
          .waitForElementByCss('.menubar-pane')
          .waitFor(this.asserters.jsCondition(menubarAnimationFinished) , 2000)
    })

    it('Should be able to logout with keyboard shortcut', function () {
      return this.browser
          .elementByTagName('body')
          .sendKeys('\uE00A L')
          .waitForElementByCss('.login button')
    })

    it('Should be able to login using [ENTER]', function () {
      return this.browser
          .elementByCss('.username').type(USER)
          .elementByCss('.password').type(PASS)
          .sendKeys('\uE006')
          .waitForElementByCss('.menubar-pane')
          .waitFor(this.asserters.jsCondition(menubarAnimationFinished) , 2000)
    })

    it('Should be able to logout with menubar', function () {
      return this.browser
          .elementByCss('.menubar-pane .logout a')
          .click()
          .waitForElementByCss('.login button')
    })
});
