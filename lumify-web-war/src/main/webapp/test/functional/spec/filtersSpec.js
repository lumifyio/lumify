var utils = require('../utils');

describe('Filters', function () {

    before(function() {
        return utils.login.call(this)
              .waitForElementByCss('.menubar-pane .search a')
                .should.eventually.exist
              .click()
              .waitFor(this.asserters.jsCondition(utils.animations.openSearchAnimationFinished))
                .should.eventually.be.ok
              .waitForElementByCss('.search-query:focus')
                .should.eventually.exist
    });

    it('Should open filters when query focused', function () {
        return this.browser
              .waitForElementByCss('.search-filters', this.asserters.isDisplayed)
                .should.eventually.exist
    });
});
