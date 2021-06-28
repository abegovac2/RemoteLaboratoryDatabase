package ba.unsa.etf.ugradbeni.server.controller;

import ba.unsa.etf.ugradbeni.server.model.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;

public class ServerController {

    public FlowPane rooms;
    public Button activationButton;
    public Button addRoom;

    private boolean activated = false;

    public ImageView serverStatus;

    private MessagingClient messageController, roomController;

    public ServerController() {
    }

    @FXML
    public void initialize() {
        rooms.setDisable(true);

    }

    @FXML
    public void addRoomAction(ActionEvent event) throws IOException {
        if (activated) {
            var primaryStage = (Stage) rooms.getScene().getWindow();
            Stage addRoomStage = new Stage();
            Parent root = FXMLLoader.load(getClass().getResource("/view/addRoom.fxml"));
            addRoomStage.setTitle("Adding new room");
            addRoomStage.setScene(new Scene(root, USE_COMPUTED_SIZE, USE_COMPUTED_SIZE));
            addRoomStage.show();
            addRoomStage.setOnHiding(windowEvent -> listAllRooms());

        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error!");
            alert.setContentText("Server not responding!");
            alert.showAndWait();
        }

    }


    private void listAllRooms() {
        var db = RemoteLabDAO.getInstance();
        rooms.getChildren().clear();
        List<Room> Rooms = db.getRooms();
        for (Room room : Rooms) {
            Button grupa = new Button("" + room.getRoomName() + "");
            grupa.setMinWidth(100);
            grupa.setMinHeight(100);
            rooms.getChildren().add(grupa);
            grupa.setOnMouseClicked(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Confirmation");
                alert.setHeaderText("Notice");
                alert.setContentText("Do you want to delete room \"" + grupa.getText() + "\"?");

                Optional<ButtonType> result = alert.showAndWait();
                if (result.get() == ButtonType.OK) {
                    db.deleteRoom(room);
                    listAllRooms();
                }
            });

        }
    }

    @FXML
    public void activateButton(ActionEvent event) {
        serverStatus.setImage(new Image("pictures/loading.gif"));
        if (activated) { // deactivates server
            rooms.setDisable(true);
            new Thread(() -> {
                try {
                    messageController.unsubscribeFromTopic("" + ThemesMqtt.BASE + ThemesMqtt.ALL_MESSAGES_FROM_ROOMS, null);
                    messageController.unsubscribeFromTopic("" + ThemesMqtt.BASE + ThemesMqtt.MESSAGE + "/+" + ThemesMqtt.SEND_REFRESH, null);
                    roomController.unsubscribeFromTopic("" + ThemesMqtt.BASE + ThemesMqtt.ROOM_REFRESH_SEND, null);
                    //roomController.unsubscribeFromTopic("" + ThemesMqtt.BASE + ThemesMqtt.ROOM_ADD_NEW, null);

                } catch (MqttException e) {
                    e.printStackTrace();
                }
                serverStatus.setImage(new Image("pictures/database_red.png"));
                Platform.runLater(() -> activationButton.setText("ACTIVATE SERVER"));
            }).start();
        } else { //activates server
            rooms.setDisable(false);
            rooms.getChildren().clear();

            new Thread(() -> {
                try {
                    var msgMap = functionSetupForChat();
                    var roomMap = functionSetupforRooms();

                    messageController = new MessagingClient(UUID.randomUUID().toString(), msgMap);
                    roomController = new MessagingClient(UUID.randomUUID().toString(), roomMap);

                    String messageLoggerTopic = "" + ThemesMqtt.BASE + ThemesMqtt.ALL_MESSAGES_FROM_ROOMS;
                    String roomMessagesRefresh = "" + ThemesMqtt.BASE + ThemesMqtt.MESSAGE + "/+" + ThemesMqtt.SEND_REFRESH;
                    String roomListRefresh = "" + ThemesMqtt.BASE + ThemesMqtt.ROOM_REFRESH_SEND;

                    messageController.subscribeToTopic(messageLoggerTopic, null, 0);
                    messageController.subscribeToTopic(roomMessagesRefresh, null, 0);
                    roomController.subscribeToTopic(roomListRefresh, null, 0);

                } catch (MqttException e) {
                    e.printStackTrace();
                    serverStatus.setImage(new Image("pictures/database_red.png"));
                    Platform.runLater(() -> activationButton.setText("ACTIVATE SERVER"));
                }
                serverStatus.setImage(new Image("pictures/database_green.png"));
                Platform.runLater(() -> {
                    activationButton.setText("DEACTIVATE SERVER");
                    listAllRooms();
                });
            }).start();
        }
        activated = !activated;
    }

    private HashMap<String, MqttOnRecive> functionSetupforRooms() {
        HashMap<String, MqttOnRecive> functionMap = new HashMap<>();

        functionMap.put("send",
                (String topic, MqttMessage mqttMessage) ->
                        new Thread(() -> {
                            var db = RemoteLabDAO.getInstance();
                            List<Room> rooms = db.getRooms();
                            try {
                                JSONArray array = new JSONArray();
                                for (var room : rooms) {
                                    JSONObject obj = new JSONObject();
                                    obj.put("RoomId", room.getId());
                                    obj.put("RoomName", room.getRoomName());
                                    array.put(obj);
                                }
                                JSONObject obj = new JSONObject();
                                obj.put("ListOfRooms", array);
                                roomController.sendMessage("" + ThemesMqtt.BASE + ThemesMqtt.ROOM_REFRESH_RECIVE, obj.toString(), 0);
                                System.out.println("Room refresh: " + obj.toString());
                            } catch (MqttException | JSONException e) {
                                e.printStackTrace();
                            }
                        }).start()
        );


        functionMap.put("add", (String topic, MqttMessage mqttMessage) ->
                new Thread(() -> {
                    var db = RemoteLabDAO.getInstance();
                    try {
                        JSONObject obj = new JSONObject(new String(mqttMessage.getPayload()));
                        var room = new Room(
                                obj.getInt("RoomId"),
                                obj.getString("RoomName")
                        );
                        new Thread(() -> db.addRoom(room)).start();
                        System.out.println("Room created: " + room.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }).start());

        return functionMap;
    }

    private HashMap<String, MqttOnRecive> functionSetupForChat() {
        HashMap<String, MqttOnRecive> functionMap = new HashMap<>();
        functionMap.put("message",
                (String topic, MqttMessage mqttMessage) ->
                        new Thread(() -> {
                            var db = RemoteLabDAO.getInstance();
                            try {
                                JSONObject msg = new JSONObject(new String(mqttMessage.getPayload()));
                                var message = new Message(
                                        -1,
                                        msg.getString("Message"),
                                        msg.getInt("RoomId")
                                );
                                db.writeMessage(message);

                                System.out.println("New message: " + message.getMessage());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }).start()
        );

        functionMap.put("refresh", (String topic, MqttMessage mqttMessage) ->
                new Thread(() -> {
                    try {
                        var db = RemoteLabDAO.getInstance();
                        JSONObject obj = new JSONObject(new String(mqttMessage.getPayload()));
                        var room = new Room(
                                obj.getInt("RoomId"),
                                obj.getString("RoomName")
                        );
                        var msgList = db.readMessages(room.getId());
                        if (msgList.isEmpty()) {
                            messageController.sendMessage(
                                    "" + ThemesMqtt.BASE + ThemesMqtt.MESSAGE + "/" + room.getRoomName() + ThemesMqtt.RECIVE_REFRESH,
                                    "{\"RoomHasBeenDeleted\" : 404}", 0);
                        } else {
                            JSONArray array = new JSONArray();
                            for (var msg : msgList) {
                                JSONObject message = new JSONObject();
                                message.put("Id", msg.getId());
                                message.put("Message", msg.getMessage());
                                message.put("RoomId", msg.getRoomId());
                                array.put(message);
                            }
                            JSONObject message = new JSONObject();
                            message.put("ListOfMessages", array);
                            messageController.sendMessage(
                                    "" + ThemesMqtt.BASE + ThemesMqtt.MESSAGE + "/" + room.getRoomName() + ThemesMqtt.RECIVE_REFRESH,
                                    message.toString(), 0);

                            System.out.println("Message refresh: " + message.toString());
                        }
                    } catch (MqttException | JSONException e) {
                        e.printStackTrace();
                    }
                }).start());

        return functionMap;
    }


}
