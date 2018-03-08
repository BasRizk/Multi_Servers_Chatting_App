import com.intellij.ide.commander.AbstractListBuilder;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;


public class AppChatGUI {
    private JPanel pnl_AppLook;
    private JPanel pnl_joinScreen;
    private JPanel pnl_chatScreen;
    private JTextField text_userName;
    private JButton btn_join;
    private JLabel lbl_username;
    private JButton btn_refreshClientList;
    private JButton btn_sendMsg;
    private JTextField txt_chatHere;
    private JPanel pnl_userName;
    private JTextPane txt_chatMsgs;
    private JList list_onlineClients;
    private JTabbedPane tab_receiver;

    private AppUser backend;
    private ArrayList<JLabel> onlineClientsJLabels;
    private DefaultListModel<String> onlineClientsModel;
    private String currentClient;
    private Hashtable<String, String> msgsHistory;

    public AppChatGUI(int portNumber) throws IOException {

        backend = new AppUser(portNumber, this);


        msgsHistory = new Hashtable<String, String>();

        onlineClientsModel = new DefaultListModel<>();
        list_onlineClients.setModel(onlineClientsModel);

        btn_join.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                login();

            }
        });

        btn_refreshClientList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mousePressed(e);
                refreshClientList();
            }
        });

        list_onlineClients.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {

                    String data = "";
                    if(list_onlineClients.getSelectedIndex() != -1) {

                        data = (String) list_onlineClients.getSelectedValue();
                        currentClient = data;
                        tab_receiver.setTitleAt(0,data.toUpperCase());

                        if(msgsHistory.containsKey(currentClient.toLowerCase())) {
                            String pastMsgs = msgsHistory.get(currentClient.toLowerCase());
                            txt_chatMsgs.setText(pastMsgs);
                        } else {
                            txt_chatMsgs.setText("");
                        }
                    }

                }
            }
        });

        btn_sendMsg.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                sendMsg();
            }
        });

        txt_chatHere.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);
                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMsg();
                }
            }
        });
        text_userName.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                super.keyTyped(e);

                if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    login();
                }

                if(e.getKeyCode() == KeyEvent.VK_SPACE) {

                }

            }
        });
    }

    private void login() {

        try {
            boolean loggedIn = backend.join(text_userName.getText());
            if(loggedIn) {
                pnl_chatScreen.setVisible(true);
                pnl_joinScreen.setVisible(false);
                refreshClientList();

                txt_chatMsgs.setText( "You Chat, We Chat !" + "\n\n"
                        + "Welcome " + backend.getUsername() + "\n"
                        + "=> You have just entered (You Chat, We Chat !), hope you the best experience." + "\n\n"
                        + "=> To Chat with anyone :" + "\n"
                        + "     - Select recipient from the list here on the left." + "\n"
                        + "     - Type down your message in the down bar." + "\n"
                        + "     - Press ENTER, or SEND." + "\n"
                        + "     - Have Fun :) ");

            } else {
                JOptionPane.showMessageDialog(pnl_AppLook,
                        "Please enter a valid username.");
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(pnl_AppLook,
                    "Error occurred while joining the network.");
        }



    }


    public void showMessage(String comingMsg) {


        String sender = comingMsg.split(":")[0].replace(" ","").toLowerCase();

        if(sender.toLowerCase().startsWith("server")) {
            if(comingMsg.contains("OK")){
                return;
            }
        }

        if(sender != null) {
            txt_chatMsgs.setText(txt_chatMsgs.getText() + "\n" +
                    comingMsg);

        } else if( sender.toLowerCase() == currentClient.toLowerCase()){
            txt_chatMsgs.setText(txt_chatMsgs.getText() + "\n" +
                    comingMsg);
        }


        msgsHistory.put(sender.toLowerCase(), txt_chatMsgs.getText());

        refreshClientList();


        //txt_chatMsgs.setText(txt_chatMsgs.getText() + comingMsg);
    }

    private void refreshClientList(){

        try {
            onlineClientsModel.removeAllElements();
            String [] onlineClients = backend.getClientList().split(",");
            for(String client : onlineClients) {
                client = client.replace(" ","");
                if(client != "") {
                    JButton newClient = new JButton(client);
                    System.out.println(newClient.getText());
                    onlineClientsModel.addElement(client);
                }
            }

        } catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(pnl_AppLook,
                    "Error occurred while trying to get Clients.");
        }
    }

    private void sendMsg() {

        String sender = backend.getUsername();
        String dest = currentClient;
        String msg = txt_chatHere.getText();

        if(dest == null | msg == null | dest == "" | msg == "" ){
            return;
        }

        try {
            backend.chatOnNetwork(dest, msg);

            txt_chatHere.setText("");
            txt_chatMsgs.setText(txt_chatMsgs.getText() + "\n" +
                    sender + ": " + msg);

            msgsHistory.put(currentClient.toLowerCase(), txt_chatMsgs.getText());

        } catch (IOException e1) {

            e1.printStackTrace();
            JOptionPane.showMessageDialog(pnl_AppLook,
                    "Error occurred while sending a message.");
        }

        refreshClientList();

    }

    public JPanel getPnl_AppLook() {
        return pnl_AppLook;
    }

    public AppUser getBackend() {
        return backend;
    }
    /*
    public static void main(String [] args) {


        JFrame appFrame = new JFrame("You chat, We chat !");

        try {
            AppChatGUI appChatGUI = new AppChatGUI(6889);
            appFrame.setContentPane(appChatGUI.pnl_AppLook);
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
                            appChatGUI.backend.cutConnection();
                        } catch (IOException e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(appChatGUI.pnl_AppLook,
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
    */

}
