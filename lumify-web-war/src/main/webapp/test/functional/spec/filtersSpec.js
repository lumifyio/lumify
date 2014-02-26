var EXPECTED_PROPERTY_COUNT = 13,
    EXPECTED_FIRST_PROPERTY_NAME = 'abbreviation',
    EXPECTED_LAST_PROPERTY_NAME = 'Title',
    utils = require('../utils')

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
              .clear()
    })

    it('Should open filters when query focused', function () {
        return this.browser
              .waitForElementByCss('.search-filters', this.asserters.isDisplayed)
                .should.eventually.exist
              .waitForElementByCss('.entity-filters', this.asserters.isNotDisplayed)
                .should.eventually.exist
              .waitForElementByCss('.prop-filters', this.asserters.isDisplayed)
                .should.eventually.exist
    })

    it('Should show all available properties on Add filter click', function() {
        return this.browser
              .openAddFilterMenu().should.eventually.exist
              .waitFor(
                  this.asserters.jsCondition("$('.prop-filters .dropdown-menu li').length === " + EXPECTED_PROPERTY_COUNT),
                  utils.animationTimeout
              ).should.eventually.be.ok
              .elementByCss('.prop-filters .dropdown-menu li')
              .text().should.become(EXPECTED_FIRST_PROPERTY_NAME)
              .elementByCss('.prop-filters .dropdown-menu li:nth-child(' + EXPECTED_PROPERTY_COUNT + ')')
              .text().should.become(EXPECTED_LAST_PROPERTY_NAME)
    })

    it('Should be able to add filter', function() {
        var Q = this.Q;

        return this.browser
              .openAddFilterMenu().should.eventually.exist
              .elementByCss('.prop-filters .dropdown-menu li').click()
              .waitForElementByCss('.configuration .predicate-row')
              .text().should.become('Contains')
              .waitForElementByCss('.configuration .input-row input')
              .getValue().should.become('')
              .waitForElementByCss('.newrow input')
              .then(function(el) {
                  return Q.all([
                      el.getValue().should.become(''),
                      el.getAttribute('placeholder').should.become('Add Filter')
                  ])
              })
    })

    it('Should be able to set property value', function() {
        return this.browser
               .elementByCss('.configuration .input-row input')
               .type('US')
               .waitForElementByCss('.filter-info .message:not(:empty)')
                   .text().should.become('1 filter applied')
               .searchForText('*')
               .waitForElementByCss('.search-results-summary .entities')
                   .text().should.become('NO ENTITIES')

               .elementByCss('.configuration .input-row input')
               .type('A')
               .waitForSearchFinished()
               .active()
               .getValue().should.become('USA')
    })

    it('Should be able to remove property value', function() {
        return this.browser
               .elementByCss('.prop-filters button.remove')
               .click()
               .waitFor(this.asserters.jsCondition("$('.prop-filters li').length === 2"), utils.animationTimeout)
               .waitForSearchFinished().should.eventually.be.ok
    })


    it('Should be able to set date property')

    it('Should be able to set geolocation property')

    it('Should be able to set number property')

    it('Should be able to clear filters from search')

})
