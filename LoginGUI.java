/*
The LoginGUI.java file is what handles the entire GUI for the user, as well as the main driver file 
for the project. It is the first thing the user sees when they run the .jar file.

All technical features are talked about in the Project Report, but there will be extensive comments throughout the
file to explain what each portion of the file does and how it communicates with the other java files

Authors and Responsibilities: 

Kevin Leger: GUI components creation, functions for Login button, RunServer, stopServer, openRegistrationGUI, 
             and logic for pleasing GUI taking into account all user operations on the GUI
Ryan Brennan: Functions for eraseComments and loadServer
Nate Czarnecki: Logging username and password to correct file
*/

// Importing all necassary libraries
import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
// javax.swing is the library that gives access to aesthetically pleasing GUIs which can be coded in Java
import javax.swing.*;

// It MUST implement ActionListener in order for the buttons in the GUI to be scripted
public class LoginGUI implements ActionListener {

    // Initializing all the labels and buttons that will go on the GUI
    private static JLabel userLabel;
    private static JTextField userText;
    private static JLabel passwordLabel;
    private static JTextField passwordText;
    private static JButton button;
    private static JLabel success;
    private static JButton registerButton;
    private static JButton startServerButton;
    private static JButton stopServerButton;
    private static JButton loadServerButton;
    private static JButton eraseCommentsButton;
    private static JLabel serverStatusLabel;

    // boolean to keep track of what state the server is in. Used for the socket loop in the Server code
    public static boolean runServer = false;
    // creating a new instance of the server
    private static HttpsServer server = new HttpsServer();


    public static void main(String[] args) {

        // The panel is what on the frame of the GUI, and the frame is the external casing for the GUI
        // Setting the size and name of the frame, as well as having the "x" on the GUI stop the program
        JPanel panel = new JPanel();
        JFrame frame = new JFrame("User Login");
        frame.setSize(300, 345);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);

        panel.setLayout(null);

        // Setting the information for all of the items that will be on the screen.
        // setBounds() will put the right dimensions on the item
        // making sure the item gets added to the panel
        userLabel = new JLabel("Username");
        userLabel.setBounds(10, 20, 80, 25);
        panel.add(userLabel);

        userText = new JTextField(20);
        userText.setBounds(100, 20, 165, 25);
        panel.add(userText);

        passwordText = new JTextField(20);
        passwordText.setBounds(100, 50, 165, 25);
        panel.add(passwordText);

        passwordLabel = new JLabel("Password");
        passwordLabel.setBounds(10, 50, 80, 25);
        panel.add(passwordLabel);

        button = new JButton("Login");
        button.setBounds(10, 80, 80, 25);
        button.addActionListener(new LoginGUI());
        panel.add(button);

        success = new JLabel("");
        success.setBounds(10, 110, 300, 25);
        panel.add(success);

        registerButton = new JButton("Register");
        registerButton.setBounds(160, 80, 100, 25);

