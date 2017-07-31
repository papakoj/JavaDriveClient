import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class UIClient extends Application {
	public static void main(String[] args) {
		launch(args);
	}

	private static final java.io.File DATA_STORE_DIR = new java.io.File(
			System.getProperty("user.home"), ".credentials/drive-java-client");


	@Override
	public void start(final Stage primaryStage) {
		primaryStage.setTitle("GOOGLE DRIVE JAVA CLIENT");

		if (!alreadyAuthorized()) {
			Button btn = new Button("LOGIN TO GOOGLE DRIVE");
			final GoogleOauthServer googleOauthServer = new GoogleOauthServer();

			final Thread t = new Thread(googleOauthServer);
			final GridPane settingsGrid = new GridPane();
			final Text notReadyText = new Text("Waiting for user authorization.\n Please check your browser.\n Click the done button when authorized.");
			settingsGrid.add(notReadyText, 0, 0);
			final Scene settings = new Scene(settingsGrid, 300, 250);
			final Button newBtn = new Button("Done with authorization");
			settingsGrid.add(newBtn, 1, 0);
			btn.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					System.out.println("Hello World!");
					primaryStage.setScene(settings);
					t.start();
				}
			});

			newBtn.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					if (googleOauthServer.isDone) {
						notReadyText.setText("Thanks for authorizing");
						t.interrupt();
						settingsGrid.getChildren().remove(newBtn);
					} else {
						notReadyText.setText("Not done.");
					}

				}
			});


			StackPane root = new StackPane();
			root.getChildren().add(btn);
			primaryStage.setScene(new Scene(root, 300, 250));
			primaryStage.show();
		} else {
			GridPane loggedinGrid = new GridPane();
			Scene loggedInScene = new Scene(loggedinGrid, 300, 250);
			Button selectStorage = new Button("Select storage location");
			selectStorage.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent event) {
					DirectoryChooser directoryChooser = new DirectoryChooser();
					directoryChooser.setTitle("Select storage location");
					directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
					File f = directoryChooser.showDialog(primaryStage);
					if (f != null) {
						Quickstart quickstart = new Quickstart();
						quickstart.syncDrive(f);
					}
				}
			});
			loggedinGrid.add(selectStorage, 0, 0);
			primaryStage.setScene(loggedInScene);
			primaryStage.show();
		

		}
	}

	public static boolean alreadyAuthorized() {
		JSONParser parser = new JSONParser();

		Object obj = null;
		try {
			obj = parser.parse(new FileReader(DATA_STORE_DIR));
		} catch (ParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("No credentials found");
			System.out.println("Please re-authenticate the application");
		}
		JSONObject jsonObject = (JSONObject) obj;
		System.out.println(jsonObject);
		String accessToken = (String) jsonObject.get("access_token");
		if (!accessToken.isEmpty()) {
			return true;
		}
		return false;
	}
}