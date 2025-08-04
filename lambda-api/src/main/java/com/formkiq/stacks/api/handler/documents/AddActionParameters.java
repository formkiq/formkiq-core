/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.formkiq.stacks.api.handler.documents;

import com.formkiq.graalvm.annotations.Reflectable;
import com.formkiq.module.ocr.AwsTextractQuery;
import com.formkiq.module.ocr.OcrEngine;
import com.formkiq.module.ocr.OcrOutputType;

import java.util.List;

/**
 * Parameters for an action. Many are optional; types follow the OpenAPI spec.
 */
@Reflectable
public record AddActionParameters(List<AwsTextractQuery> ocrTextractQueries, String ocrParseTypes,
    OcrEngine ocrEngine, OcrOutputType ocrOutputType, String ocrNumberOfPages,
    String addPdfDetectedCharactersAsText, String url, String characterMax, String engine,
    String notificationType, String notificationToCc, String notificationToBcc,
    String notificationSubject, String notificationText, String notificationHtml, String tags,
    String mappingId, String eventBusName, String width, String height, String path,
    String outputType) {
}
