package services.initialisation

import javax.inject.{Inject, Singleton}

import services.ssh.{SshConnectionTag, SshService}

@Singleton
class Initialisation @Inject() (sshService: SshService) {
  sshService.connect(SshConnectionTag("localhost-tag"), "localhost", 18942, "igor", "igor")
}
