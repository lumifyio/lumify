
define([
    'flight/lib/component',
    'util/video/scrubber',
    'util/audio/scrubber',
    './image/image',
    '../withTypeContent',
    '../withHighlighting',
    'detail/dropdowns/termForm/termForm',
    'detail/properties',
    'tpl!./artifact',
    'tpl!./transcriptEntry',
    'tpl!util/alert',
    'util/range',
    'util/formatters',
    'service/ontology',
    'service/vertex',
    'data'
], function(
    defineComponent,
    VideoScrubber,
    AudioScrubber,
    Image,
    withTypeContent, withHighlighting,
    TermForm,
    Properties,
    template,
    transcriptEntryTemplate,
    alertTemplate,
    rangeUtils,
    formatters,
    OntologyService,
    VertexService,
    appData) {
    'use strict';

    return defineComponent(Artifact, withTypeContent, withHighlighting);

    function Artifact() {

        this.ontologyService = new OntologyService();
        this.vertexService = new VertexService();

        this.defaultAttrs({
            previewSelector: '.preview',
            audioPreviewSelector: '.audio-preview',
            currentTranscriptSelector: '.currentTranscript',
            imagePreviewSelector: '.image-preview',
            detectedObjectLabelsSelector: '.detected-object-labels',
            detectedObjectSelector: '.detected-object',
            detectedObjectTagSelector: '.detected-object-tag',
            artifactSelector: '.artifact-image',
            propertiesSelector: '.properties',
            titleSelector: '.artifact-title',
            textSelector: '.text'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('click', {
                detectedObjectSelector: this.onDetectedObjectClicked
            });
            this.on('copy cut', {
                textSelector: this.onCopyText
            });
            this.on('scrubberFrameChange', this.onScrubberFrameChange);
            this.on('playerTimeUpdate', this.onPlayerTimeUpdate);
            this.on('DetectedObjectCoordsChange', this.onCoordsChanged);
            this.on('termCreated', this.onTeardownDropdowns);
            this.on('dropdownClosed', this.onTeardownDropdowns);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.after('tearDownDropdowns', this.onTeardownDropdowns);

            this.$node.on('mouseenter.detectedObject mouseleave.detectedObject', 
                          this.attr.detectedObjectTagSelector, 
                          this.onDetectedObjectHover.bind(this));
            this.before('teardown', function() {
                self.$node.off('.detectedObject');
            })

            this.loadArtifact();
        });

        this.before('teardown', function() {
            this.select('propertiesSelector').teardownComponent(Properties);
        });

        this.onCopyText = function(event) {
            var selection = getSelection(),
                target = event.target;

            if (!selection.isCollapsed && selection.rangeCount === 1) {

                var $anchor = $(selection.anchorNode),
                    $focus = $(selection.focusNode),
                    offsets = [];
                
                [
                    {el: $anchor, offset: selection.anchorOffset}, 
                    {el: $focus, offset: selection.focusOffset}
                ].forEach(function(node) {
                    var offset = 
                        (node.el.parent('.entity').data('info') || {}).start || 
                        (node.el.prev('.entity').data('info') || {}).end || 
                        0;

                    offsets.push(offset + node.offset);
                });

                offsets = _.sortBy(offsets, function(a, b) {
                    return a - b 
                });

                var range = selection.getRangeAt(0),
                    output = {},
                    contextRange = rangeUtils.expandRangeByWords(range, 4, output),
                    context = contextRange.toString(),
                    contextHighlight =
                        '...' +
                        output.before + 
                        '<span class="selection">' + selection.toString() + '</span>' +
                        output.after + 
                        '...';

                this.trigger('copydocumenttext', {
                    startOffset: offsets[0],
                    endOffset: offsets[1],
                    snippet: contextHighlight,
                    vertexId: this.attr.data.id,
                    text: selection.toString(),
                    vertexTitle: formatters.vertex.prop(this.attr.data, 'title')
                });
            }
        };

        this.onVerticesUpdated = function(event, data) {
            var matching = _.findWhere(data.vertices, { id: this.attr.data.id });

            if (matching) {
                this.select('titleSelector').html(
                    formatters.vertex.prop(matching, 'title')
                );
            }
        };

        this.loadArtifact = function() {
            var self = this,
                vertex = self.attr.data;

            this.handleCancelling(appData.refresh(vertex))
                .done(this.handleVertexLoaded.bind(this));
        };

        this.handleVertexLoaded = function(vertex) {
            var self = this,
                properties = vertex && vertex.properties;

            if (properties) {
                this.videoTranscript = ('http://lumify.io#videoTranscript' in properties) ?
                    properties['http://lumify.io#videoTranscript'].value : {};
                this.videoDuration = ('http://lumify.io#videoDuration' in properties) ?
                    properties['http://lumify.io#videoDuration'].value : 0;
            }

            vertex.detectedObjects = vertex.detectedObjects.sort(function(a, b) {
                var aX = a.x1, bX = b.x1;
                return aX - bX;
            });

            this.$node.html(template({
                vertex: vertex,
                fullscreenButton: this.fullscreenButton([vertex.id]),
                auditsButton: this.auditsButton(),
                formatters: formatters
            }));

            this.select('detectedObjectLabelsSelector').show();

            Properties.attachTo(this.select('propertiesSelector'), { data: vertex });

            this.handleCancelling(this.vertexService.getArtifactHighlightedTextById(vertex.id))
                .done(function(artifactText, status, xhr) {
                    var displayType = vertex.concept.displayType;
                    if (xhr.status === 204 && displayType != 'image' && displayType != 'video') {
                        self.select('textSelector').html(alertTemplate({ error: 'No Text Available' }));
                    } else {
                        self.select('textSelector').html(!artifactText ?
                             '' :
                             artifactText.replace(/[\n]+/g, '<br><br>\n'));
                    }
                    self.updateEntityAndArtifactDraggables();
                    if (self[displayType + 'Setup']) {
                        self[displayType + 'Setup'](vertex);
                    }
            });
        };

        this.onPlayerTimeUpdate = function(evt, data) {
            var time = data.currentTime * 1000;
            this.updateCurrentTranscript(time);
        };

        this.onScrubberFrameChange = function(evt, data) {
            var frameIndex = data.index,
                numberOfFrames = data.numberOfFrames,
                time = (this.videoDuration / numberOfFrames) * frameIndex;

            this.updateCurrentTranscript(time);
        };

        this.updateCurrentTranscript = function(time) {
            var transcriptEntry = this.findTranscriptEntryForTime(time),
                html = '';

            if (transcriptEntry) {
                html = transcriptEntryTemplate({
                    transcriptEntry: transcriptEntry,
                    formatTimeOffset: this.formatTimeOffset
                });
            }
            this.select('currentTranscriptSelector').html(html);
        };

        this.findTranscriptEntryForTime = function(time) {
            if(!this.videoTranscript || !this.videoTranscript.entries) {
                return null;
            }
            var bestMatch = this.videoTranscript.entries[0];
            for (var i = 0; i < this.videoTranscript.entries.length; i++) {
                if(this.videoTranscript.entries[i].start <= time) {
                    bestMatch = this.videoTranscript.entries[i];
                }
            }
            return bestMatch;
        };

        this.formatTimeOffset = function(time) {
            return sf('{0:h:mm:ss}', new sf.TimeSpan(time));
        };

        this.onDetectedObjectClicked = function(event) {
            event.preventDefault();
            var self = this,
                $target = $(event.target),
                info = $target.closest('.label-info').data('info');
            this.$node.find('.focused').removeClass('focused')
            $target.closest('.label-info').parent().addClass('focused');

            require(['util/actionbar/actionbar'], function(ActionBar) {
                self.ActionBar = ActionBar;
                ActionBar.teardownAll();
                self.off('.actionbar');

                if ($target.hasClass('resolved')) {

                    ActionBar.attachTo($target, {
                        alignTo: 'node',
                        actions: {
                            Open: 'open.actionbar',
                            Unresolve: 'unresolve.actionbar'
                        }
                    });

                    self.on('open.actionbar', function() {

                        self.trigger('selectObjects', {
                            vertices: [
                                {
                                    id: $target.data('info').graphVertexId
                                }
                            ]
                        });
                    });
                    self.on('unresolve.actionbar', function() {
                        _.defer(self.showForm.bind(self), info, this.attr.data, $target);
                    });

                } else {

                    ActionBar.attachTo($target, {
                        alignTo: 'node',
                        actions: {
                            Resolve: 'resolve.actionbar'
                        }
                    });

                    self.on('resolve.actionbar', function() {
                        _.defer(self.showForm.bind(self), info, this.attr.data, $target);
                    })
                }
            });

            this.trigger(this.select('imagePreviewSelector'), 'DetectedObjectEdit', info);
        };

        this.onCoordsChanged = function(event, data) {
            var self = this,
                vertex = appData.vertex(this.attr.data.id),
                detectedObject,
                width = parseFloat(data.x2) - parseFloat(data.x1),
                height = parseFloat(data.y2) - parseFloat(data.y1);

            if (vertex.detectedObjects) {
                detectedObject = $.extend(true, {}, _.find(vertex.detectedObjects, function(obj) {
                    if (obj.entityVertex) {
                        return obj.entityVertex.id === data.id;
                    }

                    return obj['http://lumify.io#rowKey'] === data.id;
                }));
            }

            if (width < 5 || height < 5) {
                this.$node.find('.underneath').teardownComponent(TermForm)
                return;
            }

            detectedObject = detectedObject || {};
            if (data.id === 'NEW') {
                detectedObject.isNew = true;
            }
            detectedObject.x1 = data.x1;
            detectedObject.y1 = data.y1;
            detectedObject.x2 = data.x2;
            detectedObject.y2 = data.y2;
            this.showForm(detectedObject, this.attr.data, this.$node);
            this.trigger(this.select('imagePreviewSelector'), 'DetectedObjectEdit', detectedObject);
            this.select('detectedObjectLabelsSelector').show();
            this.$node.find('.detected-object-labels .detected-object').each(function() {
                if ($(this).data('info')['http://lumify.io#rowKey'] === data.id) {
                    $(this).closest('span').addClass('focused')
                }
            });
        };

        this.onTeardownDropdowns = function() {
            this.$node.find('.detected-object-labels .focused').removeClass('focused')
            this.trigger(this.select('imagePreviewSelector'), 'DetectedObjectDoneEditing');
        };

        this.onDetectedObjectHover = function(event) {
            var $target = $(event.target),
                tag = $target.closest('.detected-object-tag'),
                badge = tag.find('.label-info'),
                info = badge.data('info');

            this.trigger(
                this.select('imagePreviewSelector'),
                event.type === 'mouseenter' ? 'DetectedObjectEnter' : 'DetectedObjectLeave',
                info
            );
        };

        this.audioSetup = function(vertex) {
            AudioScrubber.attachTo(this.select('audioPreviewSelector'), {
                rawUrl: vertex.imageRawSrc
            })
        };

        this.videoSetup = function(vertex) {
            VideoScrubber.attachTo(this.select('previewSelector'), {
                rawUrl: vertex.imageRawSrc,
                posterFrameUrl: vertex.imageSrc,
                videoPreviewImageUrl: vertex.imageFramesSrc,
                allowPlayback: true
            });
        };

        this.imageSetup = function(vertex) {
            var self = this,
                data = {
                    src: vertex.imageRawSrc,
                    id: vertex.id
                };
            Image.attachTo(this.select('imagePreviewSelector'), { data: data });
            this.before('teardown', function() {
                self.select('imagePreviewSelector').teardownComponent(Image);
            });
        };

        this.showForm = function(dataInfo, artifactInfo, $target) {
            this.$node.find('.underneath').teardownComponent(TermForm)
            var root = $('<div class="underneath">')
                .insertAfter($target.closest('.type-content').find('.detected-object-labels'));

            TermForm.attachTo (root, {
                artifactData: artifactInfo,
                dataInfo: dataInfo,
                existing: !!dataInfo.graphVertexId,
                detectedObject: true
            });
        };
     }
});
