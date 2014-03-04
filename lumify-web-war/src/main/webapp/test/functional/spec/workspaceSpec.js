var utils = require('../utils'),
    browser, mainWindowSize, mainSession, altSession;

describe('Workspace', function () {

    before(utils.login)

    it('Should open workspace with menubar', function () {
        browser = this.browser;
        return this.browser
            .clickMenubarIcon('workspaces')
            .waitFor(this.asserters.jsCondition(utils.animations.openWorkspaceAnimationFinished) , utils.animationTimeout)
    })

    it('Should be able to add new workspace', function() {
        var title = 'My new workspace';

        return this.browser
            .elementByCss('.new-workspace input')
            .type(title)
            .sendKeys(this.KEYS.Return)
            .waitForElementByCss('.workspaces-list li:nth-child(3).active', utils.requestTimeout)
            .should.eventually.exist
            .text().should.eventually.include(title)
    })

    it('Should be able to rename workspace', function() {
        var Q = this.Q,
            newTitle = 'Another name';

        return this.browser
            .elementByCss('.workspaces-list li:nth-child(3) .disclosure')
            .click()
            .waitForElementByCss('.workspace-form', this.asserters.isDisplayed)
                .should.eventually.exist
            .elementByCss('input.workspace-title').getValue().should.become('My new workspace')
            .elementByCss('input.workspace-title').clear().type(newTitle)
            .waitForElementByCss(
                '.workspaces-list li:nth-child(3) .nav-list-title',
                utils.assertions.includesText(newTitle), utils.requestTimeout)
            .text().should.eventually.include(newTitle)
    })

    it('Should be able to delete workspace', function() {
        return this.browser
            .elementByCss('.workspace-form button.delete')
            .click()
            .waitForElementByCss('.workspaces-list li:nth-child(3).new-workspace', utils.requestTimeout)
            .should.eventually.exist
    })

    it('Should be able to copy workspace', function() {
        return this.browser
            .elementByCss('.workspaces-list .disclosure')
            .click()
            .waitForElementByCss('.workspace-form', this.asserters.isDisplayed)
            .elementByCss('.workspace-form button.copy')
            .click()
            .waitForElementByCss(
                '.workspaces-list li:nth-child(2) .nav-list-title',
                utils.assertions.includesText('Copy of Default - ' + utils.username), 
                utils.requestTimeout)
            .waitForElementByCss('.workspaces-list li:nth-child(2) .disclosure')
                .should.eventually.exist
            .click()
            .waitForElementByCss('.workspace-form', this.asserters.isDisplayed)
            .elementByCss('.workspace-form button.delete')
            .click()
            .waitForElementByCss('.workspaces-list li:nth-child(3).new-workspace', utils.requestTimeout)
                .should.eventually.exist
    })

    it('Should be able to share workspace', function () {
        return this.browser
            .getSessionId()
            .then(function(sessionId) {
                mainSession = sessionId;
                return browser
                    .getWindowSize()
                    .then(function(s) {
                        mainWindowSize = { width:s.width, height:s.height }
                        return browser;
                    })
                    .setWindowPosition(0, 0)
            })
            .sessionCapabilities()
            .then(function(capabilities) {
                return browser
                    .detach()
                    .init(capabilities)
                    .getSessionId().then(function(sessionId) { altSession = sessionId; return browser; })
                    .setWindowSize(mainWindowSize.width,mainWindowSize.height)
                    .setWindowPosition(500,0)
            })
            .login(utils.usernameAlt, 'password')
            .clickMenubarIcon('workspaces')
            .waitFor(this.asserters.jsCondition(utils.animations.openWorkspaceAnimationFinished) , utils.animationTimeout)
            .elementByCss('.workspaces-list button.disclosure')
            .click()
            .waitForElementByCss('.workspace-form', this.asserters.isDisplayed)
            .elementByCss('.share-form input')
            .type('selenium')
            .waitForElementByCss('.share-form .dropdown-menu', this.asserters.isDisplayed)
            .waitForElementByCss('.share-form .dropdown-menu li[data-value="' + utils.username + '"]')
            .click()
            .waitFor(this.asserters.jsCondition("/View/i.test($('.user-row .permissions').text())"), utils.requestTimeout)

            .waitForElementByCss('.workspaces-list .nav-list-subtitle')
                .text().should.become('Shared with 1 person')
    })

    it('Should show shared workspace in other users share list', function() {
        return this.browser

            // Switch back to main
            .detach().then(function() { return browser.attach(mainSession) })

            .waitForElementByCss('.workspaces-list li:nth-child(6)')
                .should.eventually.exist

    })

    it('Should be able to open shared workspace', function() {
        return this.browser
            .elementByCss('.workspaces-list li:nth-child(6) .nav-list-title')
                .click()
                .text().should.become('Default - ' + utils.usernameAlt)

            .waitFor(this.asserters.jsCondition("$('.workspace-overlay h1.name').text() === 'Default - " + utils.usernameAlt + "'"), utils.requestTimeout)
            .waitForElementByCss('.workspace-overlay .subtitle').text().should.become('read only')

    })

    it('Should be able to change access to edit', function() {
        return this.browser

            // Switch back to alt
            .detach().then(function() { return browser.attach(altSession) })
            .waitForElementByCss('.user-row .badge').click()
            .waitForElementByCss('.workspace-form .popover', this.asserters.isDisplayed)
            .waitForElementByCss('.permissions-list input[data-permissions=WRITE]').click()
            .waitFor(this.asserters.jsCondition("/Edit/i.test($('.user-row .permissions').text())"), utils.requestTimeout)

    })

    it('Should have workspace thats not read only', function() {
        return this.browser
            .detach().then(function() { return browser.attach(mainSession) })
            .waitFor(this.asserters.jsCondition("/no changes/i.test($('.workspace-overlay .subtitle').text())"), utils.requestTimeout)
    })

    it('Should be able to remove access', function() {
        return this.browser

            // Switch back to alt
            .detach().then(function() { return browser.attach(altSession) })
            .waitForElementByCss('.user-row .badge').click()
            .waitForElementByCss('.workspace-form .popover', this.asserters.isDisplayed)
            .waitForElementByCss('.permissions-list .remove-access').click()
            .waitFor(this.asserters.jsCondition("$('.user-row').length === 0"), utils.requestTimeout)

            // Quit alt browser
            .quit()
            .detach().then(function() { return browser.attach(mainSession) })
    })

    it('Should no longer show the shared workspace and switch to default', function() {
        return this.browser
            .waitFor(this.asserters.jsCondition("$('.workspaces-list > li').length === 5"), utils.requestTimeout)
                .should.eventually.be.ok
            .waitForElementByCss('.workspaces-list > li:nth-child(2).active')
                .should.eventually.exist
                .text().should.become('Default - ' + utils.username)

    })

})
