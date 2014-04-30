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
            .waitForElementByCss('.new-workspace input')
            .type(title)
            .sendKeys(this.KEYS.Return)
            .waitFor(this.asserters.jsCondition("$('.workspaces-list > li').length === 6"), utils.requestTimeout)
            .waitForElementByCss(
                '.workspaces-list > li:nth-child(3)',
                this.asserters.textInclude(title),
                utils.requestTimeout, 10
            ).should.eventually.exist
    })

    it('Should be able to rename workspace', function() {
        var Q = this.Q,
            newTitle = 'Another name';

        return this.browser
            .waitForElementByCss('.workspaces-list > li:nth-child(3).active .disclosure')
            .click()
            .waitForElementByCss('.workspace-form', this.asserters.isDisplayed, utils.requestTimeout)
                .should.eventually.exist
            .waitForElementByCss('input.workspace-title').getValue().should.become('My new workspace')
            .waitForElementByCss('input.workspace-title').clear().type(newTitle)
            .waitForElementByCss(
                '.workspaces-list > li:nth-child(2) .nav-list-title',
                this.asserters.textInclude(newTitle),
                utils.requestTimeout
            ).should.eventually.be.ok
    })

    it('Should be able to delete workspace', function() {
        return this.browser
            .waitForElementByCss('.workspace-form button.delete')
            .click()
            .waitForElementByCss('.workspaces-list > li:nth-child(3).new-workspace', utils.requestTimeout)
            .should.eventually.exist
            .waitFor(this.asserters.jsCondition("$('.workspaces-list > li').length === 5"), utils.requestTimeout)
                .should.eventually.be.ok
            .waitForElementByCss('.workspaces-list > li:nth-child(2).active')
                .should.eventually.exist
    })

    xit('Should be able to copy workspace', function() {
        return this.browser
            .waitForElementByCss('.workspaces-list > li:nth-child(2) .disclosure')
            .click()
            .waitForElementByCss('.workspace-form', this.asserters.isDisplayed, utils.requestTimeout)
            .waitForElementByCss('.workspace-form button.copy').should.eventually.exist
            .click()
            .getValue().should.become('Copying')
            .waitForElementByCss('.workspace-form', this.asserters.isNotDisplayed, utils.requestTimeout)
            .waitFor(this.asserters.jsCondition("$('.workspaces-list > li').length === 6"), utils.requestTimeout).should.eventually.be.ok
            .waitForElementByCss(
                '.workspaces-list > li:nth-child(2).active .nav-list-title',
                this.asserters.textInclude('Copy of Default - ' + utils.username),
                utils.requestTimeout).should.eventually.exist
            .waitForElementByCss('.workspaces-list > li:nth-child(2).active .disclosure')
            .click()
            .waitForElementByCss('.workspace-form', this.asserters.isDisplayed, utils.requestTimeout)
            .waitForElementByCss('.workspace-form button.delete')
            .click()
            .waitForElementByCss('.workspaces-list > li:nth-child(3).new-workspace', utils.requestTimeout)
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
            })
            .sessionCapabilities()
            .then(function(capabilities) {
                return browser
                    .detach()
                    .init(capabilities)
                    .getSessionId().then(function(sessionId) { altSession = sessionId; return browser; })
                    .setWindowSize(mainWindowSize.width,mainWindowSize.height)
                    .setWindowPosition(mainWindowSize.width,0)
            })
            .login(utils.usernameAlt, 'password')
            .clickMenubarIcon('workspaces')
            .waitFor(this.asserters.jsCondition(utils.animations.openWorkspaceAnimationFinished) , utils.animationTimeout)
            .waitForElementByCss('.workspaces-list button.disclosure')
            .click()
            .waitForElementByCss('.workspace-form', this.asserters.isDisplayed, utils.requestTimeout)
            .waitForElementByCss('.workspace-form .share-form input')
            .type(utils.username)
            .waitForElementByCss('.share-form .dropdown-menu', this.asserters.isDisplayed, utils.requestTimeout)
            .waitForElementByCss('.share-form .dropdown-menu > li:not(:empty)', utils.requestTimeout)
            .click()
            .waitForElementByCss('.user-row .permissions', this.asserters.textInclude('View'), utils.requestTimeout)
                .should.eventually.exist
            .waitForElementByCss('.workspaces-list .nav-list-subtitle')
                .text().should.become('Shared with 1 person')
    })

    it('Should show shared workspace in other users share list', function() {
        return this.browser

            // Switch back to main
            .detach().then(function() { return browser.attach(mainSession) })

            .waitForElementByCss('.workspaces-list > li:nth-child(6)')
                .should.eventually.exist

    })

    it('Should be able to open shared workspace', function() {
        return this.browser
            .waitForElementByCss('.workspaces-list > li:nth-child(6) .nav-list-title')
                .click()
                .text().should.become('Default - ' + utils.usernameAlt)
            .waitForElementByCss('.workspaces-list > li:nth-child(6).active')
                .should.eventually.exist
            .waitForElementByCss(
                '.workspace-overlay h1.name',
                this.asserters.textInclude('Default - ' + utils.usernameAlt),
                utils.requestTimeout
            ).should.eventually.exist
            .waitForElementByCss('.workspace-overlay .subtitle').text().should.become('read only')
    })

    it('Should be able to change access to edit', function() {
        return this.browser

            // Switch back to alt
            .detach().then(function() { return browser.attach(altSession) })
            .waitForElementByCss('.user-row .badge').click()
            .waitForElementByCss('.workspace-form .popover', this.asserters.isDisplayed)
            .waitForElementByCss('.permissions-list input[data-permissions=WRITE]').click()
            .waitForElementByCss(
                '.user-row .permissions',
                this.asserters.textInclude('Edit'), utils.requestTimeout)

    })

    it('Should have workspace thats not read only', function() {
        return this.browser
            .detach().then(function() { return browser.attach(mainSession) })
            .waitForElementByCss(
                '.workspace-overlay .subtitle',
                this.asserters.textInclude('no changes'), utils.requestTimeout)
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
