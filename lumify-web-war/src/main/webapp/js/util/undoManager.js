
define([], function() {
    'use strict';

    /**
     * Manages undo,redo stack, and keyboard events to trigger
     *
     * After an action:
     *
     *  undoManager.performedAction( actionName, undoFunction, redoFunction);
     */
    function UndoManager() {
        var self = this;

        this.undos = [];
        this.redos = [];

        $(function() {
            $(document).on({
                keydown: self._handleKey.bind(self)
            });
        });
    }

    UndoManager.prototype.performedAction = function(name, options) {
        if (name && 
             options && 
             typeof options.undo === 'function' &&
             typeof options.redo === 'function') {

            this.undos.push({
                name: name,
                undo: options.undo.bind(options.bind || this),
                redo: options.redo.bind(options.bind || this)
            });
        } else {
            throw new Error('Invalid performedAction arguments');
        }
    };

    UndoManager.prototype.canUndo = function() {
        return !!this.undos.length;
    };

    UndoManager.prototype.canRedo = function() {
        return !!this.redos.length;
    };

    UndoManager.prototype.reset = function() {
        this.undos = [];
        this.redos = [];
    };

    UndoManager.prototype.performUndo = function() {
        _performWithStacks('undo', this.undos, this.redos);
    };

    UndoManager.prototype.performRedo = function() {
        _performWithStacks('redo', this.redos, this.undos);
    };

    UndoManager.prototype._isUndo = function(character, event) {
        return (
            // Windows
            (character === 'Z' && event.ctrlKey) || 
            // Mac
            (character === 'Z' && event.metaKey && !event.shiftKey)
        );
    };

    UndoManager.prototype._isRedo = function(character, event) {
        return ( 
            // Windows
            (character === 'Y' && event.ctrlKey) || 
            // Mac
            (character === 'Z' && event.metaKey && event.shiftKey)
        );
    };

    UndoManager.prototype._handleKey = function(event) {
        var character = String.fromCharCode(event.which).toUpperCase();

        if (this._isUndo(character, event)) {
            this.performUndo();
            event.preventDefault();
        }

        if (this._isRedo(character, event)) {
            this.performRedo();
            event.preventDefault();
        }
    };

    function _performWithStacks(name, stack1, stack2) {
        var action, undo;

        if (stack1.length) {
            action = stack1.pop();
            undo = action.undo;

            undo();

            stack2.push({
                action: action.name,
                undo: action.redo,
                redo: action.undo
            });
        }
    }

    return new UndoManager();
});
