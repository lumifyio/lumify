var utils = require('../utils');

describe('Search', function () {

    before(utils.login);

    it('Should be able to toggle search with menubar', function () {
        return this.browser
          .clickMenubarIcon('search')
          .waitFor(this.asserters.jsCondition(utils.animations.openSearchAnimationFinished) , utils.animationTimeout)
            .should.eventually.be.ok
          .waitForElementByCss('.search-query:focus')
          .elementByCss('.menubar-pane .search a')
          .click()
          .waitFor(this.asserters.jsCondition(utils.animations.closeSearchAnimationFinished) , utils.animationTimeout)
    })

    it('Should be able to toggle search with shortcut', function () {
        return this.browser
          .waitForElementByCss('.menubar-pane .search a')
            .should.eventually.exist
          .elementByTagName('body')
          .sendKeys('/')
          .waitFor(this.asserters.jsCondition(utils.animations.openSearchAnimationFinished) , utils.animationTimeout).should.eventually.be.ok
          .waitForElementByCss('.search-query:focus')
          .elementByTagName('body')
          .sendKeys(this.KEYS.Escape)
          .sendKeys(this.KEYS.Escape)
          .waitFor(this.asserters.jsCondition(utils.animations.closeSearchAnimationFinished) , utils.animationTimeout)
    })

    it('Should be able to search *', function() {
        var self = this,
            browser = this.browser,
            Q = this.Q,
            expected = '?';

        return browser
          .waitForElementByCss('.menubar-pane .search a').should.eventually.exist
          .click()
          .waitFor(this.asserters.jsCondition(utils.animations.openSearchAnimationFinished) , utils.animationTimeout).should.eventually.exist
          .waitForElementByCss('.search-query:focus').should.eventually.exist
          .type('*')
          .keys(this.KEYS.Return)
          .elementByCss('.search-results').getComputedCss('display').should.become('none')
          .then(function() {
              var concept = 'Raw',
                  el = browser.waitForElementByCss('.search-results-summary a[title=' + concept + '] .badge:not(:empty)');

              return Q.all([
                  el.text().then(function(number) {
                     expected = number;
                     return browser;
                  }),
                  el.click()
              ])
          })
          .elementByCss('.search-results').getComputedCss('display').should.become('block')
          .then(function() {
            return browser.waitFor(
                self.asserters.jsCondition("$('.search-results .vertex-item').length === " + expected),
                utils.requestTimeout
            ).should.eventually.be.ok
          })
    })

    it('Should be able to drag result to graph', function() {
        return this.browser
            .elementByCss('.vertex-item')
            .moveTo()
            .buttonDown()
            .elementByCss('.graph-pane')
            .moveTo(550, 100)
            .sleep(100)
            .moveTo(600, 150)
            .sleep(100)
            .buttonUp()
            .waitFor(this.asserters.jsCondition("$('.cytoscape-container').cytoscape('get').nodes().length === 1"), 1000)
            .should.eventually.be.ok
            .elementByCss('.graph-pane .controls button[data-event=fit]')
            .click()
            .elementByCss('.graph-pane')
            .moveTo(625,275)
            .buttonDown()
            .buttonUp()
            .sleep(100)
            .elementByTagName('body')
            .sendKeys(this.KEYS.Delete)
            .sendKeys(this.KEYS['Back space'])
            .waitFor(this.asserters.jsCondition("$('.cytoscape-container').cytoscape('get').nodes().length === 0"), 1000)
            .should.eventually.be.ok
    })

    it('Should trigger workspace save event', function() {
        return this.browser
            .waitForElementByCss('.menubar-pane .activity.animating', 2000).should.eventually.exist
            .waitForElementByCss('.menubar-pane .activity:not(.animating)', utils.requestTimeout).should.eventually.exist
    })

})


