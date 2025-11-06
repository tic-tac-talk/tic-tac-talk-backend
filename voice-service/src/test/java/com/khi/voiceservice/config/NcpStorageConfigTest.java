package com.khi.voiceservice.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;

@SpringBootTest
class NcpStorageConfigTest {

    @Autowired
    private S3Client s3Client;

    @Test
    void NCP_S3_연결_테스트() {
        ListBucketsResponse response = s3Client.listBuckets();

        Assertions.assertThat(response.buckets()).isNotNull();

        response.buckets().forEach(b -> System.out.println(" - " + b.name()));
    }
}