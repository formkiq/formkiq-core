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
package com.formkiq.stacks.dynamodb.attributes;

import com.formkiq.stacks.dynamodb.schemas.Schema;
import com.formkiq.stacks.dynamodb.schemas.SchemaAttributes;
import com.formkiq.stacks.dynamodb.schemas.SchemaService;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.formkiq.aws.dynamodb.objects.Objects.notNull;

/**
 * {@link Function} to convert {@link Collection} {@link DocumentAttributeRecord} to {@link List}
 * {@link SchemaAttributes}.
 */
public class DocumentAttributeRecordsToSchemaAttributes
    implements Function<Collection<DocumentAttributeRecord>, List<SchemaAttributes>> {

  /** {@link SchemaService}. */
  private final SchemaService schemaService;
  /** {@link String}. */
  private final String site;

  /**
   * constructor.
   * 
   * @param schema {@link SchemaService}
   * @param siteId {@link String}
   */
  public DocumentAttributeRecordsToSchemaAttributes(final SchemaService schema,
      final String siteId) {
    this.schemaService = schema;
    this.site = siteId;
  }

  @Override
  public List<SchemaAttributes> apply(
      final Collection<DocumentAttributeRecord> documentAttributeRecords) {

    Schema schema = this.schemaService.getSitesSchema(site);

    List<Schema> schemas = documentAttributeRecords.stream()
        .filter(a -> DocumentAttributeValueType.CLASSIFICATION.equals(a.getValueType()))
        .map(a -> this.schemaService.findClassification(site, a.getStringValue()))
        .map(this.schemaService::getSchema)
        .map(a -> this.schemaService.mergeSchemaIntoClassification(schema, a)).toList();

    if (schema != null && notNull(schemas).isEmpty()) {
      schemas = List.of(schema);
    }

    return schemas.stream().map(Schema::getAttributes).toList();
  }
}
