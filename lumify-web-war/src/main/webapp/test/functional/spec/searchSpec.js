// TODO: refactor out
var USER = 'selenium',
    PASS = 'password',
    menubarAnimationFinished = "$('.menubar-pane').offset().left >= -1",
    openSearchAnimationFinished = "$('.search-pane').offset().left >= ($('.menubar-pane').width() - 5)",
    closeSearchAnimationFinished = "$('.search-pane').offset().left < (-1 * $('.search-pane').width())";

describe.only('Search', function () {

    before(function() {
        var self = this,
            browser = this.browser;

        // TODO: refactor login out
        return this.browser
          .get('https://localhost:8443')
          .sessionCapabilities().then(function(c) { self.capabilities = c; })
          .waitForElementByCss('.login button')
          .text().should.become('Log In')
          .elementByCss('.username').type(USER)
          .elementByCss('.password').type(PASS)
          .elementByCss('.login button').click()
          .waitForElementByCss('.menubar-pane')
          .waitFor(this.asserters.jsCondition(menubarAnimationFinished) , 2000)
    })

    it('Should be able to toggle search with menubar', function () {
        return this.browser
          .waitForElementByCss('.menubar-pane .search a').should.eventually.be.ok
          .click()
          .waitFor(this.asserters.jsCondition(openSearchAnimationFinished) , 2000).should.eventually.be.ok
          .waitForElementByCss('.search-query:focus') 
          .elementByCss('.menubar-pane .search a')
          .click()
          .waitFor(this.asserters.jsCondition(closeSearchAnimationFinished) , 2000)
    })

    it('Should be able to search *', function() {
        var browser = this.browser, Q = this.Q;

        return browser
          .waitForElementByCss('.menubar-pane .search a').should.eventually.be.ok
          .click()
          .waitFor(this.asserters.jsCondition(openSearchAnimationFinished) , 2000).should.eventually.exist
          .waitForElementByCss('.search-query:focus').should.eventually.exist
          .type('*')
          .sendKeys(this.KEYS.Return)
          .elementByCss('.search-results').getComputedCss('display').should.become('none')
          .then(function() {
              var concept = 'Raw',
                  expected = '8',
                  el = browser.waitForElementByCss('.search-results-summary a[title=' + concept + '] .badge:not(:empty)');

              return Q.all([
                  el.text().should.become(expected),
                  el.getAttribute('title').should.become(expected),
                  el.click()
              ])
          })
          .elementByCss('.search-results').getComputedCss('display').should.become('block')
          .waitFor(this.asserters.jsCondition("$('.search-results .vertex-item').length === 8") , 500).should.eventually.be.ok
    })

})


