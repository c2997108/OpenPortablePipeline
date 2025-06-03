package application;


import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class ScriptCell extends ListCell<ScriptNode> {
	//private HBox hbox1;
	//public JobCell() {
	//	hbox1 = new HBox();
	//}

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
		Button button1 = new Button("", new ImageView(new Image("file:image/iconmonstr-share-8-24.png", 16, 16, false, false)));
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
		Tooltip tooltipbutton1 = new Tooltip("Open an exapmle"); // example, not exapmle
		Tooltip.install(button1, tooltipbutton1);

            hBox.getChildren().add(button1);

		label1.setMinWidth(300d); // Consider adjusting if space is tight with icon
		label2.setMaxHeight(15d);
		Tooltip tooltip = new Tooltip(jNode.explanation);
		Tooltip.install(hBox, tooltip); // Tooltip on HBox might be broad, consider on label1 or label2
            hBox.getChildren().add(label1);
            hBox.getChildren().add(label2);
		setGraphic(hBox);
        }
	}
}
