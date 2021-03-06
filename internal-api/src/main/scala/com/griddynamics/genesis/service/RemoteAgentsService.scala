/*
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
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */

package com.griddynamics.genesis.service

import com.griddynamics.genesis.common.CRUDService
import com.griddynamics.genesis.api._
import scala.concurrent.Future
import com.griddynamics.genesis.api.AgentStatus._
import com.griddynamics.genesis.api.RemoteAgent
import com.griddynamics.genesis.api.AgentStatus.AgentStatus
import com.griddynamics.genesis.api.ConfigProperty

trait RemoteAgentsService extends CRUDService[RemoteAgent, Int]{
  def findByTags(tags: Seq[String]): Seq[RemoteAgent]
  def status(agent: RemoteAgent) : Future[(AgentStatus, Option[JobStats])]
  def status(agents: Seq[RemoteAgent]) : Future[Seq[(RemoteAgent, (AgentStatus, Option[JobStats]))]]
  def getConfiguration(key: Int): ExtendedResult[Seq[ConfigProperty]]
  def putConfiguration(values: Map[String, String], key: Int): ExtendedResult[RemoteAgent]
}
