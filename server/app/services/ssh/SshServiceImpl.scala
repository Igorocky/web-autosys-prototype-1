package services.ssh

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Singleton
import org.apache.commons.pool2.impl.{DefaultPooledObject, GenericObjectPool, GenericObjectPoolConfig}
import org.apache.commons.pool2.{ObjectPool, PooledObject, PooledObjectFactory}
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

@Singleton
class SshServiceImpl @Inject() (actorSystem: ActorSystem, appLifecycle: ApplicationLifecycle) extends SshService {
  val log: Logger = Logger(this.getClass())

  private final val EXECUTION_CONTEXT_CONFIG_NAME: String = "execution-contexts.ssh-service"
  private final val MAX_TOTAL = 1

  implicit private val executionContext = actorSystem.dispatchers.lookup(EXECUTION_CONTEXT_CONFIG_NAME)

  private var connections: Map[SshConnectionTag, ObjectPool[SshConnection]] = Map()

  appLifecycle.addStopHook{()=>
    close()
    Future.successful(())
  }

  override def executeCommand(connectionTag: SshConnectionTag, command: String,
                              params: Option[Map[String, String]] = None, prompt: Option[String] = None,
                              timeoutMillis: Option[Long] = None): Future[Array[String]] = Future {
    log.debug(s"executeCommand on ${connectionTag.name}: '$command'")
    val sshConnection: SshConnection = connections(connectionTag).borrowObject()
    try {
      val res = sshConnection.exec(command, params, prompt, timeoutMillis)
      log.debug(s"executeCommand on ${connectionTag.name} results in: '$res'")
      res
    } finally {
      connections(connectionTag).returnObject(sshConnection)
    }
  }

  override def connect(newConnectionTag: SshConnectionTag, host: String, port: Int, login: String, password: String): Future[Unit] = Future {
    log.debug(s"creating pool for: newConnectionTag = $newConnectionTag, host = $host, port = $port, login = $login")
    connections += newConnectionTag -> new GenericObjectPool(
      new PooledObjectFactory[SshConnection] {
        override def destroyObject(p: PooledObject[SshConnection]): Unit = p.getObject.close()

        override def validateObject(p: PooledObject[SshConnection]): Boolean = p.getObject.validate()

        override def activateObject(p: PooledObject[SshConnection]): Unit = {}

        override def passivateObject(p: PooledObject[SshConnection]): Unit = {}

        override def makeObject(): PooledObject[SshConnection] = {
          new DefaultPooledObject(new JSchSshConnection(host, port, login, password))
        }
      },
      new GenericObjectPoolConfig {
        setTestOnCreate(true)
        setTestOnBorrow(true)
        setBlockWhenExhausted(true)
        setMaxTotal(MAX_TOTAL)
      }
    )
    log.debug(s"pool created for: newConnectionTag = $newConnectionTag, host = $host, port = $port, login = $login")
    Right(())
  }

  def close() = {
    connections.foreach{case (tag, pool) => pool.close()}
  }
}
