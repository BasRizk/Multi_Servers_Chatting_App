import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Users_OnServer_1 {

	public static void main(String [] args) throws UnknownHostException, IOException {
		
		AppUser user = new AppUser(6789, null);
		user.runApp();
		
	}
	
}
