package org.transitime.avl;

import java.io.InputStream;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.transitime.avl.PollUrlAvlModule;
import org.transitime.db.structs.AvlReport;
import org.transitime.modules.Module;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;
import org.json.JSONTokener;
import org.transitime.configData.AvlConfig;

public class TraccarAvlModule extends PollUrlAvlModule {

	private static String avlURL="http://jsonplaceholder.typicode.com/posts/1";
//      private static String avlURL="http://www.rozklady.kiedybus.pl/kombus/dane.json";


        public static String positionsURL="";//"http://iplaner.pl:8081/api/positions/";

        public TraccarAvlModule(String agencyId) {
								super(agencyId);
								AvlConfig avlConfig = new AvlConfig();
								this.positionsURL = "http://iplaner.pl:8081/api/positions/";//avlConfig.avlPositionsUrl();
        }

        @Override
        protected String getUrl() {

                return avlURL;
        }

	/*private JSONArray getJson(String url){
	};
	private ArrayList<Position> getPositions(JSONArray json){
	}
	private ArrayList<Device> getDevices(JSONArray json){
	}
	private Credentials getCredentials(){
		String filename = "credentials.txt";
		  try {
		   File file = new File(fileName);
		   Scanner scanner = new Scanner(file);
		   while (scanner.hasNext()) {
		    String loginpass = (String)scanner.next();
		    String[] arr = loginpass.split(",");
		    Credentials c = new Credentials();
		    c.login = arr[0];
		    c.password = arr[1];
		    return c;
		   }
		   scanner.close();
		  } catch (FileNotFoundException e) {
		   e.printStackTrace();
		  }
		 }
	}*/
        @Override
        protected Collection<AvlReport> processData(InputStream inp) throws Exception {

	ArrayList<AvlReport> avlReports = new ArrayList<AvlReport>();
        String username = Credentials.username;
        String password = Credentials.password;

URL url = new URL(this.positionsURL);
URLConnection uc = url.openConnection();
String userpass = username + ":" + password;
String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
uc.setRequestProperty ("Authorization", basicAuth);
InputStream in = uc.getInputStream();

//      InputStream in = new URL(this.devicesURL).openStream();

        Scanner s = new Scanner(in).useDelimiter("\\A");
        String JSON_DATA = s.hasNext() ? s.next() : "";
        System.out.println("JSON DATA: " + JSON_DATA);

//      JSONObject obj = new JSONObject(JSON_DATA);
                                        JSONArray array = (JSONArray) new JSONTokener(JSON_DATA).nextValue();
                for (int i=0; i<array.length(); ++i) {
                                                JSONObject entry = array.getJSONObject(i);
                                                String deviceId = String.valueOf(entry.getInt("deviceId"));
                                                String vehicleId = String.valueOf(entry.getInt("id"));


                                                Double latitude = entry.getDouble("latitude");
                                                Double longitude = entry.getDouble("longitude");
float heading=Float.NaN;
float speed = (float)entry.getDouble("speed");
                                                String time = entry.getString("deviceTime");

                                                        //2016-09-07 17:02:48
						SimpleDateFormat dateformater=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

                                                        Date timestamp=dateformater.parse(time.replace( "T" , " " ));
Calendar cal = new GregorianCalendar();
    cal.setTime(timestamp);
    cal.add(Calendar.HOUR_OF_DAY, 2); //plus 2 hours
//System.out.println("data:"+vehicleId+ timestamp.getTime()+ latitude+longitude+ heading+ speed);

						if ( ( deviceId.equals("10") == true ) || ( deviceId.equals("11") == true ) || ( deviceId.equals("12") == true ) || ( deviceId.equals("4") == true ) ){
					AvlReport avlReport = null;
						if ( deviceId.equals("10") == true )
	                                                avlReport =
                                                               new AvlReport("swarzedz4009", cal.getTime().getTime(), latitude,
                                                                                longitude, heading, speed, "swarzedz4009");
						if ( deviceId.equals("11") == true )
	                                                avlReport =
                                                               new AvlReport("swarzedz4006", cal.getTime().getTime(), latitude,
                                                                                longitude, heading, speed, "swarzedz4006");
						if ( deviceId.equals("12") == true )
	                                                avlReport =
                                                               new AvlReport("swarzedz4007", cal.getTime().getTime(), latitude,
                                                                                longitude, heading, speed, "swarzedz4007");
						if ( deviceId.equals("4") == true )
																									avlReport =
																															 new AvlReport("nissan", cal.getTime().getTime(), latitude,
																																								longitude, heading, speed, "nissan");
						//avlReport.setAssignment("497-1",AvlReport.AssignmentType.ROUTE_ID);
	                                        processAvlReport(avlReport);
						avlReports.add(avlReport);
}
                                                        }
return null;
        }
        /**
         * Just for debugging
         */
        public static void main(String[] args) {
                // Create a WexfordCoachAvlModule for testing
                Module.start("org.transitime.custom.kombus.KombusAvlModule");
        }
}
/*class Device {
	public String id;
	public String name;
}
class Position {
	public String deviceId;
	public String vehicleId;
	public Double latitude;
	public Double longitude;
	public float heading;
	public float speed;
	public Date timestamp;
}
class Credentials {
	public String login;
	public String password;
}*/
