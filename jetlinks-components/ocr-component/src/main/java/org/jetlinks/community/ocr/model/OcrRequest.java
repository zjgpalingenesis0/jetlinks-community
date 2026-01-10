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
package org.jetlinks.community.ocr.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * OCR识别请求
 *
 * @author JetLinks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OCR识别请求")
public class OcrRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件ID（可以是s_file表的ID，或完整路径）", required = true)
    private String fileId;

    @Schema(description = "OCR识别选项")
    private OcrOptions options;

    /**
     * OCR识别选项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "OCR识别选项")
    public static class OcrOptions implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "指定OCR提供商（baidu/aliyun/tesseract），默认使用配置的默认提供商")
        private String provider;

        @Schema(description = "是否检测图片方向，默认false")
        private Boolean detectDirection;

        @Schema(description = "识别语言（CHN_ENG/ENG/JAP/KOR等），默认CHN_ENG")
        private String language;

        @Schema(description = "是否返回文字位置，默认true")
        private Boolean returnTextPosition;

        @Schema(description = "其他选项（扩展参数）")
        private Map<String, Object> extra;
    }
}
