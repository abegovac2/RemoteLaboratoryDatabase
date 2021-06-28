package ba.unsa.etf.ugradbeni.server.model;

public class Room {
    private final int id;
    private final String roomName;

    public Room(int id, String roomName) {
        this.roomName = roomName;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getRoomName() {
        return roomName;
    }

    @Override
    public String toString() {
        return "{" +
                "\"RoomId\": " + id + " ," +
                "\"RoomName\": \"" + roomName + "\"" +
                "}";
    }
}
