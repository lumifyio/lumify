

define(['util/undoManager'], function(undoManager) {

    var _ = function() { };

    describe('undoManager', function() {

        beforeEach(function() {
            undoManager.reset();
        });

        it('should exist', function() {
            expect(undoManager).to.be.a('object');
        });

        it('should have a performedAction method', function() {
            expect(undoManager).to.respondTo('performedAction');
        });

        it('should require proper arguments to performedAction', function() {
            expect(undoManager.performedAction.bind(undoManager)).to.throw(Error);

            expect(function() {
                undoManager.performedAction( 'test' );
            }).to.throw(Error);

            expect(function() {
                undoManager.performedAction( 'test', { undo: ' ', redo: _ } );
            }).to.throw(Error);

            expect(function() {
                undoManager.performedAction( 'test', { undo: _, redo: ' ' } );
            }).to.throw(Error);

            expect(function() {
                undoManager.performedAction( 'test', { undo: _, redo: _ } );
            }).to.not.throw(Error);
        });

        it('should bind callbacks if specified in options', function(done) {
            var x = { message:'bound object' };

            undoManager.performedAction( 'test', {
                undo: function() {
                    expect(this).to.equal(x);
                },
                redo: function() {
                    expect(this).to.equal(x);
                    expect(this.message).to.equal(x.message);
                    done();
                },
                bind: x
            });

            undoManager.performUndo();
            undoManager.performRedo();
        });

        it('should be able to undo an action', function(done) {
            undoManager.performedAction( 'test undo', {
                undo: function() {
                    done();
                },
                redo: _
            });

            undoManager.performUndo();
        });

        it('should be able to undo, then redo an action', function() {
            var initial = 42, 
                other = 32,
                x = initial;

            x = other;
            undoManager.performedAction( 'test undo', {
                undo: function() {
                    x = initial;
                },
                redo: function() {
                    x = other;
                }
            });

            expect(x).to.equal(other);

            undoManager.performUndo();
            expect(x).to.equal(initial);

            undoManager.performRedo();
            expect(x).to.equal(other);

            undoManager.performUndo();
            expect(x).to.equal(initial);

            expect(undoManager.undos.length).to.equal(0);
            expect(undoManager.redos.length).to.equal(1);
        });

        it('should know when undo is possible', function() {
            expect(undoManager.canUndo()).to.equal(false);

            undoManager.performedAction( 'test undo', { undo:_, redo:_ });
            expect(undoManager.canUndo()).to.equal(true);

            undoManager.performUndo();
            expect(undoManager.canUndo()).to.equal(false);
        });

        it('should know when redo is possible', function() {
            expect(undoManager.canRedo()).to.equal(false);

            undoManager.performedAction( 'test undo', { undo:_, redo:_ });
            expect(undoManager.canRedo()).to.equal(false);

            undoManager.performUndo();
            expect(undoManager.canRedo()).to.equal(true);

            undoManager.performRedo();
            expect(undoManager.canRedo()).to.equal(false);
        });

        it('should not blow up if can not undo and do anyway', function() {
            expect(undoManager.canUndo()).to.equal(false);
            expect(undoManager.performUndo.bind(undoManager)).to.not.throw(Error);
        });

        it('should not blow up if can not redo and do anyway', function() {
            expect(undoManager.canRedo()).to.equal(false);
            expect(undoManager.performRedo.bind(undoManager)).to.not.throw(Error);
        });

        it('should handle undo key events on mac', function() {
            var Z = 90;

            shouldPerformUndoWithEvent( 
                jQuery.Event( "keydown", { which: Z, metaKey: true } )
            );
        });

        it('should handle undo key events on windows', function() {
            var Z = 90;

            shouldPerformUndoWithEvent( 
                jQuery.Event( "keydown", { which: Z, ctrlKey: true } )
            );
        });

        it('should handle redo key events on mac', function() {
            var Z = 90;

            shouldPerformRedoWithEvent( 
                jQuery.Event( "keydown", { which: Z, metaKey: true, shiftKey: true } )
            );
        });

        it('should handle redo key events on windows', function() {
            var Y = 89;

            shouldPerformRedoWithEvent( 
                jQuery.Event( "keydown", { which: Y, ctrlKey: true } )
            );
        });



        function shouldPerformUndoWithEvent(event) {
            canUndo(false);
            undoManager.performedAction( 'test undo', { undo:_, redo:_ });
            canUndo(true);
            undoManager._handleKey(event);
            canUndo(false);
        }

        function shouldPerformRedoWithEvent(event) {
            canRedo(false);
            undoManager.performedAction( 'test undo', { undo:_, redo:_ });
            canUndo(true);
            undoManager.performUndo();
            canUndo(false);
            canRedo(true);
            undoManager._handleKey(event);
            canUndo(true);
            canRedo(false);
        }

        function canUndo(expected) {
            expect(undoManager.canUndo()).to.equal(expected);
        }
        function canRedo(expected) {
            expect(undoManager.canRedo()).to.equal(expected);
        }
    });
});
