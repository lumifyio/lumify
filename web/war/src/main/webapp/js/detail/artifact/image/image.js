
define([
    'flight/lib/component',
    'tpl!./image',
    'util/withAsyncQueue',
    'util/privileges',
    'util/detectedObjects/withFacebox'
], function(defineComponent, template, withAsyncQueue, Privileges, withFacebox) {
    'use strict';

    return defineComponent(ImageView, withAsyncQueue, withFacebox);

    function ImageView() {

        this.defaultAttrs({
            imageSelector: 'img',
            artifactImageSelector: '.artifact-image',
        });

        this.after('initialize', function() {
            this.$node
                .addClass('loading')
                .html(template({ data: this.attr.data }));

            var self = this,
                image = this.select('imageSelector'),
                imageEl = image.get(0),
                naturalWidth = imageEl.naturalWidth,
                naturalHeight = imageEl.naturalHeight;

            if (naturalWidth === 0 || naturalHeight === 0) {
                image.on('load', this.onImageLoaded.bind(this))
            } else {
                this.onImageLoaded();
            }
        });

        this.onImageLoaded = function() {
            this.$node.removeClass('loading');

            if (Privileges.missingEDIT) {
                return this.$node.css('cursor', 'default')
            }

            var artifactImage = this.select('artifactImageSelector');
            this.initializeFacebox(artifactImage);
        }
    }
});
