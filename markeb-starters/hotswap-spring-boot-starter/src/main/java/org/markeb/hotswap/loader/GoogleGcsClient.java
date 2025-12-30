package org.markeb.hotswap.loader;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.*;
import org.markeb.hotswap.config.HotSwapProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Google Cloud Storage 客户端实现
 */
public class GoogleGcsClient implements OssClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleGcsClient.class);

    private final Storage storage;
    private final String bucketName;

    public GoogleGcsClient(HotSwapProperties.OssConfig config) {
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucketName = config.getBucketName();
        log.info("Google GCS client initialized, bucket: {}", bucketName);
    }

    public GoogleGcsClient(Storage storage, String bucketName) {
        this.storage = storage;
        this.bucketName = bucketName;
    }

    @Override
    public InputStream getObject(String objectKey) throws IOException {
        try {
            Blob blob = storage.get(BlobId.of(bucketName, objectKey));
            if (blob == null) {
                throw new IOException("Object not found: " + objectKey);
            }
            return new ByteArrayInputStream(blob.getContent());
        } catch (Exception e) {
            throw new IOException("Failed to get object: " + objectKey, e);
        }
    }

    @Override
    public boolean doesObjectExist(String objectKey) {
        Blob blob = storage.get(BlobId.of(bucketName, objectKey));
        return blob != null && blob.exists();
    }

    @Override
    public List<String> listObjects(String prefix) {
        List<String> objects = new ArrayList<>();
        Page<Blob> blobs = storage.list(bucketName, Storage.BlobListOption.prefix(prefix));
        for (Blob blob : blobs.iterateAll()) {
            objects.add(blob.getName());
        }
        return objects;
    }

    @Override
    public void downloadObject(String objectKey, String localPath) throws IOException {
        try (InputStream is = getObject(objectKey)) {
            Files.copy(is, Path.of(localPath), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public String getType() {
        return "google";
    }

}

