package yonsei.highfive.dbconnector;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONException;
import org.json.JSONObject;

import edu.stanford.junction.JunctionException;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public class DBConnector extends JunctionActor {
	/**
	 *  Switchboard Setup
	 */
	private static final String switchboard = "165.132.214.212";
	private static final XMPPSwitchboardConfig config = new XMPPSwitchboardConfig(switchboard);
	private static final JunctionMaker jxMaker = JunctionMaker.getInstance(config);
	/**
	 * DB �¾�
	 * ���� ������ ������ 3306��Ʈ�� ���������Ƿ� �ӽ÷�
	 * boom1492.iptime.org ���� �̿�
	 */
	public static Connection makeConnection(){
		String url = "jdbc:mysql://boom1492.iptime.org:3306/librarydb";
		String id = "highfive";
		String password = "fivehigh";
		Connection con = null;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection(url, id, password);
		}catch(ClassNotFoundException e){
			System.out.println("����̹��� ã�� �� �����ϴ�.");
		}catch(SQLException e){
			e.printStackTrace();
			System.out.println("���ῡ �����Ͽ����ϴ�.");
		}
		return con;
	}
	private static Connection con = makeConnection();
	
	public DBConnector(){
		super("dbc");
	}
	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		// TODO Auto-generated method stub
		try {
			if(message.has("service")){
				String service = message.getString("service");
				//========================================================//
				if(service.equals("certification")){
					String id = message.getString("id");
					String pw = message.getString("pw");
					boolean cert = false;
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM users");
					while(rs.next()){
						if(rs.getString("user_id").equals(id) && rs.getString("user_pw").equals(pw)){
							cert = true;
						}
					}
					if(cert==true){
						JSONObject ack = new JSONObject();
						ack.put("accept", "true");
						sendMessageToRole("user", ack);
						// �Ŀ� sendMessageToRole�� sendMessageToActor�� �ٲپ��־����
						// ����� pidgin�� �̿��� ������� �������� �����
					}else{
						JSONObject ack = new JSONObject();
						ack.put("accept", "false");
						sendMessageToRole("user", ack);
						// �Ŀ� sendMessageToRole�� sendMessageToActor�� �ٲپ��־����
						// ����� pidgin�� �̿��� ������� �������� �����
					}
				}
				//========================================================//
				else if(service.equals("circulation")){
					
				}
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] argv) {
		
		DBConnector dbc = new DBConnector();
		try {
			jxMaker.newJunction(URI.create("junction://"+switchboard+"/db"), dbc);
			System.out.println(switchboard + " : Junction Connected");
			synchronized (dbc) {
				try {
					dbc.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (JunctionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}
}
