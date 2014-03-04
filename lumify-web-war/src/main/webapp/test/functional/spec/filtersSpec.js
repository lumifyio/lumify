var EXPECTED_PROPERTY_COUNT = 13,
    EXPECTED_FIRST_PROPERTY_NAME = 'abbreviation',
    EXPECTED_LAST_PROPERTY_NAME = 'Title',
    PROPERTY_WITH_DATE = 'birth date',
    PROPERTY_WITH_GEOLOCATION = 'Geo Location',
    PROPERTY_WITH_NUMBER = 'net income',
    utils = require('../utils'),
    today = new Date(),
    pad = function(num, padding) {
        var str = '' + num;
        while (str.length < padding) str = '0' + str;
        return str;
    },
    TODAYS_DATE_FORMATTED = [
        today.getFullYear(),
        pad(today.getMonth() + 1, 2),
        pad(today.getDate(), 2)
    ].join('-');

describe('Filters', function () {

    before(function() {
        return utils.login.call(this)
              .clickMenubarIcon('search')
              .waitFor(this.asserters.jsCondition(utils.animations.openSearchAnimationFinished), utils.animationTimeout)
                .should.eventually.be.ok
              .waitForElementByCss('.search-query:focus', utils.animationTimeout)
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


    it('Should be able to set date property', function() {
        var Q = this.Q;

        return this.browser
               .openAddFilterMenu().should.eventually.exist
               .elementByCss('.prop-filters .dropdown-menu li[data-value="' + PROPERTY_WITH_DATE + '"]').click()
               .waitForElementByCss('.configuration .predicate-row .predicate option')
               .then(function(option) {
                   return Q.all([
                       option.isSelected().should.eventually.be.ok,
                       option.getValue().should.become('<'),
                       option.text().should.become('Before')
                   ])
               })
               .waitForElementByCss('.configuration .input-row input')
               .then(function(input) {
                   return Q.all([
                       input.getAttribute('placeholder').should.become('YYYY-MM-DD'),
                       input.click()
                   ]);
               })
               .waitForElementByCss('.datepicker-dropdown', this.asserters.isDisplayed)
                   .should.eventually.exist
               .elementByCss('>' , '.datepicker-days .day.active')
               .click()
               .elementByCss('.configuration .input-row input')
               .getValue().should.become(TODAYS_DATE_FORMATTED)
    })

    it('Should be able to set date property to between', function() {
        return this.browser
               .elementByCss('.predicate option[value=range]')
               .click()
               .waitForElementByCss('.range-only', this.asserters.isDisplayed)
                   .should.eventually.exist
               .click()
               .waitForElementByCss('.datepicker-dropdown', this.asserters.isDisplayed)
                   .should.eventually.exist
               .elementByCss('>' , '.datepicker-days .day.active')
               .click()
               .elementByCss('.configuration .input-row input.range-only')
               .getValue().should.become(TODAYS_DATE_FORMATTED)
    })

    it('Should be able to set geolocation property', function() {
        var parentSelector = '.prop-filters > li:nth-child(3)';

        return this.browser
               .openAddFilterMenu(3).should.eventually.exist
               .elementByCss(parentSelector + ' .dropdown-menu li[data-value="' + PROPERTY_WITH_GEOLOCATION + '"]').click()
               .waitForElementByCss(parentSelector + ' .input-prepend').text().should.become('Latitude')
               .waitForElementByCss(parentSelector + ' .input-prepend:nth-child(2)').text().should.become('Longitude')
               .waitForElementByCss(parentSelector + ' .input-prepend:nth-child(3)').text().should.become('Radius')
               .elementByCss(parentSelector + ' .input-prepend:nth-child(1) .input-row input').type('42.123')
               .elementByCss(parentSelector + ' .input-prepend:nth-child(2) .input-row input').type('-87.231')
               .elementByCss(parentSelector + ' .input-prepend:nth-child(3) .input-row input').type('1000')
               // TODO: make sure search doesn't show error message
    })

    it('Should be able to set number property', function() {
        var Q = this.Q,
            parentSelector = '.prop-filters > li:nth-child(4)';

        return this.browser
               .openAddFilterMenu(4).should.eventually.exist
               .elementByCss(parentSelector + ' .dropdown-menu li[data-value="' + PROPERTY_WITH_NUMBER + '"]').click()
               .waitForElementByCss(parentSelector + ' .predicate-row .predicate option')
               .then(function(option) {
                   return Q.all([
                       option.isSelected().should.eventually.be.ok,
                       option.getValue().should.become('<'),
                       option.text().should.become('less then')
                   ])
               })
               .elementByCss(parentSelector + ' .input-row input').type('100000')
    })

    it('Should be able to clear filters from search', function() {
        return this.browser
               .elementByCss('.filter-info .message')
               .moveTo()
               .sleep(100)
               .elementByCss('.filter-info .action')
               .click()
               .waitFor(this.asserters.jsCondition("$('.prop-filters li').length === 2"), utils.animationTimeout)

    })

})
