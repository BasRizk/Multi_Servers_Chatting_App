import javax.swing.*;
import java.io.IOException;

/**
 * Created by Xbass on 3/7/2018.
 */
public class UserGUI_onServer_2 {

    public static void main(String [] args) {

        int portNumber_1 = 6789;
        int portNumber_2 = 6889;

        JFrame appFrame = new JFrame("You chat, We chat !");

        try {
            AppChatGUI appChatGUI = new AppChatGUI(portNumber_2);
            appFrame.setContentPane(appChatGUI.getPnl_AppLook());
            //appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            appFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            appFrame.pack();
            appFrame.setVisible(true);

            appFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            appFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    if (JOptionPane.showConfirmDialog(appFrame,
                            "Are you sure to want to quit?", "Really Closing?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
                        try {
                            appChatGUI.getBackend().cutConnection();
                        } catch (IOException e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(appChatGUI.getPnl_AppLook(),
                                    "Connection closed previously.");
                        }
                        System.exit(0);
                    }
                }
            });

        } catch (IOException e) {
            JOptionPane.showMessageDialog(appFrame,
                    "Server might not be running currently.");
            System.exit(0);
        }


    }

}
