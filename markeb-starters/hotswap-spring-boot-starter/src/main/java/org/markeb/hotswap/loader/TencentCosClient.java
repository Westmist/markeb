package org.markeb.hotswap.loader;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.region.Region;
import org.markeb.hotswap.config.HotSwapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 腾讯云 COS 客户端实现
 */
public class TencentCosClient implements OssClient {

    private static final Logger log = LoggerFactory.getLogger(TencentCosClient.class);

    private final COSClient cosClient;
    private final String bucketName;

    public TencentCosClient(HotSwapProperties.OssConfig config) {
        COSCredentials cred = new BasicCOSCredentials(
                config.getAccessKeyId(),
                config.getAccessKeySecret()
        );
        ClientConfig clientConfig = new ClientConfig(new Region(config.getRegion()));
        this.cosClient = new COSClient(cred, clientConfig);
        this.bucketName = config.getBucketName();
        log.info("Tencent COS client initialized, bucket: {}", bucketName);
    }

    public TencentCosClient(COSClient cosClient, String bucketName) {
        this.cosClient = cosClient;
        this.bucketName = bucketName;
    }

    @Override
    public InputStream getObject(String objectKey) throws IOException {
        try {
            COSObject cosObject = cosClient.getObject(bucketName, objectKey);
            return cosObject.getObjectContent();
        } catch (Exception e) {
            throw new IOException("Failed to get object: " + objectKey, e);
        }
    }

    @Override
    public boolean doesObjectExist(String objectKey) {
        return cosClient.doesObjectExist(bucketName, objectKey);
    }

    @Override
    public List<String> listObjects(String prefix) {
        ObjectListing listing = cosClient.listObjects(bucketName, prefix);
        return listing.getObjectSummaries().stream()
                .map(COSObjectSummary::getKey)
                .toList();
    }

    @Override
    public void downloadObject(String objectKey, String localPath) throws IOException {
        try {
            cosClient.getObject(new GetObjectRequest(bucketName, objectKey), new File(localPath));
        } catch (Exception e) {
            throw new IOException("Failed to download object: " + objectKey, e);
        }
    }

    @Override
    public String getType() {
        return "tencent";
    }

    public void shutdown() {
        if (cosClient != null) {
            cosClient.shutdown();
        }
    }

}

