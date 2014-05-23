
describeMixin('search/types/withSearch', function() {

    var mockConcepts;

    beforeEach(function() {
        setupComponent();

        mockConcepts = function(concepts) {
            var self = this,
                c = this.component,
                d = $.Deferred()

            c.on('serviceRequest', function(event, data) {
                if (data.service === 'ontology' && data.method === 'concepts') {
                    c.trigger('serviceRequestCompleted', {
                        requestId: data.requestId,
                        success: true,
                        result: concepts
                    })
                    _.defer(d.resolve)
                }
            })
            return d
        }.bind(this);
    })

    it('should show entities row with loading on search', function(done) {
        var c = this.component;
        c.trigger('searchRequestBegan');

        var entitiesRow = c.$node.find('li')
        entitiesRow.should.not.be.empty

        mockConcepts([])
            .done(function() {
                expect(c.$node.find('.search-concepts li .badge.loading').length).to.equal(0)
                done()
            })
        c.trigger('searchRequestCompleted', { success: true, results: {} });
    })

    it.only('should show concepts for results', function(done) {
        var c = this.component;
        c.trigger('searchRequestBegan');

        mockConcepts({
            byId: {
                thing: { displayName: 'thing', id: 'thingid', pluralDisplayName: 'things' },
                raw: { displayName: 'raw', id: 'rawid', pluralDisplayName: 'raws', parentConcept: 'thing' },
                doc: { displayName: 'doc', id: 'docid', pluralDisplayName: 'docs', parentConcept: 'raw' },
                pdf: { displayName: 'pdf', id: 'pdfid', pluralDisplayName: 'pdfs', parentConcept: 'doc' }
            }
        })
            .done(function() {
                expect(c.$node.find('.search-concepts li').length).to.equal(4)

                var item = c.$node.find('.search-concepts li.nav-header').next('li')

                item.find('.title').text().should.equal('raw')
                item = item.next().find('.title').text().should.equal('doc')
                item = item.next().find('.title').text().should.equal('pdf')

                done()
            })

        c.trigger('searchRequestCompleted', {
            success: true,
            results: {
                verticesCount: {
                    pdf: 1,
                    doc: 1
                }
            }
        });
    })

    it('should clear concepts on clearSearch events', function() {
        var c = this.component

        c.trigger('searchRequestBegan');
        c.$node.find('li .badge.loading').toArray().should.not.be.empty

        c.trigger('clearSearch')
        c.select('conceptsSelector').html().should.be.empty
    })

    it('should clear filters on clearSearch events')
})