        // linking every written function function to the correct button button on the GUI
        // Done through the addActionListener and ActionPerformed functions
        // Every time the button is clicked it will run the script its linked to
        registerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openRegistrationGUI();
            }
        });
        panel.add(registerButton);

        startServerButton = new JButton("Start Server");
        startServerButton.setBounds(10, 140, 120, 25);
        startServerButton.setVisible(false); // Initially set to invisible
        startServerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                runServer();
            }
        });
        panel.add(startServerButton);

        stopServerButton = new JButton("Stop Server");
        stopServerButton.setBounds(150, 140, 120, 25);
        stopServerButton.setVisible(false);
        stopServerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopServer();
            }
            });
        panel.add(stopServerButton);

        //RYAN COMMENT
        eraseCommentsButton = new JButton("Erase Comments");
        eraseCommentsButton.setBounds(10, 180, 260, 25);
        eraseCommentsButton.setVisible(false);
        eraseCommentsButton.setBackground(new Color(255, 120, 120));
        eraseCommentsButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                eraseComments();
            }
            });
        panel.add(eraseCommentsButton);

        serverStatusLabel = new JLabel("Server is running! Go here: https://localhost");
        serverStatusLabel.setBounds(10, 215, 290, 25);
        serverStatusLabel.setVisible(false);
        panel.add(serverStatusLabel);

        loadServerButton = new JButton("Launch Website");
        loadServerButton.setBounds(10, 240, 260, 40);
        loadServerButton.setVisible(false);
        loadServerButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loadServer();
            }});
        panel.add(loadServerButton);

        // Show the UI Panel!!!
        frame.setVisible(true);
    }

    /*
    Create an instance of RegisterGUI when the register button is clicked
    Call the showRegistrationGUI function which will display the Registration GUI
    The function also displays everything on the GUI back to false besides the oginal username and password
    labels and the Login and Register buttons.
    */
    private static void openRegistrationGUI() {
        serverStatusLabel.setVisible(false);
        startServerButton.setVisible(false);
        eraseCommentsButton.setVisible(false);
        loadServerButton.setVisible(false);
        stopServer();
        success.setVisible(false);
        RegisterGUI registerGUI = new RegisterGUI();
        registerGUI.showRegistrationGUI();
    }

    /*
    Displays all of the labels and buttons that are supposed to be displayed after the server is started.
    changes the runServer boolean to true (VERY IMPORTANT)
    tries to create a new thread of the HttpsServer from the orignal initialization and run it
    If it isnt able to make the instance of the server, log it in exceptions logger
    */
    private static void runServer() {
        stopServerButton.setVisible(true);
        loadServerButton.setVisible(true);
        serverStatusLabel.setText("Server is running at: https://localhost");
        serverStatusLabel.setVisible(true);
        runServer = true;
        try{
            new Thread(server).start();
        } catch(Exception E){
            HttpsServer.excLogger.log(Level.WARNING, E.toString());
        }
    }

    /*
    Remove all of the labels and buttons to maintain a pleasing interface
    Set the runServer boolean to false, so the server will stop accepting new connections.
    */
    private static void stopServer() {
        serverStatusLabel.setText("Server stopped");
        stopServerButton.setVisible(false);
        loadServerButton.setVisible(false);
        runServer = false;
    }

    // Runs the PHP script to erase all fo the comments on the chicken blog post
    // It it runs into an error, log it with the exceptions logger
    private static void eraseComments(){
        try {
            Files.copy(new File("RootDir/media/RyanMedia/RyanCommentsDefault.html").toPath(), new File("RootDir/media/RyanMedia/RyanComments.html").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e){
            HttpsServer.excLogger.log(Level.WARNING, e.toString());
        }
    }

    // Get the URI for localhost and automatically direct the user to a new tab of their default browser 
    // with the localhost address
    // It it runs into an error, log it with the exceptions logger
    private static void loadServer() {
        try {
            Desktop.getDesktop().browse(new URI("https://localhost"));
        } catch (Exception e){
            HttpsServer.excLogger.log(Level.WARNING, e.toString());
        }
    }

    /*
    Reads from the .csv file and checks to see if the username and passwqors that were submitted match up with
    anything from the .csv file
    */
    public void actionPerformed(ActionEvent e) {
        // get the information from the two textfields and store them in the username and password variables
        success.setVisible(true);
        String username = userText.getText();
        String password = passwordText.getText();

        // get the filePath to the .csv file
        String filePath = "RootDir/Users.csv";

        // Initializes a boolean to see if we have found a match from the .csv file
        boolean found = false;

        // All logging intialization for the User_Login logger
        Logger userLogger = Logger.getLogger("User Login");
        FileHandler fhUser;
        SimpleFormatter formatter = new SimpleFormatter();

        // Creating a new Bufferedreader from the filePath so we are able to read the .csv file one
        // line at a time
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;

            //More initializtion for the logging handler
            fhUser = new FileHandler("RootDir/Logs/user_login.txt", true);
            fhUser.setFormatter(formatter);
            userLogger.addHandler(fhUser);
            userLogger.setUseParentHandlers(false);

            // while loop that will go through every single line of the .csv file
            while ((line = reader.readLine()) != null) {
                // Split the line into fields using a comma as the delimiter
                String[] fields = line.split(",");
                String csvUser = fields[0];
                String csvPass = fields[1].trim();

                // Making sure the file can be written to for logging the username and password
                String fileDir = "RootDir/Logs/user_login.txt";
				HttpsServer.checkLines(fileDir);

                // Checks if the current line from the csv file matches with the info that the user input
                // If it does, do everything needed to the GUI, log the username and password, and break out of the loop
                if (csvUser.equals(username) && csvPass.equals(password)) {
                    success.setText("Login successful :)");
                    found = true;
                    eraseCommentsButton.setVisible(true);
                    startServerButton.setVisible(true);
                    userLogger.log(Level.INFO, "User has logged in to server. Username: " + username + " Password: " + password);
                    break;
                }
                
            }
        // If we got through the entire file without finding a matching username and password, display an unsuccessful login
        if (!found) {
            success.setText("Login unsuccessful :(");
            eraseCommentsButton.setVisible(false);
            startServerButton.setVisible(false);
            serverStatusLabel.setVisible(false);
        }
        // An exception logger just in case any issue arises with this function
        } catch (Exception E) {
            HttpsServer.excLogger.log(Level.WARNING, e.toString());
        }

    }

}
