package application;

public class ScriptNode {
	public String filename;
	public String explanation;
	public ScriptNode() {
		this.filename="";
		this.explanation="";
	}
	public ScriptNode(String filename) {
		this.filename=filename;
		this.explanation="";
	}
	public ScriptNode(String filename, String explanation) {
		this.filename=filename;
		this.explanation=explanation;
	}
}
