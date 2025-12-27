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
package com.formkiq.stacks.dynamodb.folders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.formkiq.aws.dynamodb.folders.FolderIndexRecord;
import org.junit.jupiter.api.Test;

/**
 * Unit Test for {@link FolderIndexRecord}.
 */
class FolderIndexRecordTest {

  @Test
  void createIndexKeyReturnsParentIdAndPath() {
    FolderIndexRecord r =
        new FolderIndexRecord().parentDocumentId("parent123").path("/a/b/c.txt").type("file");

    String indexKey = r.createIndexKey("site1");
    assertEquals("parent123#" + "/a/b/c.txt", indexKey);
  }

  @Test
  void pkGsi1ThrowsWhenDocumentIdMissing() {
    FolderIndexRecord r =
        new FolderIndexRecord().parentDocumentId("p1").path("/a/b").type("folder");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> r.pkGsi1("site1"));
    assertEquals("'documentId' is required", ex.getMessage());
  }

  @Test
  void pkGsi2BucketUsesFirstTwoCharactersOfFilenameNormalized() {
    FolderIndexRecord r =
        new FolderIndexRecord().parentDocumentId("p1").path("/Some/Path/Report.PDF").type("file");

    String pk = r.pkGsi2("site1");
    assertEquals("global#filename#re", pk);
  }

  @Test
  void pkGsi2UsesFilenameLowercaseAndLeftPadsBucketToTwoChars() {
    // filename "A.pdf" -> normalized "a.pdf" -> leftPad to 2 => "a_"
    FolderIndexRecord r =
        new FolderIndexRecord().parentDocumentId("p1").path("/Some/Path/A.PDF").type("file");

    String pk = r.pkGsi2("site1");
    assertEquals("global#filename#a.", pk);
  }

  @Test
  void pkThrowsWhenParentDocumentIdMissing() {
    FolderIndexRecord r = new FolderIndexRecord().path("/a/b/c.txt").type("file");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> r.pk("site1"));
    assertEquals("'parentDocumentId' is required", ex.getMessage());
  }

  @Test
  void skBuildsFileAndFolderPrefixesAndLowercasesPath() {
    FolderIndexRecord file =
        new FolderIndexRecord().parentDocumentId("p1").path("/A/B/Report.PDF").type("file");

    assertEquals(FolderIndexRecord.INDEX_FILE_SK + "/a/b/report.pdf", file.sk());

    FolderIndexRecord folder =
        new FolderIndexRecord().parentDocumentId("p1").path("/A/B/MyFolder").type("folder");

    assertEquals(FolderIndexRecord.INDEX_FOLDER_SK + "/a/b/myfolder", folder.sk());
  }

  @Test
  void skGsi2UsesFileOrFolderPrefixAndLowercasesFilename() {
    FolderIndexRecord file =
        new FolderIndexRecord().parentDocumentId("p1").path("/a/b/Report.PDF").type("file");

    assertEquals(FolderIndexRecord.INDEX_FILE_SK + "report.pdf", file.skGsi2());

    FolderIndexRecord folder =
        new FolderIndexRecord().parentDocumentId("p1").path("/a/b/MyFolder").type("folder");

    assertEquals(FolderIndexRecord.INDEX_FOLDER_SK + "myfolder", folder.skGsi2());
  }

  @Test
  void skThrowsWhenPathOrTypeMissing() {
    FolderIndexRecord r1 = new FolderIndexRecord().type("file").parentDocumentId("p1");
    IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, r1::sk);
    assertEquals("'path' and 'type' is required", ex1.getMessage());

    FolderIndexRecord r2 = new FolderIndexRecord().path("/a/b.txt").parentDocumentId("p1");
    IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, r2::sk);
    assertEquals("'path' and 'type' is required", ex2.getMessage());
  }
}
