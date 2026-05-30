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
package com.formkiq.testutils.dbloader;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link Function} to convert CSV {@link Path} to {@link List} {@link AttributeValue}.
 */
public class CsvToAttributeValueMap implements Function<Path, List<Map<String, AttributeValue>>> {
  @Override
  public List<Map<String, AttributeValue>> apply(final Path path) {

    List<Map<String, AttributeValue>> list = new ArrayList<>();

    try (Reader in = new FileReader(path.toFile(), StandardCharsets.UTF_8)) {

      CSVFormat format =
          CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).get();

      for (CSVRecord record : format.parse(in)) {
        Map<String, AttributeValue> attrs = record.toMap().entrySet().stream()
            .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
            .collect(Collectors.toMap(Map.Entry::getKey, e -> AttributeValue.fromS(e.getValue())));

        list.add(attrs);
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return list;
  }
}
