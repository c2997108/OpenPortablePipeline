package application;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

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
	        
	        //PP_BIN_DIRの設定
	        String ppBinDir = System.getProperty("PP_BIN_DIR");
	        if(ppBinDir == null || ppBinDir.compareTo("") == 0) {
	        	//PP_BIN_DIRシステムプロパティが設定されていなければ、カレントフォルダを指定
	        	ppBinDir="./"; //こっちは/をつけないと上手くいかないはず
	        	System.setProperty("PP_BIN_DIR", ppBinDir);
	        }
	        if(System.getProperty("os.name").toLowerCase().startsWith("mac")) {
	        	// 例：/Applications/PortablePipeline.app/Contents/app/PortablePipeline.jar
	        	Path jarPath = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	        	if(jarPath.getParent().getFileName().toString().compareTo("app")==0
	        			&& jarPath.getParent().getParent().getFileName().toString().compareTo("Contents")==0) {
	        		// Macで/Applicationsフォルダに配置されたものであるなら
	        		ppBinDir=jarPath.getParent().toString();
	        		System.setProperty("PP_BIN_DIR", ppBinDir);
	        	}
	        }
	        System.out.println("Binary Dir: " + ppBinDir);
	        
	        //PP_OUT_DIRの設定
	        String ppOutDir = System.getProperty("PP_OUT_DIR");
	        if(ppOutDir == null || ppOutDir.compareTo("") == 0) {
	        	//PP_OUT_DIRシステムプロパティが設定されていなければ、カレントフォルダを指定
	        	ppOutDir="./"; //こっちは/をつけなくても平気なはず。でもつけても平気だろうからつけておく
	        	System.setProperty("PP_OUT_DIR", ppOutDir);
	        }
	        if(System.getProperty("os.name").toLowerCase().startsWith("mac")) {
	        	// 例：/Applications/PortablePipeline.app/Contents/app/PortablePipeline.jar
	        	Path jarPath = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	        	if(jarPath.getParent().getFileName().toString().compareTo("app")==0
	        			&& jarPath.getParent().getParent().getFileName().toString().compareTo("Contents")==0) {
	        		// Macで/Applicationsフォルダに配置されたものであるなら
	        		ppOutDir=System.getProperty("user.home")+"/pp_out/";
	        		System.setProperty("PP_OUT_DIR", ppOutDir);
	        	}
	        }
	        System.out.println("Output Base Dir: " + ppOutDir);
	        
			BorderPane root = (BorderPane)FXMLLoader.load(getClass().getResource("JobWindow.fxml"));
			Scene scene = new Scene(root,1000,600);
	        Image icon = new Image( "file:"+ppBinDir+"/image/pipe.png" );
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