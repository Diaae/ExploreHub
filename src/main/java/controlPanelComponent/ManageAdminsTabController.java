package controlPanelComponent;


import alerts.CustomAlertType;
import authentification.loginProcess.CurrentAccountSingleton;
import handlers.MessageHandler;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXTextField;
import handlers.Convenience;
import handlers.UploadImage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import mainUI.MainPane;
import models.Account;
import models.Admin;
import models.Courses;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.File;
import java.util.Optional;
import java.util.UUID;

/**
 * Class which serves as a controller for the admins management view
 *
 * @author Gheorghe Mironica
 */
@SuppressWarnings("JpaQueryApiInspection")
public class ManageAdminsTabController{

    @FXML
    private JFXTextField firstnameText, lastnameText, emailText;
    @FXML
    private ImageView adminPicture;
    @FXML
    private String imageURL;
    @FXML
    private JFXButton uploadButton, deleteAdminButton, createAdminButton;
    @FXML
    private JFXListView<Admin> adminListView;
    private ObservableList<Admin> adminObservableList;
    private EntityManager entityManager;
    private Account account;
    private String NAME_PATTERN = "^[a-zA-Z]*$";
    private String EMAIL_PATTERN = "[a-zA-Z0-9._]+@hs-ulm\\.(de)$";
    private String ADMIN_CREATED_MESSAGE = "Admin account has been successfully created!";
    private String PASSWORD_GENERATED = "Random password has been generated and sent to you over email.";
    private Admin selectedAdmin;

    /**
     * Method which initializes the view ManageEventsTabController
     */
    @SuppressWarnings("JpaQueryApiInspection")
    public void initialize() {
        try{
            imageURL = "/IMG/icon-account.png";
            account = CurrentAccountSingleton.getInstance().getAccount();
            entityManager = account.getConnection();
            if(selectedAdmin == null){
                deleteAdminButton.setDisable(true);
            }
            TypedQuery<Admin> tq1 = entityManager.createNamedQuery("Admin.findAdmins", Admin.class);
            adminObservableList = FXCollections.observableArrayList();
            adminObservableList.addAll(tq1.getResultList());
            int index = adminObservableList.indexOf(account);
            adminObservableList.remove(index);
            adminListView.setItems(adminObservableList);
            adminListView.setCellFactory(customListViewCell -> new AdminCellController());

        }catch(Exception e){
            handleConnection();
        }

    }

    /**
     * This method handles the click on create button, it's suppose to create new User with admin privileges
     * @param e {@link Event} mouse clicked on Create button, Event is input parameter
     */
    @FXML
    private void createAdmin(Event e){
        String firstname = firstnameText.getText();
        String lastname = lastnameText.getText();
        String email = emailText.getText();
        String rand = generateString();

        try{
            if(isFormInvalid(firstname, lastname, email)){
                return;
            }
            Admin newAdmin = new Admin(firstname, lastname, email, 1, rand, fetchCourse(), imageURL);

            entityManager.getTransaction().begin();
            entityManager.persist(newAdmin);
            entityManager.getTransaction().commit();

            clearView(e);
            adminObservableList.add(newAdmin);
            Convenience.showAlert(CustomAlertType.SUCCESS, ADMIN_CREATED_MESSAGE + ' ' + PASSWORD_GENERATED);
        }catch(Exception ex){
            handleConnection();
        }

        try{
            MessageHandler.getMessageHandler().sendNewPassword(rand, email);
        } catch(Exception ex){
            // unexistent email
        }
    }

    /**
     * This method handles the click on delete button, it's suppose to selected admin from listview
     * @param e {@link Event} mouse clicked on Delete button, Event is input parameter
     */
    @FXML
    private void deleteAdmin(Event e){
        try{
            if (selectedAdmin != null) {
                Optional<ButtonType> response = Convenience.showAlertWithResponse(CustomAlertType.CONFIRMATION,
                        "Are you sure you want to permanently remove " +
                        selectedAdmin.getFirstname()+" "+selectedAdmin.getLastname() + " ?",
                        ButtonType.YES, ButtonType.CANCEL);

                if (response.isPresent() && response.get() == ButtonType.CANCEL) {
                    return;
                } else {
                    entityManager.getTransaction().begin();
                    entityManager.remove(selectedAdmin);
                    entityManager.getTransaction().commit();
                    adminObservableList.remove(selectedAdmin);
                    clearView(e);
                }
            }
        }catch(Exception ex){
            handleConnection();
        }
    }

