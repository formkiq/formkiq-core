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
package com.formkiq.testutils.aws;

import com.formkiq.client.api.SchemasApi;
import com.formkiq.client.invoker.ApiClient;
import com.formkiq.client.invoker.ApiException;
import com.formkiq.client.model.AttributeSchemaCompositeKey;
import com.formkiq.client.model.AttributeSchemaOptional;
import com.formkiq.client.model.SchemaAttributes;
import com.formkiq.client.model.SetSitesSchemaRequest;

import java.util.Collections;
import java.util.List;

public class FkqSchemaService {

  /**
   * Add Composite Key.
   *
   * @param client {@link ApiClient}
   * @param siteId {@link String}
   * @param name {@link String}
   * @param compositeKeys {@link List} {@link String}
   * @throws ApiException ApiException
   */
  public static void setSitesSchema(final ApiClient client, final String siteId, final String name,
      final List<String> compositeKeys) throws ApiException {

    SchemasApi api = new SchemasApi(client);

    List<AttributeSchemaOptional> optionalKeys =
        compositeKeys.stream().map(key -> new AttributeSchemaOptional().attributeKey(key)).toList();

    SchemaAttributes attributes = new SchemaAttributes().allowAdditionalAttributes(Boolean.TRUE)
        .optional(optionalKeys).compositeKeys(Collections
            .singletonList(new AttributeSchemaCompositeKey().attributeKeys(compositeKeys)));
    api.setSitesSchema(siteId, new SetSitesSchemaRequest().name(name).attributes(attributes));
  }
}
