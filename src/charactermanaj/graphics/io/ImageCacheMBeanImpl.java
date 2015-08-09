package charactermanaj.graphics.io;

import java.lang.management.ManagementFactory;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

public final class ImageCacheMBeanImpl implements ImageCacheMBean {

    private static ImageCacheMBeanImpl singleton = new ImageCacheMBeanImpl();

    private ImageCacheMBeanImpl() {
        super();
    }

    public static ImageCacheMBeanImpl getSingleton() {
        return singleton;
    }

    public static void setupMBean() throws JMException {
        MBeanServer srv = ManagementFactory.getPlatformMBeanServer();
        srv.registerMBean(
                new StandardMBean(singleton, ImageCacheMBean.class),
                new ObjectName("CharacterManaJ:type=ImageCache,name=Singleton"));
    }

    private long readCount;

    private long cacheHitCount;

    private long totalBytes;

    private long maxBytes;

    private int totalCount;

    private int instanceCount;

    public synchronized long getReadCount() {
        return readCount;
    }

    public synchronized void setReadCount(long readCount) {
        this.readCount = readCount;
    }

    public synchronized long getCacheHitCount() {
        return cacheHitCount;
    }

    public synchronized void setCacheHitCount(long cacheHitCount) {
        this.cacheHitCount = cacheHitCount;
    }

    public synchronized long getTotalBytes() {
        return totalBytes;
    }

    public synchronized void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public synchronized long getMaxBytes() {
        return maxBytes;
    }

    public synchronized void setMaxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    public synchronized void incrementReadCount(boolean cacheHit) {
        readCount++;
        if (cacheHit) {
            cacheHitCount++;
        }
    }

    public synchronized void cacheIn(long bytes) {
        totalCount++;
        totalBytes += bytes;
        if (totalBytes > maxBytes) {
            maxBytes = totalBytes;
        }
    }

    public synchronized void cacheOut(long bytes) {
        totalCount--;
        totalBytes -= bytes;
    }

    public synchronized int getTotalCount() {
        return totalCount;
    }

    public synchronized int getInstanceCount() {
        return instanceCount;
    }

    public synchronized void incrementInstance() {
        instanceCount++;
    }

    public synchronized void decrementInstance() {
        instanceCount--;
    }

    public synchronized void reset() {
        cacheHitCount = 0;
        readCount = 0;
        totalCount = 0;
        totalBytes = 0;
        maxBytes = 0;
    }

    @Override
    public String toString() {
        synchronized (this) {
            StringBuilder buf = new StringBuilder();
            buf.append("imageCacheMBean ");
            buf.append(cacheHitCount);
            buf.append("/");
            buf.append(readCount);
            return buf.toString();
        }
    }
}
