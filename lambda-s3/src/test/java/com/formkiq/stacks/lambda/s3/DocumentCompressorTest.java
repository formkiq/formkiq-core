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
package com.formkiq.stacks.lambda.s3;

import static com.formkiq.aws.dynamodb.SiteIdKeyGenerator.createS3Key;
import static com.formkiq.stacks.lambda.s3.util.FileUtils.loadFileAsByteArray;
import static com.formkiq.testutils.aws.DynamoDbExtension.DOCUMENTS_TABLE;
import static com.formkiq.testutils.aws.TestServices.BUCKET_NAME;
import static com.formkiq.testutils.aws.TestServices.STAGE_BUCKET_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilder;
import com.formkiq.aws.dynamodb.DynamoDbConnectionBuilderExtension;
import com.formkiq.aws.dynamodb.model.DynamicDocumentItem;
import com.formkiq.aws.s3.S3ConnectionBuilder;
import com.formkiq.aws.s3.S3Service;
import com.formkiq.aws.s3.S3ServiceExtension;
import com.formkiq.module.lambdaservices.AwsServiceCache;
import com.formkiq.module.lambdaservices.ClassServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentService;
import com.formkiq.stacks.dynamodb.DocumentServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentServiceImpl;
import com.formkiq.stacks.dynamodb.DocumentVersionService;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceExtension;
import com.formkiq.stacks.dynamodb.DocumentVersionServiceNoVersioning;
import com.formkiq.testutils.aws.DynamoDbExtension;
import com.formkiq.testutils.aws.DynamoDbHelper;
import com.formkiq.testutils.aws.DynamoDbTestServices;
import com.formkiq.testutils.aws.LocalStackExtension;
import com.formkiq.testutils.aws.TestServices;

/**
 * Unit Test for {@link DocumentCompressor}.
 */
@ExtendWith(LocalStackExtension.class)
@ExtendWith(DynamoDbExtension.class)
public class DocumentCompressorTest {
  /** {@link S3ConnectionBuilder}. */
  // private static S3ConnectionBuilder s3Builder;
  /** {@link DynamoDbConnectionBuilder}. */
  private static DynamoDbConnectionBuilder dbBuilder;
  /** {@link DynamoDbHelper}. */
  private static DynamoDbHelper dbHelper;
  /** {@link S3Service}. */
  private static S3Service s3;
  /** {@link DocumentService}. */
  private static DocumentService documentService;
  /** {@link DocumentCompressor}. */
  private DocumentCompressor compressor;
  /** {@link AwsServiceCache}. */
  private static AwsServiceCache serviceCache;

  /**
   * Before Each Test.
   */
  @BeforeEach
  public void before() {
    dbHelper.truncateTable(DOCUMENTS_TABLE);
    s3.deleteAllFiles(STAGE_BUCKET_NAME);
    s3.deleteAllFiles(BUCKET_NAME);

    this.compressor = new DocumentCompressor(serviceCache);
  }

  /**
   * Before All Tests.
   * 
   * @throws Exception Exception
   */
  @BeforeAll
  public static void beforeClass() throws Exception {

    Map<String, String> env = new HashMap<>();
    env.put("DOCUMENTS_TABLE", DOCUMENTS_TABLE);
    env.put("DOCUMENT_VERSIONS_PLUGIN", DocumentVersionServiceNoVersioning.class.getName());

    S3ConnectionBuilder s3Builder = TestServices.getS3Connection(null);
    dbBuilder = DynamoDbTestServices.getDynamoDbConnection();

    dbHelper = DynamoDbTestServices.getDynamoDbHelper(null);
    s3 = new S3Service(s3Builder);
    documentService = new DocumentServiceImpl(dbBuilder, DOCUMENTS_TABLE,
        new DocumentVersionServiceNoVersioning());

    serviceCache = new AwsServiceCache().environment(env);
    serviceCache.register(S3ConnectionBuilder.class,
        new ClassServiceExtension<S3ConnectionBuilder>(s3Builder));
    serviceCache.register(DynamoDbConnectionBuilder.class,
        new DynamoDbConnectionBuilderExtension(dbBuilder));
    serviceCache.register(DocumentService.class, new DocumentServiceExtension());
    serviceCache.register(S3Service.class, new S3ServiceExtension());
    serviceCache.register(DocumentVersionService.class, new DocumentVersionServiceExtension());
  }

