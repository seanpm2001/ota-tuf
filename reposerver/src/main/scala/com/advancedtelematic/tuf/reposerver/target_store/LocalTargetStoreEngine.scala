package com.advancedtelematic.tuf.reposerver.target_store

import java.io.File
import java.nio.file.Files

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.util.FastFuture
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import com.advancedtelematic.libtuf.data.TufDataType.{RepoId, TargetFilename}
import com.advancedtelematic.tuf.reposerver.http.Errors
import com.advancedtelematic.tuf.reposerver.target_store.TargetStoreEngine.{TargetBytes, TargetRetrieveResult, TargetStoreResult}
import org.slf4j.LoggerFactory

import scala.concurrent.Future

object LocalTargetStoreEngine {
  private val _log = LoggerFactory.getLogger(this.getClass)

  def apply(root: String)(implicit system: ActorSystem, mat: Materializer): LocalTargetStoreEngine = {
    val f = new File(root)
    if(!f.exists() && !f.getParentFile.canWrite) {
      throw new IllegalArgumentException(s"Could not open $root as local target store")
    } else if (!f.exists()) {
      Files.createDirectory(f.toPath)
      _log.info(s"Created local fs blob store directory: $root")
    }

    _log.info(s"local fs blob store set to $root")
    new LocalTargetStoreEngine(f)
  }
}

class LocalTargetStoreEngine(root: File)(implicit val system: ActorSystem, val mat: Materializer) extends TargetStoreEngine {
  import system.dispatcher

  val log = LoggerFactory.getLogger(this.getClass)

  override def store(repoId: RepoId, filename: TargetFilename, fileData: Source[ByteString, Any]): Future[TargetStoreResult] = {
    val sink = localFileSink(repoId, filename, fileData)
    write(fileData, sink)
  }

  override def retrieve(repoId: RepoId, filename: TargetFilename): Future[TargetRetrieveResult] = {
    val storePath = root.toPath.resolve(storageFilename(repoId, filename))

    if(!storePath.toFile.canRead)
      Future.failed(Errors.TargetNotFoundError)
    else {
      val size = storePath.toFile.length()

      val source = FileIO.fromPath(storePath).mapMaterializedValue {
        _.flatMap { ioResult =>
          if (ioResult.wasSuccessful)
            FastFuture.successful(Done)
          else
            FastFuture.failed(ioResult.getError)
        }
      }

      Future.successful(TargetBytes(source, size))
    }
  }

  protected def localFileSink(repoId: RepoId,
                              filename: TargetFilename,
                              fileData: Source[ByteString, Any]): Sink[ByteString, Future[(Uri, Long)]] = {
    val storePath = root.toPath.resolve(storageFilename(repoId, filename))

    Files.createDirectories(storePath.getParent)

    val uri = Uri(storePath.toAbsolutePath.toString)

    FileIO.toPath(storePath).mapMaterializedValue {
      _.flatMap { result =>
        if(result.wasSuccessful)
          Future.successful((uri, result.count))
        else
          Future.failed(result.getError)
      }
    }
  }
}