
describeComponent('configuration/plugins/visibility/visibilityEditor', function(VisibilityEditor) {

    var VALUE = 'A value',
        NEW_VALUE = 'a NEW value';

    beforeEach(function() {
        setupComponent(this, { value: VALUE })
    })

    it('Should populate the input field with value attribue', function() {
        expect(this.$node.find('input').val()).to.equal(VALUE);
    })

    it('Should populate the input field with value attribue trimmed', function() {
        setupComponent({ value: '\n      trimmed value    \n\n  ' })
        expect(this.$node.find('input').val()).to.equal('trimmed value');
    })

    it('Should fire change events when field changes', function(done) {
        this.$node.on('visibilitychange', function(event, data) {
            data.should.have.property('value').equal(NEW_VALUE)
            data.should.have.property('valid').equal(true)

            done();
        })
        this.$node.find('input').val(' \n    ' + NEW_VALUE + '   \n \n').change();
    })

    it('Should clear value on event', function() {
        this.$node.trigger('visibilityclear');
        expect(this.$node.find('input').val()).to.equal('');
    })

    it('Should accept falsy values', function() {
        setupComponent({ value: 0 })
        expect(this.$node.find('input').val()).to.equal('0');
    })

    it('Should accept no value', function() {
        setupComponent({})
        expect(this.$node.find('input').val()).to.equal('');
    })

})
