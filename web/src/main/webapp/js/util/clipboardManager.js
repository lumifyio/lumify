
define([
    'flight/lib/component'
], function(defineComponent) {
    'use strict';

    /**
     * Manages clipboard events
     */
    function ClipboardManager() {

        this.after('initialize', function() {
            var self = this;

            this.textarea = $('<textarea class="clipboardManager" autocomplete="false" spellcheck="false" style="position:absolute;z-index:1000;"/>')
                .css({
                    position: 'absolute',
                    zIndex: 10,
                    cursor: 'pointer',
                    top: '-10000px',
                    left: '-10000px'
                })
                .on({
                    cut: this.onCut.bind(this),
                    copy: this.onCopy.bind(this),
                    paste: this.onPaste.bind(this)
                })
                .appendTo(document.body);

            this.on('click', this._onClick);
            this.on('clipboardClear', this.clear);
            this.on('clipboardSet', this.set);
            this.on('clipboardFocus', this.focus);
        });

        this.focus = function() {
            this.textarea.focus();
        };

        this.set = function(event, data) {
            this.textarea.val(data && data.text || '').focus().select();
        };

        this.clear = function() {
            this.textarea.val('');
        };

        this.onPaste = function(event) {
            var self = this;
            _.defer(function() {
                var textarea = self.textarea,
                    val = textarea.val();

                console.debug('Clipboard: Paste', val);

                self.trigger('clipboardPaste', { data:val });
                textarea.val('').focus();
            });
        };

        this.onCopy = function() {
            console.debug('Clipboard: Copy', this.textarea.val());
            var val = this.textarea.val();

            if (val.length) {
                var vertices = this._verticesInData(val);

                if (vertices && vertices.length === 1)
                    this.trigger('displayInformation', { message: 'Copied vertex' });
                if (vertices && vertices.length > 1)
                    this.trigger('displayInformation', { message: 'Copied ' + vertices.length + ' vertices' });
                else if (!vertices)
                    this.trigger('displayInformation', { message: 'Copied data' });
            }
        };

        this.onCut = function() {
            var val = this.textarea.val();
            console.debug('Clipboard: Cut', val);
            this.trigger('clipboardCut', { data:val });
        };

        this._onClick = function(event) {
            if ($(event.target).is('input,select')) return;
            if (window.getSelection().isCollapsed === false) return;
            this.focus();
        };

        this._verticesInData = function(val) {
            var match = val.match(/#v=([0-9,]+)$/);
            if (match && match.length === 2) {
                return match[1].split(',');
            }
            return null;
        };
    }


    return defineComponent(ClipboardManager);
});
