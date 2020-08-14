package org.transitime.avl;

import java.io.InputStream;
import org.json.JSONObject;
import org.transitime.avl.PollUrlAvlModule;
import org.transitime.config.StringConfigValue;
import org.transitime.db.structs.AvlReport;
import org.transitime.modules.Module;
import java.util.*;
import java.net.HttpURLConnection;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.StreamGobbler;


public class GlobalTeamAvlModuleNew extends PollUrlAvlModule {

	private static String avlURL = "http://jsonplaceholder.typicode.com/posts/1";
	
	private String token = "";
		
	private static StringConfigValue globalTeamPositionUrl = 
			new StringConfigValue("transitime.avl.globalteam.positionurl",
					"positions",
					"The URL of the NextBus feed to use.");
	
	private static String getGlobalTeamPositionUrl() {
		return globalTeamPositionUrl.getValue();
	}
	
	private static StringConfigValue globalTeamDevicesUrl = 
			new StringConfigValue("transitime.avl.globalteam.devicesurl", 
					"devices",
					"The URL of the NextBus feed to use.");
	
	private static String getGlobalTeamDevicesUrl() {
		return globalTeamDevicesUrl.getValue();
	}
	
	private static StringConfigValue globalTeamAuthenticateUrl = 
			new StringConfigValue("transitime.avl.globalteam.authenticateurl", 
					"authenticate",
					"The URL of the NextBus feed to use.");
	
	private static String getGlobalTeamAuthenticateUrl() {
		return globalTeamAuthenticateUrl.getValue();
	}

	
	private static StringConfigValue globalTeamUsername = 
			new StringConfigValue("transitime.avl.globalteam.username", 
					"username",
					"Username.");
	
	private static String getGlobalTeamUsername() {
		return globalTeamUsername.getValue();
	}
	
	private static StringConfigValue globalTeamCode = 
			new StringConfigValue("transitime.avl.globalteam.code", 
					"code",
					"Code.");
	
	private static String getGlobalTeamCode() {
		return globalTeamCode.getValue();
	}
	
	private static StringConfigValue globalTeamPassword = 
			new StringConfigValue("transitime.avl.globalteam.password", 
					"123",
					"Password.");
	
	private static String getGlobalTeamPassword() {
		return globalTeamPassword.getValue();
	}
	
	private String getPositions() throws Exception{
		String jsonInputString = "{\"login\": \"" + getGlobalTeamUsername() + "\", \"code\": \"" + 
				getGlobalTeamCode() + "\", \"token\": \"" + token + "\"}";
		byte[] input = jsonInputString.getBytes("UTF-8");
		java.net.URL url = new URL(
				getGlobalTeamPositionUrl());
		System.out.println("GetPositions");
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Content-Length", Integer.toString(input.length));
		con.setDoOutput(true);
		con.setDoInput(true);
		
		
		OutputStream os = con.getOutputStream();
		os.write(input);
		os.close();
		con.connect();
		
		
        int status = con.getResponseCode();//this cannot be invoked before data stream is ready when performing HTTP POST
        if(status == 403) {
        	return "Invalid Credentials";
        }

		StringBuilder response = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {

			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}

		}

