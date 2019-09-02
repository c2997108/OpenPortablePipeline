package application;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;

public class JobWindowController {

    @FXML
    private BorderPane bp;

    @FXML
    private ResourceBundle resources;

    @FXML
    private URL location;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab tabJobList;

    @FXML
    private ListView<JobNode> joblist;

    @FXML
    private ListView<String> joblog;

    @FXML
    private Tab tabAnalysis;

    @FXML
    private ListView<ScriptNode> scriptlist;

    @FXML
    private GridPane analysisGrid;

    @FXML
    private ScrollPane analysisScrollPane;

    @FXML
    private Button buttonRun;

    @FXML
    private Tab tabSettings;

    @FXML
    private TextField hostname;

    @FXML
    private TextField port;

    @FXML
    private TextField user;

    @FXML
    private TextField password;

    @FXML
    private TextArea privatekey;

    @FXML
    private TextField workfolder;

    @FXML
    private TextField outputfolder;

    @FXML
    private TextField scriptfolder;

    @FXML
    private TextField imagefolder;

    @FXML
    private ToggleGroup preset;

    @FXML
    private Button savesetting;

    @FXML
    private Button searchbtn;

    @FXML
    private TextField searchtxt;

    String file_id_rsa = "id_rsa.txt";

    ObservableList<String> listRecords = FXCollections.observableArrayList();
    //ObservableList<String> listRecordsJob = FXCollections.observableArrayList();
    static ObservableList<JobNode> jobNodes = FXCollections.observableArrayList();
    ObservableList<String> listScripts = FXCollections.observableArrayList();
    ObservableList<ScriptNode> ScriptNodes = FXCollections.observableArrayList();
    ObservableList<ScriptNode> ScriptNodesOrig = FXCollections.observableArrayList();

    //static ChannelSftp channelSftp = null;
    static Session session = null;
    static Channel channel = null;

    ArrayNode arrayNode;

    String selectedScript = null;
    String cmd = null;
    String savedOutputFolder = null;
    String selectedPreset = null;
    String savedOpenFolder = ".";
    boolean isSending = false;

    Map<String, Map<String, String>> settings = new LinkedHashMap<String, Map<String, String>>();
    String[] settingPresetKey = {"direct","direct (SGE)","ddbj","shirokane","WSL","Mac"};
    String[] settingItemKey = {"hostname", "port", "user", "password", "privatekey", "workfolder", "imagefolder"};

