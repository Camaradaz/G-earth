import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

@ExtensionInfo(
        Title = "GImageInjector",
        Description = "Put backgrounds only in the client :)",
        Version = "1.0.0",
        Author = "Julianty"
)

public class GImageInjector extends ExtensionForm implements Initializable {
    public TableView <Product> tableView;
    public TableColumn <Product, String> columnImageUrl;
    public TableColumn <Product, Integer> columnFurniId;
    public TableColumn <Product, String> columnOffSetX;
    public TableColumn <Product, String> columnOffSetY;
    public TableColumn <Product, String> columnOffSetZ;

    ObservableList<Product> dataObservableList = FXCollections.observableArrayList();

    @Override // Importante, sin esto el TableView no funciona
    public void initialize(URL location, ResourceBundle resources) {
        columnImageUrl.setCellValueFactory(new PropertyValueFactory<>("productImageUrl")); // Atributos de la clase Product!
        columnFurniId.setCellValueFactory(new PropertyValueFactory<>("productFurniID"));
        columnOffSetX.setCellValueFactory(new PropertyValueFactory<>("productOffSetX"));
        columnOffSetY.setCellValueFactory(new PropertyValueFactory<>("productOffSetY"));
        columnOffSetZ.setCellValueFactory(new PropertyValueFactory<>("productOffSetZ"));
        tableView.setItems(dataObservableList);
    }

    // Look NFT hr-3322-1347.hd-600-8.ch-4025-106-105.lg-4066-107.sh-3089-1425.he-4258.ea-3822-106.cc-3572-1423-106

    public TextField textImage, textHeader;
    public Button buttonAdd, buttonErase;
    public Slider sliderOffSetX, sliderOffSetY, sliderOffSetZ;
    public Label labelOffSetX, labelOffSetY, labelOffSetZ;

    public int currentFurniID = -1;
    public String selectedUrlTable;
    public int selectedIdTable;
    public double selectedOffsetXTable, selectedOffsetYTable, selectedOffsetZTable;

    DecimalFormat df = new DecimalFormat("#.00");


