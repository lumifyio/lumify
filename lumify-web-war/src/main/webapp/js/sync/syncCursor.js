define([
    'flight/lib/component',
    'tpl!./syncCursor'
], function(defineComponent, template) {
    'use strict';

    var CURSOR_RATE_LIMIT_PER_SECOND = 0.03,
        SECOND = 1000;

    return defineComponent(SyncCursor);

    function SyncCursor() {
        this.currentUser = null;
        this.cursorsByUserId = {};

        this.defaultAttrs({
            bodySelector: 'body'
        });

        this.after('initialize', function() {
            this.on('mousemove', this.onMouseMove);
            this.on('focus', this.onFocus);
            this.on('blur', this.onBlur);

            this.on(document, 'onlineStatusChanged', this.onOnlineStatusChanged);
            this.on(document, 'syncCursorMove', this.onRemoteCursorMove);
            this.on(document, 'syncCursorFocus', this.onRemoteCursorFocus);
            this.on(document, 'syncCursorBlur', this.onRemoteCursorBlur);
            this.on(document, 'windowResize', this.updateWindowSize);

            this.updateWindowSize();
        });

        this.after('teardown', function() {
            var self = this;
            Object.keys(this.cursorsByUserId).forEach(function(userId) {
                var cursor = self.cursorsByUserId[userId];
                cursor.remove();
            });
        });

        this.onOnlineStatusChanged = function(evt, data) {
            this.currentUser = data.user;
        };

        this.updateWindowSize = function() {
            var w = $(window);
            this.windowWidth = w.width();
            this.windowHeight = w.height();
        };

        this.getCursor = function(user) {
            var cursor = this.cursorsByUserId[user.id];
            if (cursor) {
                return cursor;
            }
            cursor = $(template({user: user})).appendTo(document.body);
            this.cursorsByUserId[user.id] = cursor;
            return cursor;
        };

        // Remote bound events trigger these
        this.onRemoteCursorMove = function(e, data) {
            if (this.currentUser.id == data.user.id) {
                return;
            }

            var buffer = 10,
                w = this.windowWidth,
                h = this.windowHeight,
                cursor = this.getCursor(data.user);

            cursor.css({
                display: 'block',
                left: Math.min(w, data.x),
                top: Math.min(h, data.y)
            })
                .toggleClass('offscreen-x', data.x > (w - buffer))
                .toggleClass('offscreen-y', data.y > (h - buffer));
        };

        this.onRemoteCursorFocus = function(e, data) {
            if (this.currentUser.id == data.user.id) {
                return;
            }
            var cursor = this.getCursor(data.user);
            cursor.addClass('focus');
        };

        this.onRemoteCursorBlur = function(e, data) {
            if (this.currentUser.id == data.user.id) {
                return;
            }
            var cursor = this.getCursor(data.user);
            cursor.removeClass('focus');
        };

        // Local handlers
        var timeout;
        this.update = function(e) {
            this.trigger(document, 'syncCursorMove', {
                x: e.pageX,
                y: e.pageY,
                w: this.windowWidth,
                h: this.windowHeight,
                user: this.currentUser
            });
            this.lastSend = Date.now();
        };

        this.onFocus = function() {
            this.trigger(document, 'syncCursorFocus', {
                user: this.currentUser
            });
        };

        this.onBlur = function() {
            this.trigger(document, 'syncCursorBlur', {
                user: this.currentUser
            });
        };

        this.onMouseMove = function(e) {
            var now = Date.now(),
                nextSendDate = this.lastSend ? this.lastSend + (SECOND * CURSOR_RATE_LIMIT_PER_SECOND) : now;

            clearTimeout(timeout);

            if (now < nextSendDate) {
                timeout = setTimeout(this.update.bind(this, e), nextSendDate - now);
                return;
            }

            this.update(e);
        };
    }
});
