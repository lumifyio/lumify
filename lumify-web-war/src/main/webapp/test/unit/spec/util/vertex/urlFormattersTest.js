
define(['util/vertex/urlFormatters'], function(f) {

    describe('vertex formatters', function() {

        it('should have be able to create vertex urls', function() {
            f.vertexUrl.url(['1,', '2'], 'work space')
                .should.equal(location.href + '#v=1%2C,2&w=work%20space')

            f.vertexUrl.url(['1,', '2'], '')
                .should.equal(location.href + '#v=1%2C,2&w=')
        })

        it('should have be able to create fragments from vertices', function() {
            f.vertexUrl.fragmentUrl(['1,', '2'], 'work space')
                .should.equal('#v=1%2C,2&w=work%20space')
        })

        it('should return null from invalid urls', function() {
            expect(f.vertexUrl.parametersInUrl(location.href)).to.be.null
            expect(f.vertexUrl.parametersInUrl(location.href + '#v=')).to.be.null
        })

        it('should have be able to extract vertices from urls', function() {
            var p = f.vertexUrl.parametersInUrl('#v=1%2C,2%20x&w=')

            p.should.have.property('vertexIds').that.deep.equals(['1,', '2 x'])
            p.should.have.property('workspaceId').that.equals('')

            var p = f.vertexUrl.parametersInUrl(location.href + '#v=1%2C,2%20x')

            p.should.have.property('vertexIds').that.deep.equals(['1,', '2 x'])
            p.should.have.property('workspaceId').that.equals('')
        })

        it('should have be able to extract vertices and workspace from urls', function() {
            var p = f.vertexUrl.parametersInUrl(location.href + '#v=1%2C,2%20x&w=my%20big%20workspace')

            p.should.have.property('vertexIds').that.deep.equals(['1,', '2 x'])
            p.should.have.property('workspaceId').that.equals('my big workspace')
        })

    });
});
