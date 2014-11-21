
define([
    'flight/lib/component',
    'tpl!./image',
    'data',
    'util/retina',
    'util/withFileDrop',
    'util/privileges',
    'util/vertex/formatters',
    'util/withDataRequest'
], function(
    defineComponent,
    template,
    appData,
    retina,
    withFileDrop,
    Privileges,
    F,
    withDataRequest) {
    'use strict';

    // Limit previews to 1MB since it's a dataUri
    var MAX_PREVIEW_FILE_SIZE = 1024 * 1024;

    return defineComponent(ImageView, withFileDrop, withDataRequest);

    function ImageView() {

        this.defaultAttrs({
            canvasSelector: 'canvas',
            fileSelector: 'input',
            acceptedTypesRegex: /^image\/(jpe?g|png)$/i
        });

        this.after('initialize', function() {
            var self = this;

            this.on('fileprogress', this.onUpdateProgress);
            this.on('filecomplete', this.onUploadComplete);
            this.on('fileerror', this.onUploadError);
            this.on(document, 'iconUpdated', this.onUpdateIcon);
            this.updateImageBackgroundSize = _.throttle(this.updateImageBackgroundSize.bind(this), 100);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);

            this.updateImageBackground();

            if (Privileges.canEDIT) {
                this.$node.html(template({
                    Privileges: Privileges
                }));

                this.select('fileSelector').on({
                    click: function() {
                        self.$node.addClass('file-hover'); this.value = null;
                    },
                    change: this.onFileChange.bind(this)
                });

                this.on('setImage', this.onSetImage);

                this.$node.addClass('upload-available');
                this.$node.on({
                    mouseenter: function() {
                        $(this).addClass('file-hover');
                    },
                    mouseleave: function() {
                        $(this).removeClass('file-hover');
                    }
                });
            }
        });

        this.onSetImage = function(e, data) {
            this.handleFilesDropped(data.files);
        }

        this.onGraphPaddingUpdated = function(event, data) {
            if (data.padding.r && this.imageNaturalSize) {
                this.updateImageBackgroundSize();
            }
        };

        this.updateImageBackgroundSize = function() {
            if (this.imageNaturalSize &&
                this.imageNaturalSize[0] > 0 &&
                this.imageNaturalSize[1] > 0) {
                var widthViewport = this.$node.width(),
                    heightViewport = this.$node.height(),
                    widthImage = this.imageNaturalSize[0],
                    heightImage = this.imageNaturalSize[1],
                    ratioViewport = widthViewport / heightViewport,
                    ratioImage = widthImage / heightImage,

                    widthCover = ratioImage <= ratioViewport ?
                        widthViewport : heightViewport * ratioImage,
                    heightCover = ratioImage <= ratioViewport ?
                        widthViewport / ratioImage : heightViewport,

                    widthContain = ratioImage <= ratioViewport ?
                        heightViewport * ratioImage : widthViewport,
                    heightContain = ratioImage <= ratioViewport ?
                        heightViewport : widthViewport / ratioImage,

                    hiddenPercent = 1 - (ratioImage <= ratioViewport ?
                        heightViewport / heightCover :
                        widthViewport / widthCover
                    ),

                    // Switch to contain if cover will hide > 40%
                    shouldUseContain =
                        hiddenPercent > 0.4 ||
                        widthCover > widthImage ||
                        heightCover > heightImage;

                this.$node.css('backgroundSize',
                    shouldUseContain ?
                            (
                                Math.min(widthContain, widthImage) + 'px ' +
                                Math.min(heightContain, heightImage) + 'px'
                            ) :
                        'cover'
                );
            }
        }

        this.srcForGlyphIconUrl = function(url) {
            if (url === F.vertex.imageDetail(this.attr.data)) {
                return url;
            }
            return url ? url.replace(/\/thumbnail/, '/raw') : '';
        };

        this.updateImageBackground = function(src) {
            var self = this,
                imageUrl = this.srcForGlyphIconUrl(src || F.vertex.imageDetail(this.attr.data)),
                customImage = !!(src || !F.vertex.imageIsFromConcept(this.attr.data));

            if (imageUrl && customImage) {
                self.$node.closest('.entity-background').addClass('loading');
                var image = new Image();
                image.onload = function() {
                    self.imageNaturalSize = [image.naturalWidth, image.naturalHeight];
                    self.updateImageBackgroundSize();
                    self.$node.closest('.entity-background').removeClass('loading');
                }
                image.onerror = function() {
                    self.$node.closest('.entity-background').removeClass('loading');
                }
                image.src = imageUrl;
            }
            this.$node
                .addClass('accepts-file')
                .css({ backgroundImage: 'url("' + imageUrl + '")' })
                .toggleClass('custom-image', customImage)
                .closest('.type-content').toggleClass('custom-entity-image', customImage);
        };

        this.onFileChange = function(e) {
            this.handleFilesDropped(e.target.files);
        };

        this.handleFilesDropped = function(files) {
            if (this.$node.hasClass('uploading')) {
                return;
            }
            this.$node.removeClass('file-hover');
            if (files.length === 1) {
                var file = files[0];

                if (this.attr.acceptedTypesRegex.test(file.type)) {
                    this.$node.addClass('uploading');
                    return _.defer(function() {
                        this.handleFileDrop(file);
                    }.bind(this));
                }
            }

            this.$node.addClass('shake');
            setTimeout(function() {
                this.$node.removeClass('shake');
            }.bind(this), 1000);
        };

        this.onUpdateIcon = function(e, data) {
            var src = this.srcForGlyphIconUrl(data.src);

            if (src !== this.srcForGlyphIconUrl(F.vertex.imageDetail(this.attr.data))) {
                this.updateImageBackground(src);
            }
        };

        this.previewFile = function(file) {
            var self = this,
                reader = new FileReader();

            reader.onload = function(event) {
                self.updateImageBackground(event.target.result);
            };

            if (file.size < MAX_PREVIEW_FILE_SIZE) {
                reader.readAsDataURL(file);
            }

            this.draw(0);
        };

        this.uploadFile = function(file) {
            var self = this;

            this.manualAnimation = false;
            this.firstProgressUpdate = true;

            this.dataRequest('vertex', 'uploadImage', this.attr.data.id, file)
                .progress(function(complete) {
                    self.trigger('fileprogress', { complete: complete });
                })
                .then(function(vertex) {
                    self.trigger('filecomplete', { vertex: vertex });
                })
                .catch(function(xhr) {
                    self.trigger('fileerror', { status: xhr.status, response: xhr.error });
                })
        };

        this.handleFileDrop = function(file) {
            this.previewFile(file);
            this.uploadFile(file);
        };

        this.draw = function(complete) {
            if (!this.ctx) {
                this.canvas = this.select('canvasSelector');
                this.ctx = this.canvas[0].getContext('2d');
            }

            var c = this.ctx,
                canvas = this.canvas[0],
                w = this.canvas.width(),
                h = this.canvas.height();
            canvas.width = w * retina.devicePixelRatio;
            canvas.height = h * retina.devicePixelRatio;
            this.canvas.css({ width: w, height: h });

            var centerX = canvas.width / 2,
                centerY = canvas.height / 2,
                radius = Math.min(canvas.width, canvas.height) / 2 * 0.3;

            c.beginPath();
            c.moveTo(centerX, centerY);
            c.arc(centerX, centerY,
                  radius + 2 * retina.devicePixelRatio, -Math.PI / 2, 2 * Math.PI - (Math.PI / 2), false
            );
            c.fillStyle = 'rgba(0,0,0,0.5)';
            c.fill();

            c.beginPath();
            c.moveTo(centerX, centerY);
            c.arc(centerX, centerY,
                  radius, -Math.PI / 2, 2 * Math.PI * Math.min(1.0, complete) - (Math.PI / 2), false
            );
            c.fillStyle = 'rgba(255,255,255,0.8)';
            c.fill();

            if (complete >= 1.0) {
                setTimeout(function() {
                    c.clearRect(0, 0, canvas.width, canvas.height);
                }, 250);
            }
        };

        this.onUploadError = function() {
            this.updateImageBackground();
            this.cleanup(false);
        };

        this.onUploadComplete = function(event, data) {
            var self = this;

            if (!this.animateManuallyIfNecessary(1.0)) {
                this.cleanup(true);
            }

            this.updateImageBackground(this.srcForGlyphIconUrl(F.vertex.imageDetail(data.vertex)));
        };

        this.onUpdateProgress = function(event, data) {
            var self = this;

            if (this.animateManuallyIfNecessary(data.complete)) {
                return;
            }

            this.draw(data.complete);
        };

        this.animateManuallyIfNecessary = function(complete) {
            var self = this;

            if (this.manualAnimation) return true;

            if (this.firstProgressUpdate && complete >= 1.0) {
                this.manualAnimation = true;
                var startedUpload = Date.now();

                // Animate manually, fast upload
                requestAnimationFrame(function draw() {
                    var now = Date.now(),
                        complete = (now - startedUpload) / 500;
                    if (complete <= 1) {
                        self.draw(complete);
                        requestAnimationFrame(draw);
                    } else {
                        self.cleanup(true);
                    }
                });
                return true;
            }
            this.firstProgressUpdate = false;

            return false;
        };

        this.cleanup = function(success) {
            if (success) {
                this.draw(1.0);
            } else {
                this.ctx.clearRect(0,0,this.canvas[0].width, this.canvas[0].height);
            }
            this.$node.removeClass('uploading');
        };
    }
});
