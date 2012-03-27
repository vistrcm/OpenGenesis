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
package com.griddynamics.genesis.chef

import com.griddynamics.genesis.workflow._
import com.griddynamics.genesis.chef.step.ChefRun
import com.griddynamics.genesis.chef.action._
import com.griddynamics.genesis.exec.action._
import com.griddynamics.genesis.exec._
import com.griddynamics.genesis.plugin.{GenesisStepResult, StepExecutionContext}
import com.griddynamics.genesis.model.VmStatus

class ChefRunCoordinator(val step : ChefRun,
                         stepContext : StepExecutionContext,
                         execPluginContext : ExecPluginContext,
                         chefPluginContext : ChefPluginContext) extends ActionOrientedStepCoordinator{
    var isStepFailed = false

    def onStepStart() = {
        val (tVms, clearVms) = stepContext.vms(step).filter(_.status == VmStatus.Ready).partition(_.get(ExecVmAttrs.HomeDir).isDefined)
        val (chefVms, execVms) = tVms.partition(_.get(ChefVmAttrs.ChefNodeName).isDefined)

        val clearActions = clearVms.map(InitExecNode(stepContext.env, _))
        val execActions = execVms.map(InitChefNode(stepContext.env, _))
        val chefActions = chefVms.map(PrepareRegularChefRun(stepContext.step.id.toString, stepContext.env,
                                                            _, step.runList, step.jattrs))

        clearActions ++ execActions ++ chefActions
    }

    def onActionFinish(result : ActionResult) = result match {
        case _ if isStepFailed => {
            Seq()
        }
        case a @ ExecFinished(_, _) if (!a.isExecSuccess) => {
            isStepFailed = true
            Seq()
        }
        case ExecInitFail(a) => {
            stepContext.updateVm(a.vm)
            isStepFailed = true
            Seq()
        }

        case ExecInitSuccess(a) => {
            stepContext.updateVm(a.vm)
            Seq(InitChefNode(a.env, a.vm))
        }
        case ChefInitSuccess(a, d) => {
            stepContext.updateVm(a.vm)
            Seq(RunPreparedExec(d, a))
        }
        case ChefRunPrepared(a, d) => {
            Seq(RunPreparedExec(d, a))
        }
        case ExecFinished(RunPreparedExec(details, _ : InitChefNode), _) => {
            Seq(PrepareInitialChefRun(details.env, details.vm))
        }
        case ExecFinished(RunPreparedExec(details, _ : PrepareInitialChefRun), _) => {
            val label = "%d.%d.%s".format(stepContext.workflow.id, stepContext.step.id,
                                          stepContext.step.phase)

            Seq(PrepareRegularChefRun(label, details.env, details.vm, step.runList, step.jattrs))
        }
        case ExecFinished(RunPreparedExec(_, _ : PrepareRegularChefRun), _) => {
            Seq()
        }
        case _ => {
            isStepFailed = true
            Seq()
        }
    }

    def getStepResult() = GenesisStepResult(stepContext.step,
                                            isStepFailed = isStepFailed,
                                            envUpdate = stepContext.envUpdate(),
                                            vmsUpdate = stepContext.vmsUpdate())

    def getActionExecutor(action: Action) = action match {
        case a : InitExecNode => execPluginContext.execNodeInitializer(a)
        case a : InitChefNode => chefPluginContext.chefNodeInitializer(a)
        case a : PrepareInitialChefRun => chefPluginContext.initialChefRunPreparer(a)
        case a : PrepareRegularChefRun => chefPluginContext.regularChefRunPreparer(a)
        case a : RunExec => execPluginContext.execRunner(a)
    }

    def onStepInterrupt(signal: Signal) = Seq()
}