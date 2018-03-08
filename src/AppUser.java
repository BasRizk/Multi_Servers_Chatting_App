import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class AppUser {

	private String username;
	private Socket clientSocket;
	private boolean joined;

	private AppChatGUI appChatGUI;
	private Thread msgsListener;

	private static final String GET_CLIENT_LIST = "GETCLIENTLIST()";
	private static final String JOIN_REQUEST = "JOIN:";
	private static final String ACCEPT_RESP = "1";
	private static final String REJECT_RESP = "0";

	public AppUser(int portNum, AppChatGUI gui) throws IOException {

		this.clientSocket = new Socket("localhost", portNum);
		this.joined = false;
		this.appChatGUI = gui;

		if(appChatGUI != null) {
			msgsListener = new Thread(new AppListener(clientSocket, this));
			msgsListener.start();
		}

	}

	public boolean join(String username) throws IOException {
		System.out.println("Join operation begin process");
		
		DataOutputStream outToServer =
				new DataOutputStream(
						this.getClientSocket().getOutputStream());

		outToServer.writeBytes(JOIN_REQUEST + username + "\n");

		
		BufferedReader inFromServer = 
				new BufferedReader(
						new InputStreamReader(
								this.getClientSocket().getInputStream()));
		
		String serverResponse = inFromServer.readLine();
				
		if(serverResponse.equals(ACCEPT_RESP)) {
			this.username = username;
			this.joined = true;
			System.out.println("Welcome, You have Joined the server");
			return true;
		} else {
			System.out.println("Your name is used, try another username.");
			return false;
		}
		

	}

	public String getClientList() throws IOException {

		DataOutputStream outToServer =
				new DataOutputStream(
						this.getClientSocket().getOutputStream());

		outToServer.writeBytes(GET_CLIENT_LIST + "\n");

		BufferedReader inFromServer =
				new BufferedReader(
						new InputStreamReader(
								this.getClientSocket().getInputStream()));

		String onlineClients = inFromServer.readLine();

		return onlineClients;

	}

	public void chatOnNetwork(String dest, String msg) throws IOException{

		//TODO 1 Check Msg

		String toServerMsg = dest + "," + msg + "," + 2;

		DataOutputStream outToServer =
				new DataOutputStream(
						this.getClientSocket().getOutputStream());

		outToServer.writeBytes(toServerMsg + "\n");


	}

	public void cutConnection() throws IOException {
		this.clientSocket.close();
	}

	public static void main(String [] argv) throws IOException { 
		
		//AppUser user = new AppUser(new Socket("localhost", 6789));
		//user.runApp();
		System.out.println("Run your app out of here.");

	}


	public void runApp() throws IOException {
		String sentMsg;
		String comingMsg;
		
		BufferedReader inFromUser = null;
		BufferedReader inFromServer = null;
				
		while(! this.joined() ) {
			String username;
			
			System.out.print("Enter your username : ");
			
			inFromUser =
					new BufferedReader(new InputStreamReader(System.in));

			username = inFromUser.readLine();
			
			if(username.toUpperCase().equals("QUIT")){
				System.out.print("You just quit.");
				break;
			}else if(username!= null && username != "") {
				this.join(username);
				System.out.println();
			}
			
		}
		
		if(this.joined()) {
			System.out.println("Chat Messages will be in this form:" + "\n\n" +
					"<destination-username>,<message>" +  "\n\n" +
					" otherwise, it will be rejected" + "\n");
		}
	
		boolean anyChange = true;

		while(this.joined()) {
			
			inFromUser =
					new BufferedReader(new InputStreamReader(System.in));
			
			
			if(anyChange) {
				System.out.print( this.username + ": ");
				anyChange = false;
			}
			
			if(inFromUser.ready()) {
				
				sentMsg = inFromUser.readLine();
				

				DataOutputStream outToServer =
						new DataOutputStream(
								this.getClientSocket().getOutputStream());
				
				outToServer.writeBytes(sentMsg + "\n");
				
				if(sentMsg.toUpperCase().equals("QUIT")){
					System.out.print("You just quit.");
					break;
					
				}
	
				anyChange = true;
				
				if (sentMsg.toUpperCase().equals("GETCLIENTLIST()")) {
					anyChange = false;
				}
				
			}

			
			inFromServer =
					new BufferedReader(
							new InputStreamReader(
									this.getClientSocket().getInputStream()));
			
			if(inFromServer.ready()) {
				
				comingMsg = inFromServer.readLine();
				
				System.out.println("\n" + comingMsg);
				
				anyChange = true;
			}


		}
		
		this.getClientSocket().close();

	}
	
	private Socket getClientSocket() {
		return clientSocket;
	}
	
	private boolean joined() {
		return joined;
	}

	public String getUsername() {
		return username;
	}

	public void showMsgOnGUI(String msg) {


		if(msg != null) {
			if(msg.startsWith("Error")){
				String [] msgBody = msg.split(",");
				msg = "Error sending the msg; " + msgBody[1] + "is probably out of connection.";
			}

			appChatGUI.showMessage(msg);

		} else {
			try {
				msgsListener.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();

			}
		}


	}

}

class AppListener implements Runnable {

	private Socket listenerSocket;
	private AppUser appUser;

	public AppListener ( Socket socket, AppUser user) {

		this.listenerSocket = socket;
		this.appUser = user;
	}

	@Override
	public void run() {

		while (true) {

			String comingMsg = null;
			try {
				comingMsg = recieveMessages();
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}

			appUser.showMsgOnGUI(comingMsg);


		}

	}

	private String recieveMessages() throws IOException {


		String comingMsg = "";
		BufferedReader inFromServer =
				new BufferedReader(
						new InputStreamReader(
								this.getListenerSocket().getInputStream()));

		if(inFromServer.ready()) {

			comingMsg = inFromServer.readLine();
			return comingMsg;
		}

		return null;

	}

	public Socket getListenerSocket() {
		return listenerSocket;
	}

}