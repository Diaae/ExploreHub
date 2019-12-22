package discussionComponent.controllers;

import authentification.CurrentAccountSingleton;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import javax.persistence.EntityManager;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ContentController implements Initializable {
    private int childCountIdx;
    private EntityManager em = CurrentAccountSingleton.getInstance().getAccount().getConnection();
    private FXMLLoader loader;

    @FXML AnchorPane contentPane;
    @FXML VBox categoryObject;


    @Override
    public void initialize(URL url, ResourceBundle rb){
        childCountIdx = contentPane.getChildren().size();
        if(childCountIdx == 0){
            try{
                loader = new FXMLLoader(getClass().getResource("/FXML/discussion/categoryObject.fxml"));
                categoryObject = loader.load();
            }catch (IOException e){
                e.printStackTrace();
            }
            Text categoryType = (Text) categoryObject.lookup("#categoryType");
            categoryType.setText("General");
            contentPane.getChildren().add(categoryObject);

        }
    }
}