    @FXML
    @SuppressWarnings("deprecation")
    void onButtonSave(ActionEvent event) {
        String path = new File(".").getAbsoluteFile().getParent();
        System.out.println(path);

		try {
	        Writer out = new PrintWriter("settings.json");

	        JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(out);

	        jsonGenerator.writeStartObject();jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeStringField("hostname",hostname.getText());jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeStringField("port",port.getText());jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeStringField("user",user.getText());jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeStringField("password",password.getText());jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeStringField("privatekey",privatekey.getText());jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeStringField("workfolder",workfolder.getText());jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeStringField("preset",((RadioButton)preset.getSelectedToggle()).getText());jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeStringField("outputfolder",outputfolder.getText());jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeStringField("scriptfolder",scriptfolder.getText());jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeStringField("imagefolder",imagefolder.getText());jsonGenerator.writeRaw("\n");
	        jsonGenerator.writeEndObject();
	        jsonGenerator.flush();

	        if(!privatekey.getText().equals("")) {
	        	FileWriter file = new FileWriter(file_id_rsa);
	        	PrintWriter pw = new PrintWriter(new BufferedWriter(file));
	        	pw.write(privatekey.getText());
	        	pw.close();
	        }else {
	        	File tempfile = new File(file_id_rsa);
	        	if(tempfile.exists()) {
	            	tempfile.delete();
	            }
	        }
	        savedOutputFolder = new PPSetting().get("outputfolder");
	        selectedPreset = new PPSetting().get("preset");
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
    }

    @FXML
    void onButtonRun(ActionEvent event) {

        Task<Boolean> task   = new Task<Boolean>()
        {
            @Override
            protected Boolean call() throws Exception
            {

                for(Node i : analysisGrid.getChildren()) {
                	if(i.getId()!=null && i.getId().startsWith("txt.input.")) {
                		if(i.getId().substring(12,13).equals("r")) {
                			//System.out.println(i.getId()+":"+((TextField)i).getText());
                			if(((TextField)i).getText().equals("")) {
                				//System.out.println("no input");
                				Platform.runLater(()->{
                					Alert alert = new Alert(AlertType.WARNING);
                					alert.setContentText(i.getId().substring(14)+" is required!");
                					alert.showAndWait();
                				});
                				return false;
                			}
                		}

                	}
                }


                Platform.runLater(()->{
                	tabPane.getSelectionModel().select(tabJobList);
                });


                isSending = true;
            	int workid = 0;
            	JobNode tempJobNode = null;
                try {

                    String jobdesc = selectedScript;
                	ObjectNode feature = new ObjectMapper().createObjectNode();
                	String tempid;
                	try {
                		tempid = jobNodes.get(jobNodes.size()-1).id;
                	}catch(Exception e) {
                		tempid = "0";
                	}
                	//String tempid = arrayNode.get(arrayNode.size()-1).get("id").toString().replace("\"", "");
                	System.out.println(tempid);
                	workid = Integer.valueOf(tempid)+1;
                	feature.put("id", String.valueOf(workid));
                	feature.put("status", "preparing");
                	arrayNode.add(feature);
                	JobNode newJobNode = new JobNode(String.valueOf(workid),"preparing");
                	tempJobNode = newJobNode;
                	Platform.runLater( () -> {
                		jobNodes.add(newJobNode);
                		joblist.scrollTo(newJobNode);
                		joblist.getSelectionModel().select(newJobNode);
                    	saveJobList();
                	});
                	//Platform.runLater( () -> listRecordsJob.add(String.valueOf(workid)) );

                	//File saveFolder = new File(savedOutputFolder+"/"+workid);
                	//saveFolder.mkdirs();
                	new File(savedOutputFolder+"/"+workid+"/results").mkdirs();

        	        Writer out = new PrintWriter(savedOutputFolder+"/"+workid+"/"+"settings.json");
        	        JsonFactory jsonFactory = new JsonFactory();
        			JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(out);

        			PPSetting ppSetting = new PPSetting();
        	        jsonGenerator.writeStartObject();
        	        jsonGenerator.writeStringField("hostname",ppSetting.get("hostname"));
        	        jsonGenerator.writeStringField("port",ppSetting.get("port"));
        	        jsonGenerator.writeStringField("user",ppSetting.get("user"));
        	        jsonGenerator.writeStringField("password",ppSetting.get("password"));
        	        jsonGenerator.writeStringField("privatekey",ppSetting.get("privatekey"));
        	        jsonGenerator.writeStringField("workfolder",ppSetting.get("workfolder"));
        	        jsonGenerator.writeStringField("preset",ppSetting.get("preset"));
        	        jsonGenerator.writeStringField("outputfolder",ppSetting.get("outputfolder"));
        	        jsonGenerator.writeStringField("scriptfolder",ppSetting.get("scriptfolder"));
        	        jsonGenerator.writeStringField("imagefolder",ppSetting.get("imagefolder"));
        	        jsonGenerator.writeEndObject();
        	        jsonGenerator.flush();


                    JSch jsch = new JSch();
                    ChannelSftp c = null;

                    if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac")) {
                    	if((new File(file_id_rsa)).exists()) {
                    		jsch.addIdentity(file_id_rsa);
                    	}
                    	System.out.println("identity added ");
                    	session = jsch.getSession(ppSetting.get("user"), ppSetting.get("hostname"), Integer.valueOf(ppSetting.get("port")));
                    	System.out.println("session created.");

                    	session.setConfig("StrictHostKeyChecking", "no");
                    	session.setPassword(ppSetting.get("password"));

                    	session.connect();
                    	System.out.println("session connected.....");

                    	channel = session.openChannel("sftp");
                    	channel.setInputStream(System.in);
                    	channel.setOutputStream(System.out);
                    	channel.connect();
                    	System.out.println("shell channel connected....");

                    	c = (ChannelSftp) channel;
                    }
                    //mkdir work
                    String workdir = ppSetting.get("workfolder")+"/"+workid;
                    String resultdir = ppSetting.get("outputfolder")+"/"+workid+"/results";

                    if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac")) {
                    	String tempworkpath = "";
                    	for(String tempworkpathnode : workdir.split("/")) {
                    		if(tempworkpath.equals("")) {
                    			if(workdir.startsWith("/")) {
                    				tempworkpath="/"+tempworkpathnode;
                    			}else {
                    				tempworkpath=tempworkpathnode;
                    			}
                    		}else {
                    			tempworkpath = tempworkpath+"/"+tempworkpathnode;
                    		}
                    		try {
                    			//System.out.println("mkdir...  "+tempworkpath);
                    			c.mkdir(tempworkpath);
                    		}catch(Exception e2) {
                    			//e2.printStackTrace();
                    		}
                    	}
                    }
                    //transfer the script file
                    List<String> scriptcontList = new ArrayList<String>();
                    searchScript(ppSetting.get("scriptfolder"), selectedScript, scriptcontList);

                    if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac")) {
            			System.out.println("sending...  "+selectedScript);
                    	c.put(ppSetting.get("scriptfolder")+"/"+selectedScript, workdir);
            			System.out.println("sending...  "+"common.sh");
                    	c.put(ppSetting.get("scriptfolder")+"/common.sh", workdir);
                    	for(String scripti: scriptcontList) {
                			System.out.println("sending...  "+scripti);
                    		c.put(ppSetting.get("scriptfolder")+"/"+scripti, workdir);
                    	}
                    }
                    mkSymLinkOrCopy(ppSetting.get("scriptfolder")+"/"+selectedScript, resultdir);
                    mkSymLinkOrCopy(ppSetting.get("scriptfolder")+"/common.sh", resultdir);
                	for(String scripti: scriptcontList) {
                		mkSymLinkOrCopy(ppSetting.get("scriptfolder")+"/"+scripti, resultdir);
                	}

                    //analysisGrid.getScene().getRoot().applyCss();
                    //System.out.println("aaaa: "+((Label) analysisGrid.lookup("#txt.opt.opt_1")).getText());
            		String runcmd = "";
            		String currentOptDesc = "";
            		String numCPU = "1";
            		String numMEM = "8";
                    for(Node i : analysisGrid.getChildren()) {
                    	//System.out.println("childId: "+i.getId());
                    	if(i.getId()!=null && i.getId().startsWith("txt.optdesc.")) {
                    		currentOptDesc = ((Label)i).getText();
                    		//System.out.println("cuD: "+currentOptDesc);
                    	}
                    	if(i.getId()!=null && i.getId().startsWith("txt.input.")) {
                    		String fileid = i.getId().substring(14);
                    		String tempfiles1=((TextField)i).getText();
                    		jobdesc+=" "+fileid+":"+tempfiles1;
                            if(tempfiles1.startsWith("\"")) {
                            	tempfiles1=tempfiles1.substring(1);
                            }
                            if(tempfiles1.endsWith("\"")) {
                            	tempfiles1=tempfiles1.substring(0,tempfiles1.length()-1);
                            }
                            String[] tempfiles1s = tempfiles1.split("\",\"");

                            //modify cmd
                    		String[] cmdsplit;
                            if(runcmd.equals("")) {
                            	cmdsplit = cmd.split("[#]",-1);
                            }else {
                            	cmdsplit = runcmd.split("[#]",-1);
                            }
                            runcmd = "";
                    		for(int j=0;j<cmdsplit.length;j++) {
                    			String tempstr = cmdsplit[j];
                    			if(tempstr.equals(fileid)) {
                    				if(!tempfiles1s[0].equals("")) {
                    					if(i.getId().substring(10,11).equals("m")) {
                    						runcmd=runcmd+"'"+fileid+"'";
                    					}else {
                    						File tempFile = new File(tempfiles1s[0]);
                    						runcmd=runcmd+"'"+fileid+"/"+tempFile.getName()+"'";
                    					}
                    				}else {
                    					runcmd=runcmd+"''";
                    				}
                					j++;
                					runcmd=runcmd+cmdsplit[j];
                    			}else {
                    				if(j==0) {
                    					runcmd=tempstr;
                    				}else {
                    					runcmd=runcmd+"#"+tempstr;
                    				}
                    			}
                    		}
                    		//System.out.println(runcmd);
                    		//send files
                            for (String fileName: tempfiles1s) {
                            	if(!fileName.equals("")) {
                            		if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac")) {
                            			System.out.println("sending...  "+fileName);
                            			try {
                            				c.mkdir(workdir+"/"+fileid);
                            				new File(resultdir+"/"+fileid).mkdirs();
                            			}catch(Exception e2) {
                            				//e2.printStackTrace();
                            			}
                            			c.put(fileName, workdir+"/"+fileid);
                            		}else {
                                		System.out.println("copying...  "+fileName);
                            			try {
                            				new File(resultdir+"/"+fileid).mkdirs();
                            			}catch(Exception e2) {
                            			}
                            		}
                                    mkSymLinkOrCopy(fileName, resultdir+"/"+fileid);
                            	}
                            }
                    	}
                    	//option
                    	if(i.getId()!=null && i.getId().startsWith("txt.opt.")) {
                    		String optid = i.getId().substring(8);
                    		String tempoptString=((TextField)i).getText();
                    		jobdesc+=" "+optid+":"+tempoptString;
                    		//System.out.println("test cuD: "+currentOptDesc);
                    		if(currentOptDesc.equals("cpu threads")) {
                    			numCPU = tempoptString;
                    			//System.out.println("cpu: "+numCPU);
                    		}else if(currentOptDesc.equals("memory limit (GB)")) {
                    			numMEM = tempoptString;
                    		}
                            //modify cmd
                    		String[] cmdsplit;
                            if(runcmd.equals("")) {
                            	cmdsplit = cmd.split("[#]",-1);
                            }else {
                            	cmdsplit = runcmd.split("[#]",-1);
                            }
                            runcmd = "";
                    		for(int j=0;j<cmdsplit.length;j++) {
                    			String tempstr = cmdsplit[j];
                    			if(tempstr.equals(optid)) {
                    				runcmd=runcmd+"'"+tempoptString+"'";
                    				j++;
                    				runcmd=runcmd+cmdsplit[j];
                    			}else {
                    				if(j==0) {
                    					runcmd=tempstr;
                    				}else {
                    					runcmd=runcmd+"#"+tempstr;
                    				}
                    			}
                    		}
                    		//System.out.println(runcmd);
                    	}
                    }
                    System.out.println(selectedScript+" "+runcmd);
                    //System.out.println("done");

                    String cmdString = "";
                    try{
                    	File file;

                		if(!selectedPreset.equals("WSL")) {
                			file = new File(ppSetting.get("outputfolder")+"/"+workid+"/results/"+"wrapper.sh");
                		}else{
                			file = new File(ppSetting.get("outputfolder")+"/"+workid+"/results/"+"wrapper.bat");
                		}
                    	FileWriter filewriter = new FileWriter(file);

                    	if(selectedPreset.equals("direct")) {
                        	filewriter.write("#!/bin/bash\n");
                        	filewriter.write("export DIR_IMG="+ppSetting.get("imagefolder")+"\n");
                        	filewriter.write("nohup bash "+selectedScript+" "+runcmd+" > log.txt 2>&1 &\n");
                        	filewriter.write("echo $! > save_pid.txt\n");
                        	cmdString = "cd "+workdir+"; bash wrapper.sh";
                    	}else if(selectedPreset.equals("shirokane") || selectedPreset.equals("ddbj") || selectedPreset.equals("direct (SGE)")) {
                        	filewriter.write("#!/bin/bash\n");
                        	filewriter.write("#$ -S /bin/bash\n");
                        	filewriter.write("#$ -cwd\n");
                        	filewriter.write("#$ -pe def_slot "+numCPU+"\n");
                        	filewriter.write("#$ -l mem_req="+ Double.valueOf(numMEM)/Double.valueOf(numCPU) +"G,s_vmem="+Double.valueOf(numMEM)/Double.valueOf(numCPU)+"G\n");
                        	filewriter.write("export DIR_IMG="+ppSetting.get("imagefolder")+"\n");
                        	filewriter.write("source ~/.bashrc\n");
                        	filewriter.write("bash "+selectedScript+" "+runcmd+" > log.txt 2>&1\n");
                        	cmdString = "cd "+workdir+"; qsub wrapper.sh > save_jid.txt";
                    	}else if(selectedPreset.equals("WSL")){
                    		String curDir = new File(".").getAbsoluteFile().getParent();
                    		String wslcurDir = "/mnt/"+curDir.substring(0,1).toLowerCase()+curDir.substring(2);
                    		wslcurDir = wslcurDir.replaceAll("\\\\", "/");
                    		filewriter.write("powershell.exe start-process bash -Wait -ArgumentList '-c \\\"cd "+wslcurDir+"; echo "+password.getText()+"|sudo -S bash WSL-install.sh\\\"'\r\n");
                    		filewriter.write("powershell.exe start-process bash -Wait -verb runas -ArgumentList '-c \\\"if [ `service docker status|grep \\\" is running\\\"|wc -l` = 0 ]; then sudo cgroupfs-mount; sudo service docker start; fi\\\"'\r\n");
                    		filewriter.write("powershell.exe start-sleep -s 3\r\n");
                    		filewriter.write("powershell.exe start-process bash -Wait -verb runas -ArgumentList '-c \\\"if [ `service docker status|grep \\\" is running\\\"|wc -l` = 0 ]; then sudo cgroupfs-mount; sudo service docker start; fi\\\"'\r\n");
                    		filewriter.write("powershell.exe start-process bash -ArgumentList '-c \\\"echo Do not close this window.; bash wrapper2.sh\\\"'\r\n");

                        	File file2 = new File(ppSetting.get("outputfolder")+"/"+workid+"/results/"+"wrapper2.sh");
                        	FileWriter filewriter2 = new FileWriter(file2);
                        	filewriter2.write("#!/bin/bash\n");
                        	filewriter2.write("export DIR_IMG="+ppSetting.get("imagefolder")+"\n");
                        	filewriter2.write("nohup bash "+selectedScript+" "+runcmd+" > log.txt 2>&1 &\n");
                        	filewriter2.write("echo $! > save_pid.txt\n");
                        	filewriter2.write("wait\n"); // for win10 1803, 1809
                        	filewriter2.close();

                        	cmdString = "cmd.exe /c wrapper.bat";
                    	}else if(selectedPreset.equals("Mac")){
                        	filewriter.write("#!/bin/bash\n");
                        	filewriter.write("export DIR_IMG="+ppSetting.get("imagefolder")+"\n");
                        	filewriter.write("export PATH=/usr/local/bin:${PATH}\n");
                        	filewriter.write("export PATH=/usr/local/opt/coreutils/libexec/gnubin:${PATH}\n");
                        	filewriter.write("source ~/.bash_profile\n");
                        	filewriter.write("nohup bash "+selectedScript+" "+runcmd+" > log.txt 2>&1 &\n");
                        	filewriter.write("echo $! > save_pid.txt\n");
                        	cmdString = "bash wrapper.sh";
                    	}

                    	filewriter.close();

                    	if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac")) {
                    		c.put(ppSetting.get("outputfolder")+"/"+workid+"/results/"+"wrapper.sh", workdir);
                    	}
                    }catch(IOException e){
                    	System.out.println(e);
                    	return false;
                    }

