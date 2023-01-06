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

import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.dynamodb.model.DocumentTag;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.DocumentItemDynamoDb;
import com.formkiq.stacks.dynamodb.GlobalIndexService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /indices/search. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class IndicesSearchRequestTest extends AbstractRequestHandler {

  /** {@link GlobalIndexService}. */
  private GlobalIndexService indexWriter;

  /**
   * /indices/search by index Type "tags".
   *
   * @throws Exception an error has occurred
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testHandleSearchRequest01() throws Exception {

    this.indexWriter =
        new GlobalIndexService(DynamoDbTestServices.getDynamoDbConnection(null), DOCUMENTS_TABLE);

    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      // given
      Date now = new Date();
      String username = "joe";

      String documentId = UUID.randomUUID().toString();

      this.indexWriter.writeTagIndex(siteId, Arrays.asList("categoryId"));

      DocumentItemDynamoDb document = new DocumentItemDynamoDb(documentId, now, username);
      document.setPath("something/path.txt");
      Collection<DocumentTag> tags =
          Arrays.asList(new DocumentTag(documentId, "personId", "111", now, username),
              new DocumentTag(documentId, "categoryId", "555", now, username));
      getDocumentService().saveDocument(siteId, document, tags);

      String indexType = "tags";

      ApiGatewayRequestEvent event = toRequestEvent("/request-post-indices-search01.json");
      addParameter(event, "siteId", siteId);
      event.setIsBase64Encoded(Boolean.FALSE);
      event.setBody(GsonUtil.getInstance().toJson(Map.of("indexType", indexType)));

      // when
      String response = handleRequest(event);

      // then
      Map<String, String> m = fromJson(response, Map.class);
      assertEquals("200.0", String.valueOf(m.get("statusCode")));
      DynamicObject resp = new DynamicObject(fromJson(m.get("body"), Map.class));

      List<DynamicObject> documents = resp.getList("values");
      assertEquals(2, documents.size());
      assertEquals("categoryId", documents.get(0).get("value"));
      assertEquals("personId", documents.get(1).get("value"));
    }
  }
}
