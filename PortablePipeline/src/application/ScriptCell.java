package application;


import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;

public class ScriptCell extends ListCell<ScriptNode> {
	//private HBox hbox1;
	//public JobCell() {
	//	hbox1 = new HBox();
	//}

    String ppBinDir = System.getProperty("PP_BIN_DIR");
    
	@Override
    protected void updateItem(ScriptNode jNode, boolean empty) {

        super.updateItem(jNode, empty);

        if(empty || jNode == null) { // Added jNode == null check for safety
		setText(null);
		setGraphic(null);
        }else {
            //System.out.println(jNode.filename);

		HBox hBox = new HBox(5d); // Reduced spacing a bit to accommodate icon
            hBox.getChildren().clear(); // Clear previous children

            // Add icon
            Image icon = jNode.getIcon();
            if (icon != null) {
                ImageView iconView = new ImageView(icon);
                iconView.setFitWidth(24); // Set icon size
                iconView.setFitHeight(24);
                hBox.getChildren().add(iconView);
            } else {
                // Optional: Add a placeholder if icon is null, or leave empty space
                // For now, just adds a bit of space if no icon, can be adjusted.
                // ImageView placeholder = new ImageView();
                // placeholder.setFitWidth(16);
                // placeholder.setFitHeight(16);
                // hBox.getChildren().add(placeholder);
            }

		Label label1 = new Label(jNode.filename);
		Label label2 = new Label(jNode.explanation);


		String baseUrl = "http://suikou.fs.a.u-tokyo.ac.jp/pp/";
		// Consider making the icon size for the button consistent (e.g., 16x16)
		Button button1 = new Button("", new ImageView(new Image("file:"+ ppBinDir + "/image/iconmonstr-share-8-24.png", 16, 16, false, false)));
		try {
			button1.setOnAction((ActionEvent e) -> {
				System.out.println(jNode.filename);
				final String OS_NAME = System.getProperty("os.name").toLowerCase();
				try {
					Runtime rt = Runtime.getRuntime();
					String cmd = "";
					if(OS_NAME.startsWith("windows")) {
						cmd = "cmd /c start "+baseUrl+"/"+jNode.filename+"/index.html";
					}else if(OS_NAME.startsWith("mac")) {
						cmd = "open "+baseUrl+"/"+jNode.filename+"/index.html";
					}else if(OS_NAME.startsWith("linux")) {
						cmd = "firefox "+baseUrl+"/"+jNode.filename+"/index.html";
					}
					System.out.println(cmd);
					rt.exec(cmd);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			});
		}catch(Exception e) {
			e.printStackTrace();
		}
		Tooltip tooltipbutton1 = new Tooltip("Open an example"); // example, not exapmle
		Tooltip.install(button1, tooltipbutton1);

            hBox.getChildren().add(button1);

            Button buttonSource = new Button(); // Text removed later
            buttonSource.setText(""); // Set empty text for icon button
            Image sourceIcon = new Image("file:"+ ppBinDir + "/image/scode.png", 16, 16, false, false); // Consistent with button1
            ImageView sourceIconView = new ImageView(sourceIcon);
            buttonSource.setGraphic(sourceIconView);

            Tooltip tooltipButtonSource = new Tooltip("View source code"); // Tooltip name changed for clarity
            Tooltip.install(buttonSource, tooltipButtonSource);
            buttonSource.setOnAction((ActionEvent e) -> {
                if (jNode != null && jNode.filename != null) {
                    // Ensure JobWindowController.instance is not null if used directly
                    if (JobWindowController.instance != null) {
                        JobWindowController.instance.handleShowSourceCode(jNode.filename);
                    } else {
                        // Fallback or error handling if controller instance is not available
                        System.err.println("JobWindowController instance is not available.");
                        // Optionally show an alert to the user
                    }
                }
            });
            hBox.getChildren().add(buttonSource);

		label1.setMinWidth(300d); // Consider adjusting if space is tight with icon
		label2.setMaxHeight(15d);
		Tooltip tooltip = new Tooltip(jNode.explanation);
		Tooltip.install(hBox, tooltip); // Tooltip on HBox might be broad, consider on label1 or label2
            hBox.getChildren().add(label1);
            hBox.getChildren().add(label2);

            // Add mouse event handlers for background change
            hBox.setOnMouseEntered((MouseEvent event) -> {
                hBox.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, CornerRadii.EMPTY, Insets.EMPTY)));
            });

            hBox.setOnMouseExited((MouseEvent event) -> {
                hBox.setBackground(null); // Reset to default background
            });

		setGraphic(hBox);
        }
	}
}
