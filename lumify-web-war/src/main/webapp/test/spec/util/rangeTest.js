
define([
    'util/range',
    'testutils/selectionUtils'
], function(r, selectionUtils) {


    describe('expandRangeByWords', function() {
        var node;

        beforeEach(function() {
            node = $('<div>').appendTo(document.body);
        });

        afterEach(function() {
            node.remove();
        });

        it('should expand range by n words with only textnodes', function() {

            var range = selectionUtils.createRange(node, 'This is a [test] of the emergency broadcast system');

            expect(range.startContainer).to.equal(node[0].childNodes[0]);
            expect(range.startOffset).to.equal(10);
            expect(range.endContainer).to.equal(node[0].childNodes[0]);
            expect(range.endOffset).to.equal(14);

            var expanded = r.expandRangeByWords(range, 1);

            expect(expanded.startContainer).to.equal(node[0].childNodes[0]);
            expect(expanded.startOffset).to.equal(8);
            expect(expanded.endContainer).to.equal(node[0].childNodes[0]);
            expect(expanded.endOffset).to.equal(17);

        });

        it('should expand range with two words', function() {
            var range = selectionUtils.createRange(node, 'Autism affects [information] processing in the');
            expect(r.expandRangeByWords(range, 2).toString()).to.equal('Autism affects information processing in');
        });

        it('should expand range with more words than it has before', function() {
            var range = selectionUtils.createRange(node, 'Autism [affects information] processing in the');
            expect(r.expandRangeByWords(range, 2).toString()).to.equal('Autism affects information processing in');
        });

        it('should expand range with more words than it has after', function() {
            var range = selectionUtils.createRange(node, 'Autism affects [information processing in] the');
            expect(r.expandRangeByWords(range, 2).toString()).to.equal('Autism affects information processing in the');
        });
    });
});
