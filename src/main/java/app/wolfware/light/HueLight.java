package app.wolfware.light;

import java.util.ArrayList;
import java.util.List;

public class HueLight {

	private class Light {
		public int id;
		public String name;
	};

	private List<Light> lights = new ArrayList<Light>();

	public final static boolean ON = true;
	public final static boolean OFF = false;

	public final static int STATE_ALL = 0;
	public final static int STATE_ANY = 1;

	public final static int ALERT_NONE = 0;
	public final static int ALERT_SELECT = 1;
	public final static int ALERT_LSELECT = 2;

	public final static int EFFECT_NONE = 3;
	public final static int EFFECT_COLORLOOP = 4;

	public final static int CT_LIGHT_BULB = 370;
	public final static int CT_HALOGEN_LAMP = 333;
	public final static int CT_FLUORESCENT_LAMP = 250;
	public final static int CT_DAYLIGHT = 182;
	public final static int CT_COOL_WHITE = 153;

	public void add(int lightID, String lightName) {
		Light light = new Light();
		light.id = lightID;
		light.name = lightName;
		lights.add(light);
	}

	public int get(String lightName) {
		for (Light room : lights) {
			if (room.name.equals(lightName)) {
				return room.id;
			}
		}
		return -1;
	}

	public void clear() {
		lights.clear();
	}

	// Convert any RGB color to a Philips Hue XY values
	// The Code is based on:
	// https://stackoverflow.com/questions/22564187/rgb-to-philips-hue-hsb
	public double[] convertRGBtoXY(int r, int g, int b) {
		// For the hue bulb the corners of the triangle are:
		// -Red: 0.675, 0.322
		// -Green: 0.4091, 0.518
		// -Blue: 0.167, 0.04
		double[] normalizedToOne = new double[3];
		normalizedToOne[0] = ((double) r / 255);
		normalizedToOne[1] = ((double) g / 255);
		normalizedToOne[2] = ((double) b / 255);

		double red, green, blue;

		// Make red more vivid
		if (normalizedToOne[0] > 0.04045) {
			red = Math.pow((normalizedToOne[0] + 0.055) / (1.0 + 0.055), 2.4);
		} else {
			red = (normalizedToOne[0] / 12.92);
		}

		// Make green more vivid
		if (normalizedToOne[1] > 0.04045) {
			green = Math.pow((normalizedToOne[1] + 0.055) / (1.0 + 0.055), 2.4);
		} else {
			green = (normalizedToOne[1] / 12.92);
		}

		// Make blue more vivid
		if (normalizedToOne[2] > 0.04045) {
			blue = Math.pow((normalizedToOne[2] + 0.055) / (1.0 + 0.055), 2.4);
		} else {
			blue = (normalizedToOne[2] / 12.92);
		}

		double X = (red * 0.649926 + green * 0.103455 + blue * 0.197109);
		double Y = (red * 0.234327 + green * 0.743075 + blue * 0.022598);
		double Z = (red * 0.0000000 + green * 0.053077 + blue * 1.035763);

		double x = X / (X + Y + Z);
		double y = Y / (X + Y + Z);

		return new double[] { x, y };
	}

}
