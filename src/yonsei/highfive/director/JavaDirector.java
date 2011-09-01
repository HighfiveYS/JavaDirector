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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import yonsei.highfive.circulation.BookSpec;
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
	 * mysql DB server와 연결
	 * @return 
	 */
	public static Connection makeConnection(){
		String url = "jdbc:mysql://165.132.214.212:3306/librarydb";
		String id = "highfive";
		String password = "fivehigh";
		Connection con = null;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection(url, id, password);
		}catch(ClassNotFoundException e){
			System.out.println("드라이버를 찾을 수 없습니다.");
		}catch(SQLException e){
			e.printStackTrace();
			System.out.println("연결에 실패하였습니다.");
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
				//==========================학사정보인증과정==============================//
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
						System.out.println(id + "님이 학사 인증되었습니다.");
						// 후에 sendMessageToRole을 sendMessageToActor로 바꾸어주어야함
						// 현재는 pidgin을 이용한 디버깅을 목적으로 사용함
					}else{
						JSONObject ack = new JSONObject();
						ack.put("accept", "false");
						sendMessageToRole("user", ack);
						// 후에 sendMessageToRole을 sendMessageToActor로 바꾸어주어야함
						// 현재는 pidgin을 이용한 디버깅을 목적으로 사용함
					}
				}
				////////////////////////////////////////////////////////////////////////
				//==========================도서확인과정==============================//
				else if(service.equals("checkbook")){
					String book_id = message.getString("bookid");
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM books");
					String title = null, author = null, publisher = null, borrower = null;
					while(rs.next()){
						if(rs.getString("book_id").equals(book_id)){
							title = rs.getString("title");
							author = rs.getString("author");
							publisher = rs.getString("publisher");
							borrower = rs.getString("borrower");
						}
					}
					JSONObject ack = new JSONObject();
					JSONObject book = new JSONObject();
					ack.put("service", "checkbook");
					book.put("bookid", book_id);
					book.put("title", title);
					book.put("author", author);
					book.put("publisher", publisher);
					if(borrower==null)
						book.put("borrower", "null");
					else
						book.put("borrower", borrower);
					ack.put("book", book);
					sendMessageToRole("user", ack);
				}
				//////////////////////////////////////////////////////////////////////
				//============================도서대출반납=========================//
				else if(service.equals("borrowbook")){
					String user_id = message.getString("userid");
					String book_id = message.getString("bookid");
					Statement stmt = con.createStatement();
					int rs = stmt.executeUpdate("UPDATE books SET borrower = " + user_id + " WHERE book_id = " + book_id + " AND borrower IS NULL");
					if(rs!=0){
						JSONObject ack = new JSONObject();
						ack.put("service", "borrowbook");
						ack.put("ack", "true");
						sendMessageToRole("user", ack);
						System.out.println(user_id + "님이 " + book_id +"를 대여하였습니다.");
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
					int rs = stmt.executeUpdate("UPDATE books SET borrower = NULL WHERE book_id =" + book_id + " AND borrower = " + user_id);
					if(rs!=0){
						JSONObject ack = new JSONObject();
						ack.put("service", "returnbook");
						ack.put("ack", "true");
						sendMessageToRole("user", ack);
						System.out.println(user_id + "님이 " + book_id +"를 반납하였습니다.");
					} else{
						JSONObject ack = new JSONObject();
						ack.put("service", "returnbook");
						ack.put("ack", "false");
						sendMessageToRole("user", ack);
					}
				}
				///////////////////////////////////////////////////////////////////////
				//==============대여도서목록===========================================//
				else if(service.equals("checkborrowed")){
					String user_id = message.getString("userid");
					Statement stmt = con.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM books WHERE borrower = "+ user_id);
					JSONObject ack = new JSONObject();
					ack.put("service", "checkborrowed");
					JSONArray books = new JSONArray();
					while(rs.next()){
						String bookid = rs.getString("book_id");
						String title = rs.getString("title");
						String author = rs.getString("author");
						String publisher = rs.getString("publisher");
						String borrower = rs.getString("borrower");
						BookSpec bookspec = new BookSpec(bookid, title, author, publisher, borrower);
						JSONObject book = bookspec.getJSON();
						books.put(book);
					}
					ack.put("book", books);
					sendMessageToRole("user", ack);
				}
				//////////////////////////////////////////////////////////////////////
				//======================도서검색 목록=====================================//
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
						BookSpec bookspec = new BookSpec(bookid, title, author, publisher, borrower);
						JSONObject book = bookspec.getJSON();
						books.put(book);
					}
					ack.put("book", books);
					sendMessageToRole("user", ack);
				}
				////////////////////////////////////////////////////////////////////////
				//============컨트롤러 디렉터==============================================//
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
				//==========================좌석확인과정==============================//
				else if(service.equals("checkseat")){
					String SeatID = message.getString("SeatID");
					
					Statement stmt = con.createStatement();
					
					ResultSet rs = stmt.executeQuery("SELECT * FROM seats");
					
					String UserID = null, StartTime = null, EndTime = null;
					
					while(rs.next()){
						if(rs.getString("SeatID").equals(SeatID)){
							UserID = rs.getString("UserID");
							StartTime = rs.getString("StartTime");
							EndTime = rs.getString("EndTime");
						}
					}
					JSONObject ack = new JSONObject();
					JSONObject seat = new JSONObject();
					ack.put("service", "checkseat");
					seat.put("SeatID", SeatID);
					seat.put("UserID", UserID);
					seat.put("StartTime", StartTime);
					seat.put("EndTime", EndTime);

					sendMessageToRole("user", ack);
				}
				//////////////////////////////////////////////////////////////////////
				//============================좌석배정반납연장=========================//
				else if(service.equals("occupyseat")){
					String SeatID = message.getString("SeatID");
					String UserID = message.getString("UserID");
					int Hour = message.getInt("Hour");

					Statement stmt = con.createStatement();
					
					int su = stmt.executeUpdate("UPDATE seats SET UserID = " + UserID + " WHERE SeaetID = " + SeatID + " AND UserID IS NULL");

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
					
					int ss = stmt.executeUpdate("UPDATE seats SET StartTime = " + StartTime + " WHERE SeaetID = " + SeatID);
					int se = stmt.executeUpdate("UPDATE seats SET EndTime = " + EndTime + " WHERE SeaetID = " + SeatID);
					
					if(su!=0 && ss!=0 && se!=0){
						JSONObject ack = new JSONObject();
						ack.put("service", "occupyseat");
						ack.put("ack", "true");
						sendMessageToRole("user", ack);
						System.out.println(UserID + "님이 " + SeatID +" 좌석을 배정받았습니다.");
					} else{
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
					
					int su = stmt.executeUpdate("UPDATE seats SET UserID = " + UserID + " WHERE SeaetID = " + SeatID);
					int ss = stmt.executeUpdate("UPDATE seats SET StartTime = '0000-00-00 00:00:00' WHERE SeaetID = " + SeatID);
					int se = stmt.executeUpdate("UPDATE seats SET EndTime = '0000-00-00 00:00:00' WHERE SeaetID = " + SeatID);
					
					if(su!=0 && ss!=0 && se!=0){
						JSONObject ack = new JSONObject();
						ack.put("service", "returnseat");
						ack.put("ack", "true");
						sendMessageToRole("user", ack);
						System.out.println(UserID+ "님이 " + SeatID +" 좌석을 반납하였습니다.");
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
					
					Date date = null;
					ResultSet rs = stmt.executeQuery("SELECT * FROM seats");
					while(rs.next()){
						String number = rs.getString("SeatID");
						if(number.equals(SeatID)){
							date = rs.getTime("EndTime");
							break;
						}
					}
					
					date.setTime(date.getTime()+(long)(1000*60*60*Hour));
					
					SimpleDateFormat yearformat = new SimpleDateFormat("yyyy");
					SimpleDateFormat monthformat = new SimpleDateFormat("MM");
					SimpleDateFormat dayformat = new SimpleDateFormat("dd");
					SimpleDateFormat timeformat = new SimpleDateFormat("HH:mm:ss");
					
					String endyear = yearformat.format(date);
					String endmonth = monthformat.format(date);
					String endday = dayformat.format(date);
					String endtime = timeformat.format(date);
							
					String EndTime = endyear + "-" + endmonth + "-" + endday + " " + endtime;
					
					int se = stmt.executeUpdate("UPDATE seats SET EndTime = " + EndTime + " WHERE SeatID =" + SeatID);
					
					if(se!=0){
						JSONObject ack = new JSONObject();
						ack.put("service", "extentseat");
						ack.put("ack", "true");
						sendMessageToRole("user", ack);
						System.out.println(UserID + "님이 " + SeatID +" 좌석을 연장하였습니다.");
					} else{
						JSONObject ack = new JSONObject();
						ack.put("service", "extentseat");
						ack.put("ack", "false");
						sendMessageToRole("user", ack);
					}
				}
				//////////////////////////////////////////////////////////////////////
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
