package application;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class ConnectSsh {

	public ConnectSsh(JsonNode node) {
		// TODO 自動生成されたコンストラクター・スタブ

	}
	
	public static Session getSession(JsonNode node, String jobDir) throws Exception{

	    Session session = null;

	    JSch jsch = new JSch();

	    try {
	    	if(!node.get("privatekey").asText().equals("")) {
	    		FileWriter file = new FileWriter(jobDir+"/"+"id_rsa");
	    		PrintWriter pw = new PrintWriter(new BufferedWriter(file));
	    		pw.write(node.get("privatekey").asText());
	    		pw.close();
	    	}
	    }catch (Exception e) {
	    }

		if((new File(jobDir+"/"+"id_rsa")).exists()) {
			jsch.addIdentity(jobDir+"/"+"id_rsa", node.get("password").asText());
			//System.out.println("identity added ");
		}
		//System.out.println(node.get("user").asText());
		session = jsch.getSession(node.get("user").asText(), node.get("hostname").asText(), Integer.valueOf(node.get("port").asText()));

		//System.out.println("session created.");

		session.setConfig("StrictHostKeyChecking", "no");
		session.setPassword(node.get("password").asText());

		session.connect();
		//System.out.println("session connected.....");
		
		return session;
	}
	public static ChannelSftp getSftpChannel(JsonNode node, String jobDir) throws Exception{

		Session session = getSession(node, jobDir);
		ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
		channelSftp.setInputStream(System.in);
		channelSftp.setOutputStream(System.out);
		channelSftp.connect();
		System.out.println("sftp channel connected....");

		return channelSftp;
		
	}

	public static ChannelExec getSshChannel(JsonNode node, String jobDir) throws Exception{

		Session session = getSession(node, jobDir);
        ChannelExec channelexec = (ChannelExec) session.openChannel("exec");
		System.out.println("ssh channel connected....");
		
		return channelexec;
		
	}

}
