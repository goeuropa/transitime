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

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

import java.lang.*;


import java.net.MalformedURLException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.StringEntity;

import org.xml.sax.InputSource;


public class ProgressAvlModule extends PollUrlAvlModule {

	private static String avlURL="http://jsonplaceholder.typicode.com/posts/1";
//      private static String avlURL="http://www.rozklady.kiedybus.pl/kombus/dane.json";



        public ProgressAvlModule(String agencyId) {
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
            Thread.sleep(16000);
						//r.executeBashCommand("docker cp /home/onebusaway/gps.xml transitime-server-instance:/gps.xml");
         } catch (Exception e) {
            System.out.println(e);
         }
	try {
		String xml1 = "";
		String xml2 = "";
		String xml3 = "";
		String sessionId = "";
		String name = "";
		String reg = "";
		String id = "";
		String dt = "";
		String lon = "";
		String lat = "";
		String sn = "";
		String spd = "";
		HashMap names = new HashMap();

		String pusername = Credentials.pusername;
		String ppassword = Credentials.ppassword;
			try {
		DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost postRequest = new HttpPost(
				"http://www.progressgps.net:5015/");

			StringEntity input = new StringEntity("<ROZKAZ>Logowanie</ROZKAZ><LOGIN>"+pusername+"</LOGIN><HASLO>"+ppassword+"</HASLO>");
			input.setContentType("application/xml");
			postRequest.setEntity(input);

			HttpResponse response = httpClient.execute(postRequest);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
					+ response.getStatusLine().getStatusCode());
			}

