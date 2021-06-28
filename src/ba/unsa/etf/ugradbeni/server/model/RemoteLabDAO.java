package ba.unsa.etf.ugradbeni.server.model;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RemoteLabDAO {

    private static RemoteLabDAO instance;

    private Connection _connection;

    public static RemoteLabDAO getInstance() {
        if (instance == null) instance = new RemoteLabDAO();
        return instance;
    }

    public static void removeInstance() {
        instance = null;
    }

    public RemoteLabDAO() {
        try {
            File database = new File("RemoteLaboratory.db");
            var res = database.createNewFile();
            if (res) {
                _connection = DriverManager.getConnection("jdbc:sqlite:RemoteLaboratory.db");
                Statement st = _connection.createStatement();
                st.executeUpdate("CREATE TABLE Messages(" +
                        "id INTEGER," +
                        "message VARCHAR(350)," +
                        "roomId INTEGER," +
                        "PRIMARY KEY(id)" +
                        ");");
                st.executeUpdate("CREATE TABLE Rooms(" +
                        "id INTEGER," +
                        "room_name VARCHAR(255)," +
                        "PRIMARY KEY(id)" +
                        ");");
                st.close();
            } else _connection = DriverManager.getConnection("jdbc:sqlite:RemoteLaboratory.db");
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void writeMessage(Message msg) {
        synchronized (this.getClass()) {
            try {
                PreparedStatement ps = _connection.prepareStatement("INSERT INTO Messages (message, roomId) VALUES (?,?);");
                ps.setString(1, msg.getMessage());
                ps.setInt(2, msg.getRoomId());
                ps.executeUpdate();
                ps.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    public List<Message> readMessages(int roomId) {
        synchronized (this.getClass()) {
            var messagesList = new ArrayList<Message>();
            try {
                PreparedStatement ps = _connection.prepareStatement(
                        "SELECT * FROM Messages WHERE roomId = ? ORDER BY id DESC LIMIT 10;"
                );
                ps.setInt(1, roomId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Message msg = new Message(
                            rs.getInt(1),
                            rs.getString(2),
                            rs.getInt(3)
                    );
                    messagesList.add(msg);
                }
                ps.close();
                rs.close();

            } catch (SQLException throwables) {
                throwables.printStackTrace();
            } finally {
                return messagesList;
            }
        }
    }

    public List<Room> getRooms() {
        synchronized (this.getClass()) {
            var roomList = new ArrayList<Room>();
            try {
                PreparedStatement ps = _connection.prepareStatement("SELECT * FROM Rooms;");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    var room = new Room(
                            rs.getInt(1),
                            rs.getString(2)
                    );
                    roomList.add(room);
                }
                ps.close();
                rs.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            } finally {
                return roomList;
            }
        }
    }

    public void addRoom(Room room) {
        synchronized (this.getClass()) {
            try {
                int roomId = getNewRoomId();
                PreparedStatement ps = _connection.prepareStatement("INSERT INTO Rooms (id,room_name) VALUES (?,?);");
                ps.setInt(1, roomId);
                ps.setString(2, room.getRoomName());
                ps.executeUpdate();
                ps.close();
                writeMessage(new Message(-1, "Welcome to " + room.getRoomName(), roomId));
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    public void deleteRoom(Room room) {
        synchronized (this.getClass()) {
            try {
                PreparedStatement ps = _connection.prepareStatement("DELETE FROM Rooms WHERE id=?");
                ps.setInt(1, room.getId());
                ps.executeUpdate();
                ps.close();

                PreparedStatement ps1 = _connection.prepareStatement("DELETE FROM Messages WHERE roomId=?");
                ps1.setInt(1, room.getId());
                ps1.executeUpdate();
                ps1.close();

            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

    private int getNewRoomId() {
        synchronized (this.getClass()) {
            int id = -1;
            try {
                PreparedStatement ps = _connection.prepareStatement("SELECT MAX(id) FROM Rooms WHERE 1 = ?;");
                ps.setInt(1, 1);
                ResultSet rs = ps.executeQuery();
                id = rs.getInt(1);
                ps.close();
                rs.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            return id+1;
        }
    }

}