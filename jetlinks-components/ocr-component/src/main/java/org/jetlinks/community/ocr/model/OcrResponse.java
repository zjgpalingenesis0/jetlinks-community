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
 * OCR识别响应
 *
 * @author JetLinks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "OCR识别响应")
public class OcrResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "响应码（200成功，其他失败）")
    private Integer code;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "OCR识别结果")
    private OcrResult result;

    /**
     * 创建成功响应
     *
     * @param result OCR识别结果
     * @return 响应对象
     */
    public static OcrResponse success(OcrResult result) {
        return OcrResponse.builder()
            .code(200)
            .message("success")
            .result(result)
            .build();
    }

    /**
     * 创建错误响应
     *
     * @param errorMessage 错误消息
     * @return 响应对象
     */
    public static OcrResponse error(String errorMessage) {
        return OcrResponse.builder()
            .code(500)
            .message(errorMessage)
            .result(null)
            .build();
    }

    /**
     * 判断响应是否成功
     *
     * @return true 如果响应码为200
     */
    public boolean isSuccess() {
        return code != null && code == 200;
    }
}
