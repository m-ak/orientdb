package com.orientechnologies.orient.core.storage.impl.local.paginated.wal.component.sbtree.page.sbtreebucket;

import com.orientechnologies.orient.core.storage.impl.local.paginated.wal.OLogSequenceNumber;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class OSBTreeBucketRemoveOperationTest {
  @Test
  public void testArraySerialization() {
    OLogSequenceNumber lsn = new OLogSequenceNumber(34, 5);
    long fileId = 23;
    long pageIndex = 5;

    int entryIndex = 5;
    byte[] key = new byte[12];
    Random random = new Random();
    random.nextBytes(key);

    byte[] value = new byte[7];
    random.nextBytes(value);

    OSBTreeBucketRemoveOperation operation = new OSBTreeBucketRemoveOperation(lsn, fileId, pageIndex, entryIndex, key, value);
    int serializedSize = operation.serializedSize();
    byte[] content = new byte[serializedSize + 1];
    int offset = operation.toStream(content, 1);

    Assert.assertEquals(content.length, offset);

    OSBTreeBucketRemoveOperation restored = new OSBTreeBucketRemoveOperation();
    offset = restored.fromStream(content, 1);

    Assert.assertEquals(content.length, offset);

    Assert.assertEquals(lsn, restored.getPageLSN());
    Assert.assertEquals(fileId, restored.getFileId());
    Assert.assertEquals(pageIndex, restored.getPageIndex());

    Assert.assertEquals(entryIndex, restored.getEntryIndex());
    Assert.assertArrayEquals(key, restored.getKey());
    Assert.assertArrayEquals(value, restored.getValue());
  }

  @Test
  public void testBufferSerialization() {
    OLogSequenceNumber lsn = new OLogSequenceNumber(34, 5);
    long fileId = 23;
    long pageIndex = 5;

    int entryIndex = 5;
    byte[] key = new byte[12];
    Random random = new Random();
    random.nextBytes(key);

    byte[] value = new byte[7];
    random.nextBytes(value);

    OSBTreeBucketRemoveOperation operation = new OSBTreeBucketRemoveOperation(lsn, fileId, pageIndex, entryIndex, key, value);
    int serializedSize = operation.serializedSize();
    ByteBuffer buffer = ByteBuffer.allocate(serializedSize + 1).order(ByteOrder.nativeOrder());
    buffer.position(1);

    operation.toStream(buffer);

    Assert.assertEquals(serializedSize + 1, buffer.position());

    OSBTreeBucketRemoveOperation restored = new OSBTreeBucketRemoveOperation();
    int offset = restored.fromStream(buffer.array(), 1);

    Assert.assertEquals(serializedSize + 1, offset);

    Assert.assertEquals(lsn, restored.getPageLSN());
    Assert.assertEquals(fileId, restored.getFileId());
    Assert.assertEquals(pageIndex, restored.getPageIndex());

    Assert.assertEquals(entryIndex, restored.getEntryIndex());
    Assert.assertArrayEquals(key, restored.getKey());
    Assert.assertArrayEquals(value, restored.getValue());
  }
}