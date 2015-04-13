package io.lumify.imageMetadataExtractor;

/**
 * NOTE: When displaying the image, Make sure to Flip first, and then Rotate.
 * Transformations must be done in this order.
 */
public class ImageTransform {
    private final boolean yAxisFlipNeeded;
    private final int cwRotationNeeded;

    public ImageTransform(boolean yAxisFlipNeeded, int cwRotationNeeded) {
        this.yAxisFlipNeeded = yAxisFlipNeeded;
        this.cwRotationNeeded = cwRotationNeeded;
    }

    public boolean isYAxisFlipNeeded() {
        return yAxisFlipNeeded;
    }

    public int getCWRotationNeeded() {
        return cwRotationNeeded;
    }

}
