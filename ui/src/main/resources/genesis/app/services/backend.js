define([
  "genesis",
  "jquery"
],

function(genesis, $) {
  /**
   * @const
   */
  var DEFAULT_TIMEOUT = 30000;

  var backend = genesis.module();

  backend.WorkflowManager = {

    cancelWorkflow: function(projectId, environmentId) {
      return $.ajax({
        url: "rest/projects/" + projectId +  "/envs/" + environmentId + "/actions",
        contentType : 'application/json',
        type: "POST",
        dataType: "json",
        data: JSON.stringify({action: 'cancel'}),
        timeout: DEFAULT_TIMEOUT
      });
    },

    /**
     *
     * @param {string} environmentId environment name
     * @param {string} workflow workflow name
     * @param variables workflow vars to be sent
     */
    executeWorkflow: function(projectId, environmentId, workflow, variables) {
      return $.ajax({
        url: 'rest/projects/' + projectId + '/envs/' + environmentId + '/actions',
        dataType: "json",
        contentType : 'application/json',
        type: "POST",
        data: JSON.stringify({
          action: 'execute',
          parameters: {
            workflow: workflow,
            variables: variables
          }
        }),
        timeout: DEFAULT_TIMEOUT,
        processData: false
      });
    },

    scheduleWorkflow: function(projectId, envId, workflow, variables, date, scheduleId, schedExpr) {
      var url = 'rest/projects/' + projectId + '/envs/' + envId + '/jobs';
      if (scheduleId)
        url += '/' + scheduleId;
      var dataJson = {
          workflow: workflow,
          executionDate: date,
          parameters: variables
      };
      if (schedExpr) dataJson['schedule'] = schedExpr
      return $.ajax({
        url: url,
        dataType: "json",
        contentType : 'application/json',
        type: scheduleId ? "PUT" : "POST",
        data: JSON.stringify(dataJson),
        timeout: DEFAULT_TIMEOUT,
        processData: false
      });

    }
  };



  backend.EnvironmentManager = {
    resetEnvStatus: function(projectId, environmentId) {
      return $.ajax({
        url: "rest/projects/" + projectId + "/envs/" + environmentId + "/actions/reset",
        type: "POST",
        timeout: DEFAULT_TIMEOUT,
        processData: false
      });
    },
    markDestroyed: function(projectId, environmentId) {
      return $.ajax({
        url: "rest/projects/" + projectId + "/envs/" + environmentId + "/actions/markDestroyed",
        type: "POST",
        timeout: DEFAULT_TIMEOUT,
        processData: false
      });
    },
    updateEnvName: function(projectId, environmentId, envName) {
      return $.ajax({
        url: "rest/projects/" + projectId + "/envs/" + environmentId,
        contentType : 'application/json',
        dataType: "json",
        type: "PUT",
        data: JSON.stringify({environment:{name: envName}}),
        timeout: DEFAULT_TIMEOUT
      })
    },

    expandLifeTime: function(env, newTtl) {
      return $.ajax({
        url: env.url() + "/timeToLive",
        type: newTtl ? "PUT" : "DELETE",
        contentType : 'application/json',
        dataType: "json",
        data: JSON.stringify({value: newTtl}),
        timeout: DEFAULT_TIMEOUT
      });

    }
  };

  backend.UserManager = {
    hasUsers: function() {
      return $.ajax({
        url: "rest/users?available",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT
      });
    },
    hasGroups: function() {
      return $.ajax({
        url: "rest/groups?available",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT
      });
    },

    whoami: function() {
      return $.ajax({
        url: "rest/",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT
      });
    },

    getUserGroups: function(username) {
      return $.ajax({
        url: "rest/users/" + username + "/groups",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      });
    }
  };

  backend.AuthorityManager = {
    roles: function() {
      var def = new $.Deferred();
      if (this._rolesListCache) {
        return def.resolve(this._rolesListCache);
      }

      var self = this;
      $.ajax({
        url: "rest/roles",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      }).done(function(roles) {
        self._rolesListCache = roles;
        def.resolve(self._rolesListCache);
      }).fail(function(jqXHR) {
        def.reject(jqXHR)
      });

      return def.promise();
    },

    projectRoles: function(projectId) {
      return $.ajax({
        url: "rest/projects/" + projectId + "/roles",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      });
    },

    saveUserRoles: function(username, roles) {
      return $.ajax({
        url: "rest/users/" + username +  "/roles",
        contentType : 'application/json',
        dataType: "json",
        type: "PUT",
        data: JSON.stringify(roles),
        timeout: DEFAULT_TIMEOUT
      });
    },

    saveGroupRoles: function(groupName, roles) {
      return $.ajax({
        url: "rest/groups/" + groupName +  "/roles",
        contentType : 'application/json',
        dataType: "json",
        type: "PUT",
        data: JSON.stringify(roles),
        timeout: DEFAULT_TIMEOUT
      });
    },

    getGroupRoles: function(groupName) {
      return $.ajax({
        url: "rest/groups/" + groupName + "/roles",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      });
    },

    haveAdministratorRights: function(projectId) {
      var def = new $.Deferred();
      $.ajax({
        url: "rest/projects/" + projectId + "/permissions",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      }).done(function(permissions) {
          def.resolve(_(permissions).indexOf("ROLE_GENESIS_PROJECT_ADMIN") != -1)
        }).fail(function(jqXHR) {
          def.reject(jqXHR)
        });
      return def;
    },

    getUserRoles: function(username) {
      return $.ajax({
        url: "rest/users/" + username + "/roles",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: true
      });
    }
  };

  backend.SettingsManager = {
    /**
     * @param {boolean} if true plugins settings are queried, system otherwise
    */
    restartRequired: function() {
     return  $.ajax({
        url:  "/rest/settings/restart",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: false
      });
    },

    coreDetails: function() {
      return $.getJSON("core-details.json");
    },

    distributionDetails: function() {
      return $.ajax({
        url: "distribution-details.json",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT,
        processData: false,
        suppressErrors: true
      })
    },

    contentList: function() {
      return $.ajax({
        url: "/content/index.json",
        dataType: "json",
        type: "GET",
        processData: true,
        suppressErrors: true
      })
    }
  };

  backend.SystemManager = {
    restart: function() {
     return $.ajax({
        url:  "/rest/system/restart",
        dataType: "json",
        type: "POST",
        timeout: DEFAULT_TIMEOUT,
        processData: false
      });
    },

    stop: function() {
      return $.ajax({
        url:  "/rest/system/stop",
        dataType: "json",
        type: "POST",
        timeout: DEFAULT_TIMEOUT,
        processData: false
      });
    }
  };

  backend.Attachments = {
    fetch: function(options) {
      return $.ajax({
        url: "rest/projects/" + options.projectId + "/envs/" + options.envId + "/steps/" + options.stepId + "/actions/" + options.actionUUID + "/attachments",
        dataType: "json",
        type: "GET",
        timeout: DEFAULT_TIMEOUT
      });
    }
  };

  function _type(name) {
    var type = "application/vnd.griddynamics.genesis." + name + "+json";
    this.name = type;

    this.get =  function(link) {
      return link.type == type && _(link.methods).contains("get")
    };

    this.edit = function(link) {
      return link.type == type && _(link.methods).contains("put")
    };

    this.delete = function(link) {
      return link.type == type && _(link.methods).contains("delete")
    };

    this.create = function(link) {
      return link.type == type && _(link.methods).contains("post")
    };

    this.any = function(link) {
      return link.type == type;
    }
  }

  backend.LinkTypes = {
    SystemSettings: new _type("SystemSettings"),
    Project: new _type("Project"),
    Environment: new _type("Environment"),
    EnvironmentDetails: new _type("EnvironmentDetails"),
    ProjectSettings: new _type("SystemSettings"),//todo:!!!!

    Workflow: new _type("Workflow"),
    WorkflowDetails: new _type("WorkflowDetails"),

    ConfigProperty: new _type("ConfigProperty"),
    User: new _type("User"),
    UserGroup: new _type("UserGroup"),
    Role: new _type("ApplicationRole"),
    Plugin: new _type("Plugin"),
    PluginDetails: new _type("PluginDetails"),
    DataBag: new _type("DataBag"),
    RemoteAgent: new _type("RemoteAgent"),

    Credentials: new _type("Credentials"),

    ServerArray: new _type("ServerArray"),
    Server: new _type("Server"),
    EnvScheduledJob: new _type("ScheduledJobDetails"),
    TemplateRepo: new _type("TemplateRepo"),
    EnvConfig: new _type("Configuration"),
    EnvConfigAccess: new _type("Access"),

    ResetAction: new _type("ResetAction$"),
    CancelAction: new _type("CancelAction$"),
    MarkDestroyedAction: new _type("MarkDestroyedAction$"),
    DataBagTemplate: new _type("DatabagTemplate")

  };

  return backend;
});
