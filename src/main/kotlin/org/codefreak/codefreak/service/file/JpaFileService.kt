package org.codefreak.codefreak.service.file

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.lang.StringUtils.startsWith
import org.codefreak.codefreak.config.AppConfiguration
import org.codefreak.codefreak.entity.FileCollection
import org.codefreak.codefreak.repository.FileCollectionRepository
import org.codefreak.codefreak.repository.FileCollectionRepository2
import org.codefreak.codefreak.service.EntityNotFoundException
import org.rauschig.jarchivelib.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermissions
import java.util.*


@Service
@ConditionalOnProperty(name = ["codefreak.files.adapter"], havingValue = "JPA")
class JpaFileService : FileService {

  @Autowired
  private lateinit var fileCollectionRepository: FileCollectionRepository

  override fun writeCollectionTar(collectionId: UUID): OutputStream {
    val collection = fileCollectionRepository.findById(collectionId).orElseGet { FileCollection(collectionId) }
    return object : ByteArrayOutputStream() {
      override fun close() {
        collection.tar = toByteArray()
        fileCollectionRepository.save(collection)
      }
    }
  }

  protected fun getCollection(collectionId: UUID): FileCollection = fileCollectionRepository.findById(collectionId)
      .orElseThrow { EntityNotFoundException("File not found") }

  override fun readCollectionTar(collectionId: UUID): InputStream {
    return ByteArrayInputStream(getCollection(collectionId).tar)
  }

  override fun readCollectionTar(collectionId: UUID, origin: PathResolver?): InputStream {
    TODO("Not yet implemented")
  }

  override fun writeCollectionTar(collectionId: UUID, destination: PathResolver?): OutputStream {
    TODO("Not yet implemented")
  }

  override fun collectionExists(collectionId: UUID): Boolean {
    return fileCollectionRepository.existsById(collectionId)
  }

  override fun deleteCollection(collectionId: UUID) {
    fileCollectionRepository.deleteById(collectionId)
  }

  override fun getCollectionMd5Digest(collectionId: UUID): ByteArray {
    return DigestUtils.md5Digest(getCollection(collectionId).tar)
  }
}


@Service
@ConditionalOnProperty(name = ["codefreak.files.adapter"], havingValue = "JPA_HDD")
class HDDFileService : FileService {
  @Autowired
  private lateinit var fileCollectionRepository: FileCollectionRepository2
  @Autowired
  private lateinit var config: AppConfiguration

  fun saveOnDisk(byteArray: ByteArray, destination: PathResolver): ByteArray
  {
    val input: ArchiveInputStream = ArchiveStreamFactory().createArchiveInputStream(BufferedInputStream(ByteArrayInputStream(byteArray)))
    try {
      var entry: ArchiveEntry?

      val permissions_folder =PosixFilePermissions.fromString(config.files.folderPermission)
      val permissions_files = PosixFilePermissions.fromString(config.files.filePermission)

      while ((input.nextEntry.also { entry = it }) != null) {
        val file: File = File(destination.path.toString(), entry!!.name)
        if (entry!!.isDirectory) {
          Files.createDirectories(file.toPath(),  PosixFilePermissions.asFileAttribute(permissions_folder))
          Files.setPosixFilePermissions(file.toPath(), permissions_folder)
        } else {
          Files.createDirectories(file.parentFile.toPath(),  PosixFilePermissions.asFileAttribute(permissions_folder))
          Files.setPosixFilePermissions(file.parentFile.toPath(), permissions_folder)
          IOUtils.copy(input, file)
          Files.setPosixFilePermissions(file.toPath(), permissions_files);
        }

        //FileModeMapper.map(entry, file)
      }
    } finally {
      IOUtils.closeQuietly(input)
    }
    return ("Path:" + destination.location).toByteArray()
  }

  fun readFromDisk(path: PathResolver): ByteArray
  {
    val paths: List<String> =  List<String>(1){path.path.toString()}
    val tarIn: ByteArrayOutputStream = createTar("tmp.tar", paths)
    return tarIn.toByteArray()
  }

  fun isDiskLink(byteArray: ByteArray): Boolean
  {
    return startsWith(String(byteArray, 0, 6), "Path:")
  }

  override fun writeCollectionTar(collectionId: UUID, destination: PathResolver?): OutputStream {
    if (destination == null || destination.path == null)
    {
      return writeCollectionTar(collectionId)
    }

    val collection = fileCollectionRepository.findById(collectionId).orElseGet { FileCollection(collectionId) }
    return object : ByteArrayOutputStream() {
      override fun close() {
        collection.tar = saveOnDisk(toByteArray(), destination)
        fileCollectionRepository.save(collection)
      }
    }
  }

  override fun writeCollectionTar(collectionId: UUID): OutputStream {
    val collection = fileCollectionRepository.findById(collectionId).orElseGet { FileCollection(collectionId) }
    return object : ByteArrayOutputStream() {
      override fun close() {
        collection.tar = toByteArray()
        fileCollectionRepository.save(collection)
      }
    }
  }

  protected fun getCollection(collectionId: UUID): FileCollection = fileCollectionRepository.findById(collectionId)
      .orElseThrow { EntityNotFoundException("File not found") }

  override fun readCollectionTar(collectionId: UUID, origin: PathResolver?): InputStream {
    val collection = getCollection(collectionId).tar
    if(isDiskLink(collection))
    {
      origin!!.location = Path.of(String(collection).substring(5))
      if (origin.path == null)
        Exception("Origin couldn't be resolved")
      return ByteArrayInputStream(readFromDisk(origin))
    }else {
      return ByteArrayInputStream(collection)
    }
  }

  override fun readCollectionTar(collectionId: UUID): InputStream {
    val collection = getCollection(collectionId).tar
    return ByteArrayInputStream(collection)
  }

  override fun collectionExists(collectionId: UUID): Boolean {
    return fileCollectionRepository.existsById(collectionId)
  }

  override fun deleteCollection(collectionId: UUID) {
    fileCollectionRepository.deleteById(collectionId)
  }

  override fun getCollectionMd5Digest(collectionId: UUID): ByteArray {
    return DigestUtils.md5Digest(getCollection(collectionId).tar)
  }
}
