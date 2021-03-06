/*
 * Gretty
 *
 * Copyright (C) 2013-2015 Andrey Hihlovskiy and contributors.
 *
 * See the file "LICENSE" for copying and usage permission.
 * See the file "CONTRIBUTORS" for complete list of contributors.
 */
package org.akhikhl.gretty

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 * @author akhikhl
 */
abstract class FarmServiceTask extends DefaultTask {

  private static Logger log = LoggerFactory.getLogger(FarmServiceTask)

  String farmName = ''

  Integer servicePort

  @TaskAction
  void action() {
    FarmConfigurer configurer = new FarmConfigurer(project)
    FarmExtension farm = new FarmExtension(project, servicePort: servicePort)
    configurer.configureFarm(farm, configurer.getProjectFarm(farmName))
    log.debug 'Sending command {} to port {}', command, farm.servicePort
    ServiceProtocol.send(farm.servicePort, command)
  }

  abstract String getCommand()
}
