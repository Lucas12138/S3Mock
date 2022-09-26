package com.adobe.testing.s3mock.dto;

import static com.adobe.testing.s3mock.dto.DtoTestUtil.serializeAndAssert;

import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

class GetObjectAttributesOutputTest {

  @Test
  void testSerialization_object(TestInfo testInfo) throws IOException {
    GetObjectAttributesOutput iut = new GetObjectAttributesOutput(
        null,
        "etag",
        null,
        1L,
        StorageClass.STANDARD
    );

    serializeAndAssert(iut, testInfo);
  }

  @Test
  void testSerialization_multiPart(TestInfo testInfo) throws IOException {
    ObjectPart part = new ObjectPart(null,
        null,
        null,
        null,
        1L,
        1);
    GetObjectAttributesParts getObjectAttributesParts = new GetObjectAttributesParts(
        1000,
        false,
        0,
        0,
        0,
        Collections.singletonList(part)
    );
    GetObjectAttributesOutput iut = new GetObjectAttributesOutput(
        null,
        "etag",
        Collections.singletonList(getObjectAttributesParts),
        1L,
        StorageClass.STANDARD
    );

    serializeAndAssert(iut, testInfo);
  }

}
