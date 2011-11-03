package yonsei.highfive.director;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import yonsei.highfive.circulation.BookSpec;
import yonsei.highfive.circulation.MediaSpec;
import yonsei.highfive.circulation.SeatSpec;

import edu.stanford.junction.JunctionException;
import edu.stanford.junction.JunctionMaker;
import edu.stanford.junction.api.activity.JunctionActor;
import edu.stanford.junction.api.messaging.MessageHeader;
import edu.stanford.junction.provider.xmpp.XMPPSwitchboardConfig;

public class JavaDirector extends JunctionActor {
	/**
	 *  Switchboard Setup
	 */
	private static final String switchboard = "165.132.214.212";
	private static final XMPPSwitchboardConfig config = new XMPPSwitchboardConfig(switchboard);
	private static final JunctionMaker jxMaker = JunctionMaker.getInstance(config);

	/**
	 * mysql DB server�� ����
	 * @return 
	 */
	public static Connection makeConnection(){
		// �����ð� DB ����� ���� ��� ������ �������� ���� �����ϱ� ���� autoreconnect ���� true �� ����
		// ��õ�Ǵ� ����� �ƴϹǷ� �ٸ� ����� ã�ƺ����Ѵ�. validationquery = "select 1" ??????
		String url = "jdbc:mysql://mobilesw.yonsei.ac.kr:3306/librarydb?autoReconnect=true";

		String id = "root";
		String password = "apmsetup";
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

	/**
	 * Role : Director
	 */
	public JavaDirector(){
		super("director");
	}
	@Override
	public void onMessageReceived(MessageHeader header, JSONObject message) {
		// TODO Auto-generated method stub
		try {
			if(message.has("service")){
				String service = message.getString("service");
				//==========================�л�������������==============================//
				if(service.equals("certification")){
					String id = message.getString("id");
					String pw = message.getString("pw");
					boolean cert = false;
					boolean inlibrary = false;
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM users");
					while(rs.next()){
						if(rs.getString("user_id").equals(id) && rs.getString("user_pw").equals(pw)){
							cert = true;
							inlibrary = rs.getBoolean("isinner");
						}
					}
					if(cert==true){
						JSONObject ack = new JSONObject();
						ack.put("accept", "true");
						ack.put("inlibrary", inlibrary);
						sendMessageToRole("user", ack);
						System.out.println(id + "���� �л� �����Ǿ����ϴ�.");
						// �Ŀ� sendMessageToRole�� sendMessageToActor�� �ٲپ��־����
						// ����� pidgin�� �̿��� ������� �������� �����
					}else{
						JSONObject ack = new JSONObject();
						ack.put("accept", "false");
						ack.put("inlibrary", inlibrary);
						sendMessageToRole("user", ack);
						// �Ŀ� sendMessageToRole�� sendMessageToActor�� �ٲپ��־����
						// ����� pidgin�� �̿��� ������� �������� �����
					}
				}
				////////////////////////////////////////////////////////////////////////
				//==========================����Ȯ�ΰ���==============================//
				else if(service.equals("checkbook")){
					String book_id = message.getString("bookid");
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM books");
					String title = null, author = null, publisher = null, borrower = null;
					String StartTime = null, EndTime = null;
					while(rs.next()){
						if(rs.getString("book_id").equals(book_id)){
							title = rs.getString("title");
							author = rs.getString("author");
							publisher = rs.getString("publisher");
							borrower = rs.getString("borrower");
							StartTime = rs.getString("StartTime");
							EndTime = rs.getString("EndTime");
						}
					}
					JSONObject ack = new JSONObject();
					JSONObject book = new JSONObject();
					ack.put("service", "checkbook");
					book.put("bookid", book_id);
					book.put("title", title);
					book.put("author", author);
					book.put("publisher", publisher);
					book.put("StartTime", StartTime);
					book.put("EndTime", EndTime);
					if(borrower==null)
						book.put("borrower", "null");
					else
						book.put("borrower", borrower);
					ack.put("book", book);
					sendMessageToRole("user", ack);
				}
				//////////////////////////////////////////////////////////////////////
				//============================��������ݳ�=========================//
				else if(service.equals("borrowbook")){
					String user_id = message.getString("userid");
					String book_id = message.getString("bookid");
					Statement stmt = con.createStatement();
					
					//
					Date date = new Date();
					Date datenext = new Date();
					datenext.setTime(date.getTime()+(long)(1000*60*60*24*14));	// ����ð����� ���� 14��
					
					SimpleDateFormat yearformat = new SimpleDateFormat("yyyy");
					SimpleDateFormat monthformat = new SimpleDateFormat("MM");
					SimpleDateFormat dayformat = new SimpleDateFormat("dd");
					SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss");
				
					String startyear = yearformat.format(date);
					String startmonth = monthformat.format(date);
					String startday = dayformat.format(date);
					String starttime = timeformat.format(date); 
					
					String endyear = yearformat.format(datenext);
					String endmonth = monthformat.format(datenext);
					String endday = dayformat.format(datenext);
					String endtime = timeformat.format(datenext);
					
					String StartTime = startyear + "-" + startmonth + "-" + startday + " " + starttime;
					String EndTime = endyear + "-" + endmonth + "-" + endday + " " + endtime;
					
					//
					int rs = stmt.executeUpdate("UPDATE books SET borrower = " + user_id + ", StartTime = '" + StartTime + "', EndTime = '" + EndTime + "' WHERE book_id = " + book_id + " AND borrower IS NULL");
					if(rs!=0){
						JSONObject ack = new JSONObject();
						ack.put("service", "borrowbook");
						ack.put("ack", "true");
						sendMessageToRole("user", ack);
						System.out.println(user_id + "���� " + book_id +"�� �뿩�Ͽ����ϴ�.");
					} else{
						JSONObject ack = new JSONObject();
						ack.put("service", "borrowbook");
						ack.put("ack", "false");
						sendMessageToRole("user", ack);
					}
				}
				else if(service.equals("returnbook")){
					String user_id = message.getString("userid");
					String book_id = message.getString("bookid");
					Statement stmt = con.createStatement();
					int rs = stmt.executeUpdate("UPDATE books SET borrower = NULL, StartTime = '1000-01-01 00:00:00', EndTime = '1000-01-01 00:00:00' WHERE book_id =" + book_id + " AND borrower = '" + user_id +"'");
					if(rs!=0){
						JSONObject ack = new JSONObject();
						ack.put("service", "returnbook");
						ack.put("ack", "true");
						sendMessageToRole("user", ack);
						System.out.println(user_id + "���� " + book_id +"�� �ݳ��Ͽ����ϴ�.");
					} else{
						JSONObject ack = new JSONObject();
						ack.put("service", "returnbook");
						ack.put("ack", "false");
						sendMessageToRole("user", ack);
					}
				}
				///////////////////////////////////////////////////////////////////////
				//==============�뿩�������===========================================//
				else if(service.equals("checkborrowed")){
					String user_id = message.getString("userid");
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM books WHERE borrower = '"+ user_id +"'");
					JSONObject ack = new JSONObject();
					ack.put("service", "checkborrowed");
					JSONArray books = new JSONArray();
					while(rs.next()){
						String bookid = rs.getString("book_id");
						String title = rs.getString("title");
						String author = rs.getString("author");
						String publisher = rs.getString("publisher");
						String borrower = rs.getString("borrower");
						String StartTime = rs.getString("StartTime");
						String EndTime = rs.getString("EndTime");
						BookSpec bookspec = new BookSpec(bookid, title, author, publisher, borrower, StartTime, EndTime);
						JSONObject book = bookspec.getJSON();
						books.put(book);
					}
					ack.put("book", books);
					sendMessageToRole("user", ack);
				}
				//////////////////////////////////////////////////////////////////////
				//======================�����˻� ���=====================================//
				else if(service.equals("searchbook")){
					String keyword = message.getString("keyword");
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM books WHERE title LIKE '%" + keyword + "%'");
					JSONObject ack = new JSONObject();
					ack.put("service", "searchbook");
					JSONArray books = new JSONArray();
					while(rs.next()){
						String bookid = rs.getString("book_id");
						String title = rs.getString("title");
						String author = rs.getString("author");
						String publisher = rs.getString("publisher");
						String borrower = rs.getString("borrower");
						String StartTime = rs.getString("StartTime");
						String EndTime = rs.getString("EndTime");
						BookSpec bookspec = new BookSpec(bookid, title, author, publisher, borrower, StartTime, EndTime);
						JSONObject book = bookspec.getJSON();
						books.put(book);
					}
					ack.put("book", books);
					sendMessageToRole("user", ack);
				}
				////////////////////////////////////////////////////////////////////////
				//============��Ʈ�ѷ� ����==============================================//
				else if(service.equals("keypress")){
					String keypress = message.getString("keypress");
					try {
						Robot robot = new Robot();
						if(keypress.equals("up")){
							robot.keyPress(KeyEvent.VK_UP);
						}
						else if(keypress.equals("down")){
							robot.keyPress(KeyEvent.VK_DOWN);
						}
						else if(keypress.equals("left")){
							robot.keyPress(KeyEvent.VK_LEFT);
						}
						else if(keypress.equals("right")){
							robot.keyPress(KeyEvent.VK_RIGHT);
						}
						else if(keypress.equals("a")){
							robot.keyPress(KeyEvent.VK_A);
						}
						else if(keypress.equals("s")){
							robot.keyPress(KeyEvent.VK_S);
						}
						else if(keypress.equals("d")){
							robot.keyPress(KeyEvent.VK_D);
						}
					} catch (AWTException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//////////////////////////////////////////////////////////////////////
					
				}
				//==========================�¼�Ȯ�ΰ���==============================//
				else if(service.equals("checkseat")){
					String SeatID = message.getString("SeatID");
					String UserIDE = message.getString("UserID");
					
					Statement stmt = con.createStatement();
					
				
					String UserID = null, StartTimeS = null, EndTimeS = null, DoubleSeatS = "No", DoubleCheck = null;
					Date StartTimeD = null, EndTimeD = null;
					
					DoubleCheck = "SELECT SeatID FROM seats WHERE UserID = '" + UserIDE + "'";
					ResultSet re = stmt.executeQuery(DoubleCheck);
					while(re.next()){
						String _SeatID = re.getString("SeatID");
						if(!SeatID.equals(_SeatID))
							DoubleSeatS = "Yes";
					}
					/*
					re.last();
					int count = re.getRow();
					if(count != 0){
						DoubleSeatS = "Yes";
					}
				*/
					ResultSet rs = stmt.executeQuery("SELECT * FROM seats");
					while(rs.next()){
						if(rs.getString("SeatID").equals(SeatID)){
							UserID = rs.getString("UserID");
							StartTimeD = rs.getTime("StartTime");
							EndTimeD = rs.getTime("EndTime");
						}
					}
					
					StartTimeS = StartTimeD.toString();
					EndTimeS = EndTimeD.toString();
					
					JSONObject ack = new JSONObject();
					JSONObject seat = new JSONObject();
					ack.put("service", "checkseat");
					seat.put("SeatID", SeatID);
					if(UserID == null){
						seat.put("UserID", "null");
					}
					else{
						seat.put("UserID", UserID);
					}
					seat.put("StartTime", StartTimeS);
					seat.put("EndTime", EndTimeS);
					seat.put("DoubleSeat", DoubleSeatS);
					ack.put("seat", seat);
					sendMessageToRole("user", ack);
				}
				//////////////////////////////////////////////////////////////////////
				//============================�¼������ݳ�����=========================//
				else if(service.equals("occupyseat")){
					String SeatID = message.getString("SeatID");
					String UserID = message.getString("UserID");
					int Hour = message.getInt("Hour");

					Statement stmt = con.createStatement();
					
					int	su = stmt.executeUpdate("UPDATE seats SET UserID = '" + UserID + "' WHERE SeatID = " + "'" + SeatID + "'" + " AND UserID IS NULL");

					Date date = new Date();
					Date datenext = new Date();
					datenext.setTime(date.getTime()+(long)(1000*60*60*Hour));

					SimpleDateFormat yearformat = new SimpleDateFormat("yyyy");
					SimpleDateFormat monthformat = new SimpleDateFormat("MM");
					SimpleDateFormat dayformat = new SimpleDateFormat("dd");
					SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss");

					String startyear = yearformat.format(date);
					String startmonth = monthformat.format(date);
					String startday = dayformat.format(date);	
					String starttime = timeformat.format(date); 

					String endyear = yearformat.format(datenext);
					String endmonth = monthformat.format(datenext);
					String endday = dayformat.format(datenext);
					String endtime = timeformat.format(datenext);

					String StartTime = startyear + "-" + startmonth + "-" + startday + " " + starttime;
					String EndTime = endyear + "-" + endmonth + "-" + endday + " " + endtime;

					int	ss = stmt.executeUpdate("UPDATE seats SET StartTime = " + "'" + StartTime + "'" + " WHERE SeatID = " + "'" + SeatID + "'");
					int	se = stmt.executeUpdate("UPDATE seats SET EndTime = " + "'" + EndTime + "'" + " WHERE SeatID = " + "'" + SeatID + "'");

					if(su!=0 && ss!=0 && se!=0){
						JSONObject ack = new JSONObject();
						JSONObject seat = new JSONObject();
						seat.put("SeatID", SeatID);
						seat.put("UserID", UserID);
						seat.put("StartTime", StartTime);
						seat.put("EndTime", EndTime);
						seat.put("DoubleSeat", "No");

						ack.put("service", "occupyseat");
						ack.put("ack", "true");
						ack.put("seat", seat);
						sendMessageToRole("user", ack);
						System.out.println(UserID + "���� " + SeatID +" �¼��� �����޾ҽ��ϴ�.");
					} 
					else{
						JSONObject ack = new JSONObject();
						ack.put("service", "occupyseat");
						ack.put("ack", "false");
						sendMessageToRole("user", ack);
					}					

				}
				else if(service.equals("returnseat")){
					String SeatID = message.getString("SeatID");
					String UserID = message.getString("UserID");
					
					Statement stmt = con.createStatement();
					
					int su = stmt.executeUpdate("UPDATE seats SET UserID = null WHERE SeatID = " + "'" + SeatID + "'");
					int ss = stmt.executeUpdate("UPDATE seats SET StartTime = '1000-01-01 10:00:00' WHERE SeatID = " + "'" + SeatID + "'");
					int se = stmt.executeUpdate("UPDATE seats SET EndTime = '1000-01-01 10:00:00' WHERE SeatID = " + "'" + SeatID + "'");
					
					String StartTime = "1000-01-01 10:00:00";
					String EndTime = "1000-01-01 10:00:00";
					
					if(su!=0 && ss!=0 && se!=0){
						JSONObject ack = new JSONObject();
						JSONObject seat = new JSONObject();
						
						seat.put("SeatID", SeatID);
						seat.put("UserID", UserID);
						seat.put("StartTime", StartTime);
						seat.put("EndTime", EndTime);
						seat.put("DoubleSeat", "No");
						
						ack.put("service", "returnseat");
						ack.put("ack", "true");
						ack.put("seat", seat);
						sendMessageToRole("user", ack);
						System.out.println(UserID+ "���� " + SeatID +" �¼��� �ݳ��Ͽ����ϴ�.");
					} else{
						JSONObject ack = new JSONObject();
						ack.put("service", "returnseat");
						ack.put("ack", "false");
						sendMessageToRole("user", ack);
					}
				}
				else if(service.equals("extentseat")){
					String SeatID = message.getString("SeatID");
					String UserID = message.getString("UserID");
					int Hour = message.getInt("Hour");
					
					Statement stmt = con.createStatement();
					
					Date dateS1 = null;
					Date dateS2 = null;
					Date dateE1 = null;
					Date dateE2 = null;
					
					ResultSet rs = stmt.executeQuery("SELECT * FROM seats");
					while(rs.next()){
						String number = rs.getString("SeatID");
						if(number.equals(SeatID)){
							dateS1 = rs.getDate("StartTime");
							dateS2 = rs.getTime("StartTime");
							dateE1 = rs.getDate("EndTime");
							dateE2 = rs.getTime("EndTime");
							break;
						}
					}
					
					dateE2.setTime(dateE2.getTime()+(long)(1000*60*60*Hour));
					
					SimpleDateFormat yearformat = new SimpleDateFormat("yyyy");
					SimpleDateFormat monthformat = new SimpleDateFormat("MM");
					SimpleDateFormat dayformat = new SimpleDateFormat("dd");
					SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss");
					
					String startyear = yearformat.format(dateS1);
					String startmonth = monthformat.format(dateS1);
					String startday = dayformat.format(dateS1);
					String starttime = timeformat.format(dateS2);
					
					String endyear = yearformat.format(dateE1);
					String endmonth = monthformat.format(dateE1);
					String endday = dayformat.format(dateE1);
					String endtime = timeformat.format(dateE2);
					
					String StartTime = startyear + "-" + startmonth + "-" + startday + " " + starttime;
					String EndTime = endyear + "-" + endmonth + "-" + endday + " " + endtime;
					
					int se = stmt.executeUpdate("UPDATE seats SET EndTime = " + "'" + EndTime + "'" + " WHERE SeatID =" + "'" + SeatID + "'");
					
					if(se!=0){
						JSONObject ack = new JSONObject();
						JSONObject seat = new JSONObject();
						
						seat.put("SeatID", SeatID);
						seat.put("UserID", UserID);
						seat.put("StartTime", StartTime.toString());
						seat.put("EndTime", EndTime.toString());
						seat.put("DoubleSeat", "No");
						ack.put("service", "extentseat");
						ack.put("ack", "true");
						ack.put("seat", seat);
						sendMessageToRole("user", ack);
						System.out.println(UserID + "���� " + SeatID +" �¼��� �����Ͽ����ϴ�.");
					} else{
						JSONObject ack = new JSONObject();
						ack.put("service", "extentseat");
						ack.put("ack", "false");
						sendMessageToRole("user", ack);
					}
				}
				//////////////////////////////////////////////////////////////////////
				else if(service.equals("checkseatlist")){
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM `seats` WHERE `UserID` IS NOT NULL");
					JSONArray seatlist = new JSONArray();
					JSONObject ack = new JSONObject();
					while(rs.next()){
						seatlist.put(rs.getInt("ind"));
					}
					ack.put("list", seatlist);
					ack.put("service", "ackcheckseatlist");
					sendMessageToRole("user", ack);
				}
				
				//==========================����Ʈ����======================================//
				else if(service.equals("managegate")){
					String UserID = message.getString("UserID");
					Statement stmt = con.createStatement();
					
					ResultSet rs = stmt.executeQuery("SELECT * FROM users");
					
					boolean exist = false;
					boolean isinner = false;

					
					while(rs.next()){
						if(rs.getString("user_id").equals(UserID)){
							exist = true;
							isinner = rs.getBoolean("isinner");
							break;
						}
					}
					
					if(isinner)
						stmt.executeUpdate("UPDATE users SET isinner = 0 WHERE user_id = '" + UserID + "'");
					else
						stmt.executeUpdate("UPDATE users SET isinner = 1 WHERE user_id = '" + UserID + "'");
					
					JSONObject ack = new JSONObject();
					ack.put("ack", "true");
					if(exist == false){
						ack.put("service", "errorgate");
					}
					else if(exist == true){
						if(!isinner){
							ack.put("service","ingate");
						}
						else{
							ack.put("service", "outgate");
						}
					}
					sendMessageToRole("user", ack);
				}
				//==========================��Ƽ�̵�� �ڷ� Ȯ�ΰ���==============================//
				else if(service.equals("checkmedia")){
					String media_id = message.getString("mediaid");
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM medias");
					String title = null, director = null, production = null, borrower = null, medialink = null;
					while(rs.next()){
						if(rs.getString("media_id").equals(media_id)){
							title = rs.getString("title");
							director = rs.getString("director");
							production = rs.getString("production");
							borrower = rs.getString("borrower");
							medialink = rs.getString("medialink");
						}
					}
					JSONObject ack = new JSONObject();
					JSONObject media = new JSONObject();
					ack.put("service", "checkmedia");
					media.put("mediaid", media_id);
					media.put("title", title);
					media.put("director", director);
					media.put("production", production);
					media.put("medialink", medialink);
					if(borrower==null)
						media.put("borrower", "null");
					else
						media.put("borrower", borrower);
					ack.put("media", media);
					sendMessageToRole("user", ack);
				}
				//////////////////////////////////////////////////////////////////////
				//============================��Ƽ�̵�� �ڷ� ����ݳ�=========================//
				else if(service.equals("borrowmedia")){
					String user_id = message.getString("userid");
					String media_id = message.getString("mediaid");
					Statement stmt = con.createStatement();
					int rs = stmt.executeUpdate("UPDATE medias SET borrower = '" + user_id + "' WHERE media_id = " + media_id + " AND borrower IS NULL");
					if(rs!=0){
						JSONObject ack = new JSONObject();
						ack.put("service", "borrowmedia");
						ack.put("ack", "true");
						sendMessageToRole("user", ack);
						System.out.println(user_id + "���� " + media_id +"�� �뿩�Ͽ����ϴ�.");
					} else{
						JSONObject ack = new JSONObject();
						ack.put("service", "borrowmedia");
						ack.put("ack", "false");
						sendMessageToRole("user", ack);
					}
				}
				else if(service.equals("returnmedia")){
					String user_id = message.getString("userid");
					String media_id = message.getString("mediaid");
					Statement stmt = con.createStatement();
					int rs = stmt.executeUpdate("UPDATE medias SET borrower = NULL WHERE media_id =" + media_id + " AND borrower = '" + user_id+"'");
					if(rs!=0){
						JSONObject ack = new JSONObject();
						ack.put("service", "returnmedia");
						ack.put("ack", "true");
						sendMessageToRole("user", ack);
						System.out.println(user_id + "���� " + media_id +"�� �ݳ��Ͽ����ϴ�.");
					} else{
						JSONObject ack = new JSONObject();
						ack.put("service", "returnmedia");
						ack.put("ack", "false");
						sendMessageToRole("user", ack);
					}
				}
				///////////////////////////////////////////////////////////////////////
				//==============�뿩 �ڷ� ���===========================================//
				else if(service.equals("checkborrowed_media")){
					String user_id = message.getString("userid");
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM medias WHERE borrower = '"+ user_id + "'");
					JSONObject ack = new JSONObject();
					ack.put("service", "checkborrowed_media");
					JSONArray medias = new JSONArray();
					while(rs.next()){
						String mediaid = rs.getString("media_id");
						String title = rs.getString("title");
						String director = rs.getString("director");
						String production = rs.getString("production");
						String borrower = rs.getString("borrower");
						String medialink = rs.getString("medialink");
						MediaSpec mediaspec = new MediaSpec(mediaid, title, director, production, borrower, medialink);
						JSONObject media = mediaspec.getJSON();
						medias.put(media);
					}
					ack.put("media", medias);
					sendMessageToRole("user", ack);
				}
				//////////////////////////////////////////////////////////////////////
				//======================��Ƽ�̵�� �ڷ� ���=====================================//
				else if(service.equals("searchmedia")){
					String keyword = message.getString("keyword");
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM medias WHERE title LIKE '%" + keyword + "%'");
					JSONObject ack = new JSONObject();
					ack.put("service", "searchmedia");
					JSONArray medias = new JSONArray();
					while(rs.next()){
						String mediaid = rs.getString("media_id");
						String title = rs.getString("title");
						String director = rs.getString("director");
						String production = rs.getString("production");
						String borrower = rs.getString("borrower");
						String medialink = rs.getString("medialink");
						MediaSpec mediaspec = new MediaSpec(mediaid, title, director, production, borrower, medialink);
						JSONObject media = mediaspec.getJSON();
						medias.put(media);
					}
					ack.put("media", medias);
					sendMessageToRole("user", ack);
				}
				////////////////////////////////////////////////////////////////////////
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
		
		JavaDirector dbc = new JavaDirector();
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
