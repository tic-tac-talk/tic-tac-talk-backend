package com.khi.voiceservice.csr;

import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ClovaSpeechClient {

    @Value("${clova.speech.secret-key}")
    private String secretKey;

    @Value("${clova.speech.invoke-url}")
    private String invokeUrl;

    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final Gson gson = new Gson();

    private Header[] getHeaders() {
        return new Header[] {
                new BasicHeader("Accept", "application/json"),
                new BasicHeader("X-CLOVASPEECH-API-KEY", secretKey)
        };
    }

    // 클로바 전사 비동기 요청
    public void asyncRecognize(String fileUrl, String callbackUrl) {
        HttpPost post = new HttpPost(invokeUrl + "/recognizer/url");

        Map<String, Object> body = new HashMap<>();
        body.put("url", fileUrl);
        body.put("callback", callbackUrl);
        body.put("language", "ko-KR");
        body.put("completion", "async");

        Map<String, String> diarizationMap = new HashMap<>();
        diarizationMap.put("mode", "auto");
        body.put("diarization", diarizationMap);

        HttpEntity httpEntity = new StringEntity(gson.toJson(body), ContentType.APPLICATION_JSON);

        post.setHeaders(getHeaders());
        post.setEntity(httpEntity);

        try (CloseableHttpResponse response = httpClient.execute(post)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            log.info("[Clova Speech] 요청 상태: " + statusCode);
            log.info("[Clova Speech] 응답: " + responseBody);

            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("Clova Speech API 요청 실패: " + responseBody);
            }
        } catch (IOException e) {
            throw new RuntimeException("Clova Speech API 요청 실패: " + e);
        }
    }
}
