

define([
    'flight/lib/component',
    'sinon',
    'detail/withHighlighting'
], function(defineComponent, sinon, withHighlighting) {

    var EXPECTED_DROPDOWN_SPEED = 1500;

    describe('withHighlighting', function() {

        this.timeout(EXPECTED_DROPDOWN_SPEED * 3);

        function clearSelection() {
            if (window.getSelection) {
                if (window.getSelection().empty) {  // Chrome
                    window.getSelection().empty();
                } else if (window.getSelection().removeAllRanges) {  // Firefox
                    window.getSelection().removeAllRanges();
                }
            } else if (document.selection) {  // IE?
                document.selection.empty();
            }
        }

        afterEach(function() {
            this.$parentNode.remove();
            this.component.teardown();
            this.$node.remove();
            clearSelection();
        });


        beforeEach(function() {
            

            clearSelection();

            this.Component = defineComponent(WithHighlightingTest, withHighlighting);
            this.$node = $('<div class="detail-pane"><div class="content"></div></div>').appendTo('body').find('.content');
            this.$parentNode = this.$node.closest('.detail-pane');
            this.component = new this.Component();
            this.component.initialize(this.$node, { 
                data: { 
                    properties: {_rowKey:'tmp-rowkey'},
                    selectionDebounce: 10
                }
            });

            function WithHighlightingTest() {

                this.after('initialize', function() {
                    this.ontologyService.clearCaches();
                    this.ontologyService._ajaxGet = function(prop) {
                        if (prop.url == 'ontology/concept') {
                            var d = $.Deferred();
                            d.resolve({
                                children:[
                                    {
                                        id:100,
                                        title:'entity',
                                        color:'rgb(255,0,0)',
                                        glyphIconHref:'first-icon',
                                        children:[
                                            {id:1, title:'First', color:'rgb(255,0,0)', glyphIconHref:'first-icon'},
                                            {id:2, title:'Second', color:'rgb(0,0,255)', glyphIconHref:'second-icon'}
                                        ]
                                    },
                                    {
                                        id:101,
                                        title:'artifact',
                                        color:'rgb(255,0,0)',
                                        glyphIconHref:'first-icon'
                                    }
                                ]
                            });
                            return d;
                        }
                    };
                    this.vertexService.createTerm = function(p, callback) {
                        callback(undefined, {
                            info: {},
                            cssClasses: ['subType-1']
                        });
                    };
                });

                this.update = function(str) {
                    this.$node.html($('<div class="text"/>').html(str));

                    var range = window.document.createRange(),
                        start = { node:null, offset:-1 },
                        end = { node:null, offset:-1 };
                    $.each(this.$node.find('.text')[0].childNodes, function(i, node) {
                        var textNode = node;
                        while (textNode && textNode.nodeType !== 3) {
                            textNode.normalize();
                            textNode = node.childNodes[0];
                        }
                        if (textNode) {
                            var startIndex = node.textContent.indexOf('[');
                            if (startIndex >= 0) {
                                textNode.textContent = textNode.textContent.replace('[', '');
                                start.node = textNode;
                                start.offset = startIndex;
                            }

                            var endIndex = textNode.textContent.indexOf(']');
                            if (endIndex >= 0) {
                                textNode.textContent = textNode.textContent.replace(']', '');
                                end.node = textNode;
                                end.offset = endIndex;
                            }
                        }
                    });

                    range.setStart(start.node, start.offset);
                    range.setEnd(end.node, end.offset);

                    setTimeout(function() {
                        window.getSelection().addRange(range);
                    }, 100);
                };
            }
        });



        it("should highlight artifacts", function(done) {
            this.component.$node.html('<span class="artifact"></span>');

            setTimeout(function() {
                var e = this.component.$node.find('.artifact');

                expect(e.eq(0).css('border-bottom-color')).to.equal('rgb(51, 51, 51)');

                done();
            }.bind(this), 500);
        });


        it("should highlight entities", function(done) {
            this.component.$node.html('<span class="entity subType-1"></span>');
            this.component.$node.append('<span class="entity subType-2"></span>');
            this.component.$node.append('<span class="entity resolved subType-1"></span>');
            this.component.$node.append('<span class="entity resolved subType-2"></span>');
            setTimeout(function() {
                var e = this.component.$node.find('.entity');

                expect(e.eq(0).css('border-bottom-color')).to.equal('rgb(0, 0, 0)');
                expect(e.eq(1).css('border-bottom-color')).to.equal('rgb(0, 0, 0)');
                expect(e.eq(2).css('border-bottom-color')).to.equal('rgb(0, 0, 0)');
                expect(e.eq(3).css('border-bottom-color')).to.equal('rgb(0, 0, 0)');

                done();
            }.bind(this), 500);
        });




        it('should create dropdown after selection', function(done) {
            
            this.component.update('A persons name is [John] <span class="entity">Appleseed</span>, blah');
            
            setTimeout(function() {
                var n = this.$node.find('.text')[0],
                    children = n.childNodes;

                n.normalize();
                expect(children[0].textContent).to.equal('A persons name is ');
                expect(children[1].nodeName).to.equal('SPAN');
                expect(children[1].innerHTML).to.equal('John');
                expect(children[2].className).to.equal('underneath');


                done();
            }.bind(this), EXPECTED_DROPDOWN_SPEED);
        });

        it('should create dropdown using existing entity as start of selection', function(done) {

            this.component.update('A persons name is <span class="entity">J[ohn</span> Appleseed], blah');

            setTimeout(function() {
                var n = this.$node.find('.text')[0],
                    children = n.childNodes;


                n.normalize();
                expect(children[0].textContent).to.equal('A persons name is ');
                expect(children[1].nodeName).to.equal('SPAN');
                expect(children[1].innerHTML).to.equal('<span class="entity focused">John</span> Appleseed');
                expect(children[2].textContent).to.equal(',');
                expect(children[3].className).to.equal('underneath');

                done();
            }.bind(this), EXPECTED_DROPDOWN_SPEED);
        });

        it('should create dropdown after selection but not in span', function(done) {
            
            this.component.update('A persons name is [John <span class="entity">Applesee]d</span>, blah');
            
            setTimeout(function() {
                var n = this.$node.find('.text')[0],
                    children = n.childNodes;

                n.normalize();
                expect(children[0].textContent).to.equal('A persons name is ');
                expect(children[1].nodeName).to.equal('SPAN');
                expect(children[2].nodeType).to.equal(1, 'Not a dropdown');
                expect(children[2].className).to.equal('underneath', 'Not after span');

                expect(children[1].innerHTML).to.equal('John <span class="entity focused">Appleseed</span>');
               
                done();
            }.bind(this), EXPECTED_DROPDOWN_SPEED);
        });

    });
});