  @Test
  void testDocumentsCompress() throws Exception {
    final List<String> filePathsToCompress =
        Arrays.asList("/255kb-text.txt", "/256kb-text.txt", "/multipart01.txt", "/multipart02.txt");
    final Map<String, Long> fileChecksums = new HashMap<>();
    for (final String path : filePathsToCompress) {
      final byte[] content = loadFileAsByteArray(this, path);
      final Long contentChecksum = getContentChecksum(content);
      final String documentId = this.createDocument("default", "JohnDoe", content);
      fileChecksums.put(documentId, contentChecksum);
    }
    final String archiveKey = "tempfiles/665f0228-4fbc-4511-912b-6cb6f566e1c0.zip";
    final ArrayList<String> documentIds = new ArrayList<>(fileChecksums.keySet());

    this.compressor = new DocumentCompressor(serviceCache);
    this.compressor.compressDocuments("default", BUCKET_NAME, STAGE_BUCKET_NAME, archiveKey,
        documentIds);

    try (InputStream zipContent = s3.getContentAsInputStream(STAGE_BUCKET_NAME, archiveKey)) {
      // Check that all files are present and content checksum is the same
      validateZipContent(zipContent, fileChecksums);
    }
  }

  @Test
  void testDocumentsCompressMultipart() throws Exception {
    final String fileToCompress = "/multipart01.txt";
    final byte[] fileContent = loadFileAsByteArray(this, fileToCompress);
    final Long checksum = getContentChecksum(fileContent);
    final Map<String, Long> fileChecksums = new HashMap<>();
    final int filesNumber = 5;
    for (int i = 0; i < filesNumber; ++i) {
      final String docId = this.createDocument(null, "JaneDoe", fileContent);
      fileChecksums.put(docId, checksum);
    }
    final String archiveKey = "tempfiles/665f0228-4fbc-4511-912b-6cb6f566e1c0.zip";
    final ArrayList<String> documentIds = new ArrayList<>(fileChecksums.keySet());

    this.compressor.compressDocuments("default", BUCKET_NAME, STAGE_BUCKET_NAME, archiveKey,
        documentIds);

    try (InputStream zipContent = s3.getContentAsInputStream(STAGE_BUCKET_NAME, archiveKey)) {
      validateZipContent(zipContent, fileChecksums);
    }
  }

  private String createDocument(final String siteId, final String userId, final byte[] content) {
    final DynamicDocumentItem item = new DynamicDocumentItem(new HashMap<>());
    item.setDocumentId(UUID.randomUUID().toString());
    item.setUserId(userId);
    item.setInsertedDate(new Date());
    final String documentId = item.getDocumentId();
    documentService.saveDocument(siteId, item, null);
    final String key = createS3Key(siteId, documentId);
    s3.putObject(BUCKET_NAME, key, content, null, null);
    return item.getDocumentId();
  }

  /**
   * Validate Zip file Contents checksums.
   * 
   * @param input {@link InputStream}
   * @param expectedEntryChecksum {@link Map}
   * @throws IOException IOException
   */
  static void validateZipContent(final InputStream input,
      final Map<String, Long> expectedEntryChecksum) throws IOException {

    ZipInputStream stream = new ZipInputStream(input);

    int count = 0;
    for (ZipEntry entry = stream.getNextEntry(); entry != null; entry = stream.getNextEntry()) {

      final String name = entry.getName();
      final byte[] content = stream.readAllBytes();

      assertTrue(expectedEntryChecksum.containsKey(name));
      assertEquals(expectedEntryChecksum.get(name), getContentChecksum(content));

      stream.closeEntry();
      count++;
    }
    stream.close();
    assertEquals(count, expectedEntryChecksum.size());
  }

  /**
   * Generate checksum based on content.
   * 
   * @param content byte[]
   * @return {@link Long}
   */
  static Long getContentChecksum(final byte[] content) {
    final CRC32 crc = new CRC32();
    crc.update(content, 0, content.length);
    long checksum = crc.getValue();
    crc.reset();
    return Long.valueOf(checksum);
  }
}
