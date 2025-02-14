/*
RegisterGUI.java is an extension of LoginGUI.java, with the repsonsibility of creating new valid users for the server's
authentication. It does this with a PrintWriter object and writer to the csv file.

Authors and Responsibilities:
Kevin Leger: 100%
*/

// Importing all necassary libraries
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

// Must implement ActionListener for the functions and buttons on the GUI to be linked
public class RegisterGUI implements ActionListener {
    // Intiializing all the framing and paneling needed, as well as the proper labels and buttons
    private JFrame registrationFrame;
    private JPanel registrationPanel;
    private JLabel userLabel;
    private JLabel passwordLabel;
    private JTextField newUsernameText;
    private JTextField newPasswordText;
    private JLabel successLabel;

    // Constructor for this class will put all of the labels and buttons on the panel, but will not
    // display them until the Register buttong is called form the Login GUI
    public RegisterGUI() {
        registrationFrame = new JFrame("User Registration");
        registrationPanel = new JPanel();
        registrationFrame.setSize(350, 300);
        registrationFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        registrationFrame.add(registrationPanel);

        registrationPanel.setLayout(null);

        userLabel = new JLabel("New Username");
        userLabel.setBounds(10, 20, 165, 25);
        registrationPanel.add(userLabel);

        passwordLabel = new JLabel("New Password");
        passwordLabel.setBounds(10, 50, 165, 25);
        registrationPanel.add(passwordLabel);

        newUsernameText = new JTextField(20);
        newUsernameText.setBounds(140, 20, 165, 25);
        registrationPanel.add(newUsernameText);

        newPasswordText = new JTextField(20);
        newPasswordText.setBounds(140, 50, 165, 25);
        registrationPanel.add(newPasswordText);

        JButton registerUserButton = new JButton("Register User");
        registerUserButton.setBounds(10, 80, 150, 25);
        registerUserButton.addActionListener(this);
        registrationPanel.add(registerUserButton);

        successLabel = new JLabel("");
        successLabel.setBounds(10, 110, 300, 25);
        registrationPanel.add(successLabel);
    }

    // Function that gets called form the Login GUI, so we only have to display the new GUI
    public void showRegistrationGUI() {
        registrationFrame.setVisible(true);
    }

    // Function that writes to the csv file
    public void actionPerformed(ActionEvent e) {
        // Gets the username and password from the user
        String newUsername = newUsernameText.getText();
        String newPassword = newPasswordText.getText();

        // gets the path to the file
        String filePath = "RootDir/Users.csv";

        // create a new PrintWriter object and write the new username and password in .csv format
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath, true))) {
            writer.println(newUsername + "," + newPassword);
            successLabel.setText("Registration successful :)");
        } catch (IOException ex) {
            ex.printStackTrace();
            successLabel.setText("Error during registration");
        }
    }
}
