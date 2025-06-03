package application;

import javafx.scene.image.Image;

public class ScriptNode {
	public String filename;
	public String explanation;
	private Image icon; // Changed to Image directly as import is added

	public ScriptNode() {
		this.filename = "";
		this.explanation = "";
		this.icon = null; // Or some default icon
	}

	public ScriptNode(String filename) {
		this.filename = filename;
		this.explanation = "";
		this.icon = null; // Or some default icon
	}

	public ScriptNode(String filename, String explanation) {
		this.filename = filename;
		this.explanation = explanation;
		this.icon = null; // Or some default icon
	}

	// New constructor with icon
	public ScriptNode(String filename, String explanation, Image icon) {
		this.filename = filename;
		this.explanation = explanation;
		this.icon = icon;
	}

	// Getter for icon
	public Image getIcon() {
		return icon;
	}
}
