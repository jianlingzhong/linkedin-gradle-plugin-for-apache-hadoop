package com.linkedin.gradle.pig;

import com.linkedin.gradle.azkaban.NamedScope;
import com.linkedin.gradle.azkaban.NamedScopeContainer;
import com.linkedin.gradle.azkaban.PigJob;

import org.gradle.api.Project;

class PigTaskHelper {

  // Build the necessary text to pass JVM parameters to Pig.
  static String buildJvmParameters(Map<String, String> jvmParameters) {
    return jvmParameters.collect() { key, val -> return "-D${key}=${val}" }.join(" ");
  }

  // Build the necessary text to pass script parameters to Pig.
  static String buildPigParameters(Map<String, String> parameters) {
    return parameters.collect() { key, val -> return "-param ${key}=${val}" }.join(" ");
  }

  // Uniquify the task names that correspond to running Pig scripts on a host, since there may be
  // more than one Pig script with the same name recursively under ${project.projectDir}/src.
  static String buildUniqueTaskName(String fileName, Set<String> taskNames) {
    if (!taskNames.contains(fileName)) {
      return fileName;
    }

    char index = '1';
    StringBuilder sb = new StringBuilder(fileName + index);
    int length = sb.length();

    while (!taskNames.contains(sb.toString())) {
      sb.setCharAt(length, ++index);
    }

    return sb.toString();
  }

  // Finds the Pig jobs configured in the Azkaban DSL and returns them as a map of the fully
  // qualified job name to the job.
  static Map<String, PigJob> findConfiguredPigJobs(Project project) {
    Map<String, PigJob> pigJobs = new LinkedHashMap<String, PigJob>();

    if (project.extensions.globalScope) {
      findConfiguredPigJobs(project.extensions.globalScope, "", pigJobs);
    }

    return pigJobs;
  }

  // Finds PigJobs configured in the DSL by recursively checking the scope containers.
  static void findConfiguredPigJobs(NamedScope scope, String prefix, Map<String, PigJob> pigJobs) {
    scope.thisLevel.each { String name, Object val ->
      if (val instanceof PigJob) {
        PigJob pigJob = (PigJob)val;
        pigJobs.put(prefix + pigJob.name, pigJob);
      }
      else if (val instanceof NamedScopeContainer) {
        NamedScopeContainer container = (NamedScopeContainer)val;
        findConfiguredPigJobs(container.scope, "${prefix}${container.scope.levelName}.", pigJobs);
      }
    }
  }
}