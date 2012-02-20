package org.sonar.ide.intellij.component;

public interface SonarModuleComponent {
  SonarModuleState getState();

  class SonarModuleState {
    public String host;
    public String user;
    public String password;
    public boolean useProjectHost = true;
    public String projectKey;
    public boolean configured;
  }
}
