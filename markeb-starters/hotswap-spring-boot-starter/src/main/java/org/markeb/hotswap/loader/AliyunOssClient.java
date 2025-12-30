package org.markeb.hotswap.loader;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectListing;
import org.markeb.hotswap.config.HotSwapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 阿里云 OSS 客户端实现
 */
public class AliyunOssClient implements OssClient {

    private static final Logger log = LoggerFactory.getLogger(AliyunOssClient.class);

    private final OSS ossClient;
    private final String bucketName;

    public AliyunOssClient(HotSwapProperties.OssConfig config) {
        this.ossClient = new OSSClientBuilder().build(
                config.getEndpoint(),
                config.getAccessKeyId(),
                config.getAccessKeySecret()
        );
        this.bucketName = config.getBucketName();
        log.info("Aliyun OSS client initialized, bucket: {}", bucketName);
    }

    public AliyunOssClient(OSS ossClient, String bucketName) {
        this.ossClient = ossClient;
        this.bucketName = bucketName;
    }

    @Override
    public InputStream getObject(String objectKey) throws IOException {
        try {
            OSSObject ossObject = ossClient.getObject(bucketName, objectKey);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            throw new IOException("Failed to get object: " + objectKey, e);
        }
    }

    @Override
    public boolean doesObjectExist(String objectKey) {
        return ossClient.doesObjectExist(bucketName, objectKey);
    }

    @Override
    public List<String> listObjects(String prefix) {
        ObjectListing listing = ossClient.listObjects(bucketName, prefix);
        return listing.getObjectSummaries().stream()
                .map(s -> s.getKey())
                .toList();
    }

    @Override
    public void downloadObject(String objectKey, String localPath) throws IOException {
        try {
            ossClient.getObject(new GetObjectRequest(bucketName, objectKey), new File(localPath));
        } catch (Exception e) {
            throw new IOException("Failed to download object: " + objectKey, e);
        }
    }

    @Override
    public String getType() {
        return "aliyun";
    }

    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

}

