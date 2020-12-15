package org.codefreak.codefreak.service.file

import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import org.springframework.util.DigestUtils
import org.springframework.util.StreamUtils

interface FileService {
  fun readCollectionTar(collectionId: UUID): InputStream
  fun readCollectionTar(collectionId: UUID, origin: PathResolver?): InputStream
  fun writeCollectionTar(collectionId: UUID, destination:PathResolver?): OutputStream
  fun writeCollectionTar(collectionId: UUID): OutputStream
  fun collectionExists(collectionId: UUID): Boolean
  fun deleteCollection(collectionId: UUID)

  fun copyCollection(oldId: UUID, newId: UUID) {
    readCollectionTar(oldId).use {
      writeCollectionTar(newId).use { out -> StreamUtils.copy(it, out) }
    }
  }

  fun getCollectionMd5Digest(collectionId: UUID): ByteArray {
    return readCollectionTar(collectionId).use { DigestUtils.md5Digest(it) }
  }

  fun getCollectionMd5Digest(collectionId: UUID, origin: PathResolver?): ByteArray {
    return readCollectionTar(collectionId, origin).use { DigestUtils.md5Digest(it) }
  }
}
