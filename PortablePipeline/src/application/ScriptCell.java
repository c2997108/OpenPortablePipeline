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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.shape.Polygon;

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
            setText(null);

            if (jNode.isCategoryHeader()) {
                HBox categoryBox = new HBox(10d);
                categoryBox.setAlignment(Pos.CENTER_LEFT);
                categoryBox.setPadding(new Insets(7, 10, 7, 10));
                categoryBox.setBackground(new Background(new BackgroundFill(Color.web("#eef4f8"), new CornerRadii(8d), Insets.EMPTY)));

                StackPane toggleBadge = new StackPane();
                toggleBadge.setMinSize(22d, 22d);
                toggleBadge.setPrefSize(22d, 22d);
                toggleBadge.setMaxSize(22d, 22d);
                toggleBadge.setStyle("-fx-background-color: white; "
                        + "-fx-background-radius: 11; "
                        + "-fx-border-color: #c8d5df; "
                        + "-fx-border-radius: 11;");

                Polygon chevron = new Polygon();
                if (jNode.isExpanded()) {
                    chevron.getPoints().addAll(new Double[]{
                            5d, 8d,
                            11d, 14d,
                            17d, 8d
                    });
                } else {
                    chevron.getPoints().addAll(new Double[]{
                            8d, 5d,
                            14d, 11d,
                            8d, 17d
                    });
                }
                chevron.setFill(Color.web("#355466"));
                toggleBadge.getChildren().add(chevron);
                categoryBox.getChildren().add(toggleBadge);

                Image icon = jNode.getIcon();
                if (icon != null) {
                    ImageView iconView = new ImageView(icon);
                    iconView.setFitWidth(20);
                    iconView.setFitHeight(20);
                    categoryBox.getChildren().add(iconView);
                }

                Label categoryLabel = new Label(jNode.getCategoryName());
                categoryLabel.setStyle("-fx-font-weight: bold; "
                        + "-fx-font-size: 13px; "
                        + "-fx-text-fill: #2f4858;");
                categoryBox.getChildren().add(categoryLabel);

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                categoryBox.getChildren().add(spacer);

                Label countBadge = new Label(String.valueOf(jNode.getChildCount()));
                countBadge.setStyle("-fx-background-color: #d6e3ec; "
                        + "-fx-background-radius: 999; "
                        + "-fx-padding: 2 8 2 8; "
                        + "-fx-font-size: 11px; "
                        + "-fx-font-weight: bold; "
                        + "-fx-text-fill: #355466;");
                categoryBox.getChildren().add(countBadge);

                categoryBox.setOnMouseEntered((MouseEvent event) -> {
                    categoryBox.setBackground(new Background(new BackgroundFill(Color.web("#e3ecf2"), new CornerRadii(8d), Insets.EMPTY)));
                    toggleBadge.setStyle("-fx-background-color: #f8fbfd; "
                            + "-fx-background-radius: 11; "
                            + "-fx-border-color: #afc3d1; "
                            + "-fx-border-radius: 11;");
                });
                categoryBox.setOnMouseExited((MouseEvent event) -> {
                    categoryBox.setBackground(new Background(new BackgroundFill(Color.web("#eef4f8"), new CornerRadii(8d), Insets.EMPTY)));
                    toggleBadge.setStyle("-fx-background-color: white; "
                            + "-fx-background-radius: 11; "
                            + "-fx-border-color: #c8d5df; "
                            + "-fx-border-radius: 11;");
                });

                setGraphic(categoryBox);
                return;
            }

		HBox hBox = new HBox(5d); // Reduced spacing a bit to accommodate icon
            hBox.setPadding(new Insets(2, 8, 2, 20));
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

		Label label1 = new Label(jNode.getDisplayName());
		Label label2 = new Label(jNode.explanation);


		String baseUrl = "http://suikou.fs.a.u-tokyo.ac.jp/pp/";
		// Consider making the icon size for the button consistent (e.g., 16x16)
		Button button1 = new Button("", new ImageView(new Image("file:"+ ppBinDir + "/image/iconmonstr-share-8-24.png", 16, 16, false, false)));
		button1.setFocusTraversable(false);
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
		Tooltip tooltipbutton1 = new Tooltip("Open help page");
		Tooltip.install(button1, tooltipbutton1);
            hBox.getChildren().add(button1);

		label1.setMinWidth(300d); // Consider adjusting if space is tight with icon
		label2.setMaxHeight(15d);
		Tooltip tooltip = new Tooltip(jNode.explanation);
		Tooltip.install(hBox, tooltip); // Tooltip on HBox might be broad, consider on label1 or label2
            hBox.getChildren().add(label1);
            hBox.getChildren().add(label2);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            hBox.getChildren().add(spacer);

            Button buttonSource = new Button("source");
            buttonSource.setFocusTraversable(false);
            buttonSource.setStyle("-fx-background-color: transparent; "
                    + "-fx-padding: 0 4 0 4; "
                    + "-fx-font-size: 10px; "
                    + "-fx-text-fill: #666666;");
            Tooltip tooltipButtonSource = new Tooltip("View source code");
            Tooltip.install(buttonSource, tooltipButtonSource);
            buttonSource.setOnAction((ActionEvent e) -> {
                if (jNode != null && jNode.filename != null) {
                    if (JobWindowController.instance != null) {
                        JobWindowController.instance.handleShowSourceCode(jNode.filename);
                    } else {
                        System.err.println("JobWindowController instance is not available.");
                    }
                }
            });
            hBox.getChildren().add(buttonSource);

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
