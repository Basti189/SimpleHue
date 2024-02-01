package app.wolfware.bridge;

import app.wolfware.light.HueLight;
import app.wolfware.room.HueRoom;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class HueBridge {

	// Debug
	private boolean mDebug = false;

	// Discover
	private final static String DISCOVERY_HOST = "discovery.meethue.com";
	private final static String SSDP_HOST = "239.255.255.250";
	private final static long SSDP_TIMEOUT = 60000;
	private final static int SSDP_PORT = 1900;
	private final static int HTTPS_PORT = 443;
	private String mApllicationName;
	private String mDeviceName;

	// Read & Write
	private final static String ROOT_FOLDER = "huebridge";

	// Connection
	private String mIP = "";
	private String mID = "";
	private String mToken = "";
	private boolean mVerify = false;

	// For method verify
	public final static int OK = 0;
	public final static int TOKEN_INVALID = 1;
	public final static int CONNECTION_REFUSED = 2;

	// Hue management
	public HueLight light = new HueLight();
	public HueRoom room = new HueRoom();

	// ****************************** [ Initialization ] ****************************** //
	
	public HueBridge() {

	}

	public void init(String application_name, String device_name) {
		File rootDir = new File(ROOT_FOLDER);
		if (!rootDir.exists()) {
			if (rootDir.mkdir()) {
				println("An Error has occurred while creating root directory");
			}
		}

		mApllicationName = application_name;
		mDeviceName = device_name;
	}

	public void setDebugOutput(boolean debug) {
		mDebug = debug;
	}

	public void println(String s) {
		if (mDebug) {
			System.out.println("<HueBridge> " + s);
		}
	}

	// ****************************** [ Discover & Token management] ****************************** //

	public void setAdress(String ip) {
		mIP = ip;
	}
	
	public void setToken(String token) {
		mToken = token;
	}

	public void setAdressAndToken(String ip, String token) {
		mIP = ip;
		mToken = token;
	}

	public boolean discoverNUPNP() {
		try {
			URL url = new URL("https://" + DISCOVERY_HOST);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			
			BufferedReader in = new BufferedReader(new InputStreamReader(
			con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.indexOf("id") != -1 && inputLine.indexOf("internalipaddress") != -1) {
					// ID
					mID = inputLine.substring(0, inputLine.indexOf(","));
					mID = mID.replace("[{\"id\":\"", "");
					mID = mID.replace("\"", "");
					println("Bridge ID: " + mID);
					
					//IP
					mIP = inputLine.substring(inputLine.indexOf(",") + 1, inputLine.length());
					mIP = mIP.replace("\"internalipaddress\":\"", "");
					mIP = mIP.replace("\"}]", "");
					println("Bridge IP: " + mIP);
					return true;
				}
			} 
			in.close();
		} catch (Exception e) {
			println("Can't connect to \"https://" + DISCOVERY_HOST + "\"");
		}
		return false;
	}
	
	public boolean discoverUPNP() {
		//TODO
		return false;
	}
	
	public boolean requestToken() {
		//TODO
		return false;
	}
	
	public boolean verify() {
		//TODO
		return false;
	}
	
	public boolean isVerified() {
		return mVerify;
	}
	
	// ****************************** [ Hue name management ] ****************************** //
	
	public int getAllLights() {
		println("Search for HueLights");
		light.clear();
		int count = 0;
		for (int lightID = 1 ; lightID <= 50 ; lightID++) {
			String content;
			if ((content = GET("lights/" + lightID)).indexOf("error") != -1) {
				continue;
			}
			light.add(lightID, getValueFromObject("name", content));
			count++;
		}
		println(count + " HueLights assigned");
		return count;
	}
	
	public int getAllRooms() {
		println("Search for HueRooms");
		room.clear();
		int count = 0;
		for (int roomID = 1 ; roomID <= 50 ; roomID++) {
			String content;
			if ((content = GET("groups/" + roomID)).indexOf("error") != -1) {
				continue;
			}
			if (getValueFromObject("type", content).equals("Room")) {
				room.add(roomID, getValueFromObject("name", content));
				count++;
			}
		}
		println(count + " HueRooms assigned");
		return count;
	}
	
	// ****************************** [ HueLight Control ] ****************************** //

	// All Lights
	// Turn Light on or off
	public boolean setLightState(int lightID, boolean on) {
		String payload = "{\"on\":";
		if (on) {
			payload += "true}";
		} else {
			payload += "false}";
		}
		if (PUT("lights/" + lightID + "/state", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// Returns light status (on/off)
	public boolean getLightState(int lightID) {
		String content;
		if ((content = GET("lights/" + lightID)).length() > 0) {
			String result = getValueFromObject("on", content);
			if (result.equals("true")) {
				return true;
			}
		}
		return false;
	}
	
	// Turns light on and set brightness or turns light off
	public boolean setLightBrightness(int lightID, int brightness) {
		String payload = "{\"on\":";
		if (brightness > 0) {
			payload += "true,";
			payload += "\"bri\":" + brightness + "}";
		} else {
			payload += "false}";
		}
		if (PUT("lights/" + lightID + "/state", payload).indexOf("succes") != -1) {
			return true;
		}
		return false;
	}
	
	// Returns brightness
	public int getLightBrightness(int lightID) {
		String content;
		if ((content = GET("lights/" + lightID)).length() > 0) {
			return getValueFromObject("bri", content, 0);			
		}
		return 0;
	}
	
	// Turns light on and set colortemperature (kelvin or mired)
	public boolean setLightColorTemperature(int lightID, int colorTemperature) {
		if (colorTemperature <= 6500 && colorTemperature >= 2000) { 		// valid kelvin
			colorTemperature = 1000000 / colorTemperature; 					// convert kelvin to mired
		} else if (colorTemperature <= 500 && colorTemperature >= 153) { 	// valid mired
			// nothing to do
		} else {															// invalid
			return false;
		}

		String payload = "{\"on\":true,";
		payload += "\"ct\":" + colorTemperature + "}";
		if (PUT("lights/" + lightID + "/state", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// Returns colortemperature in mired by default
	public int getLightColorTemperature(int lightID) {
		return getLightColorTemperature(lightID, false);
	}
	
	// Returns colortemperature in mired by default or convert to kelvin
	public int getLightColorTemperature(int lightID, boolean convertToKelvin) {
		String content;
		if ((content = GET("lights/" + lightID)).length() > 0) {
			if (convertToKelvin) {
				int colorTemperature = 1000000 / getValueFromObject("ct", content, 1);
				if (colorTemperature < 2000) {
					colorTemperature = 2000;
				} else if (colorTemperature > 6500) {
					colorTemperature = 6500;
				}
				return colorTemperature;
			} else { // default
				return getValueFromObject("ct", content, 0);
			}
		}
		return 0;
	}
	
	// Set alert mode
	public boolean setLightAlert(int lightID, int alert) {
		String payload = "{\"alert\":";
		switch(alert) {
			case HueLight.ALERT_NONE:
				payload += "\"none\"}";
				break;
			case HueLight.ALERT_SELECT:
				payload += "\"select\"}";
				break;
			case HueLight.ALERT_LSELECT:
				payload += "\"lselect\"}";
				break;
		}
		if (PUT("lights/" + lightID + "/state", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// Returns if HueLight is reachable
	public boolean isLightReachable(int lightID) {
		String content;
		if ((content = GET("lights/" + lightID)).length() > 0) {
			String result = getValueFromObject("reachable", content);
			if (result.equals("true")) {
				return true;
			}
		}
		return false;
	}
	
	// Only Color Lights
	// Turns light on and set saturation
	boolean setLightSaturation(int lightID, int saturation) {
		String payload = "{\"on\":true,";
		payload += "\"sat\":" + saturation + "}";
		if (PUT("lights/" + lightID + "/state", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// Returns saturation
	int getLightSaturation(int lightID) {
		String content;
		if ((content = GET("lights/" + lightID)).length() > 0) {
			return getValueFromObject("sat", content, 0);
		}
		return 0;
	}
	
	// Turns light on and set hue
	public boolean setLightHue(int lightID, int hue) {
		String payload = "{\"on\":true,";
		payload += "\"hue\":" + hue + "}";
		if (PUT("lights/" + lightID + "/state", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// Return hue
	public int getLightHue(int lightID) {
		String content;
		if ((content = GET("lights/" + lightID)).length() > 0) {
			return getValueFromObject("hue", content, 0);
		}
		return 0;
	}
	
	// Turns light on and set XY color
	public boolean setLightColor(int lightID, double x, double y) {
		String payload = "{\"on\":true,";
		payload += "\"xy\":[" + x + "," + y + "]}";
		if (PUT("lights/" + lightID + "/state", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// Turns light on and set XY color from rgb
	public boolean setLightColor(int lightID, int r, int g, int b) {
		double[] xy = light.convertRGBtoXY(r, g, b);
		return setLightColor(lightID, xy[0], xy[1]);
	}
	
	// Turns light on and set effect
	public boolean setLightEffect(int lightID, int effect) {
		String payload = "{\"on\":true,";
		payload += "\"effect\":";
		switch(effect) {
			case HueLight.EFFECT_NONE:
				payload += "\"none\"}";
				break;
			case HueLight.EFFECT_COLORLOOP:
				payload += "\"colorloop\"}";
				break;
		}
		if (PUT("lights/" + lightID + "/state", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// Mixed methods
	public boolean setLightBrightnessAndColorTemperature(int lightID, int brightness, int colorTemperature) {
		if (colorTemperature <= 6500 && colorTemperature >= 2000) { 		// valid kelvin
			colorTemperature = 1000000 / colorTemperature; 					// convert kelvin to mired
		} else if (colorTemperature <= 500 && colorTemperature >= 153) { 	// valid mired
			// nothing to do
		} else {															// invalid
			return false;
		}

		String payload = "{\"on\":";
		payload += "true,";
		payload += "\"bri\":" + brightness + ",";
		payload += "\"ct\":" + colorTemperature + "}";
		if (PUT("lights/" + lightID + "/state", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// ****************************** [ HueRoom Control ] ****************************** //
	
	// Turns Lights in room <roomID> on or off
	public boolean setRoomState(int roomID, boolean on) {
		String payload = "{\"on\":";
		if (on) {
			payload += "true}";
		} else {
			payload += "false}";
		}
		if (PUT("groups/" + roomID + "/action", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// Turns room on and set brightness or turns room off
	public boolean setRoomBrightness(int roomID, int brightness) {
		String payload = "{\"on\":";
		if (brightness > 0) {
			payload += "true,";
			payload += "\"bri\":" + brightness + "}";
		} else {
			payload += "false}";
		}
		if (PUT("groups/" + roomID + "/action", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// Returns brightness
	public int getRoomBrightness(int roomID) {
		String content;
		if ((content = GET("groups/" + roomID)).length() > 0) {
			return getValueFromObject("bri", content, 0);
		}
		return 0;
	}
	
	// Turns room on and set colortemperature (kelvin or mired)
	public boolean setRoomColorTemperature(int roomID, int colorTemperature) {
		if (colorTemperature <= 6500 && colorTemperature >= 2000) { 		// valid kelvin
			colorTemperature = 1000000 / colorTemperature; 					// convert kelvin to mired
		} else if (colorTemperature <= 500 && colorTemperature >= 153) { 	// valid mired
			// nothing to do
		} else {															// invalid
			return false;
		}

		String payload = "{\"on\":true,";
		payload += "\"ct\":" + colorTemperature + "}";
		println("Payload => " + payload);
		if (PUT("groups/" + roomID + "/action", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}

	// Returns colortemperature in mired by default
	public int getRoomColorTemperature(int roomID) {
		return getRoomColorTemperature(roomID, false);
	}

	// Returns colortemperature in mired by default or convert to kelvin
	public int getRoomColorTemperature(int roomID, boolean convertToKelvin) {
		String content;
		if ((content = GET("groups/" + roomID)).length() > 0) {
			if (convertToKelvin) {
				int colorTemperature = 1000000 / getValueFromObject("ct", content, 1);
				if (colorTemperature < 2000) {
					colorTemperature = 2000;
				} else if (colorTemperature > 6500) {
					colorTemperature = 6500;
				}
				return colorTemperature;
			} else { // default
				return getValueFromObject("ct", content, 0);
			}
		}
		return 0;
	}

	// Set alert mode
	public boolean setRoomAlert(int roomID, int alert) {
		String payload = "{\"alert\":";
		switch(alert) {
			case HueRoom.ALERT_NONE:
				payload += "\"none\"}";
				break;
			case HueRoom.ALERT_SELECT:
				payload += "\"select\"}";
				break;
			case HueRoom.ALERT_LSELECT:
				payload += "\"lselect\"}";
				break;
		}
		if (PUT("groups/" + roomID + "/action", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}

	// Only Color Lights
	// Turns room on and set saturation
	public boolean setRoomSaturation(int roomID, int saturation) {
		String payload = "{\"on\":true,";
		payload += "\"sat\":" + saturation + "}";
		if (PUT("groups/" + roomID + "/action", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}

	// Returns saturation
	public int getRoomSaturation(int roomID) {
		String content;
		if ((content = GET("groups/" + roomID)).length() > 0) {
			return getValueFromObject("sat", content, 0);
		}
		return 0;
	}

	// Turns room on and set hue
	public boolean setRoomHue(int roomID, int hue) {
		String payload = "{\"on\":true,";
		payload += "\"hue\":" + hue + "}";
		if (PUT("groups/" + roomID + "/action", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}

	// Returns hue
	public int getRoomHue(int roomID) {
		String content;
		if ((content = GET("groups/" + roomID)).length() > 0) {
			return getValueFromObject("hue", content, 0);
		}
		return 0;
	}

	// Turns room on and set XY color
	public boolean setRoomColor(int roomID, double x, double y) {
		String payload = "{\"on\":true,";
		payload += "\"xy\":[" + x + "," + y + "]}";
		if (PUT("lights/" + roomID + "/state", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}

	// Turns room on and set XY color from rgb
	public boolean setRoomColor(int roomID, int r, int g, int b) {
		double[] xy = light.convertRGBtoXY(r, g, b);
		return setRoomColor(roomID, xy[0], xy[1]);
	}

	// Turns room on and set effect
	public boolean setRoomEffect(int roomID, int effect) {
		String payload = "{\"on\":true,";
		payload += "\"effect\":";
		switch(effect) {
			case HueLight.EFFECT_NONE:
				payload += "\"none\"}";
				break;
			case HueLight.EFFECT_COLORLOOP:
				payload += "\"colorloop\"}";
				break;
		}
		if (PUT("groups/" + roomID + "/action", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}

	// Mixed methods
	public boolean setRoomBrightnessAndColorTemperature(int roomID, int brightness, int colorTemperature) {
		if (colorTemperature <= 6500 && colorTemperature >= 2000) { 		// valid kelvin
			colorTemperature = 1000000 / colorTemperature; 					// convert kelvin to mired
		} else if (colorTemperature <= 500 && colorTemperature >= 153) { 	// valid mired
			// nothing to do
		} else {															// invalid
			return false;
		}

		String payload = "{\"on\":";
		payload += "true,";
		payload += "\"bri\":" + brightness + ",";
		payload += "\"ct\":" + colorTemperature + "}";
		if (PUT("groups/" + roomID + "/action", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}

	// ****************************** [ HueScene Control ] ****************************** //
	
	public boolean setScene(String sceneID) {
		String payload = "{\"scene\":\"" + sceneID + "\"}";
		if (PUT("groups/" + 0 + "/action", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// ****************************** [ HueHome Control ] ****************************** //

	public boolean setHomeState(boolean on) {
		String payload = "{\"on\":";
		if (on) {
			payload += "true}";
		} else {
			payload += "false}";
		}
		if (PUT("groups/" + 0 + "/action", payload).indexOf("success") != -1) {
			return true;
		}
		return false;
	}
	
	// ****************************** [ DIY JSON Helper ] ****************************** //

	private int getValueFromObject(String name, String content, int value) {
		try {
			return Integer.valueOf(getValueFromObject(name, content));
		} catch (NumberFormatException nfe) {
			// nfe.printStackTrace();
		}
		return value;
	}
	
	private String getValueFromObject(String name, String content) {
		name = "\"" + name + "\":";
		if (content.indexOf(name) != -1) {
			content = content.substring(content.indexOf(name) + name.length(), content.length());
			if (content.startsWith("\""))  {
				content = content.substring(0, content.indexOf("\"", 1));
			} else if (content.indexOf(",") != -1) {
				content = content.substring(0, content.indexOf(","));
			} else if (content.indexOf("}") != -1) {
				content = content.substring(0, content.indexOf("}"));
			}
			content = content.replace("\"", "");
			content = content.replace("\t", "");
			content = content.replace("\n", "");
			content = content.replace("\r", "");
			content = content.replace("}", "");
			return content;
		}
		return "";
	}
	
	// ****************************** [ Connection management ] ****************************** //
	
	private String GET(String call) {
		if (mIP.equals("") || mToken.equals("")) {
			return "";
		}
		println("Call <GET>: " + call);
		try {
			URL url = new URL("http://" + mIP + "/api/" + mToken + "/" + call);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setRequestProperty("Host", mIP);
			
			BufferedReader in = new BufferedReader(new InputStreamReader(
			con.getInputStream()));
			String inputLine;
			String output = "";
			while ((inputLine = in.readLine()) != null) {
				output += inputLine;
			} 
			in.close();
			return output;
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return "";
	}
	
	private String PUT(String call, String payload) {
		if (mIP.equals("") || mToken.equals("")) {
			return "";
		}
		println("Call <PUT>: " + call);
		println("Payload: " + payload);
		try {
			URL url = new URL("http://" + mIP + "/api/" + mToken + "/" + call);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("PUT");
			con.setDoOutput(true);
			con.setRequestProperty("Host", mIP);
			con.setRequestProperty("Accept", "*/*");
			con.setRequestProperty("Content-Length", String.valueOf(payload.length()));
			con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			
			DataOutputStream dos = new DataOutputStream(con.getOutputStream());
			dos.writeBytes(payload);
			dos.flush();
			dos.close();
			
			BufferedReader in = new BufferedReader(new InputStreamReader(
			con.getInputStream()));
			String inputLine;
			String output = "";
			while ((inputLine = in.readLine()) != null) {
				output += inputLine;
			} 
			in.close();
			return output;
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return "";
	}
	
	// ****************************** [ File management ] ****************************** //
	
	

}
