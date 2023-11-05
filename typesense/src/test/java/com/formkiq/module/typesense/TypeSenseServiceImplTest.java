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
package com.formkiq.module.typesense;

import static com.formkiq.testutils.aws.TypesenseExtension.API_KEY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.testutils.aws.TypesenseExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;

/**
 * 
 * Unit Tests for {@link TypeSenseService}.
 *
 */
@ExtendWith(TypesenseExtension.class)
class TypeSenseServiceImplTest {

  /** {@link TypeSenseService}. */
  private TypeSenseService service;
  /** {@link AwsCredentials}. */
  private AwsCredentials credentials = AwsBasicCredentials.create("ABC", "XYZ");

  @BeforeEach
  public void beforeEach() {
    this.service =
        new TypeSenseServiceImpl("http://localhost:" + TypesenseExtension.getMappedPort(), API_KEY,
            Region.US_EAST_1, this.credentials);
  }

  /**
   * Add a document.
   * 
   * @throws Exception Exception
   */
  @Test
  void testAddDocument01() throws Exception {
    // given
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {
      String documentId = UUID.randomUUID().toString();
      final int maxResults = 10;

      Map<String, Object> apply =
          Map.of("path", "/something/else/My Document.pdf", "metadata#", "");

      // when
      this.service.addOrUpdateDocument(siteId, documentId, apply);

      // then
      List<String> documentIds = this.service.searchFulltext(siteId, "My Document", maxResults);

      // then
      assertFalse(documentIds.isEmpty());
      assertTrue(documentIds.contains(documentId));

      // given
      String text = "Newstuff.pdf";

      // when
      this.service.updateDocument(siteId, documentId, Map.of("path", text));

      // then
      documentIds = this.service.searchFulltext(siteId, "My Document", maxResults);
      assertFalse(documentIds.contains(documentId));

      documentIds = this.service.searchFulltext(siteId, text, maxResults);
      assertTrue(documentIds.contains(documentId));
    }
  }

  /**
   * Add a document.
   * 
   * @throws Exception Exception
   */
  @Test
  void testSearch01() throws Exception {
    // given
    String text = UUID.randomUUID().toString();
    final int maxResults = 10;
    for (String siteId : Arrays.asList(null, UUID.randomUUID().toString())) {

      // when
      List<String> documentIds = this.service.searchFulltext(siteId, text, maxResults);

      // then
      assertTrue(documentIds.isEmpty());
    }
  }
}
