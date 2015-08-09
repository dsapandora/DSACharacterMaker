package charactermanaj.graphics.io;

public interface ImageCacheMBean {

    long getReadCount();

    long getCacheHitCount();

    long getTotalBytes();

    long getMaxBytes();

    int getTotalCount();

    int getInstanceCount();

    void reset();
}
