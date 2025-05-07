package application;

import java.io.File;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class Main extends Application {
	@Override
	public void start(Stage primaryStage) {
		try {
	        String path = new File(".").getAbsoluteFile().getParent();
	        System.out.println(path);
			BorderPane root = (BorderPane)FXMLLoader.load(getClass().getResource("JobWindow.fxml"));
			Scene scene = new Scene(root,1000,600);
	        Image icon = new Image( "file:image/pipe.png" );
	        primaryStage.getIcons().add( icon );
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setOnCloseRequest(event -> {System.exit(0);});
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		launch(args);
	}
}