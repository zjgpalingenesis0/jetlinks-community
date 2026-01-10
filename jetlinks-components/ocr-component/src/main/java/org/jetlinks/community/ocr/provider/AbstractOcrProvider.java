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
package org.jetlinks.community.ocr.provider;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.ocr.model.OcrResult;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * OCR提供商抽象基类
 * 提供通用的日志和错误处理功能
 *
 * @author JetLinks
 */
@Slf4j
public abstract class AbstractOcrProvider implements OcrProvider {

    @Override
    public Mono<OcrResult> recognize(byte[] imageBytes, Map<String, Object> options) {
        long startTime = System.currentTimeMillis();

        return doRecognize(imageBytes, options)
            .doOnNext(result -> {
                long costTime = System.currentTimeMillis() - startTime;
                result.setCostTime(costTime);
                result.setProvider(getType());

                log.info("OCR识别成功: provider={}, costTime={}ms, charCount={}, confidence={}",
                    getType(), costTime, result.getCharacterCount(), result.getAverageConfidence());
            })
            .doOnError(error -> {
                long costTime = System.currentTimeMillis() - startTime;
                log.error("OCR识别失败: provider={}, costTime={}ms, error={}",
                    getType(), costTime, error.getMessage(), error);
            })
            .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }

    /**
     * 执行OCR识别的具体实现
     * 子类需要实现此方法
     *
     * @param imageBytes 图片字节数组
     * @param options    识别选项
     * @return OCR识别结果
     */
    protected abstract Mono<OcrResult> doRecognize(byte[] imageBytes, Map<String, Object> options);
}
