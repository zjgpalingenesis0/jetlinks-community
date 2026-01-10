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
package org.jetlinks.community.ocr.service;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.io.file.FileEntity;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.io.file.FileInfo;
import org.jetlinks.community.ocr.OcrProperties;
import org.jetlinks.community.ocr.model.OcrRequest;
import org.jetlinks.community.ocr.model.OcrResult;
import org.jetlinks.community.ocr.provider.OcrProvider;
import org.jetlinks.community.ocr.provider.baidu.BaiduOcrProvider;
import org.hswebframework.web.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

/**
 * OCR服务实现
 *
 * @author JetLinks
 */
@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    private final FileManager fileManager;
    private final OcrProperties ocrProperties;
    private final Map<String, OcrProvider> providers = new HashMap<>();

    @Autowired
    public OcrServiceImpl(FileManager fileManager, OcrProperties ocrProperties) {
        this.fileManager = fileManager;
        this.ocrProperties = ocrProperties;
        initializeProviders();
    }

    /**
     * 初始化OCR提供商
     */
    private void initializeProviders() {
        // 初始化百度OCR
        if (ocrProperties.getBaidu().isEnabled()) {
            String apiKey = ocrProperties.getBaidu().getApiKey();
            String secretKey = ocrProperties.getBaidu().getSecretKey();

            if (StringUtils.hasText(apiKey) && StringUtils.hasText(secretKey)) {
                BaiduOcrProvider baiduProvider = new BaiduOcrProvider(apiKey, secretKey);
                providers.put("baidu", baiduProvider);
                log.info("百度OCR提供商已初始化");
            } else {
                log.warn("百度OCR未配置API Key或Secret Key，跳过初始化");
            }
        }

        // 后续可添加阿里云、Tesseract等提供商
        log.info("OCR提供商初始化完成，共{}个提供商可用", providers.size());
    }

    @Override
    public Mono<OcrResult> recognize(OcrRequest request) {
        // 验证请求参数
        if (request == null || !StringUtils.hasText(request.getFileId())) {
            return Mono.error(new BusinessException("fileId不能为空"));
        }

        return recognizeByFileId(request.getFileId(), request.getOptions());
    }

    @Override
    public Mono<OcrResult> recognizeByFileId(String fileId) {
        return recognizeByFileId(fileId, null);
    }

    /**
     * 根据fileId进行OCR识别
     *
     * @param fileId 文件ID
     * @param options OCR选项
     * @return OCR识别结果
     */
    private Mono<OcrResult> recognizeByFileId(String fileId, OcrRequest.OcrOptions options) {
        // 查询文件信息
        return fileManager.getFile(fileId)
            .switchIfEmpty(Mono.error(new BusinessException("文件不存在: " + fileId)))
            .flatMap(fileInfo -> {
                // 读取图片文件
                return readImageFile(fileInfo)
                    .flatMap(imageBytes -> {
                        // 调用OCR识别
                        return doRecognize(imageBytes, options, fileId);
                    });
            })
            .doOnSubscribe(subscription -> {
                log.info("开始OCR识别: fileId={}", fileId);
            });
    }

    /**
     * 从文件系统读取图片文件
     *
     * @param fileInfo 文件信息
     * @return 图片字节数组
     */
    private Mono<byte[]> readImageFile(FileInfo fileInfo) {
        return Mono.fromCallable(() -> {
            // 使用DataBuffer读取文件
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();

            fileManager.read(fileInfo.getId())
                .doOnNext(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    try {
                        outputStream.write(bytes);
                    } catch (java.io.IOException e) {
                        throw new BusinessException("写入文件流失败", e);
                    } finally {
                        org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
                    }
                })
                .blockLast();

            return outputStream.toByteArray();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorMap(e -> new BusinessException("读取文件失败: " + e.getMessage(), e));
    }

    /**
     * 执行OCR识别
     *
     * @param imageBytes 图片字节数组
     * @param options    OCR选项
     * @param fileId     文件ID
     * @return OCR识别结果
     */
    private Mono<OcrResult> doRecognize(byte[] imageBytes, OcrRequest.OcrOptions options, String fileId) {
        // 获取OCR提供商
        OcrProvider provider = getProvider(options);
        if (provider == null) {
            return Mono.error(new BusinessException("没有可用的OCR提供商"));
        }

        // 构建选项
        Map<String, Object> optionMap = buildOptionMap(options);

        // 调用OCR识别
        return provider.recognize(imageBytes, optionMap)
            .map(result -> {
                result.setFileId(fileId);
                return result;
            });
    }

    /**
     * 获取OCR提供商
     *
     * @param options OCR选项
     * @return OCR提供商
     */
    private OcrProvider getProvider(OcrRequest.OcrOptions options) {
        // 如果指定了提供商，则使用指定的
        if (options != null && StringUtils.hasText(options.getProvider())) {
            String providerType = options.getProvider();
            OcrProvider provider = providers.get(providerType);
            if (provider == null) {
                log.warn("指定的OCR提供商不可用: {}", providerType);
            }
            return provider;
        }

        // 否则使用默认提供商
        String defaultProvider = ocrProperties.getDefaultProvider();
        return providers.get(defaultProvider);
    }

    /**
     * 构建选项Map
     *
     * @param options OCR选项
     * @return 选项Map
     */
    private Map<String, Object> buildOptionMap(OcrRequest.OcrOptions options) {
        Map<String, Object> optionMap = new HashMap<>();

        if (options == null) {
            // 使用默认选项
            optionMap.put("detectDirection", false);
            optionMap.put("language", "CHN_ENG");
            optionMap.put("returnTextPosition", true);
            return optionMap;
        }

        // 检测方向
        if (options.getDetectDirection() != null) {
            optionMap.put("detectDirection", options.getDetectDirection());
        } else {
            optionMap.put("detectDirection", false);
        }

        // 语言
        if (StringUtils.hasText(options.getLanguage())) {
            optionMap.put("language", options.getLanguage());
        } else {
            optionMap.put("language", "CHN_ENG");
        }

        // 返回位置
        if (options.getReturnTextPosition() != null) {
            optionMap.put("returnTextPosition", options.getReturnTextPosition());
        } else {
            optionMap.put("returnTextPosition", true);
        }

        // 其他扩展选项
        if (options.getExtra() != null) {
            optionMap.putAll(options.getExtra());
        }

        return optionMap;
    }
}
