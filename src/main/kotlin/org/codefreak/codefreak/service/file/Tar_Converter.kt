package org.codefreak.codefreak.service.file

import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.io.IOUtils
import java.io.*
import java.util.*


@Throws(IOException::class)
fun createTar(tarName: String?, pathEntries: List<String>): ByteArrayOutputStream {
  val tarOutput: ByteArrayOutputStream = ByteArrayOutputStream()
  val tarArchive: ArchiveOutputStream = TarArchiveOutputStream(tarOutput)
  for (__file in pathEntries) {
    var _file = File(__file)
    val files: MutableList<File> = ArrayList<File>()
    files.addAll(recurseDirectory(_file)!!)
    if (_file.isDirectory() && !__file.endsWith(File.separator)) {
      //_file = _file.getParentFile()
    }
    for (file in files) {
      val tarArchiveEntry = TarArchiveEntry(file, _file.toURI().relativize(file.toURI()).getPath())
      tarArchiveEntry.size = file.length()
      tarArchive.putArchiveEntry(tarArchiveEntry)
      val fileInputStream = FileInputStream(file)
      IOUtils.copy(fileInputStream, tarArchive)
      fileInputStream.close()
      tarArchive.closeArchiveEntry()
    }
  }
  tarArchive.finish()
  tarOutput.close()
  return tarOutput
}

fun recurseDirectory(directory: File?): List<File>? {
  val files: MutableList<File> = ArrayList<File>()
  if (directory != null && directory.isDirectory()) {
    for (file in directory.listFiles()) {
      if (file.isDirectory()) {
        files.addAll(recurseDirectory(file)!!)
      } else {
        files.add(file)
      }
    }
  }
  return files
}