			BufferedReader br = new BufferedReader(
	                        new InputStreamReader((response.getEntity().getContent())));

			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				xml1 += output;
			}

			httpClient.getConnectionManager().shutdown();

		  } catch (MalformedURLException e) {

			e.printStackTrace();

		  } catch (IOException e) {

			e.printStackTrace();

		  }

			 try {
			    InputSource is = new InputSource(new StringReader(xml1));
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(is);

					doc.getDocumentElement().normalize();

		System.out.println("Root element :" + doc.getDocumentElement().getNodeName());


		System.out.println("----------------------------");



			Node nNode = doc.getDocumentElement();

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;

				System.out.println("Session id : " + eElement.getElementsByTagName("IDSESJI").item(0).getTextContent());
				sessionId = eElement.getElementsByTagName("IDSESJI").item(0).getTextContent();
			}

	    } catch (Exception e) {
		e.printStackTrace();
	    }

			//actual positions

			try {
		DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost postRequest = new HttpPost(
				"http://www.progressgps.net:5015/");

			StringEntity input = new StringEntity("<ROZKAZ>GetVehicleList</ROZKAZ><IDSESJI>"+sessionId+"</IDSESJI>");
			input.setContentType("application/xml");
			postRequest.setEntity(input);

			HttpResponse response = httpClient.execute(postRequest);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
					+ response.getStatusLine().getStatusCode());
			}

			BufferedReader br = new BufferedReader(
													new InputStreamReader((response.getEntity().getContent())));

			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				xml2 += output;
			}

			httpClient.getConnectionManager().shutdown();

			} catch (MalformedURLException e) {

			e.printStackTrace();

			} catch (IOException e) {

			e.printStackTrace();

			}

			 try {
					InputSource is = new InputSource(new StringReader(xml2));
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(is);

					doc.getDocumentElement().normalize();

		System.out.println("Root element :" + doc.getDocumentElement().getNodeName());


		NodeList nList = doc.getElementsByTagName("VEH");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;

				name = eElement.getElementsByTagName("NM").item(0).getTextContent();
				reg = eElement.getElementsByTagName("RN").item(0).getTextContent();
				sn = eElement.getElementsByTagName("SN").item(0).getTextContent();
				names.put(sn,name);
			}

		}
			} catch (Exception e) {
		e.printStackTrace();
			}

			try {
		DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost postRequest = new HttpPost(
				"http://www.progressgps.net:5015/");

			StringEntity input = new StringEntity("<ROZKAZ>GetActualPositions</ROZKAZ><IDSESJI>"+sessionId+"</IDSESJI>");
			input.setContentType("application/xml");
			postRequest.setEntity(input);

			HttpResponse response = httpClient.execute(postRequest);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
					+ response.getStatusLine().getStatusCode());
			}

			BufferedReader br = new BufferedReader(
													new InputStreamReader((response.getEntity().getContent())));

			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				xml3 += output;
			}

			httpClient.getConnectionManager().shutdown();

			} catch (MalformedURLException e) {

			e.printStackTrace();

			} catch (IOException e) {

			e.printStackTrace();

			}

			 try {
					InputSource is = new InputSource(new StringReader(xml3));
					DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
					Document doc = dBuilder.parse(is);

					doc.getDocumentElement().normalize();

		System.out.println("Root element :" + doc.getDocumentElement().getNodeName());


					NodeList nList = doc.getElementsByTagName("DEV");

						for (int temp = 0; temp < nList.getLength(); temp++) {

							Node nNode = nList.item(temp);

						if (nNode.getNodeType() == Node.ELEMENT_NODE) {

							Element eElement = (Element) nNode;

							//System.out.println("Session id : " + eElement.getElementsByTagName("IDSESJI").item(0).getTextContent());
							id = eElement.getElementsByTagName("ID").item(0).getTextContent();
							dt = eElement.getElementsByTagName("DT").item(0).getTextContent();
							lon = eElement.getElementsByTagName("LON").item(0).getTextContent();
							lat = eElement.getElementsByTagName("LAT").item(0).getTextContent();
							spd = eElement.getElementsByTagName("SPD").item(0).getTextContent();

							name = (String)names.get(id);
							String[] arr = name.split(" - ");
							//System.out.println("Device: "+arr[0]+","+dt+","+lon+","+lat);

									/*System.out.println("Lat : " + eElement.getElementsByTagName("Latitude").item(0).getTextContent());
									System.out.println("Long : " + eElement.getElementsByTagName("Longtitude").item(0).getTextContent());
									System.out.println("TimeOfRecord : " + eElement.getElementsByTagName("TimeOfRecord").item(0).getTextContent());
									System.out.println("Current orient : " + eElement.getElementsByTagName("CurrentOrientation").item(0).getTextContent());
									System.out.println("Vehicle ID: " + eElement.getElementsByTagName("VehicleId").item(0).getTextContent());
									*/
									String vehicleId = arr[0];


									Double latitude = Double.parseDouble(lat);
									Double longitude = Double.parseDouble(lon);
									float heading=Float.NaN;
									float speed = Float.parseFloat(spd);
									String time = dt;

													//2016-09-07 17:02:48
									SimpleDateFormat dateformater=new SimpleDateFormat("yyMMddHHmmss");
									/*if ( time.contains("2099") == true )
										continue;*/
									Date timestamp=dateformater.parse(time);
									Calendar cal = new GregorianCalendar();
									cal.setTime(timestamp);
									//cal.add(Calendar.HOUR_OF_DAY, 2);
									//System.out.println("data:"+vehicleId+ timestamp.getTime()+ latitude+longitude+ heading+ speed);

										AvlReport avlReport =
																 new AvlReport(vehicleId, cal.getTime().getTime(), latitude,
																									longitude, heading, speed, vehicleId);
										if ( avlReport != null )
											{
												processAvlReport(avlReport);
												avlReports.add(avlReport);
											}
						}
			}
			} catch (Exception e) {
		e.printStackTrace();
			}
	} catch (Exception e) {
e.printStackTrace();
	}


return null;
}

        /**
         * Just for debugging
         */
        public static void main(String[] args) {
                // Create a WexfordCoachAvlModule for testing
                //Module.start("org.transitime.custom.kombus.KombusAvlModule");
        }
}
