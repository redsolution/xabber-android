package com.xabber.xmpp.avatar;

import org.jivesoftware.smack.util.StringUtils;

import java.net.URL;

/**
 * User Avatar metadata info model class.
 *
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User
 *      Avatar</a>
 */
public class MetadataInfo {

    public static final int MAX_HEIGHT = 65536;
    public static final int MAX_WIDTH = 65536;

    private final String id;
    private final URL url;
    private final Integer bytes;
    private final String type;
    private final short height;
    private final short width;

    /**
     * MetadataInfo constructor.
     *
     * @param id SHA-1 hash of the image data
     * @param url http(s) url of the image
     * @param bytes size of the image in bytes
     * @param type content type of the image
     * @param pixelsHeight height of the image in pixels
     * @param pixelsWidth width of the image in pixels
     */
    public MetadataInfo(String id, URL url, long bytes, String type, int pixelsHeight, int pixelsWidth) {
        /*this.id = StringUtils.requireNotNullNorEmpty(id, "ID is required.")*/;
        this.id = StringUtils.requireNotNullOrEmpty(id, "ID is required.");
        this.url = url;
        this.bytes = longToIntConverter(bytes);
        this.type = type; //StringUtils.requireNotNullOrEmpty(type, "Content Type is required.");
        //if (pixelsHeight < 0 || pixelsHeight > MAX_HEIGHT) {
        //    throw new IllegalArgumentException("Image height value must be between 0 and 65536.");
        //}
        //if (pixelsWidth < 0 || pixelsWidth > MAX_WIDTH) {
        //    throw new IllegalArgumentException("Image width value must be between 0 and 65536.");
        //}
        this.height = (short) pixelsHeight;
        this.width = (short) pixelsWidth;
    }

    public Integer longToIntConverter(long bytes) {
        if (bytes <= Integer.MAX_VALUE && bytes > 0)
            return (int) bytes;
        else if (bytes > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        else return 0;
    }

    /**
     * Get the id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the url of the avatar image.
     *
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Get the amount of bytes.
     *
     * @return the amount of bytes
     */
    public Integer getBytes() {
        return bytes;
    }

    /**
     * Get the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Get the height in pixels.
     *
     * @return the height in pixels
     */
    public short getHeight() {
        return height;
    }

    /**
     * Get the width in pixels.
     *
     * @return the width in pixels
     */
    public short getWidth() {
        return width;
    }

}