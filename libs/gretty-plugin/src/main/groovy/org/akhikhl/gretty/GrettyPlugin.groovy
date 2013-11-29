/*
 * gretty
 *
 * Copyright 2013  Andrey Hihlovskiy.
 *
 * See the file "license.txt" for copying and usage permission.
 */
package org.akhikhl.gretty

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*

final class GrettyPlugin implements Plugin<Project> {

  void apply(final Project project) {

    project.extensions.create('gretty', GrettyPluginExtension)

    project.configurations {
      grettyConfig
    }

    project.dependencies {
      providedCompile 'javax.servlet:javax.servlet-api:3.0.1'
      grettyConfig 'org.akhikhl.gretty:gretty-helper:0.0.3'
    }

    for(String overlay in project.gretty.overlays)
      project.evaluationDependsOn(overlay)

    project.afterEvaluate {

      for(String overlay in project.gretty.overlays)
        project.dependencies.add 'providedCompile', project.project(overlay)

      project.task('prepareInplaceWebAppFolder', group: 'gretty', description: 'Copies webAppDir of this web-application and all WAR-overlays (if any) to ${buildDir}/webapp') {
        doLast {
          Runner.prepareInplaceWebAppFolder(project)
        }
      } // prepareInplaceWebAppFolder

      if(project.gretty.overlays) {

        String warFileName = project.tasks.war.archiveName
        project.ext.finalWarPath = project.tasks.war.archivePath

        project.tasks.war { archiveName 'thiswar.war' }

        // 'explodeWebApps' task is only activated by 'overlayWar' task
        project.task('explodeWebApps', group: 'gretty', description: 'Explodes this web-application and all WAR-overlays (if any) to ${buildDir}/webapp') {
          for(String overlay in project.gretty.overlays)
            dependsOn "${overlay}:assemble" as String
          dependsOn project.tasks.war
          doLast {
            for(String overlay in project.gretty.overlays) {
              def overlayProject = project.project(overlay)
              project.copy {
                def overlayWarFilePath = overlayProject.ext.properties.containsKey('finalWarPath') ? overlayProject.ext.finalWarPath : overlayProject.tasks.war.archivePath
                from overlayProject.zipTree(overlayWarFilePath)
                into "${project.buildDir}/webapp"
              }
            }
            project.copy {
              from project.zipTree(project.tasks.war.archivePath)
              into "${project.buildDir}/webapp"
            }
          }
        }

        // 'overlayWar' task is only activated by 'assemble' task
        project.task('overlayWar', type: Zip, group: 'gretty', description: 'Creates WAR from exploded web-application in ${buildDir}/webapp') {
          dependsOn project.tasks.explodeWebApps
          from project.fileTree("${project.buildDir}/webapp")
          destinationDir project.tasks.war.destinationDir
          archiveName warFileName
        }

        project.tasks.assemble.dependsOn project.tasks.overlayWar
      }

      def setupInplaceWebAppDependencies = { task ->
        task.dependsOn project.tasks.classes
        task.dependsOn project.tasks.prepareInplaceWebAppFolder
        for(String overlay in project.gretty.overlays)
          task.dependsOn "${overlay}:classes" as String
      }

      def setupWarDependencies = { task ->
        task.dependsOn project.tasks.war
        // need this for stable references to ${buildDir}/webapp folder,
        // independent from presence/absence of overlays and inplace/war start mode.
        if(!project.gretty.overlays)
          task.dependsOn project.tasks.prepareInplaceWebAppFolder
      }

      project.task('jettyRun', group: 'gretty', description: 'Starts jetty server inplace, in interactive mode (keypress stops the server).') { task ->
        setupInplaceWebAppDependencies task
        task.doLast {
          new Runner(project, inplace: true, interactive: true).consoleStart()
        }
      }

      project.task('jettyRunWar', group: 'gretty', description: 'Starts jetty server on WAR-file, in interactive mode (keypress stops the server).') { task ->
        setupWarDependencies task
        task.doLast {
          new Runner(project, inplace: false, interactive: true).consoleStart()
        }
      }

      project.task('jettyStart', group: 'gretty', description: 'Starts jetty server inplace, in batch mode (\'jettyStop\' stops the server).') { task ->
        setupInplaceWebAppDependencies task
        task.doLast {
          new Runner(project, inplace: true, interactive: false).consoleStart()
        }
      }

      project.task('jettyStartWar', group: 'gretty', description: 'Starts jetty server on WAR-file, in batch mode (\'jettyStop\' stops the server).') { task ->
        setupWarDependencies task
        task.doLast {
          new Runner(project, inplace: false, interactive: false).consoleStart()
        }
      }

      project.task('jettyStop', group: 'gretty', description: 'Sends \'stop\' command to running jetty server.') {
        doLast {
          Runner.sendServiceCommand project.gretty.servicePort, 'stop'
        }
      }

      project.task('jettyRestart', group: 'gretty', description: 'Sends \'restart\' command to running jetty server.') {
        doLast {
          Runner.sendServiceCommand project.gretty.servicePort, 'restart'
        }
      }
    } // afterEvaluate
  } // apply
} // plugin
