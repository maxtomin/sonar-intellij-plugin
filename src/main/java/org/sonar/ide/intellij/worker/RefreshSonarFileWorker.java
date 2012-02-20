package org.sonar.ide.intellij.worker;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.sonar.ide.intellij.component.SonarModuleComponent;
import org.sonar.ide.intellij.component.SonarServiceLocator;
import org.sonar.wsclient.Sonar;

import javax.swing.*;

public abstract class RefreshSonarFileWorker<T> extends SwingWorker<T, Void> {
  private Project project;
  protected VirtualFile virtualFile;
  private final SonarServiceLocator serviceLocator = SonarServiceLocator.getInstance();

  protected RefreshSonarFileWorker(Project project, VirtualFile virtualFile) {
    this.project = project;
    this.virtualFile = virtualFile;
  }

  protected Sonar getSonar() {
    return serviceLocator.getSonar(project, virtualFile);
  }

  protected String getResourceKey() {
    final SonarModuleComponent.SonarModuleState sonarModuleState = getSonarModuleState();
    if (!sonarModuleState.configured) {
      return null;
    }

    final PsiManager psiManager = PsiManager.getInstance(this.project);
    final PsiFile psiFile = ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
      @Override
      public PsiFile compute() {
        return psiManager.findFile(virtualFile);
      }
    });

    if (!(psiFile instanceof PsiJavaFile)) {
      return null;
    }

    return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
      @Override
      public String compute() {
        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
        String packageName = psiJavaFile.getPackageName();
        String className = psiJavaFile.getClasses()[0].getName();

        return sonarModuleState.projectKey + ":" + packageName + "." + className;
      }
    });
  }

  private SonarModuleComponent.SonarModuleState getSonarModuleState() {
    return serviceLocator.getSonarModuleState(project, virtualFile);
  }
}
