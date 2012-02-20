package org.sonar.ide.intellij.ui;

import com.intellij.openapi.options.BaseConfigurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.sonar.ide.intellij.component.SonarProjectComponent;
import org.sonar.ide.intellij.component.SonarServiceLocator;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.ResourceQuery;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SonarProjectConfiguration extends BaseConfigurable {
  private JPanel pnlMain;
  private JTextField txtUser;
  private JTextField txtHost;
  private JPasswordField txtPassword;
  private JButton buttonTest;

  private final SonarProjectComponent sonarProjectComponent;

  private final SonarServiceLocator serviceLocator = SonarServiceLocator.getInstance();

  public SonarProjectConfiguration(Project project) {
    sonarProjectComponent = project.getComponent(SonarProjectComponent.class);

    txtHost.setText(sonarProjectComponent.getState().host);
    txtUser.setText(sonarProjectComponent.getState().user);
    txtPassword.setText(sonarProjectComponent.getState().password);

    DocumentListener setMoifiedListener = new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        setModified(true);
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        setModified(true);
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        setModified(true);
      }
    };
    txtHost.getDocument().addDocumentListener(setMoifiedListener);
    txtUser.getDocument().addDocumentListener(setMoifiedListener);
    txtPassword.getDocument().addDocumentListener(setMoifiedListener);
    buttonTest.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        testConnection();
      }
    });
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Sonar";
  }

  @Override
  public Icon getIcon() {
    return null;
  }

  @Override
  public String getHelpTopic() {
    return null;
  }

  @Override
  public JComponent createComponent() {
    return pnlMain;
  }

  @Override
  public void apply() throws ConfigurationException {
    sonarProjectComponent.getState().host = txtHost.getText();
    sonarProjectComponent.getState().user = txtUser.getText();
    sonarProjectComponent.getState().password = new String(txtPassword.getPassword());
    sonarProjectComponent.getState().configured = true;
  }

  @Override
  public void reset() {
  }

  @Override
  public void disposeUIResources() {
  }

  private void testConnection() {
    txtHost.setEnabled(false);
    txtUser.setEnabled(false);
    txtPassword.setEnabled(false);

    String host = txtHost.getText();
    String user = txtUser.getText();
    String password = new String(txtPassword.getPassword());

    final Sonar sonar = serviceLocator.getSonar(new Host(host, user, password));
    SwingWorker<List<?>, Void> worker = new SwingWorker<List<?>, Void>() {

      @Override
      protected List<?> doInBackground() throws Exception {
        ResourceQuery query = new ResourceQuery()
            .setQualifiers("TRK,BRC")
            .setDepth(1);
        return sonar.findAll(query);
      }

      @Override
      protected void done() {
        try {
          List<?> resources = get();
          JOptionPane.showMessageDialog(pnlMain, String.format("Test successful (%d resources loaded)", +resources.size()));
        } catch (InterruptedException e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(pnlMain, "Failed to connect to Sonar: connection thread was interrupted");
        } catch (ExecutionException e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(pnlMain, "Failed to connect to Sonar: " + e.getMessage());
        } finally {
          txtHost.setEnabled(true);
          txtUser.setEnabled(true);
          txtPassword.setEnabled(true);
        }
      }
    };
    worker.execute();
  }
}
