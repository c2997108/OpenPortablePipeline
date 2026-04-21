package application;

import javafx.scene.image.Image;

public class ScriptNode {
	public String filename;
	public String explanation;
	private Image icon;
	private String categoryName;
	private String displayName;
	private boolean categoryHeader;
	private boolean expanded;
	private int childCount;

	public ScriptNode() {
		this.filename = "";
		this.explanation = "";
		this.icon = null;
		this.categoryName = "";
		this.displayName = "";
		this.categoryHeader = false;
		this.expanded = true;
		this.childCount = 0;
	}

	public ScriptNode(String filename) {
		this(filename, "", null);
	}

	public ScriptNode(String filename, String explanation) {
		this(filename, explanation, null);
	}

	public ScriptNode(String filename, String explanation, Image icon) {
		this.filename = filename;
		this.explanation = explanation;
		this.icon = icon;
		this.categoryName = extractCategoryName(filename);
		this.displayName = extractDisplayName(filename);
		this.categoryHeader = false;
		this.expanded = true;
		this.childCount = 0;
	}

	public static ScriptNode createCategoryNode(String categoryName, Image icon, boolean expanded, int childCount) {
		ScriptNode node = new ScriptNode();
		node.filename = categoryName;
		node.categoryName = categoryName;
		node.displayName = categoryName;
		node.icon = icon;
		node.categoryHeader = true;
		node.expanded = expanded;
		node.childCount = childCount;
		return node;
	}

	private static String extractCategoryName(String filename) {
		int tildeIndex = filename.indexOf("~");
		if (tildeIndex > 0) {
			return filename.substring(0, tildeIndex);
		}
		return "Other";
	}

	private static String extractDisplayName(String filename) {
		int tildeIndex = filename.indexOf("~");
		if (tildeIndex >= 0 && tildeIndex + 1 < filename.length()) {
			return filename.substring(tildeIndex + 1);
		}
		return filename;
	}

	public Image getIcon() {
		return icon;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public boolean isCategoryHeader() {
		return categoryHeader;
	}

	public boolean isExpanded() {
		return expanded;
	}

	public int getChildCount() {
		return childCount;
	}
}
