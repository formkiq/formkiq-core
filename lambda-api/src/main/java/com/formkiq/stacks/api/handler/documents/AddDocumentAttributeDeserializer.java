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
package com.formkiq.stacks.api.handler.documents;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Polymorphic deserializer for AddDocumentAttribute based on presence of specific keys.
 *
 * Priority: 1) classificationId -> AddDocumentAttributeClassification 2) relationship ->
 * AddDocumentAttributeRelationship 3) entityTypeId -> AddDocumentAttributeEntity 4) else ->
 * AddDocumentAttributeStandard
 */
public final class AddDocumentAttributeDeserializer
    implements JsonDeserializer<AddDocumentAttribute> {

  @Override
  public AddDocumentAttribute deserialize(final JsonElement json, final Type typeOfT,
      final JsonDeserializationContext ctx) throws JsonParseException {

    if (json == null || !json.isJsonObject()) {
      throw new JsonParseException("Expected JSON object for AddDocumentAttribute");
    }

    AddDocumentAttribute a;
    JsonObject o = json.getAsJsonObject();

    if (o.has("classificationId")) {
      a = ctx.deserialize(o, AddDocumentAttributeClassification.class);
    } else if (o.has("relationship")) {
      a = ctx.deserialize(o, AddDocumentAttributeRelationship.class);
    } else if (o.has("entityTypeId")) {
      a = ctx.deserialize(o, AddDocumentAttributeEntity.class);
    } else {
      a = ctx.deserialize(o, AddDocumentAttributeStandard.class);
    }

    return a;
  }
}
