package app.wolfware.room;

import java.util.ArrayList;
import java.util.List;

public class HueRoom {

	private class Room {
		public int id = -1;
		public String name = "";
	};

	private List<Room> rooms = new ArrayList<Room>();

	public final static boolean ON = true;
	public final static boolean OFF = false;
	public final static int STATe_ALL = 0;
	public final static int STATE_ANY = 1;
	public final static int ALERT_NONE = 0;
	public final static int ALERT_SELECT = 1;
	public final static int ALERT_LSELECT = 2;

	public void add(int roomID, String roomName) {
		Room room = new Room();
		room.id = roomID;
		room.name = roomName;
		rooms.add(room);
	}

	public int get(String roomName) {
		if (roomName.equalsIgnoreCase("all") || roomName.equalsIgnoreCase("alle")) {
			return 0;
		}
		for (Room room : rooms) {
			if (room.name.equals(roomName)) {
				return room.id;
			}
		}
		return -1;
	}

	public void clear() {
		rooms.clear();
	}
}
