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

import org.jetlinks.community.ocr.model.OcrResult;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * OCR提供商接口
 * 所有OCR实现（百度、阿里云、Tesseract等）都需要实现此接口
 *
 * @author JetLinks
 */
public interface OcrProvider {

    /**
     * OCR文字识别
     *
     * @param imageBytes 图片字节数组
     * @param options    识别选项
     * @return OCR识别结果
     */
    Mono<OcrResult> recognize(byte[] imageBytes, Map<String, Object> options);

    /**
     * 获取提供商类型（如：baidu/aliyun/tesseract）
     *
     * @return 提供商类型
     */
    String getType();

    /**
     * 获取提供商名称
     *
     * @return 提供商名称
     */
    String getName();

    /**
     * 获取提供商优先级（数字越小优先级越高）
     *
     * @return 优先级
     */
    int getOrder();

    /**
     * 判断提供商是否可用
     *
     * @return true 如果可用
     */
    default boolean isAvailable() {
        return true;
    }
}
