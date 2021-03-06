package org.sisioh.dddbase.event.lifecycle

import org.specs2.mutable.Specification
import org.sisioh.dddbase.core.lifecycle.memory.mutable.sync.GenericSyncRepositoryOnMemory
import org.sisioh.dddbase.core.model.{EntityCloneable, Entity, Identity}
import java.util.UUID
import org.sisioh.dddbase.event.sync.SyncDomainEventSubscriber
import scala.util.{Success, Try}
import org.sisioh.dddbase.core.lifecycle.EntityIOContext
import org.sisioh.dddbase.core.lifecycle.sync.SyncEntityIOContext

class SyncRepositoryEventSupportSpec extends Specification {

  class EntityImpl(val identity: Identity[UUID]) extends Entity[Identity[UUID]]
    with EntityCloneable[Identity[UUID], EntityImpl]
    with Ordered[EntityImpl] {
    def compare(that: EntityImpl): Int = {
      identity.value.compareTo(that.identity.value)
    }
  }

  class TestRepository extends GenericSyncRepositoryOnMemory[Identity[UUID], EntityImpl]
  with SyncRepositoryEventSupport[Identity[UUID], EntityImpl] {
    protected def createEntityIOEvent(entity: EntityImpl, eventType: EventType.Value):
    EntityIOEvent[Identity[UUID], EntityImpl] = new EntityIOEvent[Identity[UUID], EntityImpl](Identity(UUID.randomUUID()), entity, eventType)
  }

  implicit val ctx = SyncEntityIOContext

  "event subscriber" should {
    "receive entity io event" in {
      var result: Boolean = false
      var resultEntity: EntityImpl = null
      var resultEventType: EventType.Value = null
      val repos = new TestRepository()
      val entity = new EntityImpl(Identity(UUID.randomUUID()))
      repos.subscribe(new SyncDomainEventSubscriber[EntityIOEvent[Identity[UUID], EntityImpl], Unit] {
        def handleEvent(event: EntityIOEvent[Identity[UUID], EntityImpl])(implicit ctx: EntityIOContext[Try]): Try[Unit] = {
          result = true
          resultEntity = event.entity
          resultEventType = event.eventType
          Success(())
        }
      })
      repos.store(entity)
      result must beTrue
      resultEntity must_== entity
      resultEventType must_== EventType.Store
    }
  }

}