    /**
     * This method handles click on upload picture button, takes as input parameter mouse click {@link Event}, is suppose
     * to upload picture for the new admin
     * @param e
     */
    @FXML
    private void uploadPicture(Event e){
        Image image;
        try{
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JPEG Files", "*.jpg"),
                    new FileChooser.ExtensionFilter("PNG Files", "*.png"));
            File file = fileChooser.showOpenDialog(((Node) e.getSource()).getScene().getWindow());

            if(file!=null){
                image = new Image(file.toURI().toString());
                uploadPicture(image);
            }
        }catch(Exception ex){
            handleConnection();
        }
    }

    /**
     * This method uploads the picture to IMGUR in a separate thread
     * using the UploadImage class.
     * @param image {@link Image} input param
     */
    private void uploadPicture(Image image){
        Thread thread1 = new Thread(()->{
            try {
                UploadImage uploader = new UploadImage(image);
                imageURL = uploader.upload();
            }catch(Exception e){
                Convenience.showAlert(CustomAlertType.WARNING, "This image can't be uploaded due to its size.");
            }
        });
        thread1.start();
        adminPicture.setImage(image);
    }

    /**
     * This method handles click on the listview, parses the selected entity, fills form
     * @param event {@link Event} Mouse click as input parameter
     */
    @FXML
    private void cellClicked(Event event){
        try {
            deleteAdminButton.setDisable(false);
            selectedAdmin = adminListView.getSelectionModel().getSelectedItem();
            if(!(selectedAdmin == null)) {
                firstnameText.setText(selectedAdmin.getFirstname());
                lastnameText.setText(selectedAdmin.getLastname());
                emailText.setText(selectedAdmin.getEmail());
                try {
                    adminPicture.setImage(new Image(selectedAdmin.getPicture()));
                } catch (Exception ex) {
                    adminPicture.setImage(new Image("/IMG/icon-account.png"));
                }
                adminPicture.setFitHeight(111);
                adminPicture.setFitWidth(111);
                uploadButton.setDisable(true);
                createAdminButton.setDisable(true);
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * This method handles click on clear button, it's suppose to clear the view
     * @param e {@link Event} Mouse click as input parameter
     */
    @FXML
    private void clearView(Event e){
        firstnameText.clear();
        lastnameText.clear();
        uploadButton.setDisable(false);
        emailText.clear();
        Image tempImage = new Image("/IMG/icon-account.png");
        adminPicture.setImage(tempImage);
        createAdminButton.setDisable(false);
        uploadButton.setText("Upload Picture");

    }

    /**
     * This method retrieves the appropriate course for new created admin (None)
     * @return {@link Courses} returns a course
     */
    protected Courses fetchCourse() {
        try{
            @SuppressWarnings("JpaQueryApiInspection")
            TypedQuery<Courses> tq1 = entityManager.createNamedQuery(
                    "Courses.findCourseByName",
                    Courses.class);
            tq1.setParameter("name", "None");
            return tq1.getSingleResult();
        }catch(Exception er){
            Alert alert = new Alert(Alert.AlertType.WARNING, "Check the internet connection...");
            alert.showAndWait();
            return new Courses();
        }
    }

    /**
     * This method validates the form
     * @param firstname {@link String} firstname
     * @param lastname {@link String} lastname
     * @param email {@link String} email
     * @return
     */
    protected boolean isFormInvalid(String firstname, String lastname, String email) {
        TypedQuery<Account> tqa = entityManager.createNamedQuery("Account.findAccountByEmail", Account.class);
        tqa.setParameter("email",email);
        try{
            Account akk = tqa.getSingleResult();
            if (!(akk == null)) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "This Account already exists");
                alert.showAndWait();
                return true;
            }
        }catch(Exception e){
            // account doesnt exit, continue
        }

        return !areFieldsValid(firstname, lastname, email);
    }

    /**
     * This method validates the fields of the form
     * @param firstname {@link String} firstname
     * @param lastname {@link String} lastname
     * @param email {@link String} email
     * @return
     */
    protected boolean areFieldsValid(String firstname, String lastname, String email) {
        boolean valid = true;
        boolean validFirstName = (!(firstname.isEmpty())&&(firstname.matches(NAME_PATTERN)));
        boolean validLastName = (!(lastname.isEmpty())&&(lastname.matches(NAME_PATTERN)));
        boolean validEmail = (!(email.isEmpty())&&(email.matches(EMAIL_PATTERN)));

        if(!validFirstName){
            firstnameText.setStyle("-fx-text-inner-color: red;");
            firstnameText.setText("Invalid First Name");
            displayError(firstnameText);
            valid = false;
        }
        if(!validLastName){
            lastnameText.setStyle("-fx-text-inner-color: red;");
            lastnameText.setText("Invalid Last Name");
            displayError(lastnameText);
            valid = false;
        }
        if(!validEmail){
            emailText.setStyle("-fx-text-inner-color: red;");
            emailText.setText("Invalid email");
            displayError(emailText);
            valid = false;
        }

        return valid;
    }

    /**
     * This method pops up an error when field are invalid
     * @param field {@link JFXTextField} as input parameter
     */
    private void displayError(JFXTextField field) {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(3000);
            } catch (Exception e) { }
            Platform.runLater(() -> {
                field.clear();
                field.setStyle("-fx-text-inner-color: black;");
            });
        });
        thread.start();
    }

    /**
     * This method generates a random, one TimeConvertor password for the newly created admin
     * @return {@link String} returns the password
     */
    protected String generateString() {
        String uuid = UUID.randomUUID().toString();
        return uuid;
    }

    /**
     * This method switches scene to the main
     * @param e {@link Event} Mouse click as input
     */
    @FXML
    private void goHome(Event e){
        try{
            Convenience.openHome();
        }catch(Exception ex){
            handleConnection();
        }
    }

    /**
     * This method handles the loss of internet connection
     * delegating it to NoInternet controller
     */
    public synchronized void handleConnection(){
        try {
            Convenience.popupDialog(MainPane.getInstance().getStackPane(), MainPane.getInstance().getBorderPane(),
                    getClass().getResource("/FXML/noInternet.fxml"));
        }catch(Exception e) { /**/ }
    }
}
