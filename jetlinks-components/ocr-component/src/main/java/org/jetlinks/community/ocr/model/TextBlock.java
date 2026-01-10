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

/**
 * OCR识别的文本块
 * 包含文字内容、位置信息和置信度
 *
 * @author JetLinks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OCR识别的文本块")
public class TextBlock implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "识别的文字内容")
    private String text;

    @Schema(description = "文本框位置信息")
    private TextBox box;

    @Schema(description = "置信度（0-1之间，越接近1越准确）")
    private Double confidence;

    /**
     * 判断文本块是否有效
     *
     * @return true 如果文字内容不为空且置信度大于0
     */
    public boolean isValid() {
        return text != null && !text.trim().isEmpty()
            && confidence != null && confidence > 0;
    }

    /**
     * 获取文字长度
     *
     * @return 文字字符数
     */
    public int getTextLength() {
        return text == null ? 0 : text.length();
    }
}
