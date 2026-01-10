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

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.ocr.service.OcrService;
import org.jetlinks.community.ocr.service.OcrServiceImpl;
import org.jetlinks.community.ocr.web.OcrController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.jetlinks.community.io.file.FileManager;

/**
 * OCR组件自动配置类
 *
 * @author JetLinks
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(OcrProperties.class)
public class OcrComponent {

    @Bean
    @ConditionalOnMissingBean
    public OcrService ocrService(FileManager fileManager, OcrProperties ocrProperties) {
        log.info("初始化OCR服务...");
        return new OcrServiceImpl(fileManager, ocrProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OcrController ocrController(OcrService ocrService) {
        log.info("初始化OCR控制器...");
        return new OcrController(ocrService);
    }
}
