/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.ocr.provider.baidu;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.ocr.model.*;
import org.jetlinks.community.ocr.provider.AbstractOcrProvider;
import org.hswebframework.web.exception.BusinessException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 百度OCR提供商实现
 * 使用REST API方式调用百度OCR服务
 *
 * @author JetLinks
 */
@Slf4j
public class BaiduOcrProvider extends AbstractOcrProvider {

    private final String apiKey;
    private final String secretKey;
    private final WebClient webClient;
    private String accessToken;
    private long tokenExpireTime;

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private static final String OCR_URL = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate";
    private static final long TOKEN_EXPIRE_BUFFER = 60000; // 提前1分钟刷新token

    /**
     * 构造函数
     *
     * @param apiKey    百度OCR API Key
     * @param secretKey 百度OCR Secret Key
     */
    public BaiduOcrProvider(String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    @Override
    protected Mono<OcrResult> doRecognize(byte[] imageBytes, Map<String, Object> options) {
        return getAccessToken()
            .flatMap(token -> {
                // 将图片转换为Base64
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                // 构建请求体
                StringBuilder requestBody = new StringBuilder();
                requestBody.append("image=").append(urlEncode(base64Image));

                // 添加选项参数
                Map<String, String> ocrOptions = buildOptions(options);
                for (Map.Entry<String, String> entry : ocrOptions.entrySet()) {
                    requestBody.append("&").append(entry.getKey()).append("=").append(entry.getValue());
                }

                // 调用百度OCR API
                return webClient.post()
                    .uri(OCR_URL + "?access_token=" + token)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(BodyInserters.fromValue(requestBody.toString()))
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(this::parseResponse)
                    .onErrorMap(e -> new BusinessException("OCR识别失败: " + e.getMessage(), e));
            });
    }

    /**
     * 获取访问令牌
     */
    private Mono<String> getAccessToken() {
        // 检查token是否有效
        if (accessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return Mono.just(accessToken);
        }

        // 获取新的token
        String tokenRequest = "grant_type=client_credentials&client_id=" + apiKey + "&client_secret=" + secretKey;

        return webClient.post()
            .uri(TOKEN_URL)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(BodyInserters.fromValue(tokenRequest))
            .retrieve()
            .bodyToMono(String.class)
            .map(response -> {
                JSONObject json = new JSONObject(response);
                String token = json.getString("access_token");
                int expiresIn = json.optInt("expires_in", 2592000); // 默认30天

                this.accessToken = token;
                this.tokenExpireTime = System.currentTimeMillis() + (expiresIn * 1000L) - TOKEN_EXPIRE_BUFFER;

                log.info("百度OCR Access Token已刷新");
                return token;
            })
            .onErrorMap(e -> new BusinessException("获取百度OCR Access Token失败: " + e.getMessage(), e));
    }

    /**
     * 构建百度OCR选项
     */
    private Map<String, String> buildOptions(Map<String, Object> options) {
        Map<String, String> baiduOptions = new HashMap<>();

        if (options != null) {
            // 是否检测图片方向
            Boolean detectDirection = (Boolean) options.get("detectDirection");
            if (detectDirection != null && detectDirection) {
                baiduOptions.put("detect_direction", "true");
            } else {
                baiduOptions.put("detect_direction", "false");
            }

            // 识别语言
            String language = (String) options.get("language");
            if (language != null && !language.isEmpty()) {
                baiduOptions.put("language_type", language);
            }

            // 是否返回顶点位置
            Boolean returnPosition = (Boolean) options.get("returnTextPosition");
            if (returnPosition != null && returnPosition) {
                baiduOptions.put("vertexes_location", "true");
            } else {
                baiduOptions.put("vertexes_location", "false");
            }
        } else {
            // 默认选项
            baiduOptions.put("detect_direction", "false");
            baiduOptions.put("vertexes_location", "false");
        }

        return baiduOptions;
    }

    /**
     * URL编码
     */
    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("URL编码失败", e);
        }
    }

    /**
     * 解析百度OCR响应
     */
    private OcrResult parseResponse(String responseStr) {
        JSONObject response = new JSONObject(responseStr);

        // 检查错误码
        if (response.has("error_code")) {
            int errorCode = response.getInt("error_code");
            String errorMsg = response.getString("error_msg");
            throw new BusinessException("百度OCR API错误 [" + errorCode + "]: " + errorMsg);
        }

        // 解析识别结果
        OcrResult result = new OcrResult();
        result.setProvider(getType());

        // 获取识别的文本块
        JSONArray wordsResult = response.optJSONArray("words_result");
        if (wordsResult == null || wordsResult.length() == 0) {
            result.setText("");
            result.setBlocks(new java.util.ArrayList<>());
            return result;
        }

        // 构建文本块列表
        java.util.List<TextBlock> blocks = new java.util.ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();

        for (int i = 0; i < wordsResult.length(); i++) {
            JSONObject wordObj = wordsResult.getJSONObject(i);

            // 获取文字内容
            String text = wordObj.getString("words");
            textBuilder.append(text);
            if (i < wordsResult.length() - 1) {
                textBuilder.append("\n");
            }

            // 获取位置信息（如果返回了位置信息）
            TextBox box = null;
            if (wordObj.has("location")) {
                JSONObject location = wordObj.getJSONObject("location");
                box = TextBox.builder()
                    .x(location.optInt("left"))
                    .y(location.optInt("top"))
                    .width(location.optInt("width"))
                    .height(location.optInt("height"))
                    .build();
            }

            // 获取置信度（如果有）
            Double confidence = null;
            if (wordObj.has("probability")) {
                JSONObject probability = wordObj.getJSONObject("probability");
                confidence = probability.optDouble("average");
            }

            // 创建文本块
            TextBlock block = TextBlock.builder()
                .text(text)
                .box(box)
                .confidence(confidence)
                .build();

            blocks.add(block);
        }

        result.setText(textBuilder.toString());
        result.setBlocks(blocks);
        result.calculateCharacterCount();
        result.calculateAverageConfidence();

        return result;
    }

    @Override
    public String getType() {
        return "baidu";
    }

    @Override
    public String getName() {
        return "百度OCR";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty()
            && secretKey != null && !secretKey.isEmpty();
    }
}
