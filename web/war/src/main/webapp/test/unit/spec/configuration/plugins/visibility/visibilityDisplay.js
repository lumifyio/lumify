
describeComponent('configuration/plugins/visibility/visibilityDisplay', function(VisibilityDisplay) {

    var VALUE = 'A value';

    beforeEach(function() {
        setupComponent(this, { value: VALUE })
    })

    it('Should populate the node with value attribue', function() {
        expect(this.component.$node.html()).to.contain(VALUE);
    })

    it('Should be public if no value specified', function() {
        setupComponent({ value: undefined })
        expect($.trim(this.component.$node.html())).to.equal('<i>public</i>');
    })

    it('Should still display falsy value', function() {
        setupComponent({ value: '0' })
        expect($.trim(this.component.$node.html())).to.equal('0');

        setupComponent({ value: 0 })
        expect($.trim(this.component.$node.html())).to.equal('0');
    })

})
