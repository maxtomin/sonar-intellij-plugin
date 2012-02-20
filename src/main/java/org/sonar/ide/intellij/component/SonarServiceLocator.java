package org.sonar.ide.intellij.component;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.connectors.Connector;
import org.sonar.wsclient.connectors.HttpClient4Connector;

/**
 * Service locator for Sonar config object and Sonar web-service wrapper.
 * Requires current project and optional module (or any indirect source of the above).
 * If the module is not specified, use project settings.
 */
public class SonarServiceLocator {
  private static SonarServiceLocator instance = new SonarServiceLocator();

  public static SonarServiceLocator getInstance() {
    return instance;
  }

  /**
   * @return Sonar project state
   */
  public SonarProjectComponent.SonarProjectState getSonarProjectState(Project project) {
    SonarProjectComponent component = project.getComponent(SonarProjectComponent.class);
    return component.getState();
  }

  /**
   * @return Sonar module state
   */
  public SonarModuleComponent.SonarModuleState getSonarModuleState(Module module) {
    SonarModuleComponent component = module.getComponent(SonarModuleComponent.class);
    return component.getState();
  }

  /**
   * @return Sonar module state for virtual file
   */
  public SonarModuleComponent.SonarModuleState getSonarModuleState(Project project, VirtualFile virtualFile) {
    Module module = ModuleUtil.findModuleForFile(virtualFile, project);
    return getSonarModuleState(module);
  }

  /**
   * @param project (mandatory)
   * @param virtualFile (optional) Use project settings if missed
   * @return Sonar service for virtual file
   */
  public Sonar getSonar(Project project, VirtualFile virtualFile) {
    Module module = virtualFile == null ? null : ModuleUtil.findModuleForFile(virtualFile, project);
    return getSonar(project, module);
  }

  /**
   * @param project (mandatory)
   * @param module (optional) Use project settings if missed
   * @return Sonar service for project or module file
   */
  public Sonar getSonar(Project project, Module module) {
    return getSonar(resolveHost(project, module));
  }

  /**
   * @return Sonar service for specific host
   */
  public Sonar getSonar(Host host) {
    return new Sonar(new HttpClient4Connector(host));
  }

  private Host resolveHost(Project project, Module module) {
    if (module != null) {
      SonarModuleComponent.SonarModuleState sonarModuleState = getSonarModuleState(module);
      if (sonarModuleState.configured && !sonarModuleState.useProjectHost) {
        return new Host(sonarModuleState.host, sonarModuleState.user, sonarModuleState.password);
      }
    }

    SonarProjectComponent.SonarProjectState sonarProjectState = getSonarProjectState(project);
    return new Host(sonarProjectState.host, sonarProjectState.user, sonarProjectState.password);
  }
}

