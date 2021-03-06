package authentification.recoverProcess;

import alerts.CustomAlertType;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import handlers.Convenience;
import handlers.HandleNet;
import handlers.MessageHandler;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import models.Account;
import models.User;
import persistenceComponent.GuestConnectionSingleton;

import javax.mail.MessagingException;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 *Class that controls the password recovery view.
 * @author Aleksejs Marmiss
 *
 */
public class RecoverController implements Initializable {
    @FXML
    private StackPane stackPane;
    @FXML
    private AnchorPane anchorpane;
    @FXML
    private JFXButton cancelButton;
    @FXML
    private JFXTextField confirmationCode;
    @FXML
    private JFXTextField recoveryEmail;
    @FXML
    private JFXButton confirmationCodeButton;
    @FXML
    private JFXButton sendEmailButton;
    @FXML
    private Label notifyUser;
    @FXML
    private Text firstRecoveryText;
    @FXML
    private Text secondRecoveryText;

    private String generatedKey = null;
    private TypedQuery<Account> checkUserQuery;
    private List<Account> users;
    private GuestConnectionSingleton connection;
    private EntityManager entityManager;
    private Thread thread = null;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        recoveryEmail.setPromptText("Please enter your email.");
        firstRecoveryText.setText("Enter the email address associated with your account,");
        secondRecoveryText.setText("then choose a new password for your account");
        confirmationCode.setDisable(true);
        confirmationCodeButton.setDisable(true);
    }

    /**
     * Method that generates confirmation code for password recovery.
     * Initialized by button
     * @param event button pressed event.
     */
    @FXML
    private void generateConfirmCode(Event event){
        recoveryEmail.setDisable(true);
        sendEmailButton.setDisable(true);

        thread = new Thread(() -> {
            boolean userExists = false;
            try {
                userExists = checkUser(recoveryEmail.getText());
            }catch (Exception internetConnection){
                Platform.runLater(() -> {
                    if(!HandleNet.hasNetConnection()) {
                        Convenience.showAlert(CustomAlertType.ERROR,
                                "Oops, looks like you have no internet connection. Try again later.");
                        Convenience.closePreviousDialog();
                    }else {
                        Convenience.showAlert(CustomAlertType.ERROR,
                                "Ooops, something went wrong. Please, try again later.");
                    }
                });
                Thread.currentThread().interrupt();
                return;
            }
            if (userExists) {
                String key = UUID.randomUUID().toString();
                generatedKey = key;
                MessageHandler messageHandler = MessageHandler.getMessageHandler();
                try {
                    messageHandler.sendRecoveryConfirmation(key, recoveryEmail.getText());
                } catch (MessagingException ade) {
                    Platform.runLater(() -> {
                        if (!HandleNet.hasNetConnection()) {
                            Convenience.showAlert(CustomAlertType.ERROR,
                                    "Oops, looks like you have no internet connection. Try again later.");
                            Convenience.closePreviousDialog();
                        } else {
                            Platform.runLater(() -> Convenience.showAlert(CustomAlertType.WARNING,
                                    "Password recovery failed. Please try again later."));
                            ade.printStackTrace();
                        }
                    });

                }
                setConfirmEnabled();
            }else{
                Platform.runLater(() -> Convenience.showAlert(CustomAlertType.ERROR,
                                        "Attempt failed. User does not exist."));
                setMailEnabled();
            }
        });
        thread.start();
    }

    /**
     *Method that performs check of confirmation code and generates new password.
     * Initialized by button
     * @param event button pressed event.
     * @throws IOException In case if failed to send a message.
     */
    @FXML
    private void confirmButton(Event event){
        confirmationCode.setDisable(true);
        confirmationCodeButton.setDisable(true);
        String userKey = confirmationCode.getText();
        Thread thread = new Thread(() -> {
            if (generatedKey != null && generatedKey.equals(userKey)){
                String generatedPassword = generatePassword();
                try {
                    if (!setNewPassword(generatedPassword)){return;}
                    MessageHandler messageHandler = MessageHandler.getMessageHandler();
                    messageHandler.sendNewPassword(generatedPassword, recoveryEmail.getText());
                }catch (Exception me){
                    if(!HandleNet.hasNetConnection()){
                        Platform.runLater(() -> Convenience.showAlert(CustomAlertType.ERROR,
                                "Oops, looks like you have no internet connection. Try again later."));
                        Convenience.closePreviousDialog();
                    }else {
                        Platform.runLater(() -> Convenience.showAlert(CustomAlertType.WARNING,
                                "Password recovery failed. Please try again later."));
                    }
                }
                Platform.runLater(() -> Convenience.showAlert(CustomAlertType.INFORMATION,
                                                                    "Your new password was sent to your email address."));
                generatedKey = null;
                Platform.runLater(Convenience::closePreviousDialog);

            }else{
                setEisabled();
            }
        });
        thread.start();
    }

    /**
     *Method that checks if the user with given email exists
     * @param email user email as a String.
     * @return  true if user exists and false if does not exist.
     */
    private boolean checkUser(String email) throws Exception{
        connection = GuestConnectionSingleton.getInstance();
        entityManager = connection.getManager();
        checkUserQuery = entityManager.createNamedQuery(
                "Account.findAccountByEmail",
                Account.class);
        checkUserQuery.setParameter("email", email);
        if(!HandleNet.hasNetConnection()){
            throw new Exception("Internet Connection lost");
        }
        try {
            users = checkUserQuery.getResultList();
        }catch (Exception internetConnection){
            throw new Exception("Internet Connection lost");
        }

        return users.size() == 1;
    }

    /**
     *Method that saves newly generated password in the database.
     * @param newPassword new password as a String.
     */
    private boolean setNewPassword(String newPassword){
        if(HandleNet.hasNetConnection()){
            try {
                Account account = users.get(0);
                account.setPassword(newPassword);
                entityManager.getTransaction().begin();
                entityManager.merge(account);
                entityManager.getTransaction().commit();
            }catch (Exception e){
                entityManager.getTransaction().rollback();
            }
        }else{
            Platform.runLater(() -> {
                try {
                    Convenience.showAlert(CustomAlertType.ERROR,
                            "Oops, looks like you have no internet connection. Try again later.");
                    Convenience.closePreviousDialog();
                } catch (Exception exc) {
                    Convenience.showAlert(CustomAlertType.ERROR,
                            "Something went wrong. Please, try again later.");
                }
            });
            return false;
        }
        return true;
    }

    /**
     *Method that generates new password.
     * @return password as a String.
     */
    private String generatePassword(){
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

            Random random = new Random();
            StringBuilder builder = new StringBuilder(10);
            for (int digitPos = 0; digitPos < 10; digitPos++) {
                builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            return builder.toString();
    }

    /**
     * Method that enables UI fields using main thread.
     */
    private void setMailEnabled(){
        Platform.runLater(() -> {
            recoveryEmail.setDisable(false);
            sendEmailButton.setDisable(false);
        });

    }

    /**
     * Method that enables UI fields using main thread.
     */
    private void setConfirmEnabled(){
        Platform.runLater(() -> {
            confirmationCode.setDisable(false);
            confirmationCodeButton.setDisable(false);
            notifyUser.setText("Confirmation code has been sent to your email.");
        });

    }

    /**
     * Method that enables UI fields using main thread.
     */
    private void setEisabled(){
        Platform.runLater(() -> {
            confirmationCode.setDisable(false);
            confirmationCodeButton.setDisable(false);
            notifyUser.setText("Wrong confirmation code");
        });

    }

    /**
     * Method that switches to the log-in page.
     */
    public void handleCancelClicked(MouseEvent mouseEvent) {
        Convenience.closePreviousDialog();
    }
}
