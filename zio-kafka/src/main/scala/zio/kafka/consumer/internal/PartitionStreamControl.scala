package zio.kafka.consumer.internal

import org.apache.kafka.common.TopicPartition
import zio._
import zio.kafka.consumer.internal.Runloop.ByteArrayCommittableRecord
import zio.stream.Take

private[internal] case class PartitionStreamControl(
  topicPartition: TopicPartition,
  interrupt: Promise[Throwable, Unit],
  drainQueue: Queue[Take[Nothing, ByteArrayCommittableRecord]],
  completed: Ref[Boolean]
) {

  def finishWith(remaining: Chunk[ByteArrayCommittableRecord]): ZIO[Any, Nothing, Unit] =
    for {
      _ <- drainQueue.offer(Take.chunk(remaining))
      _ <- drainQueue.offer(Take.end)
      _ <- interrupt.succeed(())
    } yield ()

  def isComplete: UIO[Boolean] = completed.get
  def complete: UIO[Unit]      = ZIO.logInfo(s"Completing TP ${topicPartition}") *> completed.set(true)
}