                    System.out.println(cmdString);

                    if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac")) {
                    	ChannelExec channelexec = (ChannelExec) session.openChannel("exec");
                    	//channelexec.setCommand("cd "+workdir+"; bash "+selectedScript+" "+runcmd);
                    	channelexec.setCommand(cmdString);
                    	//channelexec.setCommand("ls -l |wc -l");
                    	//channelexec.setPty(false);
                    	channelexec.connect();

                    	BufferedInputStream bin = null;
                    	//コマンド実行
                    	try {
                    		bin = new BufferedInputStream(channelexec.getInputStream());
                    		BufferedReader bufferedReader = new BufferedReader(
                    				new InputStreamReader(bin, StandardCharsets.UTF_8));

                    		String data;
                    		while ((data = bufferedReader.readLine()) != null) {
                    			System.out.println(data);
                    			String data2 = new String(data);
                    			Platform.runLater( () -> listRecords.add(data2) );
                    			//listRecords.add(data);
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

                    	//download results
                    	//channelSftp = (ChannelSftp) session.openChannel("sftp");
                    	//channelSftp = c;
                    	//recursiveFolderDownload(workdir, saveFolder.toString()+"/"+"results",c);
                    	//        lsFolderRemove(workdir);
                    	//channelSftp.exit();
                    }else {
                    	Process process = Runtime.getRuntime().exec(cmdString, null, new File(ppSetting.get("outputfolder")+"/"+workid+"/results/"));
                    	process.waitFor();
                	}

                    System.out.println(jobdesc);
                	JobNode runninngJobNode = new JobNode(String.valueOf(workid),"running",jobdesc);
                	tempJobNode = runninngJobNode;
             		Platform.runLater( () -> {
             			jobNodes.set( jobNodes.indexOf(newJobNode),runninngJobNode );
             			saveJobList();
             		});

                } catch (Exception e) {
                    System.err.println(e);
                    int finalworkid = workid;
                    JobNode finalJobNode = tempJobNode;
                	JobNode runninngJobNode = new JobNode(String.valueOf(finalworkid),"aborted",e.toString());
             		Platform.runLater( () -> {
             			jobNodes.set( jobNodes.indexOf(finalJobNode),runninngJobNode );
             			saveJobList();
             		});
                } finally {
                    if (channel != null) {
                        try {
                            channel.disconnect();
                        }
                        catch (Exception e) {
                        }
                    }
                    if (session != null) {
                        try {
                            session.disconnect();
                        }
                        catch (Exception e) {
                        }
                    }
                }

