/**
 * MIT License
 * 
 * Copyright (c) 2018 - 2020 FormKiQ
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.formkiq.module.lambda.ocr.handlers;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.aws.services.lambda.ApiResponseStatus.SC_OK;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiAuthorization;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEventUtil;
import com.formkiq.aws.services.lambda.ApiGatewayRequestHandler;
import com.formkiq.aws.services.lambda.ApiMapResponse;
import com.formkiq.aws.services.lambda.ApiRequestHandlerResponse;
import com.formkiq.aws.services.lambda.exceptions.DocumentNotFoundException;
import com.formkiq.module.lambda.ocr.pdf.PdfService;
import com.formkiq.module.lambda.ocr.pdf.PdfServicePdfBox;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/** {@link ApiGatewayRequestHandler} for "/objects/examine/{id}/pdf". */
public class ObjectExaminePdfIdHandler
    implements ApiGatewayRequestHandler, ApiGatewayRequestEventUtil {

  /** {@link ObjectExaminePdfIdHandler} URL. */
  public static final String URL = "/objects/examine/{id}/pdf";
  /** {@link PdfService}. */
  private PdfService service = new PdfServicePdfBox();

  private Map<String, Object> getFileInfo(final AwsServiceCache awsservice, final String siteId,
      final String id) throws IOException {

    Map<String, Object> fields = new HashMap<>();
    String bucket = awsservice.environment("STAGE_DOCUMENTS_S3_BUCKET");
    String s3key = String.format("tempfiles/%s", createS3Key(siteId, id));

    S3Service s3 = awsservice.getExtension(S3Service.class);

    try {
      try (InputStream is = s3.getContentAsInputStream(bucket, s3key)) {
        try (PDDocument document = PDDocument.load(is)) {
          Map<String, String> documentFields = this.service.getFields(document);
          List<Map<String, String>> fieldList = documentFields.entrySet().stream()
              .map(e -> Map.of("field", e.getKey(), "value", e.getValue()))
              .collect(Collectors.toList());
          fields.put("fields", fieldList);
        }
      }
    } catch (NoSuchKeyException e) {
      fields = null;
    }

    return fields;
  }

  @Override
  public String getRequestUrl() {
    return URL;
  }

  @Override
  public ApiRequestHandlerResponse post(final LambdaLogger logger,
      final ApiGatewayRequestEvent event, final ApiAuthorization authorization,
      final AwsServiceCache awsservice) throws Exception {

    String siteId = authorization.getSiteId();
    String id = event.getPathParameters().get("id");

    Map<String, Object> fileinfo = getFileInfo(awsservice, siteId, id);

    if (fileinfo == null) {
      throw new DocumentNotFoundException(id);
    }

    Map<String, Object> map = Map.of("fileinfo", fileinfo);
    ApiMapResponse resp = new ApiMapResponse(map);
    return new ApiRequestHandlerResponse(SC_OK, resp);
  }
}
