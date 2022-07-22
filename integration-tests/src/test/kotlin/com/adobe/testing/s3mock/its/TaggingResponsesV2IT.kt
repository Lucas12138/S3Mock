/*
 *  Copyright 2017-2022 Adobe.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.adobe.testing.s3mock.its

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.Tag
import software.amazon.awssdk.services.s3.model.Tagging

class TaggingResponsesV2IT : S3TestBase() {
  private val bucket = INITIAL_BUCKET_NAMES.iterator().next()

  /**
   * Verify that tagging can be obtained and returns expected content.
   */
  @Test
  fun testObjectTaggingWithPutObjectRequest() {
    s3ClientV2!!.putObject(
      { b: PutObjectRequest.Builder -> b.bucket(bucket).key("foo").tagging("msv=foo") },
      RequestBody.fromString("foo")
    )

    assertThat(s3ClientV2!!.getObjectTagging { b: GetObjectTaggingRequest.Builder ->
      b.bucket(
        bucket
      ).key("foo")
    }
      .tagSet())
      .contains(Tag.builder().key("msv").value("foo").build())
  }

  /**
   * Verify that tagging with multiple tags can be obtained and returns expected content.
   */
  @Test
  fun testObjectTaggingWithPutObjectRequest_multipleTags() {
    val tag1 = Tag.builder().key("tag1").value("foo").build()
    val tag2 = Tag.builder().key("tag2").value("bar").build()

    s3ClientV2!!.putObject(
      { b: PutObjectRequest.Builder ->
        b.bucket(bucket).key("multipleFoo")
        .tagging(Tagging.builder().tagSet(tag1, tag2).build())
      }, RequestBody.fromString("multipleFoo")
    )

    assertThat(s3ClientV2!!.getObjectTagging { b: GetObjectTaggingRequest.Builder ->
      b.bucket(
        bucket
      ).key("multipleFoo")
    }
      .tagSet())
      .contains(
        tag1,
        tag2
      )
  }
}
