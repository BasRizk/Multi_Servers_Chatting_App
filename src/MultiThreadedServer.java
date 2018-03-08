import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class MultiThreadedServer {
	
	private ServerSocket welcomeSocket;
	private Socket peeringSocket;
	private ArrayList<ClientOnServer> users = new ArrayList<ClientOnServer>();
	private ArrayList<String> usernames = new ArrayList<String>();
	private ArrayList<ClientOnServer> servers = new ArrayList<ClientOnServer>();
	private ArrayList<Socket> serversSockets = new ArrayList<Socket>();
	boolean isParentServer;

	private static final String OUT_REQUEST = "OUT";
	
	public MultiThreadedServer(int portNumber, boolean isParent) {
		try {
			this.welcomeSocket = new ServerSocket(portNumber);
			this.isParentServer = isParent;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String [] argv) throws IOException {
		
		System.out.println("Create Servers out of here");
		
	}
	
	public void up() throws IOException {
		
		System.out.println("Running on Port #" + welcomeSocket.getLocalPort() + "\n");
		
		if(isParentServer)
			acceptServerConnection();
		
		while(true) {
			
			Socket inConnSocket;
			try {
				
				inConnSocket = this.welcomeSocket.accept();
				
				// In case of any errors, check if socket previously used
				if(!serversSockets.contains(inConnSocket)) {
					//ListeningSocket runnableCon = new ListeningSocket(inConnSocket);
					
					ClientOnServer newClient = new ClientOnServer(inConnSocket, this);
					new Thread(newClient).start();
					
					users.add(newClient);
					//usersSockets.add(inConnSocket);
					
					System.out.println("# of Users on Server : " + users.size());
				}

				
			} catch (IOException e) {
				e.printStackTrace();
				this.welcomeSocket.close();
				this.peeringSocket.close();
				break;
			}

		}
		
	}
	
	public void connectWithServer(String host, int portNumber) {
		try {
			peeringSocket = new Socket(host, portNumber);
			ClientOnServer peeringThread = new ClientOnServer(peeringSocket, this, true);
			new Thread(peeringThread).start();
			servers.add(peeringThread);
			serversSockets.add(peeringSocket);
			
		} catch (IOException e) {
			System.out.println("Error trying to connect to external server.");

		}
	}
	
	private void acceptServerConnection() {
		//TODO use host
		System.out.println("Waiting for Server Connection");
		
		Socket inConnSocket;
		try {
			
			inConnSocket = this.welcomeSocket.accept();
			if(!serversSockets.contains(inConnSocket)) {
				
				//Treat the Server as ClientOnServer
				ClientOnServer newServer = new ClientOnServer(inConnSocket, this, true);

				new Thread(newServer).start();
				
				servers.add(newServer);
				serversSockets.add(inConnSocket);

				System.out.println("# of Servers Attached : " + serversSockets.size());
				
			}
		
		} catch (IOException e) {
			System.out.println("Error Accepting external server.");
		}

	}
	
	protected Socket getSocketOf(String username, boolean toServer) throws IOException {
		
		if(toServer) {
			for(ClientOnServer server : servers) {
				if(server.getUsername().equals(username)) {
					System.out.println("Found Socket for " + username + ".");
					return server.getSocket();
				}
			}
			System.out.println("Did not find Socket for " + username + ".");

		}

			
		for(ClientOnServer user : users) {
			if( user.getUsername() != null && user.getUsername().equals(username)) {
				System.out.println("Found Socket for " + username + ".");
				return user.getSocket();
			}
			System.out.println("Did not find Socket for " + username + ".");

		}
		

		return null;
				
	}
	
	protected boolean hasUsername(String username) {
		
		for(String user : usernames) {
			if(user.equals(username)) {
				return true;
			}
		}
		
		return false;
	}
	
	protected void deleteClientFromServer(ClientOnServer toBeDeleted) {
				
		for(int i = 0; i < users.size() ; i++ ) {
			
			if(toBeDeleted == users.get(i)) {
				users.remove(i); break;
			}
		}
		
		usernames.remove(toBeDeleted.getUsername());

		// Delete user in other servers
		System.out.println("# of Users on Server : " + users.size());
		
	}
	
	protected String getOtherUsers(String clientName) {
		
		String usersInNetwork = "";
		
		for(String name : usernames) {
			if(name != clientName) {
				usersInNetwork+= name + ", ";
			}
		}
		
		System.out.println("Sending users list ( " + usersInNetwork +  ") to " + clientName);
		return  usersInNetwork;
		
	}
	
	protected void updateUsersList(String newUsers) {
		
		String [] request = newUsers.split(":");
		String requestBody = (request[0].equals("DELETE"))? request[1]: null;
		
		if(requestBody == null) {
			
			requestBody = request[0];
			
			String [] newUsernames = requestBody.split(",");
			
			for(int i = 0; i < newUsernames.length; i++) {
				if(newUsernames[i] != null && newUsernames[i]!="") {
					this.usernames.add(newUsernames[i]);

				} else {
					//TODO Mess data, clean later
				}
			}
			
		} else { //request[0].equals("DELETE")
			String [] toBeDeletedUsernames = requestBody.split(",");
			
			for(String toBeDeleted : toBeDeletedUsernames) {
				usernames.remove(toBeDeleted);

			}			
		}

		//broadCastMembersList();

	
	}

	/*
	private void broadCastMembersList() {

		for(ClientOnServer client : users) {

			if(client.getUsername() != null) {
				String sender = client.getUsername();
				client.sendMsg(sender, sender, "BROAD:" , 2);
			}

		}

	}
	*/
	protected ClientOnServer getAnotherServer() {
		return servers.get(0);
	}


}

class ClientOnServer implements Runnable {
	
	private String username = null;
	private Socket listeningSocket;
	private MultiThreadedServer server;
	private boolean isServer;
	
	public ClientOnServer(Socket listeningSocket, MultiThreadedServer server) {
		this.listeningSocket = listeningSocket;
		this.server = server;
		this.isServer = false;
	}
	
	public ClientOnServer(Socket listeningSocket, MultiThreadedServer server, boolean isServer) {
		this.listeningSocket = listeningSocket;
		this.server = server;
		this.isServer = true;
		this.setUsername("Server_" + listeningSocket.getPort());
	}
	
	
	@Override
	public void run() {
		
		if(isServer) {
			while(acceptRequestFromServer());
			
		}else {
			
			while (!this.joinNetwork());
			
			if(this.getUsername() != null) {
				while(this.chatInNetwork());
				
				try {
					listeningSocket.close();
					this.server.deleteClientFromServer(this);
					String msg = "OUT:DELETEUSER:" + this.getUsername();
					sendMsg(this.getUsername(), this.server.getAnotherServer().getUsername(), msg, 2);
					System.out.println(this.getUsername() + " connection is closed.");
										
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
		


		
	}
		
	private boolean joinNetwork() {
		
		String usernameMsg = null;
		
		try {
			System.out.println("Waiting for username from port #" + listeningSocket.getPort());
			BufferedReader inFromClient = new BufferedReader(
					new InputStreamReader(listeningSocket.getInputStream()));
			
			usernameMsg = inFromClient.readLine().toLowerCase();
		
		} catch(Exception e) {
			System.out.println("Error has occured.");
			return true;
		}
		
		if(usernameMsg != null && usernameMsg.split(":").length == 2) {
			
			String providedUsername = usernameMsg.split(":")[1];
			
			System.out.println("Suggested Username is " + providedUsername);
			
			if(!this.server.hasUsername(providedUsername)) {
				
				this.setUsername(providedUsername);
				
				try {
					DataOutputStream outToClient = new DataOutputStream(listeningSocket.getOutputStream());
					outToClient.writeBytes( "1" + "\n" ); //1 means that it is accepted user-name
					System.out.println(providedUsername + " has joined the server.");
					return true;
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
			} else {
				try {
					DataOutputStream outToClient = new DataOutputStream(listeningSocket.getOutputStream());
					outToClient.writeBytes( "0" + "\n" ); //1 means that it is accepted user-name
					System.out.println(providedUsername + " is used before.");
					//return true;

				} catch (IOException e) {
					e.printStackTrace();
				}
				return false;
			}
		}
		return false;
	}
	
	private boolean chatInNetwork() {
		
		String clientSentence;
		boolean successfull;
		
		try {
			
			BufferedReader inFromSender = new BufferedReader(
				new InputStreamReader(this.getSocket().getInputStream()));
	
			DataOutputStream outToSender = null; //In case of errors or other msgs.

			
			clientSentence = inFromSender.readLine();
			String [] request = clientSentence.split(":");
			String requestHeader = request[0].toUpperCase();
			
			if(clientSentence != null && requestHeader.equals("GETCLIENTLIST()")) {
				
				String sender = username;
				String membersList = server.getOtherUsers(sender);
				sendMsg(sender, sender, membersList, 2);
				
			} else if (clientSentence != null & requestHeader.equals("RESP")) {
				
				String requestBody = request[1];
				String [] requestFullBody = requestBody.split(",");
				String msg = requestFullBody[0];
				String oldReceiver = requestFullBody[1];
				String oldMsg = requestFullBody[2];
				
				if(!(msg.equals("OK"))) {
					String failureMsg = "msg " + oldMsg + " to " + oldReceiver + "failed !!";
					
					outToSender = new DataOutputStream(this.getSocket().getOutputStream());
					outToSender.writeBytes( failureMsg + "\n" );
				}
				
			} else if (clientSentence != null && !requestHeader.equals("QUIT")) {
				
				String [] chatMsg = clientSentence.split(",");
				
				if(chatMsg.length == 2 | chatMsg.length == 3 && chatMsg[0] != "") {
					String sender = username;
					String dest = chatMsg[0];
					String msg = chatMsg[1];
					int ttl = 0;
					try {
						ttl = Integer.parseInt(chatMsg[2]);
					}catch(Exception e) {ttl = 2;};
					
					successfull = sendMsg(sender, dest, msg, ttl);
					
					if(!successfull) {
						//TODO maybe interrupt till you receive clear message
						outToSender = new DataOutputStream(this.getSocket().getOutputStream());
						outToSender.writeBytes( "" + dest + " does not exist in the system." + "\n" );
					}
					
				} else if(chatMsg != null) {
					
					outToSender = new DataOutputStream(this.getSocket().getOutputStream());
					outToSender.writeBytes("Please enter valid chat command (ex: <destination-username>,<message>,<ttl>)" + "\n");
					
				}
				
			} else {
				return false;
			}
			
		}catch (Exception e) {
			//TODO maybe something else than e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private boolean sendMsg(String sender, String dest, String msg, int ttl) {
		
		boolean toServer = false;

		String [] splitMsg = msg.split(":");
		toServer = (splitMsg[0].equals("OUT"));
		//msg = (toServer)? splitMsg[1] + ":" + splitMsg[2] : msg;
		
		String tmpMsg = "";
		if(toServer) {
			int i = 0;
			for(i= 1; i<splitMsg.length-1; i++) {
				tmpMsg+= splitMsg[i] + ":";
			}
			tmpMsg+=splitMsg[i];
		} else {
			tmpMsg = msg;
		}
		msg = tmpMsg;
		
		
		if(ttl > 0) {
			
			--ttl;
			Socket destSocket;
			DataOutputStream outToReciever;
			try {
				System.out.println("Aiming to send from ("+ sender + ") to (" + dest + ")" +"\n" +
						"message: " + msg + " @ TTL = " + ttl);
				destSocket = server.getSocketOf(dest, toServer); //search locally
				
				if(destSocket != null) {
					outToReciever = new DataOutputStream(destSocket.getOutputStream());
					
					if(!toServer) {
						System.out.println("Msg to Client " + dest);
						if(sender == dest)
							outToReciever.writeBytes( msg + "\n");
						else
							outToReciever.writeBytes( sender + ": " + msg + "\n");
					} else {
						System.out.println("Msg To Server " + dest);
						outToReciever.writeBytes( msg + "\n");
					}
					return true;
				
				} else {
					ClientOnServer anotherServer = server.getAnotherServer();
					
					String extendedMsg ="OUT:" + "SEND:" + sender + "," +  dest + "," + msg + "," + ttl;
					return sendMsg(sender, anotherServer.getUsername(), extendedMsg, ttl);
				}

				
			} catch (IOException e) {
				System.out.println("Faliure to sendMsg to socket of " + dest + ".");
				return false;

			}
			
		}else { 
			System.out.println("TTL of Msg to " + dest +" Socket expired.");
			return false;
		}

	}
	
	/*
	 * Called by server (client) socket to accept requests coming from
	 * other serves.
	 * 
	 *  Requests Types:
	 *  - SEND:sender,receiver,message
	 *  - GETUSERS:sender
	 *
	 */
	
	private boolean acceptRequestFromServer() {
		System.out.println("Attempt to accept a Request by another server.");
		
		String getSentence;
		BufferedReader inFromSender = null;
		
		try {
			inFromSender = new BufferedReader(
					new InputStreamReader(this.getSocket().getInputStream()));
			
			System.out.println("Waiting to recieve a message.");
			getSentence = inFromSender.readLine();
			System.out.println("Message recieved: " + getSentence);


			String [] chatMsg = getSentence.split(":");
			String requestHeader = chatMsg[0];
			
			System.out.println("Going to serve request -> " + requestHeader);
						
			if(requestHeader.equals("SEND")) {
				String [] requestBody = chatMsg[1].split(",");
				String sender = requestBody[0];
				String receiver = requestBody[1];
				String msg = requestBody[2];
				int ttl = Integer.parseInt(requestBody[3]);
				
				boolean successfull = sendMsg(sender, receiver, msg, ttl);
				String responseMsg = (successfull)? "OUT:SENT:RESP:OK" :"OUT:SENT:RESP:ERROR";
				responseMsg+= "," + sender + "," + receiver + "," + msg;
				sendMsg(this.getUsername(), this.getUsername(), responseMsg, 2);
				
				String consoleLog = (successfull)? "Served.": "Socket not found.";
				System.out.println(consoleLog);

			} else if (requestHeader.equals("UPDATEUSERS")) {
				
				String newUsernames = chatMsg[1];
				System.out.println("new username : " + newUsernames + " to be saved.");
				this.server.updateUsersList(newUsernames);
				
			} else if (requestHeader.equals("DELETEUSER")) {
				
				String toBeDeleted = chatMsg[1];
				this.server.updateUsersList("DELETE:" + username);
				System.out.println(toBeDeleted + " connection is closed.");

				
			} else if (requestHeader.equals("SENT")) {
				String [] requestBody = chatMsg[2].split(",");
				String client = requestBody[1];
				String responseMsgToClient = requestBody[0] + "," + requestBody[2] + "," + requestBody[3];
				//int ttl = Integer.parseInt(requestBody[4]);

				boolean successfull = sendMsg(this.getUsername(), client, responseMsgToClient, 2);
								
				String consoleLog = (successfull)? "Served.": "Socket not found.";
				System.out.println(consoleLog);
				
			} else {
				
				System.out.println("Unidentified message but in correct format has been recieved.");

			}
			
		} catch (IOException e) {
			System.out.println("Error Reaching the Upper Server.");
			return false;
		}
		return true;

	}
	
	public String getUsername() {
		return this.username;
	}
	
	private void setUsername(String username) {
		this.username = username;
		
		
		if(!isServer) {
			this.server.updateUsersList(username);
			String msgRequest = "OUT:UPDATEUSERS:" + username;
			sendMsg(this.getUsername(), server.getAnotherServer().getUsername(), msgRequest, 2);
		}
		
	}
	
	protected Socket getSocket() {
		return listeningSocket;
	}
	
}


