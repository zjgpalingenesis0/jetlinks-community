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
package org.jetlinks.community.ocr;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OCR配置属性
 *
 * @author JetLinks
 */
@Data
@ConfigurationProperties(prefix = "ocr")
public class OcrProperties {

    /**
     * 默认OCR提供商（baidu/aliyun/tesseract）
     */
    private String defaultProvider = "baidu";

    /**
     * 是否启用OCR功能
     */
    private boolean enabled = true;

    /**
     * 百度OCR配置
     */
    private BaiduProperties baidu = new BaiduProperties();

    /**
     * 阿里云OCR配置
     */
    private AliyunProperties aliyun = new AliyunProperties();

    /**
     * Tesseract OCR配置
     */
    private TesseractProperties tesseract = new TesseractProperties();

    /**
     * 百度OCR配置
     */
    @Data
    public static class BaiduProperties {
        /**
         * API Key
         */
        private String apiKey;

        /**
         * Secret Key
         */
        private String secretKey;

        /**
         * 是否启用
         */
        private boolean enabled = true;
    }

    /**
     * 阿里云OCR配置
     */
    @Data
    public static class AliyunProperties {
        /**
         * Access Key ID
         */
        private String accessKeyId;

        /**
         * Access Key Secret
         */
        private String accessKeySecret;

        /**
         * 地域ID（如：cn-hangzhou）
         */
        private String regionId = "cn-hangzhou";

        /**
         * 是否启用
         */
        private boolean enabled = false;
    }

    /**
     * Tesseract OCR配置
     */
    @Data
    public static class TesseractProperties {
        /**
         * Tesseract数据路径（tessdata目录）
         */
        private String datapath;

        /**
         * 识别语言（如：chi_sim+eng）
         */
        private String language = "chi_sim+eng";

        /**
         * 是否启用
         */
        private boolean enabled = false;
    }
}
