package application;

public class JobNode {
	public String id;
	public String status;
	public String desc;
	public JobNode() {
		this.id="";
		this.status="";
		this.desc="";
	}
	public JobNode(String id, String status) {
		this.id = id;
		this.status = status;
		this.desc = "";
	}
	public JobNode(String id, String status, String desc) {
		this.id = id;
		this.status = status;
		this.desc = desc;
	}
	public void setStatus(String status) {
		this.status = status;
	}
}
