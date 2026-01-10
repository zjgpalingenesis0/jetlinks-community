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
package org.jetlinks.community.ocr.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.ocr.model.OcrRequest;
import org.jetlinks.community.ocr.model.OcrResponse;
import org.jetlinks.community.ocr.model.OcrResult;
import org.jetlinks.community.ocr.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * OCR文字识别控制器
 *
 * @author JetLinks
 */
@RestController
@RequestMapping("/ocr")
@Resource(id = "ocr", name = "OCR文字识别")
@Tag(name = "OCR文字识别")
public class OcrController {

    private final OcrService ocrService;

    @Autowired
    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping("/recognize")
    @Authorize
    @ResourceAction(id = "recognize", name = "OCR识别")
    @Operation(summary = "OCR文字识别", description = "对已上传的图片进行文字识别")
    public Mono<OcrResponse> recognize(@RequestBody OcrRequest request) {
        return ocrService.recognize(request)
            .map(OcrResponse::success)
            .onErrorResume(e -> {
                String message = e.getMessage();
                if (e instanceof BusinessException) {
                    message = e.getMessage();
                } else {
                    message = "OCR识别失败: " + message;
                }
                return Mono.just(OcrResponse.error(message));
            });
    }

    @GetMapping("/recognize/{fileId}")
    @Authorize
    @ResourceAction(id = "recognize", name = "OCR识别")
    @Operation(summary = "根据文件ID进行OCR识别", description = "根据文件ID对图片进行文字识别")
    public Mono<OcrResponse> recognizeByFileId(
        @PathVariable @Parameter(description = "文件ID") String fileId,
        @RequestParam(required = false) @Parameter(description = "OCR提供商") String provider,
        @RequestParam(required = false) @Parameter(description = "是否检测方向") Boolean detectDirection,
        @RequestParam(required = false) @Parameter(description = "识别语言") String language
    ) {
        return ocrService.recognizeByFileId(fileId)
            .map(OcrResponse::success)
            .onErrorResume(e -> Mono.just(OcrResponse.error("OCR识别失败: " + e.getMessage())));
    }
}
