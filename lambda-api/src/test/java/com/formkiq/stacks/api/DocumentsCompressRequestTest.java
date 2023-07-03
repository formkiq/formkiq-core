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
package com.formkiq.stacks.api;

import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class DocumentsCompressRequestTest extends AbstractRequestHandler {
  private Gson gson = null;
  private S3Service s3 = null;

  @Override
  @BeforeEach
  public void before() throws Exception {
    super.before();
    this.gson = GsonUtil.getInstance();
    this.s3 = new S3Service(TestServices.getS3Connection(null));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testHandlePostDocumentsCompress() throws Exception {
    ApiGatewayRequestEvent event = toRequestEvent("/request-post-documents-compress.json");
    final String response = handleRequest(event);

    final Map<String, String> responseMap = this.gson.fromJson(response, Map.class);
    final String responseStatus = String.valueOf(responseMap.get("statusCode"));
    assertEquals("201.0", responseStatus);
    final Map<String, Object> responseBody = this.gson.fromJson(responseMap.get("body"), Map.class);
    assertTrue(responseBody.containsKey("downloadUrl"));
    assertTrue(responseBody.containsKey("compressionId"));

    // S3
    final String compressionId = String.valueOf(responseBody.get("compressionId"));
    final String zipTaskS3Key = String.format("tempfiles/%s.json", compressionId);
    final String zipTaskS3File = this.s3.getContentAsString(STAGE_BUCKET_NAME, zipTaskS3Key, null);
    assertNotNull(zipTaskS3File);
    final Map<String, Object> s3FileMap = this.gson.fromJson(zipTaskS3File, Map.class);
    assertTrue(s3FileMap.containsKey("compressionId"));
    assertTrue(s3FileMap.containsKey("downloadUrl"));
    assertTrue(s3FileMap.containsKey("documentIds"));
  }
}
