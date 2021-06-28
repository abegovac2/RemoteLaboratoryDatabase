package ba.unsa.etf.ugradbeni.server.controller;

import ba.unsa.etf.ugradbeni.server.model.RemoteLabDAO;
import ba.unsa.etf.ugradbeni.server.model.Room;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.regex.Pattern;

public class AddRoomController {

    public TextField roomName;
    private final RemoteLabDAO instance = RemoteLabDAO.getInstance();


    @FXML
    public void initialize() {
    }

    public AddRoomController() {
    }

    private Boolean checkNumberLetter(String name) {
        String regex = "^[a-zA-Z0-9]+$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(name).matches();
    }

    @FXML
    public void addRoomAction(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error!");

        if (roomName.getText().trim().isEmpty()) {
            alert.setContentText("Name field can not be empty!");
            alert.showAndWait();
        } else if (!checkNumberLetter(roomName.getText())) {
            alert.setContentText("Name field should contain only letters and numbers!");
            alert.showAndWait();
        } else if (instance.getRooms().contains(new Room(-1, roomName.getText()))) {
            alert.setContentText("This name is already used!");
            alert.showAndWait();
        } else {
            instance.addRoom(new Room((instance.getRooms().size() + 1), roomName.getText()));
            cancleAction(event);
        }
    }

    @FXML
    public void cancleAction(ActionEvent event) {
        ((Stage) roomName.getScene().getWindow()).close();
    }
}
