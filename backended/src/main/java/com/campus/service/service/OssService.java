package com.campus.service.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Service
public class OssService {

    @Value("${aliyun.oss.endpoint}") // 从 application.yml 读
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    private static final long EXPIRE_MS = 365L * 24 * 3600 * 1000;

    public String uploadImage(MultipartFile file) {
        try {
            // 1. 生成唯一文件名：campus/随机ID_原文件名
            String fileName = "campus/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            // 2. 创建OSS客户端（连阿里云）
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            InputStream inputStream = file.getInputStream();
            // 3. 把文件上传到阿里云
            ossClient.putObject(bucketName, fileName, inputStream);

            Date expiration = new Date(System.currentTimeMillis() + EXPIRE_MS);
            // 4. 生成一个有效期1年的图片链接
            URL signedUrl = ossClient.generatePresignedUrl(bucketName, fileName, expiration);
            ossClient.shutdown();
            // 5. 返回这个链接（强制HTTPS）
            return signedUrl.toString().replaceFirst("^http://", "https://");
        } catch (Exception e) {
            throw new RuntimeException("图片上传失败: " + e.getMessage());
        }
    }

    // 获取图片的签名URL
    public String getSignedUrl(String objectKey) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        Date expiration = new Date(System.currentTimeMillis() + EXPIRE_MS);
        URL signedUrl = ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
        ossClient.shutdown();
        return signedUrl.toString().replaceFirst("^http://", "https://");
    }

    // 从 URL 下载图片并上传到 OSS
    public String uploadFromUrl(String imageUrl) {
        try {
            String fileName = "campus/poster_" + UUID.randomUUID() + ".png";
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            java.net.URL url = new java.net.URL(imageUrl);
            try (InputStream inputStream = url.openStream()) {
                ossClient.putObject(bucketName, fileName, inputStream);
            }
            Date expiration = new Date(System.currentTimeMillis() + EXPIRE_MS);
            com.aliyun.oss.model.GeneratePresignedUrlRequest request =
                new com.aliyun.oss.model.GeneratePresignedUrlRequest(bucketName, fileName, com.aliyun.oss.HttpMethod.GET);
            request.setExpiration(expiration);
            request.addQueryParameter("response-content-disposition", "inline");
            URL signedUrl = ossClient.generatePresignedUrl(request);
            ossClient.shutdown();
            return signedUrl.toString().replaceFirst("^http://", "https://");
        } catch (Exception e) {
            throw new RuntimeException("海报上传失败: " + e.getMessage());
        }
    }
}