		return response.toString();
	
	}
	
	private void authenticate() throws Exception{
		String jsonInputString = "{\"login\": \"" + getGlobalTeamUsername() + "\", \"code\": \"" + 
				getGlobalTeamCode() + "\", \"password\": \"" + getGlobalTeamPassword() + "\"}";
		byte[] input = jsonInputString.getBytes("UTF-8");
		java.net.URL url = new URL(
				getGlobalTeamAuthenticateUrl());
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Content-Length", Integer.toString(input.length));
		con.setDoOutput(true);
		con.setDoInput(true);
		
		
		OutputStream os = con.getOutputStream();
		os.write(input);
		os.close();
		con.connect();
		
		StringBuilder response = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {

			String responseLine = null;
			while ((responseLine = br.readLine()) != null) {
				response.append(responseLine.trim());
			}

		}

		JSONObject json = new JSONObject(response.toString());
		token = json.get("token").toString();
		
	}
	

	public GlobalTeamAvlModuleNew(String agencyId) {
		super(agencyId);
	}

	@Override
	protected String getUrl() {

		return avlURL;
	}

	@Override
	protected Collection<AvlReport> processData(InputStream inp) throws Exception {

		ArrayList<AvlReport> avlReports = new ArrayList<AvlReport>();

		try {

			String response = getPositions();
			if(response.equals("Invalid Credentials")) {
				authenticate();
				return null;
			}

			JSONObject json = new JSONObject(response.toString());
			ObjectMapper m = new ObjectMapper();
			PositionList positionList = m.readValue(json.toString(), PositionList.class);
			Calendar cal = new GregorianCalendar();
			DeviceList deviceList = getDeviceList();
			Device deviceResult = new Device();
			for (Position p : positionList.getPositionList()) {
				for(Device d : deviceList.getDeviceList()) {
					if(d.deviceId.equals(p.getDeviceId())) {
						deviceResult = d;
						break;
					}
				}
				cal.set(p.getDateTime().getYear(), p.getDateTime().getMonth(), p.getDateTime().getDay(),
						p.getDateTime().getHour(), p.getDateTime().getMinute(), p.getDateTime().getSeconds());
				AvlReport avlReport = new AvlReport(deviceResult.deviceName, cal.getTime().getTime(), p.getCoordinate().getLatitude(),
						p.getCoordinate().getLongitude(), p.getSpeed(), p.getHeading(), deviceResult.deviceName);
				processAvlReport(avlReport);
				avlReports.add(avlReport);
			}

			Thread.sleep(1000 * 5);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	DeviceList getDeviceList() throws Exception {
		DeviceList deviceList = new DeviceList();
		try {
			
			String jsonInputString = "{\"login\": \"" + getGlobalTeamUsername() + "\", \"code\": \"" + 
					getGlobalTeamCode() + "\", \"token\": \"" + token + "\"}";
			byte[] input = jsonInputString.getBytes("UTF-8");
			java.net.URL url = new URL(
					getGlobalTeamDevicesUrl());
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("Content-Length", Integer.toString(input.length));
			con.setDoOutput(true);
			con.setDoInput(true);
			
			
			OutputStream os = con.getOutputStream();
			os.write(input);
			os.close();
			con.connect();


			StringBuilder response = new StringBuilder();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {

				String responseLine = null;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}

			}

			JSONObject json = new JSONObject(response.toString());
			ObjectMapper m = new ObjectMapper();
			deviceList = m.readValue(json.toString(), DeviceList.class);

			Thread.sleep(1000 * 5);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return deviceList;
	}


	/**
	 * Just for debugging
	 */
	public static void main(String[] args) {
		// Create a WexfordCoachAvlModule for testing
		Module.start("org.transitime.avl.GlobalTeamAvlModuleNew");
	}
}

class DeviceList {
	List<Device> deviceList;

	public DeviceList(List<Device> deviceList) {
		super();
		this.deviceList = deviceList;
	}

	public DeviceList() {
		super();
	}

	public List<Device> getDeviceList() {
		return deviceList;
	}

	public void setDeviceList(List<Device> deviceList) {
		this.deviceList = deviceList;
	}

	@Override
	public String toString() {
		return "DeviceList [deviceList=" + deviceList + "]";
	}

}

class Device {
	String deviceId;
	String deviceName;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public Device(String deviceId, String deviceName) {
		this.deviceId = deviceId;
		this.deviceName = deviceName;
	}

	public Device() {
		super();
	}

	@Override
	public String toString() {
		return "Device [deviceId=" + deviceId + ", deviceName=" + deviceName + "]";
	}

}

class PositionList {
	List<Position> positionList;

	public List<Position> getPositionList() {
		return positionList;
	}

	public void setPositionList(List<Position> positionList) {
		this.positionList = positionList;
	}

	public PositionList() {
		super();
	}

	public PositionList(List<Position> positionList) {
		this.positionList = positionList;
	}

	@Override
	public String toString() {
		return "PositionList [positionList=" + positionList + "]";
	}

}

class Position {
	String deviceId;
	Coordinate coordinate;
	int heading;
	int speed;
	String ignitionState;
	DateTime dateTime;

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public void setCoordinate(Coordinate coordinate) {
		this.coordinate = coordinate;
	}

	public int getHeading() {
		return heading;
	}

	public void setHeading(int heading) {
		this.heading = heading;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public String getIgnitionState() {
		return ignitionState;
	}

	public void setIgnitionState(String ignitionState) {
		this.ignitionState = ignitionState;
	}

	public DateTime getDateTime() {
		return dateTime;
	}

	public void setDateTime(DateTime dateTime) {
		this.dateTime = dateTime;
	}

	public Position() {
		super();
	}

	public Position(String deviceId, Coordinate coordinate, int heading, int speed, String ignitionState,
			DateTime dateTime) {
		this.deviceId = deviceId;
		this.coordinate = coordinate;
		this.heading = heading;
		this.speed = speed;
		this.ignitionState = ignitionState;
		this.dateTime = dateTime;
	}

	@Override
	public String toString() {
		return "Position [deviceId=" + deviceId + ", coordinate=" + coordinate + ", heading=" + heading + ", speed="
				+ speed + ", ignitionState=" + ignitionState + ", dateTime=" + dateTime + "]";
	}

}

class Coordinate {
	float latitude;
	float longitude;

	public Coordinate(float latitude, float longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public float getLatitude() {
		return latitude;
	}

	public void setLatitude(float latitude) {
		this.latitude = latitude;
	}

	public float getLongitude() {
		return longitude;
	}

	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}

	public Coordinate() {
		super();
	}

	@Override
	public String toString() {
		return "Coordinate [latitude=" + latitude + ", longitude=" + longitude + "]";
	}

}

class DateTime {
	int year;
	int month;
	int day;
	int hour;
	int minute;
	int seconds;
	String timezone;

	public DateTime(int year, int month, int day, int hour, int minute, int seconds, String timezone) {
		this.year = year;
		this.month = month;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
		this.seconds = seconds;
		this.timezone = timezone;
	}

	public DateTime() {
		super();
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getDay() {
		return day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public int getHour() {
		return hour;
	}

	public void setHour(int hour) {
		this.hour = hour;
	}

	public int getMinute() {
		return minute;
	}

	public void setMinute(int minute) {
		this.minute = minute;
	}

	public int getSeconds() {
		return seconds;
	}

	public void setSeconds(int seconds) {
		this.seconds = seconds;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	@Override
	public String toString() {
		return "DateTime [year=" + year + ", month=" + month + ", day=" + day + ", hour=" + hour + ", minute=" + minute
				+ ", seconds=" + seconds + ", timezone=" + timezone + "]";
	}

}
