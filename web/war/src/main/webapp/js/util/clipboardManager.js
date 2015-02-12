
define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    return defineComponent(ClipboardManager);

    /**
     * Manages clipboard events
     */
    function ClipboardManager() {

        this.after('initialize', function() {
            var self = this;

            // Fullscreen view isn't loaded yet but this isn't needed
            // if it will be
            if (/^#?v=/.test(window.location.hash)) {
                return;
            }

            this.textarea = $('<textarea class="clipboardManager" autocomplete="false" spellcheck="false" />')
                .css({
                    position: 'absolute',
                    zIndex: 10,
                    cursor: 'pointer',
                    top: '-300px',
                    height: '10px',
                    boxSizing: 'border-box'
                })
                .on({
                    cut: this.onCut.bind(this),
                    copy: this.onCopy.bind(this),
                    paste: this.onPaste.bind(this),
                    keydown: this.onKeyDown.bind(this),
                    keyup: this.onKeyDown.bind(this)
                })
                .appendTo(document.body);

            this.on('click', this._onClick);
            this.on('clipboardClear', this.clear);
            this.on('clipboardSet', this.set);
            this.on('clipboardFocus', this.focus);
        });

        this.onKeyDown = function(event) {
            this.textarea.val(this.lastSetData && this.lastSetData.text || '').focus().select();
        };

        this.focus = function() {
            this.textarea.focus();
        };

        this.set = function(event, data) {
            this.lastSetData = data;
            this.textarea.val(data && data.text || '').focus().select();
        };

        this.clear = function() {
            this.lastSetData = null;
            this.textarea.val('');
        };

        this.onPaste = function(event) {
            var self = this;
            _.defer(function() {
                var textarea = self.textarea,
                    val = textarea.val();

                console.debug('Clipboard: Paste', val);

                self.trigger('clipboardPaste', { data: val });
                self.lastSetData = null;
                textarea.val('').focus();
            });
        };

        this.onCopy = function() {
            console.debug('Clipboard: Copy', this.textarea.val());

            var self = this,
                val = this.textarea.val();

            if (val.length) {
                require(['util/vertex/urlFormatters'], function(F) {
                    var parameters = F.vertexUrl.parametersInUrl(val),
                        vertices = parameters && parameters.vertexIds;

                    if (vertices && vertices.length === 1) {
                        self.trigger('displayInformation', { message: i18n('vertex.clipboard.copy.one') });
                    } else if (vertices && vertices.length > 1) {
                        self.trigger('displayInformation', {
                            message: i18n('vertex.clipboard.copy.some', vertices.length)
                        });
                    } else if (!vertices) {
                        self.trigger('displayInformation', { message: i18n('clipboard.copy') });
                    }
                })
            }
        };

        this.onCut = function() {
            var val = this.textarea.val();
            console.debug('Clipboard: Cut', val);
            this.trigger('clipboardCut', { data: val });
        };

        this._onClick = function(event) {
            var inFocus = $(':focus');

            // Check for previous focus, since we are going to steal it to
            // support browser cut/copy/paste events
            if (inFocus.length) {
                this.trigger(inFocus[0], 'focusLostByClipboard');
            } else {
                this.trigger(event.target, 'focusLostByClipboard');
            }

            if ($(event.target).is('input,select,textarea')) return;
            if (window.getSelection().isCollapsed === false) return;

            this.focus();
        };

    }
});
