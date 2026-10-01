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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.formkiq.aws.dynamodb.ID;
import com.formkiq.aws.dynamodb.documents.DocumentArtifact;
import com.formkiq.aws.dynamodb.documents.DocumentRecordBuilder;
import com.formkiq.aws.dynamodb.model.DocumentRecordSet;
import com.formkiq.aws.dynamodb.model.DocumentTagRecord;
import com.formkiq.aws.dynamodb.model.DocumentTagRecordBuilder;
import com.formkiq.stacks.dynamodb.SaveDocumentOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamicObject;
import com.formkiq.aws.services.lambda.ApiGatewayRequestEvent;
import com.formkiq.lambda.apigateway.util.GsonUtil;
import com.formkiq.stacks.dynamodb.GlobalIndexService;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;

/** Unit Tests for request /indices/search. */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class IndicesSearchRequestTest extends AbstractRequestHandler {

  /**
   * /indices/search by index Type "tags".
   *
   * @throws Exception an error has occurred
   */
  @Test
  public void testHandleSearchRequest01() throws Exception {

    GlobalIndexService indexWriter =
        new GlobalIndexService(DynamoDbTestServices.getDynamoDbConnection(), DOCUMENTS_TABLE);

    for (String siteId : Arrays.asList(null, ID.uuid())) {
      // given
      Date now = new Date();
      String username = "joe";

      var document = DocumentArtifact.of(ID.uuid(), null);

      indexWriter.writeTagIndex(siteId, new ArrayList<>(List.of("categoryId")));

      var item = new DocumentRecordBuilder().document(document).insertedDate(now).userId(username)
          .path("something/path.txt").build(siteId);

      Collection<DocumentTagRecord> tags0 = new DocumentTagRecordBuilder().document(document)
          .tagKey("personId").tagValue("111").insertedDate(now).userId(username).build(siteId);
      Collection<DocumentTagRecord> tags1 = new DocumentTagRecordBuilder().document(document)
          .tagKey("categoryId").tagValue("555").insertedDate(now).userId(username).build(siteId);
      var drs = new DocumentRecordSet(item, null,
          Stream.concat(tags0.stream(), tags1.stream()).toList(), null);
      getDocumentService().saveDocument(siteId, drs, new SaveDocumentOptions());

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
