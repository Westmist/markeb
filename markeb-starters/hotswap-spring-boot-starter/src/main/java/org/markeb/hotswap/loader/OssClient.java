package org.markeb.hotswap.loader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * OSS 客户端抽象接口
 * <p>
 * 支持多种 OSS 实现：阿里云、腾讯云、MinIO、AWS S3 等。
 * </p>
 */
public interface OssClient {

    /**
     * 获取对象
     *
     * @param objectKey 对象路径
     * @return 输入流
     * @throws IOException 获取失败
     */
    InputStream getObject(String objectKey) throws IOException;

    /**
     * 获取对象字节数组
     *
     * @param objectKey 对象路径
     * @return 字节数组
     * @throws IOException 获取失败
     */
    default byte[] getObjectBytes(String objectKey) throws IOException {
        try (InputStream is = getObject(objectKey)) {
            return is.readAllBytes();
        }
    }

    /**
     * 检查对象是否存在
     *
     * @param objectKey 对象路径
     * @return 是否存在
     */
    boolean doesObjectExist(String objectKey);

    /**
     * 列出指定前缀的对象
     *
     * @param prefix 前缀
     * @return 对象路径列表
     */
    List<String> listObjects(String prefix);

    /**
     * 下载对象到本地文件
     *
     * @param objectKey 对象路径
     * @param localPath 本地路径
     * @throws IOException 下载失败
     */
    void downloadObject(String objectKey, String localPath) throws IOException;

    /**
     * 获取 OSS 类型
     *
     * @return OSS 类型标识
     */
    String getType();

}