                isSending = false;
                return true;
            }
        };

        // タスクを実行1
        Thread t = new Thread( task );
        t.setDaemon( true );
        t.start();
    }

    @FXML
    void initialize() {
        assert hostname != null : "fx:id=\"hostname\" was not injected: check your FXML file 'JobWindow.fxml'.";
        assert port != null : "fx:id=\"port\" was not injected: check your FXML file 'JobWindow.fxml'.";
        assert user != null : "fx:id=\"user\" was not injected: check your FXML file 'JobWindow.fxml'.";
        assert password != null : "fx:id=\"password\" was not injected: check your FXML file 'JobWindow.fxml'.";
        assert privatekey != null : "fx:id=\"privatekey\" was not injected: check your FXML file 'JobWindow.fxml'.";
        assert outputfolder != null : "fx:id=\"outputfolder\" was not injected: check your FXML file 'JobWindow.fxml'.";
        assert scriptfolder != null : "fx:id=\"scriptfolder\" was not injected: check your FXML file 'JobWindow.fxml'.";
        assert preset != null : "fx:id=\"preset\" was not injected: check your FXML file 'JobWindow.fxml'.";
        assert savesetting != null : "fx:id=\"savesetting\" was not injected: check your FXML file 'JobWindow.fxml'.";

    	analysisGrid.getColumnConstraints().addAll(new ColumnConstraints(18), new ColumnConstraints(40), new ColumnConstraints(40), new ColumnConstraints(2));

    	for(String iString : settingPresetKey) {
    		Map<String, String> tempMap = new LinkedHashMap<String, String>();
//    		for(String jString : settingItemKey) {
//    			tempMap.put(jString, "");
//    		}
    		tempMap.put("changed", "F");
    		settings.put(iString, tempMap);
    	}

        analysisScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        analysisScrollPane.setFitToWidth(true);
        //String path = new File(".").getAbsoluteFile().getParent();
        //System.out.println(path);
        //ObjectMapper mapper = new ObjectMapper();
        //JsonNode node;
        PPSetting ppSetting;
        try {
            ppSetting = new PPSetting();
            //node = mapper.readTree(new File("settings.json"));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        buttonRun.setDisable(true);
        hostname.setText(ppSetting.get("hostname"));
        port.setText(ppSetting.get("port"));
        user.setText(ppSetting.get("user"));
        password.setText(ppSetting.get("password"));
        privatekey.setText(ppSetting.get("privatekey"));
        if(ppSetting.get("workfolder").equals("")) {
        	workfolder.setText("work");
        }else {
            workfolder.setText(ppSetting.get("workfolder"));
        }
        if(ppSetting.get("outputfolder").equals("")) {
        	outputfolder.setText("output");
        }else {
            outputfolder.setText(ppSetting.get("outputfolder"));
        }
        if(ppSetting.get("scriptfolder").equals("")) {
        	scriptfolder.setText("scripts");
        }else {
            scriptfolder.setText(ppSetting.get("scriptfolder"));
        }
        if(ppSetting.get("imagefolder").equals("")) {
            imagefolder.setText("~/img");
        }else {
        	imagefolder.setText(ppSetting.get("imagefolder"));
        }
        preset.getToggles().forEach(
        		s -> {
        			//System.out.println(((RadioButton)s).getText());
        			if(((RadioButton)s).getText().equals(ppSetting.get("preset"))){
        				s.setSelected(true);
        			};
        		}
        		);
        savedOutputFolder = ppSetting.get("outputfolder");
        selectedPreset = ppSetting.get("preset");

		switch(selectedPreset) {
		case "direct":
			break;
		case "direct (SGE)":
			break;
		case "ddbj":
			password.setDisable(true);
			password.setText("");
			break;
		case "shirokane":
			password.setDisable(true);
			password.setText("");
			break;
		case "WSL":
			hostname.setDisable(true);
			hostname.setText("");
			port.setDisable(true);
			port.setText("");
			privatekey.setDisable(true);
			privatekey.setText("");
			imagefolder.setDisable(true);
			imagefolder.setText("");
			workfolder.setDisable(true);
			workfolder.setText("");
			break;
		case "Mac":
			hostname.setDisable(true);
			hostname.setText("");
			port.setDisable(true);
			port.setText("");
			privatekey.setDisable(true);
			privatekey.setText("");
			user.setDisable(true);
			user.setText("");
			password.setDisable(true);
			password.setText("");
			imagefolder.setDisable(true);
			imagefolder.setText("");
			workfolder.setDisable(true);
			workfolder.setText("");
			break;
		default:
			System.out.println("no preset value");
		}

		tabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) ->{
			//System.out.println(oldTab.getText()+"; "+ newTab.getText());
			if(oldTab.getText().compareTo("Settings")==0) {
				onButtonSave(null);
			}
		});

        joblog.setItems(listRecords);

        joblist.setCellFactory(new Callback<ListView<JobNode>, ListCell<JobNode>>(){
        	@Override
        	public ListCell<JobNode> call(ListView<JobNode> listView){
        		return new JobCell();
        	}
        });
        //ObservableList<JobNode> jobNodes = FXCollections.observableArrayList();
    	int numOfJob = 0;
        ObjectMapper mapperJob = new ObjectMapper();
        try {
        	arrayNode = new ObjectMapper().createArrayNode();
        	List<JobNode> jobs = mapperJob.readValue(new File("jobs.json"), new TypeReference<List<JobNode>>(){});


            for(JobNode job : jobs) {
            	//listRecordsJob.add(job.id);

            	ObjectNode feature = new ObjectMapper().createObjectNode();
            	feature.put("id", job.id);
            	feature.put("status", job.status);
            	arrayNode.add(feature);

            	JobNode tempJobNode = new JobNode(job.id, job.status, job.desc);
            	jobNodes.add(tempJobNode);
            	numOfJob++;
            }

            //System.out.println(mapperJob.writeValueAsString(arrayNode));

        } catch (IOException e) {
            //e.printStackTrace();
        }
        joblist.setItems(jobNodes);
        int numOfJobFinal = numOfJob;
        Platform.runLater(() -> {joblist.scrollTo(numOfJobFinal-1);});

        joblist.getSelectionModel().selectedItemProperty().addListener(
            new ChangeListener<Object>(){
           	    @Override
           	    public void changed(ObservableValue<?> observable, Object oldVal, Object newVal) {
        	        // 実行する処理
           	    	JobNode tempNode = (JobNode)newVal;
           	    	System.out.println(((JobNode)newVal).id);
           	    	listRecords.clear();
           	    	try {
						List<String> lines = Files.readAllLines(Paths.get(new PPSetting().get("outputfolder")+"/"+tempNode.id+"/results/log.txt"), StandardCharsets.UTF_8);
						List<String> templines = new ArrayList<String>();
						for(String string : lines) {
							if(templines.size()>=1000) {
								templines.remove(0);
							}
							templines.add(string);
						}
	        	    	for(String string : templines) {
	               	    	//System.out.println(string);

	                 		Platform.runLater( () -> listRecords.add(string) );
	                    	Platform.runLater( () -> joblog.scrollTo(listRecords.size()-1)  );
	        	    	}
	        	    } catch (IOException e) {
	        	    	//e.printStackTrace();
           		   	}
           		}
            }
        );


        searchbtn.setOnAction((ActionEvent event)->{
        	//System.out.println(searchtxt.getText());
        	Platform.runLater( ()->{
        		ScriptNodes.clear();
            	for(ScriptNode sNode : ScriptNodesOrig) {
            		if(sNode.filename.contains(searchtxt.getText()) || sNode.explanation.contains(searchtxt.getText())){
            			ScriptNodes.add(sNode);
            		}
            	}

        	});
        });


        File[] scriptFiles = new File(ppSetting.get("scriptfolder")).listFiles();
        java.util.Arrays.sort(scriptFiles, new java.util.Comparator<File>() {
    		public int compare(File file1, File file2){
    		    return file1.getName().toLowerCase().compareTo(file2.getName().toLowerCase());
    		}
    	    });
        for(File script : scriptFiles) {
        	if(!script.getName().equals("common.sh")) {
        		String explanationString = "";
        		boolean explanationfield = false;
        		try {
        			BufferedReader br = new BufferedReader(new FileReader(script));
        			try {
        				while (true) {
        					String line = br.readLine();
        					if (line == null) {
        						break;
        					}

        					//explanation
        					if(line.equals("'")) {
        						explanationfield = false;
        					}
        					if(explanationfield) {
        						explanationString+=line+"\n";
        					}
        					if(line.equals("explanation='")) {
        						explanationfield=true;
        					}
        				}
        			}finally {
        				br.close();
        			}
        		} catch (Exception e) {
        			// TODO 自動生成された catch ブロック
        			e.printStackTrace();
        		}
        		ScriptNodes.add(new ScriptNode(script.getName(), explanationString));
        		ScriptNodesOrig.add(new ScriptNode(script.getName(), explanationString));
        		listScripts.add(script.getName());
        	}
        }

        scriptlist.setCellFactory(new Callback<ListView<ScriptNode>, ListCell<ScriptNode>>(){
        	@Override
        	public ListCell<ScriptNode> call(ListView<ScriptNode> listView){
        		return new ScriptCell();
        	}
        });
        scriptlist.setItems(ScriptNodes);
        scriptlist.getSelectionModel().selectedItemProperty().addListener(
        	new ChangeListener<Object>(){
       		    @Override
       		    public void changed(ObservableValue<?> observable, Object oldVal, Object newVal) {
    		        // 実行する処理
       		    	//System.out.println(newVal);
       		    	buttonRun.setDisable(false);
       		    	selectedScript = ((ScriptNode)newVal).filename;
       		    	List<InputItem> listInputItems = new ArrayList<>();
       		    	List<OptionItem> listOptionItems = new ArrayList<>();

       		    	//スクリプトファイルを読み取ってInput, Optionを設定する
       		    	try {
       		    	    BufferedReader br = new BufferedReader(new FileReader(new PPSetting().get("scriptfolder")+"/"+selectedScript));
       		    	    try {
       		    	    	boolean inputfield = false;
       		    	    	boolean optionfield = false;
       		    	    	boolean optionlongfield = false;
       		    	    	String optionlongfieldid = "";
       		    	    	String optionlongfieldstr = "";
       		    	        while (true) {
       		    	            String line = br.readLine();
       		    	            if (line == null) {
       		    	                break;
       		    	            }
       		    	            //Input
       		    	            if(line.equals("'")) {
       		    	            	inputfield = false;
       		    	            }
       		    	            if(inputfield) {
       		    	            	try {
       		    	            		String[] arraystr = line.split(":",-1);
       		    	            		InputItem tempItem = new InputItem();
       		    	            		tempItem.id = arraystr[0];
       		    	            		tempItem.num = arraystr[1];
       		    	            		tempItem.filetype = arraystr[3];
       		    	            		tempItem.desc = arraystr[2];
       		    	            		listInputItems.add(tempItem);
       		    	            		//System.out.println("in:"+arraystr[0]);
       		    	            	}catch(Exception e2) {

       		    	            	}
       		    	            }
       		    	            if(line.equals("inputdef='")) {
       		    	            	inputfield=true;
       		    	            }

       		    	            //Option
       		    	            if(line.equals("'")) {
       		    	            	optionfield = false;
       		    	            }
       		    	            if(optionfield) {
       		    	            	try {
       		    	            		String[] arraystr = line.split(":",-1);
       		    	            		OptionItem tempItem = new OptionItem();
       		    	            		tempItem.id = arraystr[0];
       		    	            		tempItem.defaultopt = arraystr[2];
       		    	            		tempItem.desc = arraystr[1];
       		    	            		listOptionItems.add(tempItem);
       		    	            		//System.out.println("op:"+arraystr[0]);
       		    	            	}catch(Exception e2) {

       		    	            	}
       		    	            }
       		    	            if(line.equals("optiondef='")) {
       		    	            	optionfield=true;
       		    	            }

       		    	            if(line.equals("#</option detail>")) {
       		    	            	optionlongfield = false;
       		    	            }
       		    	            if(optionlongfield) {
       		    	            	try {
       		    	            		if(line.startsWith("#</") && line.endsWith(">")) {
       		    	            			for(OptionItem item: listOptionItems) {
       		    	            				if(item.id.equals(optionlongfieldid)) {
       		    	            					item.longdesc=optionlongfieldstr;
       		    	            				}
       		    	            			}
       		    	            			optionlongfieldid = "";
       		    	            			optionlongfieldstr = "";
       		    	            		}
       		    	            		else if(line.startsWith("#<") && line.endsWith(">")) {
       		    	            			optionlongfieldid = line.substring(2, line.length()-1);
       		    	            			//System.out.println(optionlongfieldid);
       		    	            		}else {
       		    	            			if(optionlongfieldstr.equals("")) {
       		    	            				optionlongfieldstr=line;
       		    	            			}else {
       		    	            				optionlongfieldstr=optionlongfieldstr+"\n"+line;
       		    	            			}
       		    	            		}
       		    	            	}catch(Exception e2) {

       		    	            	}
       		    	            }
       		    	            if(line.equals("#<option detail>")) {
       		    	            	optionlongfield=true;
       		    	            }

       		    	            //cmd
       		    	            if(line.startsWith("runcmd=\"$0")) {
       		    	            	cmd = line.substring(11,line.length()-1);
       		    	            }


       		    	            //System.out.println(line.toUpperCase());
       		    	        }
       		    	    } finally {
       		    	        br.close();
       		    	    }
       		    	} catch (IOException e) {
       		    	    e.printStackTrace();
       		    	}

       		    	analysisGrid.getChildren().clear();
//       		    	analysisGrid.setStyle("-fx-border-style: solid inside;"+
//                            "-fx-border-width: 2;" +
//                            "-fx-border-insets: 5;" +
//                            "-fx-border-radius: 5;" +
//                            "-fx-border-color: blue;");
       		    	//Input分だけボックスを作る
       		    	int num_item = 0;
       		    	for(InputItem item : listInputItems) {
       		    		num_item++;
           		    	Button b = new Button();
           		    	b.setText(item.id);
           		    	b.setId(item.id);
           		    	String tempprefix;
   						if(item.num.contains("directory")) {
   							tempprefix="txt.input.m.";
   						}else {
   							tempprefix="txt.input.s.";
   						}
   						if(item.num.contains("option")) {
   							tempprefix+="o.";
   						}else {
   							tempprefix+="r.";
   						}
		    	    	String finaltempprefix = tempprefix;
           		    	b.setOnAction(
           		    		(ActionEvent event)->{
           		    			String bid = ((Button)event.getSource()).getId();
           		    	    	FileChooser fileChooser = new FileChooser();
           		    	    	fileChooser.setInitialDirectory(new File( savedOpenFolder));
           		    	    	List<String> filetypeList = Arrays.asList(item.filetype.split(","));
           		    	    	ExtensionFilter extfilterExtensionFilter = new ExtensionFilter(item.desc, filetypeList);
           		    	    	fileChooser.getExtensionFilters().add(extfilterExtensionFilter);
           		    	    	List<File> f2;
           		    	    	if(item.num.contains("directory")) {
           		    	    		f2 = fileChooser.showOpenMultipleDialog(((Button)event.getSource()).getScene().getWindow());
           		    	    	}else {
           		    	    		f2 = new ArrayList<File>();
           		    	    		f2.add(fileChooser.showOpenDialog(((Button)event.getSource()).getScene().getWindow()));
           		    	    	}
           		    	    	if(f2!=null) {
           		    	    		StringBuilder val = new StringBuilder();
           		    	    		f2.forEach(s -> {
           		    	    			if(s!=null) {
           		    	    				val.append(",\""+s+"\"");
           		    	    				savedOpenFolder = s.getParent();
           		    	    			}
           		    	    		});
           		    	    		try {
           		    	    			for(Node tempnode : analysisGrid.getChildren()) {
       		    	    					//System.out.println("test2:"+tempnode.getId());
       		    	    					try {
       		    	    						if(tempnode.getId().equals(finaltempprefix+bid)) {
       		    	    							//System.out.println("test:"+bid);
       		    	    							((TextField) tempnode).setText(new String(val).substring(1));
       		    	    						}
       		    	    					}catch(Exception e2) {

       		    	    					}
           		    	    			}
           		    	    		}catch(Exception e) {

           		    	    		}
           		    			}
           		    	    	System.out.println(((Button)event.getSource()).getId());
           		    		}
           		    	);
           		    	//StackPane cellPane = new StackPane();
           		    	//cellPane.setStyle("border-bottom-color: blue;");
           		    	//cellPane.getChildren().add(b);
           		    	analysisGrid.add(b, 1, num_item-1);
           		    	Label tempLabel = new Label(item.desc);
           		    	Tooltip tooltip = new Tooltip();
           		    	tooltip.setText(item.desc);
       		    		tempLabel.setTooltip(tooltip);
           		    	analysisGrid.add(tempLabel, 2, num_item-1);
           		    	TextField t = new TextField();
           		    	t.setId(finaltempprefix+item.id);
           		    	analysisGrid.add(t, 3, num_item-1);
       		    	}

       		    	int num_opt = 0;
       		    	//Option分だけテキストボックスを作る
       		    	for(OptionItem item : listOptionItems) {
       		    		num_opt++;
           		    	Tooltip tooltip = new Tooltip();
           		    	Tooltip shorttooltip = new Tooltip();
           		    	tooltip.setText(item.longdesc);
           		    	shorttooltip.setText(item.desc);
       		    		Label tempLabel = new Label(item.desc);
       		    		tempLabel.setId("txt.optdesc."+item.id);
       		    		tempLabel.setTooltip(shorttooltip);
           		    	analysisGrid.add(tempLabel, 1, num_item+ num_opt-1);
           		    	//TextArea tempArea = new TextArea(item.longdesc);
           		    	//tempArea.setEditable(false);
           		    	//tempArea.setTooltip(tooltip);
           		    	//analysisGrid.add(tempArea, 2, num_item+ num_opt-1);
           		    	TextField t = new TextField(item.defaultopt);
           		    	t.setId("txt.opt."+item.id);
           		    	if(item.longdesc.compareTo("")!=0) {
           		    		t.setTooltip(tooltip);
           		    	}
           		    	analysisGrid.add(t, 2, num_item+ num_opt-1, 2,1);
           		    	if(item.longdesc.compareTo("")!=0) {
           		    		Button b1 = new Button();
           		    		b1.setText("?");
           		    		b1.setOnAction(
           		    				(ActionEvent event)->{

           		    					// 新しいウインドウを生成
           		    					Stage newStage = new Stage();
           		    					// モーダルウインドウに設定
           		    					//newStage.initModality(Modality.APPLICATION_MODAL);
           		    					// オーナーを設定
           		    					newStage.initOwner(bp.getScene().getWindow());
           		    					newStage.setTitle(item.desc);

           		    					// 新しいウインドウ内に配置するコンテンツを生成
           		    					TextArea helpTextArea = new TextArea(item.longdesc);

           		    					newStage.setScene(new Scene(helpTextArea));

           		    					// 新しいウインドウを表示
           		    					newStage.show();
           		    				}
           		    				);
           		    		analysisGrid.add(b1, 4, num_item+ num_opt-1);
           		    	}
       		    	}
       		    }
       		}
        );

        //timer
        Timeline timer = new Timeline(new KeyFrame(Duration.millis(30*1000), new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event) {
        		if(!isSending) {
        			System.out.println("check the job status: "+new Date().toString());

        			for(JobNode tempJobNode : jobNodes) {
        				//System.out.println(tempJobNode.id);
        				if(tempJobNode.status.equals("running")) {
        					ObjectMapper mapper = new ObjectMapper();
        					JsonNode node;
        					try {
        						node = mapper.readTree(new File(savedOutputFolder+"/"+tempJobNode.id+"/"+"settings.json"));

        						if(!node.get("privatekey").asText().equals("")) {
        							FileWriter file = new FileWriter(savedOutputFolder+"/"+tempJobNode.id+"/"+"id_rsa");
        							PrintWriter pw = new PrintWriter(new BufferedWriter(file));
        							pw.write(node.get("privatekey").asText());
        							pw.close();
        						}else {
        							File tempfile = new File(savedOutputFolder+"/"+tempJobNode.id+"/"+"id_rsa");
        							if(tempfile.exists()) {
        								tempfile.delete();
        							}
        						}

        					} catch (Exception e) {
        						e.printStackTrace();
        						tempJobNode.status="aborted";
        						saveJobList();
        						continue;
        					}


        					try {
        						//        						JSch jsch = new JSch();
        						//
        						//        						if((new File(savedOutputFolder+"/"+tempJobNode.id+"/"+"id_rsa")).exists()) {
        						//        							jsch.addIdentity(savedOutputFolder+"/"+tempJobNode.id+"/"+"id_rsa");
        						//        						}
        						//        						//System.out.println("identity added ");
        						//        						session = jsch.getSession(node.get("user").asText(), node.get("hostname").asText(), Integer.valueOf(node.get("port").asText()));
        						//        						//System.out.println("session created.");
        						//
        						//        						session.setConfig("StrictHostKeyChecking", "no");
        						//        						session.setPassword(node.get("password").asText());
        						//
        						//        						session.connect();
        						//        						//System.out.println("session connected.....");
        						//
        						//        						channel = session.openChannel("sftp");
        						//        						channel.setInputStream(System.in);
        						//        						channel.setOutputStream(System.out);
        						//        						channel.connect();
        						//        						System.out.println("sftp channel connected....");
        						//
        						//        						ChannelSftp channelSftp = (ChannelSftp) channel;

        						//download results
        						String workid = tempJobNode.id;
        						String workdir = node.get("workfolder").asText()+"/"+workid;
        						File saveFolder = new File(node.get("outputfolder").asText()+"/"+workid);

        						ChannelSftp channelSftp =null;

        						if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")) {
        							channelSftp = ConnectSsh.getSftpChannel(node, savedOutputFolder+"/"+tempJobNode.id);

        							try {
        								channelSftp.ls(workdir);
        							}catch (Exception e) {
        								System.err.println("no folder: "+workdir);
        								tempJobNode.status="aborted";
        								saveJobList();
        								continue;
        							}
        						}
        						try {
        							if(!(new File(saveFolder.toString()+"/"+"results")).exists()) {
        								(new File(saveFolder.toString()+"/"+"results")).mkdirs();
        							}

        							if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")) {
        								channelSftp.get(workdir+"/log.txt", saveFolder.toString()+"/"+"results");
        							}

        							if( joblist.getSelectionModel().getSelectedItem().id.equals(workid) ) {

        								listRecords.clear();
        								try {
        									List<String> lines = Files.readAllLines(Paths.get(saveFolder.toString()+"/results/log.txt"), StandardCharsets.UTF_8);
        									List<String> templines = new ArrayList<String>();
        									for(String string : lines) {
        										if(templines.size()>=1000) {
        											templines.remove(0);
        										}
        										templines.add(string);
        									}
        				        	    	for(String string : templines) {
        										//System.out.println(string);

        										Platform.runLater( () -> listRecords.add(string) );
        										Platform.runLater( () -> joblog.scrollTo(listRecords.size()-1)  );
        									}
        								} catch (IOException e) {
        									e.printStackTrace();
        								}
        							}
        							//recursiveFolderDownload(workdir+"/log.txt", saveFolder.toString()+"/"+"results", channelSftp);
        						}catch (Exception e2) {
        							System.err.println("no log.txt in "+workdir);
        						}
        						try {
        							if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")) {
        								try {
        									channelSftp.get(workdir+"/fin_status", saveFolder.toString()+"/"+"results");
        									//recursiveFolderDownload(workdir+"/fin_status", saveFolder.toString()+"/"+"results", channelSftp);
        								}catch (Exception e) {
        									System.out.println("job: "+workdir+" is running");
        								}
        							}

        							if(new File(saveFolder.toString()+"/"+"results"+"/"+"fin_status").exists()) {
        								System.out.println("job: "+tempJobNode.id+" was finished.");
        								if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")) {
        									recursiveFolderDownload(workdir, saveFolder.toString()+"/"+"results", channelSftp);
        									lsFolderRemove(workdir, channelSftp);
        								}

        								String exitSatusString = "aborted";
        								try {
        									List<String> exitStatuStrings = Files.readAllLines(Paths.get(saveFolder.toString()+"/"+"results"+"/"+"fin_status"), StandardCharsets.UTF_8);
        									if(exitStatuStrings.get(0).equals("0")) {
        										exitSatusString="finished";
        									}
        								} catch (IOException e) {
        									e.printStackTrace();
        								}
        								String tempString = new String(exitSatusString);
        								Platform.runLater( () -> jobNodes.set( jobNodes.indexOf(tempJobNode),new JobNode(tempJobNode.id, tempString, tempJobNode.desc) ) );
        								Platform.runLater( () -> saveJobList() );
        							}
        						}catch (Exception e2) {
        							System.out.println("download error: "+workdir);
        						}

        						if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")) {
        							channelSftp.exit();
        						}
        					} catch (Exception e) {
        						// TODO 自動生成された catch ブロック
        						e.printStackTrace();
        					}
        				}
        			}
        		}
        	}
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        preset.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
        	@Override
        	public void changed(ObservableValue<? extends Toggle> ov, Toggle oldToggle, Toggle newToggle) {
        		//System.out.println(((RadioButton) newToggle).getText());
        		String mode = ((RadioButton) newToggle).getText();

        		Map<String, String> tempSettingtMap = new LinkedHashMap<String,  String>();
                tempSettingtMap.put("hostname",hostname.getText());
                tempSettingtMap.put("port",port.getText());
                tempSettingtMap.put("user",user.getText());
                tempSettingtMap.put("password",password.getText());
                tempSettingtMap.put("privatekey",privatekey.getText());
                tempSettingtMap.put("workfolder",workfolder.getText());
                tempSettingtMap.put("imagefolder",imagefolder.getText());
                tempSettingtMap.put("changed","T");
                settings.put(((RadioButton) oldToggle).getText(), tempSettingtMap);

        		hostname.setDisable(false);
        		port.setDisable(false);
				password.setDisable(false);
				privatekey.setDisable(false);
				user.setDisable(false);
				password.setDisable(false);
				imagefolder.setDisable(false);
				workfolder.setDisable(false);

				if(settings.get(mode).get("changed").compareTo("T")==0) {
					hostname.setText(settings.get(mode).get("hostname"));
					port.setText(settings.get(mode).get("port"));
					user.setText(settings.get(mode).get("user"));
					password.setText(settings.get(mode).get("password"));
					privatekey.setText(settings.get(mode).get("privatekey"));
					workfolder.setText(settings.get(mode).get("workfolder"));
					imagefolder.setText(settings.get(mode).get("imagefolder"));
					switch(mode) {
					case "direct":
						break;
					case "direct (SGE)":
						break;
					case "ddbj":
						password.setDisable(true);
						password.setText("");
						break;
					case "shirokane":
						password.setDisable(true);
						password.setText("");
						break;
					case "WSL":
						hostname.setDisable(true);
						hostname.setText("");
						port.setDisable(true);
						port.setText("");
						privatekey.setDisable(true);
						privatekey.setText("");
						imagefolder.setDisable(true);
						imagefolder.setText("");
						workfolder.setDisable(true);
						workfolder.setText("");
						break;
					case "Mac":
						hostname.setDisable(true);
						hostname.setText("");
						port.setDisable(true);
						port.setText("");
						privatekey.setDisable(true);
						privatekey.setText("");
						user.setDisable(true);
						user.setText("");
						password.setDisable(true);
						password.setText("");
						imagefolder.setDisable(true);
						imagefolder.setText("");
						workfolder.setDisable(true);
						workfolder.setText("");
						break;
					default:
						System.out.println("no preset value");
					}

				}else {
					switch(mode) {
					case "direct":
						hostname.setText("");
						port.setText("22");
						workfolder.setText("work");
						imagefolder.setText("~/img");
						break;
					case "direct (SGE)":
						hostname.setText("");
						port.setText("22");
						workfolder.setText("work");
						imagefolder.setText("~/img");
						break;
					case "ddbj":
						hostname.setText("gw.ddbj.nig.ac.jp");
						port.setText("22");
						password.setDisable(true);
						password.setText("");
						workfolder.setText("work");
						imagefolder.setText("~/img");
						break;
					case "shirokane":
						hostname.setText("slogin.hgc.jp");
						port.setText("22");
						password.setDisable(true);
						password.setText("");
						workfolder.setText("work");
						imagefolder.setText("~/img");
						break;
					case "WSL":
						hostname.setDisable(true);
						hostname.setText("");
						port.setDisable(true);
						port.setText("");
						privatekey.setDisable(true);
						privatekey.setText("");
						imagefolder.setDisable(true);
						imagefolder.setText("");
						workfolder.setDisable(true);
						workfolder.setText("");
						break;
					case "Mac":
						hostname.setDisable(true);
						hostname.setText("");
						port.setDisable(true);
						port.setText("");
						privatekey.setDisable(true);
						privatekey.setText("");
						user.setDisable(true);
						user.setText("");
						password.setDisable(true);
						password.setText("");
						imagefolder.setDisable(true);
						imagefolder.setText("");
						workfolder.setDisable(true);
						workfolder.setText("");
						break;
					default:
						System.out.println("no preset value");
					}
				}
        	}
		});
    }


    static void saveJobList() {
        ArrayNode jobArrayNode = new ObjectMapper().createArrayNode();
        for(JobNode jobNode: jobNodes) {
        	ObjectNode feature = new ObjectMapper().createObjectNode();
        	feature.put("id", jobNode.id);
        	feature.put("status", jobNode.status);
        	feature.put("desc", jobNode.desc);
        	jobArrayNode.add(feature);
        }
    	try {
			//System.out.println(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(jobArrayNode));
        	//FileWriter file = new FileWriter("jobs.json");
        	//PrintWriter pw = new PrintWriter(new BufferedWriter(file));
        	PrintWriter pw = new PrintWriter(new BufferedWriter
                    (new OutputStreamWriter(new FileOutputStream("jobs.json"),"UTF-8")));

        	pw.write(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(jobArrayNode));
        	pw.close();
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
    }

    public static String readAll(final String path) throws IOException {
        return Files.lines(Paths.get(path), Charset.forName("UTF-8"))
            .collect(Collectors.joining("\n"));
    }

    void searchScript(String scriptFolderPath, String scriptPathString, List<String> scriptcontList ) throws IOException {

        String scriptcontString = readAll(scriptFolderPath+"/"+scriptPathString);
        scriptcontString = scriptcontString.replaceAll("[ \t']", "\n");
        String[] scriptcontStrings = scriptcontString.split("\n");
        for(String scripti: scriptcontStrings) {
        	if(scripti.startsWith("\"$scriptdir\"/")) {
        		if(!scriptcontList.contains(scripti.substring(13))) {
        			scriptcontList.add(scripti.substring(13));
        			searchScript(scriptFolderPath, scripti.substring(13), scriptcontList);
        		}
        	}
        }

    }

    void mkSymLinkOrCopy(String target, String link) {
    	if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac")) {
    		try {
    			Files.createSymbolicLink(Paths.get(link+"/"+Paths.get(target).getFileName()), Paths.get(target).toAbsolutePath());
    		} catch (IOException e) {
    			System.err.println(e.getMessage());
    			try {
    				Files.copy(Paths.get(target), Paths.get(link+"/"+Paths.get(target).getFileName()));
    				new File(link+"/"+Paths.get(target).getFileName()).setLastModified(Calendar.getInstance().getTimeInMillis());
    				//Files.copy(Paths.get(target), Paths.get(link));
    			} catch (IOException e1) {
    				// TODO 自動生成された catch ブロック
    				e1.printStackTrace();
    			}
    		}
    	}else {
    		try {
    			Files.copy(Paths.get(target), Paths.get(link+"/"+Paths.get(target).getFileName()));
    		} catch (IOException e1) {
    			// TODO 自動生成された catch ブロック
    			e1.printStackTrace();
    		}

        }
    }

    /**
     * This method is called recursively to download the folder content from SFTP server
     *
     * @param sourcePath
     * @param destinationPath
     * @throws SftpException
     */
    @SuppressWarnings("unchecked")
    public static void recursiveFolderDownload(String sourcePath, String destinationPath, ChannelSftp channelSftp) throws SftpException {
        Vector<ChannelSftp.LsEntry> fileAndFolderList = channelSftp.ls(sourcePath); // Let list of folder content

        if(!(new File(destinationPath)).exists()) {
        	(new File(destinationPath)).mkdirs();
        }

        //Iterate through list of folder content
        for (ChannelSftp.LsEntry item : fileAndFolderList) {

            if (!item.getAttrs().isDir()) { // Check if it is a file (not a directory).
            	System.out.println("download information... "+sourcePath + "/" + item.getFilename());
            	//System.out.println("sftp size:  "+item.getAttrs().getSize());
            	//System.out.println("local size: "+new File(destinationPath + "/" + item.getFilename()).length());
                if (!(new File(destinationPath + "/" + item.getFilename())).exists()
                        || (item.getAttrs().getMTime() > Long.valueOf(new File(destinationPath + "/" + item.getFilename()).lastModified() / (long) 1000).intValue())
                        || (item.getAttrs().getSize() > new File(destinationPath + "/" + item.getFilename()).length()) ) { // Download only if changed later.

                	System.out.println("downloading... "+sourcePath + "/" + item.getFilename());
                    channelSftp.get(sourcePath + "/" + item.getFilename(),
                            destinationPath + "/" + item.getFilename()); // Download file from source (source filename, destination filename).
                }
            } else if (!(".".equals(item.getFilename()) || "..".equals(item.getFilename()))) {
                new File(destinationPath + "/" + item.getFilename()).mkdirs(); // Empty folder copy.
                recursiveFolderDownload(sourcePath + "/" + item.getFilename(),
                        destinationPath + "/" + item.getFilename(), channelSftp); // Enter found folder on server to read its contents and create locally.
            }
        }
    }
    public static void lsFolderRemove(String dir, ChannelSftp channelSftp) {
    	try {
    		//System.out.println(dir);
    		//channelSftp.cd(dir);
    	    Vector<ChannelSftp.LsEntry> list = channelSftp.ls(dir); // List source directory structure.
    	    for (ChannelSftp.LsEntry oListItem : list) { // Iterate objects in the list to get file/folder names.
    	        if (!oListItem.getAttrs().isDir()) { // If it is a file (not a directory).
    	        	channelSftp.rm(dir + "/" + oListItem.getFilename()); // Remove file.
    	        	System.out.println(dir + "/" + oListItem.getFilename()+" was removed");
    	        } else if (!(".".equals(oListItem.getFilename()) || "..".equals(oListItem.getFilename()))) { // If it is a subdir.
    	            try {
    	            	channelSftp.rmdir(dir + "/" + oListItem.getFilename());  // Try removing subdir.
        	        	System.out.println(dir + "/" + oListItem.getFilename()+" was removed");
    	            } catch (Exception e) { // If subdir is not empty and error occurs.
    	                lsFolderRemove(dir + "/" + oListItem.getFilename(), channelSftp); // Do lsFolderRemove on this subdir to enter it and clear its contents.
    	            }
    	        }
    	    }

    	    channelSftp.rmdir(dir); // Finally remove the required dir.
        	System.out.println(dir+" was removed");
    	} catch (SftpException sftpException) {
    	    System.out.println("Removing " + dir + " failed. It may be already deleted.");
    	}
    }
}
