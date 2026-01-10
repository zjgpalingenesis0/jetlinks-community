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
 * 文本框位置信息
 * 用于表示识别出的文字在图片中的位置
 *
 * @author JetLinks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文本框位置信息")
public class TextBox implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "左上角X坐标（像素）")
    private Integer x;

    @Schema(description = "左上角Y坐标（像素）")
    private Integer y;

    @Schema(description = "文本框宽度（像素）")
    private Integer width;

    @Schema(description = "文本框高度（像素）")
    private Integer height;

    /**
     * 计算文本框面积
     *
     * @return 面积（平方像素）
     */
    public Integer getArea() {
        if (width == null || height == null) {
            return 0;
        }
        return width * height;
    }

    /**
     * 判断是否为有效文本框
     *
     * @return true 如果坐标和尺寸都有效
     */
    public boolean isValid() {
        return x != null && y != null && width != null && height != null
            && width > 0 && height > 0;
    }
}
