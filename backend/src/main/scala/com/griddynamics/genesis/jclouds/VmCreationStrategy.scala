/**
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   @Project:     Genesis
 *   @Description: Execution Workflow Engine
 */
package com.griddynamics.genesis.jclouds

import com.griddynamics.genesis.model.{VirtualMachine, Environment}
import collection.JavaConversions.asScalaSet
import org.jclouds.ec2.compute.options.EC2TemplateOptions
import com.griddynamics.genesis.util.Logging
import org.jclouds.compute.domain.NodeState
import com.griddynamics.executors.provision.VmMetadataFuture
import org.jclouds.compute.options.TemplateOptions
import org.gdjclouds.provider.gdnova.v100.GDNovaTemplateOptions

trait VmCreationStrategy {
    def createVm(env: Environment, vm: VirtualMachine): VmMetadataFuture
}

class DefaultVmCreationStrategy(val pluginContext: JCloudsPluginContext) extends VmCreationStrategy with Logging {

    import DefaultVmCreationStrategy._

    val computeService = pluginContext.computeContext.getComputeService

    class DefaultVmMetadataFuture(instanceId : String) extends VmMetadataFuture {
        def getMetadata : Option[String] = {
            val node = Option(computeService.getNodeMetadata(instanceId))
            log.debug("Requested node metadata: '%s'", node)
            node.filter(_.getState == NodeState.RUNNING).map(_.getId)
        }
    }

    def createVm(env: Environment, vm: VirtualMachine) : VmMetadataFuture = {
        val nodes = computeService.createNodesInGroup(group(env, vm), 1, template(env, vm))
        new DefaultVmMetadataFuture(nodes.headOption.map(_.getId).get)
    }

    protected def template(env: Environment, vm: VirtualMachine) = {
        computeService.templateBuilder.imageId(vm.imageId.get)
            .hardwareId(vm.hardwareId.get)
            .options(templateOptions(env, vm))
            .build
    }

    protected def group(env: Environment, vm: VirtualMachine) = {
        "%s.%s.%s".format(pluginContext.nodeNamePrefix, java.lang.System.currentTimeMillis() / 1000,
            env.templateName.take(APP_NAME_MAXLEN)).take(VM_GROUP_MAXLEN)
    }

    protected def templateOptions(env: Environment, vm: VirtualMachine) = {
        computeService.templateOptions().blockUntilRunning(false)
    }
}

object DefaultVmCreationStrategy {
    val VM_GROUP_PREFIX = "genesis"
    val VM_GROUP_MAXLEN = 20
    val APP_NAME_MAXLEN = 4
}

class Ec2VmCreationStrategy(computeContext: JCloudsPluginContextImpl,
                            keyPair: String,
                            securityGroup: String) extends DefaultVmCreationStrategy(computeContext) {

//    import DefaultVmCreationStrategy._

/*
    override protected def group(env: Environment, vm: VirtualMachine) = {
        "%s.%s.%s.%d".format(VM_GROUP_PREFIX, env.name, vm.roleName, vm.id)
    }
*/

    override protected def templateOptions(env: Environment, vm: VirtualMachine) = {
        super.templateOptions(env, vm).asInstanceOf[EC2TemplateOptions]
            .keyPair(keyPair)
            .securityGroups(securityGroup)
    }
}

class GdNovaCreationStrategy(computeContext: JCloudsPluginContextImpl,
                            keyPair: String) extends DefaultVmCreationStrategy(computeContext) {

    override protected def templateOptions(env: Environment, vm: VirtualMachine): TemplateOptions = {
        super.templateOptions(env, vm).asInstanceOf[GDNovaTemplateOptions]
            .keyPair(keyPair);
    }
}
