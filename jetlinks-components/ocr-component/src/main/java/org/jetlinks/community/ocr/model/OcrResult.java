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
import java.util.List;

/**
 * OCR识别结果
 *
 * @author JetLinks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OCR识别结果")
public class OcrResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "识别的完整文字内容（多行文字用\\n分隔）")
    private String text;

    @Schema(description = "识别的文本块列表")
    private List<TextBlock> blocks;

    @Schema(description = "OCR提供商（baidu/aliyun/tesseract）")
    private String provider;

    @Schema(description = "识别耗时（毫秒）")
    private Long costTime;

    @Schema(description = "识别的字符总数")
    private Integer characterCount;

    @Schema(description = "平均置信度（0-1之间）")
    private Double averageConfidence;

    @Schema(description = "错误信息（如果识别失败）")
    private String error;

    /**
     * 判断识别结果是否有效
     *
     * @return true 如果识别到文字且置信度大于0
     */
    public boolean isValid() {
        return text != null && !text.trim().isEmpty()
            && blocks != null && !blocks.isEmpty();
    }

    /**
     * 计算平均置信度
     */
    public void calculateAverageConfidence() {
        if (blocks == null || blocks.isEmpty()) {
            this.averageConfidence = 0.0;
            return;
        }

        double sum = blocks.stream()
            .filter(block -> block.getConfidence() != null)
            .mapToDouble(TextBlock::getConfidence)
            .sum();

        this.averageConfidence = sum / blocks.size();
    }

    /**
     * 计算字符总数
     */
    public void calculateCharacterCount() {
        if (text == null) {
            this.characterCount = 0;
        } else {
            this.characterCount = text.length();
        }
    }
}
