
describeComponent('detail/dropdowns/termForm/termForm', function(TermForm) {

    afterEach(function() {
        this.$parentNode.remove();
    });

    beforeEach(function() {
        var self = this;

        this.componentConfiguration = function() {
            self.component.ontologyService._ajaxGet = function(prop) {
                var d = $.Deferred();
                if (prop.url == 'ontology/concept') {
                    d.resolve({children:[{id:1, title:'entity'}, {id:2, title:'artifact'}]});
                } else if (prop.url === 'ontology/property') {
                    d.resolve({properties:[]});
                }
                return d;
            };
            self.parentNode.normalize();
            self.component.trigger('opened');
        };

        this.setupParentForExisting = function(sentenceText) {
            self.$parentNode = $('<span/>')
                .addClass('sentence')
                .html(sentenceText);

            $('<div class="detail-pane"><div class="text"></div></div>')
                .appendTo('body').append(self.$parentNode);

            self.$node = $('<div class="dropdown"/>').appendTo(this.$parentNode);
            self.parentNode = this.$parentNode.get(0);

            self.component = new self.Component();
            self.component.initialize(self.$node, {
                mentionNode: self.$parentNode.find('.entity')
                                 .data('info', {
                                     "title":"Web",
                                     "graphVertexId":"80736",
                                     "start":110,
                                     "_subType":"44",
                                     "_rowKey":"Web\\x1FOpenNlpDictionary\\x1FPerson",
                                     "type":"TermMention",
                                     "end":113
                                 })
            });
            self.componentConfiguration();
        };

        this.setupParentForSelection = function(sentenceText) {
            self.$parentNode = $('<span/>')
                .addClass('sentence')
                .data('info', { start: 0 })
                .html(sentenceText)
                .appendTo('body');

            var mentionNode = self.$parentNode[0].childNodes[self.$parentNode[0].childNodes.length-1];
            if (mentionNode.nodeType != 1 || !$(mentionNode).hasClass('entity')) {
                mentionNode = undefined;
            } else {
                $(mentionNode).data('info', { _subType: 1});
            }

            self.$node = $('<div class="dropdown"/>').appendTo(this.$parentNode);
            self.parentNode = this.$parentNode.get(0);

            var range = window.document.createRange(),
                start = { node:null, offset:-1 },
                end = { node:null, offset:-1 };
            $.each(self.parentNode.childNodes, function(i, node) {
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

            self.component = new self.Component();
            self.component.initialize(self.$node, {
                selection: {
                    range: range
                },
                sign: range.toString(),
                mentionNode: mentionNode
            });
            self.componentConfiguration();
        };

    });

    describe('#existingTerms', function() {

        it("should open form for existing entity", function() {

            this.setupParentForExisting('offered by Amazon <span class="entity subType-1">Web</span> Services');

            this.Component.teardownAll();

            expect(this.$parentNode.find('span.entity').attr('class')).to.contain('subType-1').not.contain('focused');
        });


    });

    describe('#promoteSelectionToSpan', function() {

        it("should create span around multiple word selection", function() {

            this.setupParentForSelection('offered by [Amazon Web Services]');

            expect(this.parentNode.childNodes[0].textContent).to.equal('offered by ');
            expect(this.parentNode.childNodes[1].className).to.equal('entity focused');
            expect(this.parentNode.childNodes[1].innerHTML).to.equal('Amazon Web Services');
            expect(this.parentNode.childNodes[2].className).to.equal('dropdown');
        });

        it("should create span around single word selection", function() {

            this.setupParentForSelection('offered by [Amazon] Web Services');

            expect(this.parentNode.childNodes[0].textContent).to.equal('offered by ');
            expect(this.parentNode.childNodes[1].className).to.equal('entity focused');
            expect(this.parentNode.childNodes[1].innerHTML).to.equal('Amazon');
            expect(this.parentNode.childNodes[2].textContent).to.equal(' Web Services');
            expect(this.parentNode.childNodes[3].className).to.equal('dropdown');
        });

        it("should create span around word selection with spans", function() {

            this.setupParentForSelection('offered by [Amazon <span class="entity subType-2">Web</span> Services]');

            expect(this.parentNode.childNodes[0].textContent).to.equal('offered by ');
            expect(this.parentNode.childNodes[1].nodeName).to.equal('SPAN');
            expect(this.parentNode.childNodes[1].innerHTML).to.equal('Amazon <span class="entity subType-2 focused">Web</span> Services');

            this.Component.teardownAll();

            expect(this.parentNode.innerHTML).to.equal('offered by Amazon <span class="entity subType-2">Web</span> Services');
        });

        it("should accept selections that don't encompass an inner span at both ends of selection", function() {

            this.setupParentForSelection('<span class="entity subType-2">Jo[hnny</span> <span class="entity">App]leseed</span> is a person');
            expect(this.parentNode.childNodes[0].innerHTML)
                .to.equal('<span class="entity subType-2 focused">Johnny</span> <span class="entity focused">Appleseed</span>');

            this.Component.teardownAll();

            expect(this.$parentNode.find('.entity').eq(0).attr('class')).to
                .contain('entity')
                .contain('subType-2')
                .not.contain('focused');

            expect(this.$parentNode.find('.entity').eq(1).attr('class')).to
                .contain('entity')
                .not.contain('focused');
        });

        it("should accept selections with existing entity at the start", function() {

            this.setupParentForSelection('Some text before <span class="entity subType-2">Jo[hnny</span> Appleseed] is a person');

            expect(this.parentNode.childNodes[0].textContent).to.equal('Some text before ');
            expect(this.parentNode.childNodes[1].innerHTML).to.equal('<span class="entity subType-2 focused">Johnny</span> Appleseed');

        });

        it("should accept selections that don't encompass an inner span", function() {

            this.setupParentForSelection('offered by [Amazon <span class="entity subType-2">Web]</span> Services');

            expect(this.parentNode.childNodes[1].innerHTML)
                .to.equal('Amazon <span class="entity subType-2 focused">Web</span>');
        });

        it("should accept selections that don't encompass an inner span", function() {

            this.setupParentForSelection('offered by [Amazon <span class="entity subType-2">W]eb</span>');

            expect(this.parentNode.childNodes[1].className).to.equal('entity focused');
            expect(this.parentNode.childNodes[1].innerHTML)
                .to.equal('Amazon <span class="entity subType-2 focused">Web</span>');

            // Don't preselect value based on inner span
            expect(this.$node.find('select').val()).to.equal("");

            expect(this.$node.find('.object-sign').val()).to.equal('Amazon Web');
        });

    });

    describe('#highlightTerm', function() {

        it("should change highlighted term to loaded concept", function() {

            this.setupParentForSelection('offered by [Amazon <span class="entity subType-2">W]eb</span>');

            expect(this.parentNode.childNodes[1].innerHTML)
                .to.equal('Amazon <span class="entity subType-2 focused">Web</span>');

            // TODO: click create term

        });
    });
});
