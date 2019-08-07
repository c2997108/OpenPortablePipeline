package application;


import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

public class ScriptCell extends ListCell<ScriptNode> {
	//private HBox hbox1;
	//public JobCell() {
	//	hbox1 = new HBox();
	//}

	@Override
    protected void updateItem(ScriptNode jNode, boolean empty) {

        super.updateItem(jNode, empty);

        if(empty) {
        	setText(null);
        	setGraphic(null);
        }else {
            //System.out.println(jNode.filename);


        	Label label1 = new Label(jNode.filename);
        	Label label2 = new Label(jNode.explanation);


        	HBox hBox = new HBox(20d);
        	label1.setMinWidth(300d);
        	label2.setMaxHeight(15d);
        	Tooltip tooltip = new Tooltip(jNode.explanation);
        	Tooltip.install(hBox, tooltip);
            hBox.getChildren().add(label1);
            hBox.getChildren().add(label2);
        	setGraphic(hBox);
        }

	}

}