    @Override
    protected void onHide() {
        currentFurniID = -1; // There are no furni with negative id

        // Loop for delete all images in the client
        for (int j = tableView.getItems().size() - 1; j >= 0; j--) {
            try { Thread.sleep(560); } catch (InterruptedException ignored) {}
            int furni_id = tableView.getItems().get(j).getProductFurniID();
            sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT, String.valueOf(furni_id), false, 12345, 0));
        }
        tableView.getItems().clear();
    }

    @Override
    protected void initExtension(){ // When the extension is installed!

        // Using lambda expresion to detect the dominion
        /*onConnect((host, port, APIVersion, versionClient, client) -> {
            this.host = host.substring(5, 7); // Example: Of "game-es.habbo.com" only takes "es"
        });*/

        sliderOffSetX.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                setPosition();
                labelOffSetX.setText("offset X: " + df.format(sliderOffSetX.getValue()));
            }
        });

        sliderOffSetY.valueProperty().addListener((observable, oldValue, newValue) -> {
            setPosition();
            labelOffSetY.setText("offset Y: " + df.format(sliderOffSetY.getValue()));
        });

        sliderOffSetZ.valueProperty().addListener((observable, oldValue, newValue) -> {
            setPosition();
            labelOffSetZ.setText("offset Z: " + df.format(sliderOffSetZ.getValue())); //8700 or less in any rooms wtf
        });

        buttonAdd.setOnAction(e -> {
            /*
                Packet Structure:
                {in:ObjectAdd}{i:1}{i:3704}{i:0}{i:0}{i:1}{s:"0.0"}{s:"0.0"}{i:0}{i:1}{i:5}{s:"state"}
                {s:"0"}{s:"offsetZ"}{s:"0.0"}{s:"offsetY"}{s:"0.0"}{s:"imageUrl"}
                {s:"https://cdn.pixabay.com/photo/2016/02/13/12/26/aurora-1197753_960_720.jpg"}
                {s:"offsetX"}{s:"0.0"}{i:-1}{i:1}{i:3614808}{b:false}{b:false}
            */
            currentFurniID++;
            sendToClient(new HPacket("ObjectAdd", HMessage.Direction.TOCLIENT, currentFurniID,
                    Integer.parseInt(textHeader.getText()), 0, 0
                    , 1, "0.0", "0.0", 0, 1, 5, "state", "0", "offsetZ", String.valueOf(sliderOffSetZ.getValue()),
                    "offsetY", String.valueOf(sliderOffSetY.getValue()), "imageUrl", textImage.getText(), "offsetX",
                    String.valueOf(sliderOffSetX.getValue()), -1, 1, 12345, false, false)); // 1234 its your userid or whatever, dont care
            tableView.getItems().add(new Product(textImage.getText(), currentFurniID, sliderOffSetX.getValue(),
                    sliderOffSetY.getValue(), sliderOffSetZ.getValue()));
            tableView.getSelectionModel().selectLast(); // Selecciona el ultimo item de la tabla
        });

        // Runs when an item is added or removed from the tableView
        tableView.getItems().addListener((ListChangeListener.Change<? extends Product> c) -> {
            if(c.next()){ // Avoid exception
                if(c.wasAdded()){
                    try{
                        selectedUrlTable = tableView.getSelectionModel().getSelectedItem().getProductImageUrl();
                        selectedIdTable = tableView.getSelectionModel().getSelectedItem().getProductFurniID();
                        selectedOffsetXTable = tableView.getSelectionModel().getSelectedItem().getProductOffSetX();
                        selectedOffsetYTable = tableView.getSelectionModel().getSelectedItem().getProductOffSetY();
                        selectedOffsetZTable = tableView.getSelectionModel().getSelectedItem().getProductOffSetZ();
                    }catch (Exception ignored){}
                }
            }
        });

        buttonErase.setOnAction(event -> {
            try{
                int furni_id = tableView.getSelectionModel().getSelectedItem().getProductFurniID();
                tableView.getItems().remove(tableView.getSelectionModel().getSelectedIndex());

                // You get a black screen if you don't delete the furni id in descending order (5, 4, 3...)                         // whatever number
                sendToClient(new HPacket("ObjectRemove", HMessage.Direction.TOCLIENT, String.valueOf(furni_id), false, 12345, 0));
            } catch (Exception ignored) {}
        });

        // Happens when i give click in a row from the tableView
        tableView.setOnMouseClicked((MouseEvent)->{
            try{
                selectedUrlTable = tableView.getSelectionModel().getSelectedItem().getProductImageUrl();
                selectedIdTable = tableView.getSelectionModel().getSelectedItem().getProductFurniID();
                selectedOffsetXTable = tableView.getSelectionModel().getSelectedItem().getProductOffSetX();

                sliderOffSetX.setValue(selectedOffsetXTable);
                sliderOffSetY.setValue(selectedOffsetYTable);
                sliderOffSetZ.setValue(selectedOffsetZTable);
            }catch (Exception ignored) {}
        });

        // tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {});
    }

    public void setPosition(){
        try{
            selectedUrlTable = tableView.getSelectionModel().getSelectedItem().getProductImageUrl();
            selectedIdTable = tableView.getSelectionModel().getSelectedItem().getProductFurniID();
            selectedOffsetXTable = tableView.getSelectionModel().getSelectedItem().getProductOffSetX();
            selectedOffsetYTable = tableView.getSelectionModel().getSelectedItem().getProductOffSetY();
            selectedOffsetZTable = tableView.getSelectionModel().getSelectedItem().getProductOffSetZ();

            int indexTable = tableView.getSelectionModel().getSelectedIndex();  // Get the row index
            tableView.getItems().set(indexTable, new Product(selectedUrlTable, selectedIdTable,
                    sliderOffSetX.getValue(), sliderOffSetY.getValue(), sliderOffSetZ.getValue())); // Update the row
            tableView.getSelectionModel().select(indexTable);   // Solve a bug

            sendToClient(new HPacket("ObjectUpdate", HMessage.Direction.TOCLIENT, selectedIdTable,
                    Integer.parseInt(textHeader.getText()), 0, 0
                    , 1, "0.0", "0.0", 0, 1, 5, "state", "0", "offsetZ", String.valueOf(sliderOffSetZ.getValue()),
                    "offsetY", String.valueOf(sliderOffSetY.getValue()), "imageUrl", selectedUrlTable,
                    "offsetX", String.valueOf(sliderOffSetX.getValue()), -1, 1, 12345, false, false));
        }catch (Exception ignored){}
    }

    public void handleOpenLink() throws URISyntaxException, IOException {
        Desktop.getDesktop().browse(new URI("https://www.habbo.es/gamedata/furnidata/92785a277b9be718da767511d06797404f16f9f7"));
    }
}
