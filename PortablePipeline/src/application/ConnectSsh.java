package application;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Logger;

import javafx.application.Platform;

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

	public static List<String> getSshCmdResult(PPSetting ppSetting, String privateKeyPath, String cmdString) throws Exception{
        String destHost = ppSetting.get("hostname");
        int destPort = Integer.valueOf(ppSetting.get("port"));
        String username = ppSetting.get("user");
        String pass = ppSetting.get("password");
        String privateKey = ppSetting.get("privatekey");

        return getSshCmdResult(destHost, destPort, username, pass, privateKey, privateKeyPath, cmdString);
	}
	public static List<String> getSshCmdResult(JsonNode node, String privateKeyPath, String cmdString) throws Exception{
        String destHost = node.get("hostname").asText();
        int destPort = Integer.valueOf(node.get("port").asText());
        String username = node.get("user").asText();
        String pass = node.get("password").asText();
        String privateKey = node.get("privatekey").asText();

        return getSshCmdResult(destHost, destPort, username, pass, privateKey, privateKeyPath, cmdString);
	}
	public static List<String> getSshCmdResult(String destHost, int destPort, String username, String pass, String privateKey, String privateKeyPath, String cmdString) throws Exception{
//        String destHost = ppSetting.get("hostname");
//        int destPort = Integer.valueOf(ppSetting.get("port"));
//        String username = ppSetting.get("user");
//        String pass = ppSetting.get("password");
//        String privateKey = ppSetting.get("privatekey");

        System.out.println("ssh CMD: "+cmdString);
        List<String> list = new ArrayList<>();

	    JSch jsch = new JSch();

	    try {
	    	if(!privateKey.equals("")) {
	    		FileWriter file = new FileWriter(privateKeyPath);
	    		PrintWriter pw = new PrintWriter(new BufferedWriter(file));
	    		pw.write(privateKey);
	    		pw.close();
				jsch.addIdentity(privateKeyPath, pass);
	    	}
	    }catch (Exception e) {
	    	System.err.println(e);
	    }

        Session destSession = jsch.getSession(username, destHost, destPort);
        destSession.setConfig("StrictHostKeyChecking", "no");
		destSession.setPassword(pass); //パスワード認証の場合はこれが有効
        destSession.connect();

        System.out.println("Connected to destination!");

        // 目的サーバ上でコマンド実行
        ChannelExec channel = (ChannelExec) destSession.openChannel("exec");
        channel.setCommand(cmdString);
        channel.connect();
        
		BufferedInputStream bin = null;
		//コマンド実行
		try {
			bin = new BufferedInputStream(channel.getInputStream());
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(bin, StandardCharsets.UTF_8));

			String data;
			while ((data = bufferedReader.readLine()) != null) {
				System.out.println(data);
				String data2 = new String(data);
				list.add(data2);
			}

			// 最後にファイルを閉じてリソースを開放する
			bufferedReader.close();
		} catch (Exception e) {
			System.err.println(e);
		} finally {
			if (bin != null) {
				try {
					bin.close();
				}
				catch (IOException e) {
				}
			}
		}

        channel.disconnect();
        destSession.disconnect();
		
		return list;
	}

	public static List<String> getSshCmdResult2StepSession(PPSetting ppSetting, String privateKeyPath, String cmdString) throws Exception{
        String bastionHost = ppSetting.get("hostname"); //基本的にはddbjのgwサーバ
        int bastionPort = Integer.valueOf(ppSetting.get("port"));
        String username = ppSetting.get("user");
        String passphrase = ppSetting.get("password");
        String privateKey = ppSetting.get("privatekey");
        
        return getSshCmdResult2StepSession(bastionHost, bastionPort, username, passphrase, privateKey, privateKeyPath, cmdString);
	}
	public static List<String> getSshCmdResult2StepSession(JsonNode node, String privateKeyPath, String cmdString) throws Exception{
        String bastionHost = node.get("hostname").asText(); //基本的にはddbjのgwサーバ
        int bastionPort = Integer.valueOf(node.get("port").asText());
        String username = node.get("user").asText();
        String passphrase = node.get("password").asText();
        String privateKey = node.get("privatekey").asText();
        
        return getSshCmdResult2StepSession(bastionHost, bastionPort, username, passphrase, privateKey, privateKeyPath, cmdString);
	}
	public static List<String> getSshCmdResult2StepSession(String bastionHost, int bastionPort, String username, String passphrase, String privateKey, String privateKeyPath, String cmdString) throws Exception{
//        String bastionHost = ppSetting.get("hostname"); //基本的にはddbjのgwサーバ
//        int bastionPort = Integer.valueOf(ppSetting.get("port"));
//        String username = ppSetting.get("user");
//        String passphrase = ppSetting.get("password");
//        String privateKey = ppSetting.get("privatekey");
        String destHost = "172.19.13.1"; //a001
        
        System.out.println("ssh CMD: "+cmdString);
        List<String> list = new ArrayList<>();

////sshクライアントの詳細ログ表示
//        JSch.setLogger(new Logger() {
//            @Override
//            public boolean isEnabled(int level) {
//                return true; // 全レベル出力
//            }
//
//            @Override
//            public void log(int level, String message) {
//                System.out.println("JSchLog [" + levelToString(level) + "]: " + message);
//            }
//
//            private String levelToString(int level) {
//                switch (level) {
//                    case Logger.DEBUG: return "DEBUG";
//                    case Logger.INFO: return "INFO";
//                    case Logger.WARN: return "WARN";
//                    case Logger.ERROR: return "ERROR";
//                    case Logger.FATAL: return "FATAL";
//                    default: return "UNKNOWN";
//                }
//            }
//        });
	    JSch jsch = new JSch();

	    try {
	    	if(!privateKey.equals("")) {
	    		FileWriter file = new FileWriter(privateKeyPath);
	    		PrintWriter pw = new PrintWriter(new BufferedWriter(file));
	    		pw.write(privateKey);
	    		pw.close();
				jsch.addIdentity(privateKeyPath, passphrase);
	    	}
	    }catch (Exception e) {
	    	System.err.println(e);
	    }
		
        // 1️ 踏み台セッション作成
        Session bastionSession = jsch.getSession(username, bastionHost, bastionPort);
        bastionSession.setConfig("StrictHostKeyChecking", "no");
		//session.setPassword(node.get("password").asText()); //基本的にddbjではパスワード認証はしない
        bastionSession.connect();

        System.out.println("Connected to bastion!");

        // 2️ ポートフォワーディング (踏み台 → 目的サーバ)
        int assignedPort = bastionSession.setPortForwardingL(0, destHost, 22);
        //int assignedPort = bastionSession.setPortForwardingL(3333, destHost, 22);
        System.out.println("Local port forwarding set: localhost:" + assignedPort + " → " + destHost + ":22");

        // 3️ 目的サーバへのセッション（forwarded port経由）
        JSch destJSch = new JSch();
		destJSch.addIdentity(privateKeyPath, passphrase);

        Session destSession = destJSch.getSession(username, "127.0.0.1", assignedPort);
        //Session destSession = destJSch.getSession(username, "localhost", 3333);
        destSession.setConfig("StrictHostKeyChecking", "no");
        destSession.connect();

        System.out.println("Connected to destination!");

        // 4️ 目的サーバ上でコマンド実行
        ChannelExec channel = (ChannelExec) destSession.openChannel("exec");
        channel.setCommand(cmdString);
        //channel.setInputStream(null);
        //channel.setErrStream(System.err);

        //InputStream in = channel.getInputStream();
        //System.out.println("CMD:"+cmdString);
        channel.connect();
        
		BufferedInputStream bin = null;
		//コマンド実行
		try {
			bin = new BufferedInputStream(channel.getInputStream());
			BufferedReader bufferedReader = new BufferedReader(
					new InputStreamReader(bin, StandardCharsets.UTF_8));

			String data;
			while ((data = bufferedReader.readLine()) != null) {
				System.out.println(data);
				String data2 = new String(data);
				list.add(data2);
			}

			// 最後にファイルを閉じてリソースを開放する
			bufferedReader.close();
//			bin.close();
//
//			System.out.println("b");
//			//標準エラー出力の入手
//			bin = new BufferedInputStream(channel.getErrStream());
//			bufferedReader = new BufferedReader(
//					new InputStreamReader(bin, StandardCharsets.UTF_8));
//			System.out.println(channel.isClosed());
//			while ((data = bufferedReader.readLine()) != null) {
//				System.out.println("b");
//				System.out.println("STDERR: "+ data);
//				String data2 = new String("STDERR: "+ data);
//				list.add(data2);
//			}
//			bufferedReader.close();
//			bin.close();
			
		} catch (Exception e) {
			System.err.println(e);
		} finally {
			if (bin != null) {
				try {
					bin.close();
				}
				catch (IOException e) {
				}
			}
		}

        channel.disconnect();
        destSession.disconnect();
        bastionSession.disconnect();
		
		return list;
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
