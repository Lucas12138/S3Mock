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

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.GetObjectRetentionRequest
import software.amazon.awssdk.services.s3.model.ObjectLockRetention
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRetentionRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.MILLIS

internal class RetentionV2IT : S3TestBase() {

  @Test
  fun testGetRetentionNoBucketLockConfiguration(testInfo: TestInfo) {
    val sourceKey = UPLOAD_FILE_NAME
    val (bucketName, _) = givenBucketAndObjectV2(testInfo, sourceKey)

    Assertions.assertThatThrownBy {
      s3ClientV2.getObjectRetention(
        GetObjectRetentionRequest
          .builder()
          .bucket(bucketName)
          .key(sourceKey)
          .build()
      )
    }.isInstanceOf(S3Exception::class.java)
      .hasMessageContaining("Object Lock configuration does not exist for this bucket")
      .hasMessageContaining("Service: S3, Status Code: 404")
  }

  @Test
  fun testGetRetentionNoObjectLockConfiguration(testInfo: TestInfo) {
    val uploadFile = File(UPLOAD_FILE_NAME)
    val sourceKey = UPLOAD_FILE_NAME
    val bucketName = bucketName(testInfo)
    s3ClientV2.createBucket(
      CreateBucketRequest.builder().bucket(bucketName)
        .objectLockEnabledForBucket(true).build()
    )
    s3ClientV2.putObject(
      PutObjectRequest.builder().bucket(bucketName).key(sourceKey).build(),
      RequestBody.fromFile(uploadFile)
    )
    Assertions.assertThatThrownBy {
      s3ClientV2.getObjectRetention(
        GetObjectRetentionRequest
          .builder()
          .bucket(bucketName)
          .key(sourceKey)
          .build()
      )
    }.isInstanceOf(S3Exception::class.java)
      .hasMessageContaining("The specified object does not have a ObjectLock configuration")
      .hasMessageContaining("Service: S3, Status Code: 404")
  }

  @Test
  fun testPutAndGetRetention(testInfo: TestInfo) {
    val uploadFile = File(UPLOAD_FILE_NAME)
    val sourceKey = UPLOAD_FILE_NAME
    val bucketName = bucketName(testInfo)
    s3ClientV2.createBucket(
      CreateBucketRequest
        .builder()
        .bucket(bucketName)
        .objectLockEnabledForBucket(true)
        .build()
    )
    s3ClientV2.putObject(
      PutObjectRequest.builder().bucket(bucketName).key(sourceKey).build(),
      RequestBody.fromFile(uploadFile)
    )

    val retainUntilDate = Instant.now().plus(1, DAYS)
    s3ClientV2.putObjectRetention(
      PutObjectRetentionRequest
        .builder()
        .bucket(bucketName)
        .key(sourceKey)
        .retention(
          ObjectLockRetention.builder()
            .mode(ObjectLockRetentionMode.COMPLIANCE)
            .retainUntilDate(retainUntilDate)
            .build()
        )
        .build()
    )

    val retention = s3ClientV2.getObjectRetention(
      GetObjectRetentionRequest
        .builder()
        .bucket(bucketName)
        .key(sourceKey)
        .build()
    )
    assertThat(retention.retention().mode()).isEqualTo(ObjectLockRetentionMode.COMPLIANCE)
    //the returned date has MILLIS resolution, the local instant is in NANOS.
    assertThat(retention.retention().retainUntilDate())
      .isCloseTo(
        retainUntilDate, within(1, MILLIS)
      )
  }

  @Test
  fun testPutInvalidRetentionUntilDate(testInfo: TestInfo) {
    val uploadFile = File(UPLOAD_FILE_NAME)
    val sourceKey = UPLOAD_FILE_NAME
    val bucketName = bucketName(testInfo)
    s3ClientV2.createBucket(
      CreateBucketRequest
        .builder()
        .bucket(bucketName)
        .objectLockEnabledForBucket(true)
        .build()
    )
    s3ClientV2.putObject(
      PutObjectRequest.builder().bucket(bucketName).key(sourceKey).build(),
      RequestBody.fromFile(uploadFile)
    )

    val invalidRetainUntilDate = Instant.now().minus(1, DAYS)
    Assertions.assertThatThrownBy {
      s3ClientV2.putObjectRetention(
        PutObjectRetentionRequest
          .builder()
          .bucket(bucketName)
          .key(sourceKey)
          .retention(
            ObjectLockRetention.builder()
              .mode(ObjectLockRetentionMode.COMPLIANCE)
              .retainUntilDate(invalidRetainUntilDate)
              .build()
          )
          .build()
      )
    }.isInstanceOf(S3Exception::class.java)
      .hasMessageContaining("The retain until date must be in the future!")
      .hasMessageContaining("Service: S3, Status Code: 400")
  }
}
