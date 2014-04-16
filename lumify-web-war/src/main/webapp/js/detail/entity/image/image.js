
define([
    'flight/lib/component',
    'tpl!./image',
    'data',
    'util/retina',
    'util/withFileDrop',
    'service/vertex'
], function(
    defineComponent,
    template,
    appData,
    retina, 
    withFileDrop,
    VertexService) {
    'use strict';

    // Limit previews to 1MB since it's a dataUri
    var MAX_PREVIEW_FILE_SIZE = 1024 * 1024; 

    return defineComponent(ImageView, withFileDrop);

    function ImageView() {

        var vertexService = new VertexService();

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
            this.on('iconUpdated', this.onUpdateIcon);

            this.updateImageBackground();
            this.$node.html(template({}));

            this.select('fileSelector').on({
                click: function() {
                    self.$node.addClass('file-hover'); this.value = null; 
                },
                change: this.onFileChange.bind(this)
            });

            this.$node.addClass('upload-available');
            this.$node.on({
                mouseenter: function() {
                    $(this).addClass('file-hover'); 
                },
                mouseleave: function() {
                    $(this).removeClass('file-hover'); 
                }
            });
        });

        this.srcForGlyphIconUrl = function(url) {
            return url ? url.replace(/\/thumbnail/, '/raw') : '';
        };

        this.updateImageBackground = function(src) {
            this.$node
                .addClass('accepts-file')
                .css({ backgroundImage: 'url("' + this.srcForGlyphIconUrl(src || this.attr.data.imageSrc) + '")' })
                .toggleClass('custom-image', !!(src || !this.attr.data.imageSrcIsFromConcept));
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

            if (src !== this.srcForGlyphIconUrl(this.attr.data.imageSrc)) {
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

            vertexService.uploadImage(this.attr.data.id, file)
                .progress(function(complete) {
                    self.trigger('fileprogress', { complete: complete });
                })
                .fail(function(xhr, message, error) {
                    self.trigger('fileerror', { status: xhr.status, response: error });
                })
                .done(function(vertex) {
                    self.trigger('filecomplete', { vertex: vertex });
                });
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

            this.updateImageBackground(this.srcForGlyphIconUrl(data.vertex.imageSrc));
            
            this.trigger(document, 'updateVertices', { vertices: [data.vertex] });
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
