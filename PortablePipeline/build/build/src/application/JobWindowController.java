package application;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;

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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import javafx.util.Duration;

public class JobWindowController {

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
    private ListView<String> scriptlist;

    @FXML
    private GridPane analysisGrid;

    @FXML
    private Button buttonRun;

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

    String file_id_rsa = "id_rsa.txt";

    ObservableList<String> listRecords = FXCollections.observableArrayList();
    //ObservableList<String> listRecordsJob = FXCollections.observableArrayList();
    static ObservableList<JobNode> jobNodes = FXCollections.observableArrayList();
    ObservableList<String> listScripts = FXCollections.observableArrayList();

    //static ChannelSftp channelSftp = null;
    static Session session = null;
    static Channel channel = null;

    ArrayNode arrayNode;

    String selectedScript = null;
    String cmd = null;
    String savedOutputFolder = null;
    String selectedPreset = null;
    String savedOpenFolder = ".";

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
    	tabPane.getSelectionModel().select(tabJobList);

        Task<Boolean> task   = new Task<Boolean>()
        {
            @Override
            protected Boolean call() throws Exception
            {
                try {

                    String jobdesc = selectedScript;
                	ObjectNode feature = new ObjectMapper().createObjectNode();
                	String tempid = jobNodes.get(jobNodes.size()-1).id;
                	//String tempid = arrayNode.get(arrayNode.size()-1).get("id").toString().replace("\"", "");
                	System.out.println(tempid);
                	int workid = Integer.valueOf(tempid)+1;
                	feature.put("id", String.valueOf(workid));
                	feature.put("status", "preparing");
                	arrayNode.add(feature);
                	JobNode newJobNode = new JobNode(String.valueOf(workid),"preparing");
                	Platform.runLater( () -> {
                		jobNodes.add(newJobNode);
                		joblist.scrollTo(newJobNode);
                		joblist.getSelectionModel().select(newJobNode);
                    	saveJobList();
                	});
                	//Platform.runLater( () -> listRecordsJob.add(String.valueOf(workid)) );

                	File saveFolder = new File(savedOutputFolder+"/"+workid);
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

                    ChannelSftp c = (ChannelSftp) channel;

                    //mkdir work
                    String workdir = ppSetting.get("workfolder")+"/"+workid;
                    String resultdir = ppSetting.get("outputfolder")+"/"+workid+"/results";
                    String tempworkpath = "";
                    for(String tempworkpathnode : workdir.split("/")) {
                    	if(tempworkpath.equals("")) {
                    		tempworkpath=tempworkpathnode;
                    	}else {
                        	tempworkpath = tempworkpath+"/"+tempworkpathnode;
                    	}
                    	try {
                    		c.mkdir(tempworkpath);
                    	}catch(Exception e2) {
                    		//e2.printStackTrace();
                    	}
                    }
                    //transfer the script file
                    c.put(ppSetting.get("scriptfolder")+"/"+selectedScript, workdir);
                    mkSymLinkOrCopy(ppSetting.get("scriptfolder")+"/"+selectedScript, resultdir);
                    c.put(ppSetting.get("scriptfolder")+"/common.sh", workdir);
                    mkSymLinkOrCopy(ppSetting.get("scriptfolder")+"/common.sh", resultdir);

                    //analysisGrid.getScene().getRoot().applyCss();
                    //System.out.println("aaaa: "+((Label) analysisGrid.lookup("#txt.opt.opt_1")).getText());
            		String runcmd = "";
            		String currentOptDesc = "";
            		String numCPU = "1";
            		String numMEM = "8";
                    for(Node i : analysisGrid.getChildren()) {
                    	System.out.println("childId: "+i.getId());
                    	if(i.getId()!=null && i.getId().startsWith("txt.optdesc.")) {
                    		currentOptDesc = ((Label)i).getText();
                    		System.out.println("cuD: "+currentOptDesc);
                    	}
                    	if(i.getId()!=null && i.getId().startsWith("txt.input.")) {
                    		String fileid = i.getId().substring(12);
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
                    		System.out.println(runcmd);
                    		//send files
                            for (String fileName: tempfiles1s) {
                            	if(!fileName.equals("")) {
                            		System.out.println("sending...  "+fileName);
                            		try {
                            			c.mkdir(workdir+"/"+fileid);
                            			new File(resultdir+"/"+fileid).mkdirs();
                            		}catch(Exception e2) {
                            			//e2.printStackTrace();
                            		}
                            		c.put(fileName, workdir+"/"+fileid);
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
                    		System.out.println(runcmd);
                    	}
                    }
                    System.out.println("done");

                    String cmdString = "";
                    try{
                    	File file = new File(ppSetting.get("outputfolder")+"/"+workid+"/results/"+"wrapper.sh");
                    	FileWriter filewriter = new FileWriter(file);

                    	if(selectedPreset.equals("direct")) {
                        	filewriter.write("#!/bin/bash\n");
                        	filewriter.write("export DIR_IMG="+ppSetting.get("imagefolder")+"\n");
                        	filewriter.write("nohup bash "+selectedScript+" "+runcmd+" > log.txt 2>&1 &\n");
                        	filewriter.write("echo $! > save_pid.txt\n");
                        	cmdString = "cd "+workdir+"; bash wrapper.sh";
                    	}else if(selectedPreset.equals("shirokane")) {
                        	filewriter.write("#!/bin/bash\n");
                        	filewriter.write("#$ -S /bin/bash\n");
                        	filewriter.write("#$ -cwd\n");
                        	filewriter.write("#$ -pe def_slot "+numCPU+"\n");
                        	filewriter.write("#$ -l mem_req="+ Double.valueOf(numMEM)/Double.valueOf(numCPU) +"G,s_vmem="+Double.valueOf(numMEM)/Double.valueOf(numCPU)+"G\n");
                        	filewriter.write("export DIR_IMG="+ppSetting.get("imagefolder")+"\n");
                        	filewriter.write("bash "+selectedScript+" "+runcmd+" > log.txt 2>&1\n");
                        	cmdString = "cd "+workdir+"; qsub wrapper.sh | grep submitted > save_jid.txt";
                    	}else {

                    	}

                    	filewriter.close();
                        c.put(ppSetting.get("outputfolder")+"/"+workid+"/results/"+"wrapper.sh", workdir);
                    }catch(IOException e){
                    	System.out.println(e);
                    	return false;
                    }

                    ChannelExec channelexec = (ChannelExec) session.openChannel("exec");
                    System.out.println(cmdString);
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


                    System.out.println(jobdesc);
                	JobNode runninngJobNode = new JobNode(String.valueOf(workid),"running",jobdesc);
             		Platform.runLater( () -> {
             			jobNodes.set( jobNodes.indexOf(newJobNode),runninngJobNode );
             			saveJobList();
             		});

                } catch (Exception e) {
                    System.err.println(e);
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

        String path = new File(".").getAbsoluteFile().getParent();
        System.out.println(path);
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
        hostname.setText(ppSetting.get("hostname"));
        port.setText(ppSetting.get("port"));
        user.setText(ppSetting.get("user"));
        password.setText(ppSetting.get("password"));
        privatekey.setText(ppSetting.get("privatekey"));
        workfolder.setText(ppSetting.get("workfolder"));
        outputfolder.setText(ppSetting.get("outputfolder"));
        scriptfolder.setText(ppSetting.get("scriptfolder"));
        imagefolder.setText(ppSetting.get("imagefolder"));
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

        joblog.setItems(listRecords);

        joblist.setCellFactory(new Callback<ListView<JobNode>, ListCell<JobNode>>(){
        	@Override
        	public ListCell<JobNode> call(ListView<JobNode> listView){
        		return new JobCell();
        	}
        });
        //ObservableList<JobNode> jobNodes = FXCollections.observableArrayList();
        ObjectMapper mapperJob = new ObjectMapper();
        try {
        	List<JobNode> jobs = mapperJob.readValue(new File("jobs.json"), new TypeReference<List<JobNode>>(){});

        	arrayNode = new ObjectMapper().createArrayNode();

            for(JobNode job : jobs) {
            	//listRecordsJob.add(job.id);

            	ObjectNode feature = new ObjectMapper().createObjectNode();
            	feature.put("id", job.id);
            	feature.put("status", job.status);
            	arrayNode.add(feature);

            	jobNodes.add(new JobNode(job.id, job.status, job.desc));
            }
            System.out.println(mapperJob.writeValueAsString(arrayNode));

        } catch (IOException e) {
            e.printStackTrace();
        }
        joblist.setItems(jobNodes);
        //joblist.setItems(listRecordsJob);
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
	        	    	for(String string : lines) {
	               	    	System.out.println(string);

	                 		Platform.runLater( () -> listRecords.add(string) );
	                    	Platform.runLater( () -> joblog.scrollTo(listRecords.size()-1)  );
	        	    	}
	        	    } catch (IOException e) {
	        	    	//e.printStackTrace();
           		   	}
           		}
            }
        );


        File[] scriptFiles = new File(scriptfolder.getText()).listFiles();
        for(File script : scriptFiles) {
        	listScripts.add(script.getName());
        }
        scriptlist.setItems(listScripts);
        scriptlist.getSelectionModel().selectedItemProperty().addListener(
        	new ChangeListener<Object>(){
       		    @Override
       		    public void changed(ObservableValue<?> observable, Object oldVal, Object newVal) {
    		        // 実行する処理
       		    	//System.out.println(newVal);
       		    	selectedScript = newVal.toString();
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
       		    	            		String[] arraystr = line.split(":");
       		    	            		InputItem tempItem = new InputItem();
       		    	            		tempItem.id = arraystr[0];
       		    	            		tempItem.num = arraystr[1];
       		    	            		tempItem.filetype = arraystr[3];
       		    	            		tempItem.desc = arraystr[2];
       		    	            		listInputItems.add(tempItem);
       		    	            		System.out.println("in:"+arraystr[0]);
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
       		    	            		String[] arraystr = line.split(":");
       		    	            		OptionItem tempItem = new OptionItem();
       		    	            		tempItem.id = arraystr[0];
       		    	            		tempItem.defaultopt = arraystr[2];
       		    	            		tempItem.desc = arraystr[1];
       		    	            		listOptionItems.add(tempItem);
       		    	            		System.out.println("op:"+arraystr[0]);
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
       		    	            		}
       		    	            		else if(line.startsWith("#<") && line.endsWith(">")) {
       		    	            			optionlongfieldid = line.substring(2, line.length()-1);
       		    	            			System.out.println(optionlongfieldid);
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
       		    	//Input分だけボックスを作る
       		    	int num_item = 0;
       		    	for(InputItem item : listInputItems) {
       		    		num_item++;
           		    	Button b = new Button();
           		    	b.setText(item.id);
           		    	b.setId(item.id);
           		    	String tempprefix;
   						if(item.num.equals("m")) {
   							tempprefix="txt.input.m.";
   						}else {
   							tempprefix="txt.input.s.";
   						}
           		    	b.setOnAction(
           		    		(ActionEvent event)->{
           		    			String bid = ((Button)event.getSource()).getId();
           		    	    	FileChooser fileChooser = new FileChooser();
           		    	    	fileChooser.setInitialDirectory(new File( savedOpenFolder));
           		    	    	List<String> filetypeList = Arrays.asList(item.filetype.split(","));
           		    	    	ExtensionFilter extfilterExtensionFilter = new ExtensionFilter(item.desc, filetypeList);
           		    	    	fileChooser.getExtensionFilters().add(extfilterExtensionFilter);
           		    	    	List<File> f2;
           		    	    	if(item.num.equals("m")) {
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
       		    	    						if(tempnode.getId().equals(tempprefix+bid)) {
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
           		    	analysisGrid.add(b, 1, num_item-1);
           		    	Label tempLabel = new Label(item.desc);
           		    	analysisGrid.add(tempLabel, 2, num_item-1);
           		    	TextField t = new TextField();
           		    	t.setId(tempprefix+item.id);
           		    	analysisGrid.add(t, 3, num_item-1);
       		    	}

       		    	int num_opt = 0;
       		    	//Option分だけテキストボックスを作る
       		    	for(OptionItem item : listOptionItems) {
       		    		num_opt++;
       		    		Label tempLabel = new Label(item.desc);
       		    		tempLabel.setId("txt.optdesc."+item.id);
           		    	analysisGrid.add(tempLabel, 1, num_item+ num_opt-1);
           		    	TextArea tempArea = new TextArea(item.longdesc);
           		    	analysisGrid.add(tempArea, 2, num_item+ num_opt-1);
           		    	TextField t = new TextField(item.defaultopt);
           		    	t.setId("txt.opt."+item.id);
           		    	analysisGrid.add(t, 3, num_item+ num_opt-1);
       		    	}
       		    }
       		}
        );

        //timer
        Timeline timer = new Timeline(new KeyFrame(Duration.millis(30*1000), new EventHandler<ActionEvent>() {
        	@Override
        	public void handle(ActionEvent event) {
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
        						ChannelSftp channelSftp = ConnectSsh.getSftpChannel(node, savedOutputFolder+"/"+tempJobNode.id);

        	                    //download results
        						String workid = tempJobNode.id;
        	                    String workdir = node.get("workfolder").asText()+"/"+workid;
        						File saveFolder = new File(node.get("outputfolder").asText()+"/"+workid);

        						try {
        							channelSftp.ls(workdir);
        						}catch (Exception e) {
        							System.err.println("no folder: "+workdir);
        	                    	tempJobNode.status="aborted";
        	                    	saveJobList();
            			            continue;
								}
        						try {
        					        if(!(new File(saveFolder.toString()+"/"+"results")).exists()) {
        					        	(new File(saveFolder.toString()+"/"+"results")).mkdirs();
        					        }

        							channelSftp.get(workdir+"/log.txt", saveFolder.toString()+"/"+"results");

        							if( joblist.getSelectionModel().getSelectedItem().id.equals(workid) ) {

        			           	    	listRecords.clear();
        			           	    	try {
        									List<String> lines = Files.readAllLines(Paths.get(saveFolder.toString()+"/results/log.txt"), StandardCharsets.UTF_8);
        				        	    	for(String string : lines) {
        				               	    	System.out.println(string);

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
        							try {
            							channelSftp.get(workdir+"/fin_status", saveFolder.toString()+"/"+"results");
        								//recursiveFolderDownload(workdir+"/fin_status", saveFolder.toString()+"/"+"results", channelSftp);
        							}catch (Exception e) {
            							System.out.println("not found: "+workdir+"/fin_status");
									}

            	                    if(new File(saveFolder.toString()+"/"+"results"+"/"+"fin_status").exists()) {
            	                    	System.out.println("job: "+tempJobNode.id+" was finished.");
                	                    recursiveFolderDownload(workdir, saveFolder.toString()+"/"+"results", channelSftp);
                	                    lsFolderRemove(workdir, channelSftp);

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

        	                    channelSftp.exit();
        					} catch (Exception e) {
        						// TODO 自動生成された catch ブロック
        						e.printStackTrace();
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
        		System.out.println(((RadioButton) newToggle).getText());
        		String mode = ((RadioButton) newToggle).getText();
        		switch(mode) {
        			case "direct":
        				hostname.setText("");
        				port.setText("22");
        				password.setEditable(true);
        				break;
        			case "direct (SGE)":
        				hostname.setText("");
        				port.setText("22");
        				password.setEditable(true);
        				break;
        			case "ddbj":
        				hostname.setText("gw.ddbj.nig.ac.jp");
        				port.setText("22");
        				password.setEditable(false);
        				password.setText("");
        				break;
        			case "shirokane":
        				hostname.setText("slogin.hgc.jp");
        				port.setText("22");
        				password.setEditable(false);
        				password.setText("");
        				break;
        			default:
        				System.out.println("no preset value");
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
        	FileWriter file = new FileWriter("jobs.json");
        	PrintWriter pw = new PrintWriter(new BufferedWriter(file));
        	pw.write(new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(jobArrayNode));
        	pw.close();
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
    }

    void mkSymLinkOrCopy(String target, String link) {
    	try {
			Files.createSymbolicLink(Paths.get(link), Paths.get(target).toAbsolutePath());
		} catch (IOException e) {
			System.err.println(e.getMessage());
			try {
				Files.copy(Paths.get(target), Paths.get(link+"/"+Paths.get(target).getFileName()));
				//Files.copy(Paths.get(target), Paths.get(link));
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
                if (!(new File(destinationPath + "/" + item.getFilename())).exists()
                        || (item.getAttrs().getMTime() > Long
                                .valueOf(new File(destinationPath + "/" + item.getFilename()).lastModified()
                                        / (long) 1000)
                                .intValue())) { // Download only if changed later.

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
