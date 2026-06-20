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
package com.formkiq.aws.dynamodb.documents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.formkiq.validation.ValidationException;

import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DocumentRecordToDocumentRecordBuilder}.
 */
public class DocumentRecordToDocumentRecordBuilderTest {

  @Test
  void testApply01() {
    Date insertedDate = new Date();
    Date lastModifiedDate = new Date(insertedDate.getTime() + 1000);
    List<DocumentMetadata> metadata = List.of(new DocumentMetadata("key", "value", null));
    DocumentRecord record = DocumentRecord.builder().documentId("doc01").artifactId("artifact01")
        .artifactCategory("ocr").belongsToDocumentId("belongsTo01").path("test.txt")
        .deepLinkPath("https://example.com/test.txt").contentType("text/plain")
        .contentLength(Long.valueOf(123)).checksum("checksum").checksumType("SHA256")
        .s3version("s3version").userId("joe").version("v1").width("100").height("200")
        .timeToLive("1000").insertedDate(insertedDate).lastModifiedDate(lastModifiedDate)
        .metadata(metadata).build((String) null);

    // when
    DocumentRecord copy =
        new DocumentRecordToDocumentRecordBuilder().apply("parent01", record).build((String) null);

    // then
    assertEquals("parent01", copy.key().pk().substring("docs#".length()));
    assertEquals("doc01", copy.documentId());
    assertEquals("artifact01", copy.artifactId());
    assertEquals("ocr", copy.artifactCategory());
    assertEquals("belongsTo01", copy.belongsToDocumentId());
    assertEquals("test.txt", copy.path());
    assertEquals("https://example.com/test.txt", copy.deepLinkPath());
    assertEquals("text/plain", copy.contentType());
    assertEquals(Long.valueOf(123), copy.contentLength());
    assertEquals("checksum", copy.checksum());
    assertEquals("SHA256", copy.checksumType());
    assertEquals("s3version", copy.s3version());
    assertEquals("joe", copy.userId());
    assertEquals("v1", copy.version());
    assertEquals("100", copy.width());
    assertEquals("200", copy.height());
    assertEquals("1000", copy.timeToLive());
    assertEquals(insertedDate, copy.insertedDate());
    assertEquals(lastModifiedDate, copy.lastModifiedDate());
    assertEquals(metadata, copy.metadata());
  }

  @Test
  void testApply02() {
    assertThrows(ValidationException.class,
        () -> new DocumentRecordToDocumentRecordBuilder().apply(null, null));
  }
}
