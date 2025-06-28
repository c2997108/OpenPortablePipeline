package application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.HashMap; // Added import
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map; // Added import
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
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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
import javafx.scene.image.Image; // Added import
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.util.Callback;
import java.io.IOException;
import java.nio.file.Files;
// NOTE: Removed RichTextFX and related imports (Matcher, Pattern, Collection, Collections, WindowEvent, java.time.Duration)
// For example, java.util.regex.Pattern and Matcher might still be used elsewhere, so careful inspection is needed.
// Assuming they are not used by other parts of THIS class after this revert.
// If they are, they should NOT be removed from the import list by this operation.
// For this operation, we assume they are solely for the removed highlighting.
import java.nio.file.Paths;
import javafx.scene.control.Alert;


public class JobWindowController {

    public static JobWindowController instance;

    // Removed RichTextFX Patterns and Keyword arrays

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

    @FXML
    private CheckBox checkdelete;

    @FXML
    private Label label_privatekey;

    @FXML
    private Label label_workfolder;

    @FXML
    private Label label_imagefolder;

    String file_id_rsa = "id_rsa.txt";

    ObservableList<String> listRecords = FXCollections.observableArrayList();
    //ObservableList<String> listRecordsJob = FXCollections.observableArrayList();
    static ObservableList<JobNode> jobNodes = FXCollections.observableArrayList();
    ObservableList<String> listScripts = FXCollections.observableArrayList();
    ObservableList<ScriptNode> ScriptNodes = FXCollections.observableArrayList();
    ObservableList<ScriptNode> ScriptNodesOrig = FXCollections.observableArrayList();

    //static ChannelSftp channelSftp = null;
    //static Session session = null;
    //static Channel channel = null;

    ArrayNode arrayNode;

    String selectedScript = null;
    String cmd = null;
    String savedOutputFolder = null;
    String selectedPreset = null;
    String savedOpenFolder = ".";
    boolean isSending = false;

    Map<String, Map<String, String>> settings = new LinkedHashMap<String, Map<String, String>>();
    String[] settingPresetKey = {"ssh","ssh (SGE)","ddbj","shirokane","WSL","Mac","Linux","Linux (SGE)"};
    String[] settingItemKey = {"hostname", "port", "user", "password", "privatekey", "workfolder", "imagefolder"};

    String ppBinDir = System.getProperty("PP_BIN_DIR");
    String ppOutDir = System.getProperty("PP_OUT_DIR");
    
    @FXML
    @SuppressWarnings("deprecation")
    void onButtonSave(ActionEvent event) {
        String path = new File(".").getAbsoluteFile().getParent();
        System.out.println(path);

		try {
	        Writer out = new PrintWriter(PPSetting.getBaseDir()+"settings.json");

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
	        if(checkdelete.isSelected()==true) {
		        jsonGenerator.writeStringField("checkdelete", "true");jsonGenerator.writeRaw("\n");
	        }else {
		        jsonGenerator.writeStringField("checkdelete", "false");jsonGenerator.writeRaw("\n");
	        }
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
	        savedOutputFolder = PPSetting.getBaseDir() + new PPSetting().get("outputfolder");
	        selectedPreset = new PPSetting().get("preset");
		} catch (Exception e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
    }

    @FXML
    void onButtonRun(ActionEvent event) {

	String userdir = System.getProperty("user.dir");
	if((containsUnicode(userdir)||userdir.contains(" "))
			&&( (((RadioButton)preset.getSelectedToggle()).getText()).equals("WSL")||(((RadioButton)preset.getSelectedToggle()).getText()).equals("Mac")) ) {
		Alert dialogAlert = new Alert(AlertType.INFORMATION, "Don't put Portable Pipelines in a folder which contains Unicode or space characters.",
										ButtonType.YES);
		dialogAlert.showAndWait();
	}else {

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
					//jobリストの最大IDを取得
					String tempid;
					try {
						tempid = jobNodes.get(jobNodes.size()-1).id;
					}catch(Exception e) {
						tempid = "0";
					}
					//String tempid = arrayNode.get(arrayNode.size()-1).get("id").toString().replace("\"", "");
					System.out.println(tempid);

					//workフォルダの中の最大IDを取得
					int tempworkid=0;
					try {
						File[] listFile = new File(savedOutputFolder).listFiles();
						for(File file: listFile) {
							int tempworkid2 = Integer.valueOf(file.getName());
							if(tempworkid2>tempworkid) {
								tempworkid=tempworkid2;
							}
						}
					}catch(Exception e2) {
					}

					if(tempworkid>Integer.valueOf(tempid)) {
						tempid=String.valueOf(tempworkid);
					}

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
					jsonGenerator.writeStringField("checkdelete",ppSetting.get("checkdelete"));
					jsonGenerator.writeEndObject();
					jsonGenerator.flush();

////sshクライアントの詳細ログ表示
//    			        JSch.setLogger(new Logger() {
//    			            @Override
//    			            public boolean isEnabled(int level) {
//    			                return true; // 全レベル出力
//    			            }
//
//    			            @Override
//    			            public void log(int level, String message) {
//    			                System.out.println("JSchLog [" + levelToString(level) + "]: " + message);
//    			            }
//
//    			            private String levelToString(int level) {
//    			                switch (level) {
//    			                    case Logger.DEBUG: return "DEBUG";
//    			                    case Logger.INFO: return "INFO";
//    			                    case Logger.WARN: return "WARN";
//    			                    case Logger.ERROR: return "ERROR";
//    			                    case Logger.FATAL: return "FATAL";
//    			                    default: return "UNKNOWN";
//    			                }
//    			            }
//    			        });
					JSch jsch = new JSch();
					ChannelSftp csftp = null;
					Session jsesion = null;
					Channel channel = null;

					if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac") && !selectedPreset.equals("Linux") && !selectedPreset.equals("Linux (SGE)")) {
						if((new File(file_id_rsa)).exists()) {
							System.out.println(file_id_rsa);
							jsch.addIdentity(file_id_rsa, ppSetting.get("password"));
						}
						//System.out.println("identity added ");
						jsesion = jsch.getSession(ppSetting.get("user"), ppSetting.get("hostname"), Integer.valueOf(ppSetting.get("port")));
						//System.out.println("session created.");

						jsesion.setConfig("StrictHostKeyChecking", "no");
						jsesion.setPassword(ppSetting.get("password"));

						jsesion.connect();
						System.out.println("session connected.....");

						channel = jsesion.openChannel("sftp");
						channel.setInputStream(System.in);
						channel.setOutputStream(System.out);
						channel.connect();
						System.out.println("SFTP channel connected....");

						csftp = (ChannelSftp) channel;
					}
					//mkdir work
					String workdir = ppSetting.get("workfolder")+"/"+workid;
					String resultdir = PPSetting.getBaseDir() + ppSetting.get("outputfolder")+"/"+workid+"/results";

					if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac") && !selectedPreset.equals("Linux") && !selectedPreset.equals("Linux (SGE)")) {
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
								csftp.mkdir(tempworkpath);
							}catch(Exception e2) {
								//e2.printStackTrace();
							}
						}
					}
					//transfer the script file
					List<String> scriptcontList = new ArrayList<String>();
					searchScript(ppSetting.get("scriptfolder"), selectedScript, scriptcontList);

					if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac") && !selectedPreset.equals("Linux") && !selectedPreset.equals("Linux (SGE)")) {
						System.out.println("sending...  "+selectedScript);
						csftp.put(ppSetting.get("scriptfolder")+"/"+selectedScript, workdir);
						System.out.println("sending...  "+"common.sh");
						csftp.put(ppSetting.get("scriptfolder")+"/common.sh", workdir);
						System.out.println("sending...  "+"pp.py");
						csftp.put(ppSetting.get("scriptfolder")+"/pp.py", workdir);
						for(String scripti: scriptcontList) {
							System.out.println("sending...  "+scripti);
							csftp.put(ppSetting.get("scriptfolder")+"/"+scripti, workdir);
						}
					}

					String target;
					String link=resultdir;
					target=ppSetting.get("scriptfolder")+"/"+selectedScript;
					Files.copy(Paths.get(target), Paths.get(link+"/"+Paths.get(target).getFileName()));
					new File(link+"/"+Paths.get(target).getFileName()).setLastModified(Calendar.getInstance().getTimeInMillis());
					target=ppSetting.get("scriptfolder")+"/common.sh";
					Files.copy(Paths.get(target), Paths.get(link+"/"+Paths.get(target).getFileName()));
					new File(link+"/"+Paths.get(target).getFileName()).setLastModified(Calendar.getInstance().getTimeInMillis());
					target=ppSetting.get("scriptfolder")+"/pp.py";
					Files.copy(Paths.get(target), Paths.get(link+"/"+Paths.get(target).getFileName()));
					new File(link+"/"+Paths.get(target).getFileName()).setLastModified(Calendar.getInstance().getTimeInMillis());
					//シンボリックリンクよりもログのために実体をコピーしておく。
					//mkSymLinkOrCopy(ppSetting.get("scriptfolder")+"/"+selectedScript, resultdir);
					//mkSymLinkOrCopy(ppSetting.get("scriptfolder")+"/common.sh", resultdir);
					//mkSymLinkOrCopy(ppSetting.get("scriptfolder")+"/pp.py", resultdir);
					for(String scripti: scriptcontList) {
						//mkSymLinkOrCopy(ppSetting.get("scriptfolder")+"/"+scripti, resultdir);
						target=ppSetting.get("scriptfolder")+"/"+scripti;
					Files.copy(Paths.get(target), Paths.get(link+"/"+Paths.get(target).getFileName()));
					new File(link+"/"+Paths.get(target).getFileName()).setLastModified(Calendar.getInstance().getTimeInMillis());
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
									if(getStatusByWorkId(workid).equals("cancelled")) {
										throw new Exception("Cancelled");
									}
									if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac") && !selectedPreset.equals("Linux") && !selectedPreset.equals("Linux (SGE)")) {
										System.out.println("sending...  "+fileName);
										try {
											csftp.mkdir(workdir+"/"+fileid);
											new File(resultdir+"/"+fileid).mkdirs();
										}catch(Exception e2) {
											//e2.printStackTrace();
										}
										csftp.put(fileName, workdir+"/"+fileid);
									}else {
										System.out.println("creating hard link...  "+fileName);
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
						String outputFile = PPSetting.getBaseDir() + ppSetting.get("outputfolder")+"/"+workid+"/results/"+"wrapper.sh";

						String templateFile = "";
						if(selectedPreset.equals("ssh")) {
							templateFile=ppBinDir + "/templates/ssh-wrapper.sh";
							cmdString = "cd "+workdir+"; bash wrapper.sh";
						}else if(selectedPreset.equals("shirokane")) {
							templateFile=ppBinDir + "/templates/shirokane-wrapper.sh";
							//2>&1を>の前に書くことで、標準出力のみsave_jid.txtに保存し、標準エラー出力を標準出力として出力
							cmdString = "cd "+workdir+"; qsub -terse wrapper.sh 2>&1 > save_jid.txt";
						}else if(selectedPreset.equals("ddbj")) {
							templateFile=ppBinDir + "/templates/ddbj-wrapper.sh";
							cmdString = "cd "+workdir+"; sbatch --parsable wrapper.sh 2>&1 > save_jid.txt";
						}else if(selectedPreset.equals("ssh (SGE)")) {
							templateFile=ppBinDir + "/templates/sshsge-wrapper.sh";
							cmdString = "cd "+workdir+"; qsub -terse wrapper.sh 2>&1 > save_jid.txt";
						}else if(selectedPreset.equals("WSL")){
							String curDir = new File(PPSetting.getBaseDir() + ".").getAbsoluteFile().getParent();
							String wslcurDir = "/mnt/"+curDir.substring(0,1).toLowerCase()+curDir.substring(2);
							wslcurDir = wslcurDir.replaceAll("\\\\", "/");
							// ppBinDirはPP_BIN_DIRのシステムプロパティで書きかえないファイル
							// getBaseDir()は結局PP_OUT_DIRのシステムプロパティで書きかえるファイル
							try (BufferedReader reader = new BufferedReader(new FileReader(ppBinDir + "/templates/WSL-wrapper.bat"));
					             BufferedWriter writer = new BufferedWriter(new FileWriter(PPSetting.getBaseDir() + ppSetting.get("outputfolder")+"/"+workid+"/results/"+"wrapper.bat"))) {
								String line;
								while ((line = reader.readLine()) != null) {
									line = line.replace("@wslcurDir@", wslcurDir);
									line = line.replace("@password@", password.getText());
									writer.write(line+"\n");
								}
							}

							templateFile=ppBinDir + "/templates/WSL-wrapper.sh";
							cmdString = "cmd.exe /c start cmd.exe /k "+ ppBinDir.replaceAll("/", "\\\\")+"\\wrapper.bat";
						}else if(selectedPreset.equals("Mac")){
							templateFile=ppBinDir + "/templates/Mac-wrapper.sh";
							cmdString = "bash wrapper.sh";
						}else if(selectedPreset.equals("Linux")){
							templateFile=ppBinDir + "/templates/Linux-wrapper.sh";
							cmdString = "bash wrapper.sh";
						}else if(selectedPreset.equals("Linux (SGE)")) {
							templateFile=ppBinDir + "/templates/Linuxsge-wrapper.sh";
							cmdString = "bash wrapper.sh";
						}

							try (BufferedReader reader = new BufferedReader(new FileReader(templateFile));
					             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
								String line;
								while ((line = reader.readLine()) != null) {
									line = line.replace("@imagefolder@", ppSetting.get("imagefolder"));
									line = line.replace("@selectedScript@", selectedScript);
									line = line.replace("@runcmd@", runcmd);
									line = line.replace("@numCPU@", numCPU);
									line = line.replace("@numMEM_1core@", String.valueOf(Double.valueOf(numMEM)/Double.valueOf(numCPU)));
									line = line.replace("@numMEM_total@", numMEM);
									writer.write(line+"\n");
								}
							}


						if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac") && !selectedPreset.equals("Linux") && !selectedPreset.equals("Linux (SGE)")) {
							csftp.put(PPSetting.getBaseDir() + ppSetting.get("outputfolder")+"/"+workid+"/results/"+"wrapper.sh", workdir);
						}
					}catch(IOException e){
						//System.out.println(e);
						e.printStackTrace();
						return false;
					}

					System.out.println("CMD: "+cmdString);

					if(!selectedPreset.equals("WSL") && !selectedPreset.equals("Mac") && !selectedPreset.equals("Linux") && !selectedPreset.equals("Linux (SGE)")) {
						System.out.println("SFTP session closed.");
						jsesion.disconnect(); //ここでいったんsftp用のセッションは閉じる

						List<String> cmdResults;
						if(selectedPreset.equals("ddbj")) {
							cmdResults = ConnectSsh.getSshCmdResult2StepSession(ppSetting, file_id_rsa, cmdString);
						}else {
							cmdResults = ConnectSsh.getSshCmdResult(ppSetting, file_id_rsa, cmdString);
						}
						cmdResults.forEach(item -> listRecords.add(item));

					}else {
						Process process = Runtime.getRuntime().exec(cmdString, null, new File(PPSetting.getBaseDir() + ppSetting.get("outputfolder")+"/"+workid+"/results/"));
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
					//System.err.println(e);
					e.printStackTrace();
					int finalworkid = workid;
					JobNode finalJobNode = tempJobNode;
					JobNode runninngJobNode = new JobNode(String.valueOf(finalworkid),"aborted",e.toString());
					Platform.runLater( () -> {
						jobNodes.set( jobNodes.indexOf(finalJobNode),runninngJobNode );
						saveJobList();
					});
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

    }

    @FXML
    void initialize() {
        instance = this;
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
            //e.printStackTrace();
		try {
			//settings.jsonが無い場合に作成する。
			Writer out = new PrintWriter(PPSetting.getBaseDir()+"settings.json");
			JsonFactory jsonFactory = new JsonFactory();
			JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(out);
			jsonGenerator.writeStartObject();jsonGenerator.writeRaw("\n");

			final String OS_NAME = System.getProperty("os.name").toLowerCase();
			try {
				if(OS_NAME.startsWith("windows")) {
				jsonGenerator.writeStringField("user","your_wsl_user_name");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("password","your_wsl_password");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("preset","WSL");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("outputfolder","output");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("scriptfolder",ppBinDir + "/scripts");jsonGenerator.writeRaw("\n");
				}else if(OS_NAME.startsWith("mac")) {
					jsonGenerator.writeStringField("preset","Mac");jsonGenerator.writeRaw("\n");
					jsonGenerator.writeStringField("outputfolder","output");jsonGenerator.writeRaw("\n");
					jsonGenerator.writeStringField("scriptfolder",ppBinDir + "/scripts");jsonGenerator.writeRaw("\n");
				}else if(OS_NAME.startsWith("linux")) {
				jsonGenerator.writeStringField("imagefolder","$HOME/img");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("preset","Linux");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("outputfolder","output");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("scriptfolder",ppBinDir + "/scripts");jsonGenerator.writeRaw("\n");
				}else {
				jsonGenerator.writeStringField("hostname","m208.s");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("port","22");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("user","user2");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("password","user2");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("privatekey","");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("workfolder","work");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("preset","ssh");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("outputfolder","output");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("scriptfolder",ppBinDir + "/scripts");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("imagefolder","$HOME/img");jsonGenerator.writeRaw("\n");
				jsonGenerator.writeStringField("checkdelete", "true");jsonGenerator.writeRaw("\n");
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}

			jsonGenerator.writeEndObject();
			jsonGenerator.flush();

			ppSetting = new PPSetting();
		}catch(Exception e2) {
			e2.printStackTrace();
                return;
		}
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
		scriptfolder.setText(ppBinDir + "/scripts");
        }else {
            scriptfolder.setText(ppSetting.get("scriptfolder"));
        }
        if(ppSetting.get("imagefolder").equals("")) {
            imagefolder.setText("$HOME/img"); // Default image folder
        }else {
		imagefolder.setText(ppSetting.get("imagefolder"));
        }
        if(ppSetting.get("checkdelete").equals("true")) {
		checkdelete.setSelected(true);
        }else {
		checkdelete.setSelected(false);
        }
        String tempppSettingPreset = ppSetting.get("preset");
        preset.getToggles().forEach(
			s -> {
				//System.out.println(((RadioButton)s).getText());
				if(((RadioButton)s).getText().equals(tempppSettingPreset)){
					s.setSelected(true);
				};
			}
			);
        savedOutputFolder = PPSetting.getBaseDir() + ppSetting.get("outputfolder");
        selectedPreset = ppSetting.get("preset");

		switch(selectedPreset) {
		case "ssh":
			break;
		case "ssh (SGE)":
			break;
		case "ddbj":
			//password.setDisable(true);
			//password.setText("");
			break;
		case "shirokane":
			//password.setDisable(true);
			//password.setText("");
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
			checkdelete.setDisable(true);
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
			checkdelete.setDisable(true);
			break;
		case "Linux":
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
			workfolder.setDisable(true);
			workfolder.setText("");
			checkdelete.setDisable(true);
			break;
		case "Linux (SGE)":
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
			workfolder.setDisable(true);
			workfolder.setText("");
			checkdelete.setDisable(true);
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
        //jobファイルから読み込んで履歴を表示
	int numOfJob = 0;
        ObjectMapper mapperJob = new ObjectMapper();
        try {
		arrayNode = new ObjectMapper().createArrayNode();
		List<JobNode> jobs = mapperJob.readValue(new File(PPSetting.getBaseDir()+"jobs.json"), new TypeReference<List<JobNode>>(){});


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
			try {
			JobNode tempNode = (JobNode)newVal;
			System.out.println("Job ID: "+((JobNode)newVal).id +" is selected.");
			listRecords.clear();
			//System.out.println(new PPSetting().get("outputfolder")+"/"+tempNode.id+"/results/log.txt");
						List<String> lines = Files.readAllLines(Paths.get(PPSetting.getBaseDir() + new PPSetting().get("outputfolder")+"/"+tempNode.id+"/results/log.txt"), StandardCharsets.UTF_8);
			//System.out.println(lines.size());
						List<String> templines = new ArrayList<String>();
						for(String string : lines) {
				//System.out.println(string);
							if(templines.size()>=1000) {
								templines.remove(0);
							}
							if(!string.equals("")) {
								templines.add(string);
							}
						}
				for(String string : templines) {
				//System.out.println(string);

					Platform.runLater( () -> listRecords.add(string) );
				Platform.runLater( () -> joblog.scrollTo(listRecords.size()-1)  );
				}
			    } catch (Exception e) {
				//e.printStackTrace();
				}
			}
            }
        );

        //textボックスでEnterが押された場合
        searchtxt.setOnAction((ActionEvent event)->{
		//System.out.println(searchtxt.getText());
		Platform.runLater( ()->{
			ScriptNodes.clear();
		for(ScriptNode sNode : ScriptNodesOrig) {
			if(sNode.filename.toUpperCase().contains(searchtxt.getText().toUpperCase()) || sNode.explanation.toUpperCase().contains(searchtxt.getText().toUpperCase())){
				ScriptNodes.add(sNode);
			}
		}

		});
        });
        //searchボタンが押された場合
        searchbtn.setOnAction((ActionEvent event)->{
		//System.out.println(searchtxt.getText());
		Platform.runLater( ()->{
			ScriptNodes.clear();
		for(ScriptNode sNode : ScriptNodesOrig) {
			if(sNode.filename.toUpperCase().contains(searchtxt.getText().toUpperCase()) || sNode.explanation.toUpperCase().contains(searchtxt.getText().toUpperCase())){
				ScriptNodes.add(sNode);
			}
		}

		});
        });

        // Icon loading logic
        Map<String, Image> categoryIcons = new HashMap<>();
        String defaultIconPath = "file:"+ ppBinDir + "/image/pipe_icon.png";
        Image defaultIcon = null;
        try {
            defaultIcon = new Image(defaultIconPath);
        } catch (Exception e) {
            System.err.println("Failed to load default icon: " + defaultIconPath);
            // Optionally, load a fallback built-in icon or use null
        }

        File[] scriptFiles = new File(ppSetting.get("scriptfolder")).listFiles();
        if (scriptFiles != null) {
            java.util.Arrays.sort(scriptFiles, new java.util.Comparator<File>() {
			public int compare(File file1, File file2){
			    return file1.getName().toLowerCase().compareTo(file2.getName().toLowerCase());
			}
		});
            for(File scriptFile : scriptFiles) {
		if(!scriptFile.getName().equals("common.sh") && !scriptFile.getName().equals("pp.py") && !scriptFile.getName().equals("pp")) {
			String explanationString = "";
			boolean explanationfield = false;
			try {
				BufferedReader br = new BufferedReader(new FileReader(scriptFile));
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

                    Image scriptIcon = defaultIcon;
                    String fileName = scriptFile.getName();
                    String categoryName = "";
                    int tildeIndex = fileName.indexOf("~");
                    if (tildeIndex > 0) {
                        categoryName = fileName.substring(0, tildeIndex);
                    }

                    if (!categoryName.isEmpty()) {
                        if (categoryIcons.containsKey(categoryName)) {
                            scriptIcon = categoryIcons.get(categoryName);
                        } else {
                            String categoryIconPath = "file:"+ ppBinDir + "/image/" + categoryName + ".png";
                            try {
                                Image loadedIcon = new Image(categoryIconPath);
                                scriptIcon = loadedIcon;
                                categoryIcons.put(categoryName, loadedIcon);
                            } catch (Exception e) {
                                System.err.println("Failed to load category icon: " + categoryIconPath + ". Using default.");
                                categoryIcons.put(categoryName, defaultIcon); // Store default even if load fails, to avoid retrying
                            }
                        }
                    }
                    ScriptNodes.add(new ScriptNode(fileName, explanationString, scriptIcon));
                    ScriptNodesOrig.add(new ScriptNode(fileName, explanationString, scriptIcon)); // Assuming ScriptNodeOrig also needs icons
			listScripts.add(fileName);
		}
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
				String myinputlabel = item.desc;
				Button b = new Button();
				b.setText(item.id);
				b.setId(item.id);
				String tempprefix;
						if(item.num.contains("directory")) {
							tempprefix="txt.input.m.";
							myinputlabel+=" (multiple files)";
						}else {
							tempprefix="txt.input.s.";
						}
						if(item.num.contains("option")) {
							tempprefix+="o.";
						}else {
							tempprefix+="r.";
							myinputlabel="* "+myinputlabel;
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
				Label tempLabel = new Label(myinputlabel);
				Tooltip tooltip = new Tooltip();
				tooltip.setText(myinputlabel);
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
        Timeline timer = new Timeline(new KeyFrame(javafx.util.Duration.millis(30*1000), new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			if(!isSending) {
				System.out.println("check the job status: "+new Date().toString());

				for(JobNode tempJobNode : jobNodes) {
					//System.out.println(tempJobNode.id);
					String keyFilePath = savedOutputFolder+"/"+tempJobNode.id+"/"+"id_rsa";
					if(tempJobNode.status.equals("running")) {
						ObjectMapper mapper = new ObjectMapper();
						JsonNode node;
						try {
							//ジョブごとのサーバへの接続に使用した認証ファイルを読み込み
							node = mapper.readTree(new File(savedOutputFolder+"/"+tempJobNode.id+"/"+"settings.json"));

							if(!node.get("privatekey").asText().equals("")) {
								FileWriter file = new FileWriter(keyFilePath);
								PrintWriter pw = new PrintWriter(new BufferedWriter(file));
								pw.write(node.get("privatekey").asText());
								pw.close();
							}else {
								File tempfile = new File(keyFilePath);
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
							String workid = tempJobNode.id;
							String workdir = node.get("workfolder").asText()+"/"+workid;
							File saveFolder = new File(PPSetting.getBaseDir() + node.get("outputfolder").asText()+"/"+workid);

							ChannelSftp channelSftp =null;

							if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")
									&& !node.get("preset").asText().equals("Linux") && !node.get("preset").asText().equals("Linux (SGE)")) {
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

								if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")
										&& !node.get("preset").asText().equals("Linux") && !node.get("preset").asText().equals("Linux (SGE)")) {
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
										System.out.println("Maybe it is because the job is still in the process of being thrown.");
										return;
									}
								}
								//recursiveFolderDownload(workdir+"/log.txt", saveFolder.toString()+"/"+"results", channelSftp);
							}catch (Exception e2) {
								System.err.println("no log.txt in "+workdir);
								//まだlog.txtが出来ていない状態（＝まだサーバの待ち行列に入っていて実行されていない状態が多い）の時の待ち順確認
								try {
								if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")
										&& !node.get("preset").asText().equals("Linux") && !node.get("preset").asText().equals("Linux (SGE)")) {
										ChannelExec channelExec = ConnectSsh.getSshChannel(node, savedOutputFolder+"/"+tempJobNode.id);

										List<String> listStr;
										if(!node.get("preset").asText().equals("ddbj")) {
											listStr = ConnectSsh.getSshCmdResult(node, keyFilePath, "cd "+workdir+"; qstat -u '*'|awk '$5==\"qw\"'|awk -v id=`cat save_jid.txt` '$1==id{print NR}'");
										}else { //ddbjの時
											listStr = ConnectSsh.getSshCmdResult2StepSession(node, keyFilePath, "cd "+workdir+"; squeue --all -o '%.2t %.18i %.10M %Q' --sort=-p|awk -v id=`cat save_jid.txt` '$1==\"PD\"{n++; split($2,arr,\"_\"); if(arr[1]==id){m=n}} END{print m+0}'");
										}
										try {
											String data2 = "Your job will be the "+ listStr.get(0) +" th job to be executed in the entire grid engine.";
											System.out.println(data2);
											Platform.runLater( () -> listRecords.add(data2) );
										}catch(Exception e) {
											System.err.println(e);
										}
								}
								}catch(Exception e3) {
									System.err.println(e3);
								}
							}
							//結果ファイルの転送
							try {
								if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")
										&& !node.get("preset").asText().equals("Linux") && !node.get("preset").asText().equals("Linux (SGE)")) {
									try {
										channelSftp.get(workdir+"/fin_status", saveFolder.toString()+"/"+"results");
										//recursiveFolderDownload(workdir+"/fin_status", saveFolder.toString()+"/"+"results", channelSftp);
									}catch (Exception e) {
										System.out.println("#There is no fin_status file for job id "+tempJobNode.id+". It is still running.");
									}
								}

								if(new File(saveFolder.toString()+"/"+"results"+"/"+"fin_status").exists()) {
									System.out.println("job: "+tempJobNode.id+" was finished.");
									if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")
										&& !node.get("preset").asText().equals("Linux") && !node.get("preset").asText().equals("Linux (SGE)")) {
										recursiveFolderDownload(workdir, saveFolder.toString()+"/"+"results", channelSftp);
										//System.out.println("debug: "+node.get("checkdelete").asText());
										if(node.get("checkdelete").asText().equals("true")) {
										lsFolderRemove(workdir, channelSftp);
										}
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
								}else{
									//fin_statusがない場合でもsave_pid, save_jidのプロセスが起動しているか調べる
									System.out.println("#Check whether a server process is running. job: "+tempJobNode.id);
									int returnCode = 0;
									if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")
											&& !node.get("preset").asText().equals("Linux") && !node.get("preset").asText().equals("Linux (SGE)")) {
										//ChannelExec channelExec = ConnectSsh.getSshChannel(node, savedOutputFolder+"/"+tempJobNode.id);

										List<String> listStr;
										if(node.get("preset").asText().equals("ssh")) {
											listStr = ConnectSsh.getSshCmdResult(node, keyFilePath, "cd "+workdir+"; ps -p `cat save_pid.txt`|grep python|wc -l");
											//channelExec.setCommand("cd "+workdir+"; ps -p `cat save_pid.txt`|grep python|wc -l");
										}else {
										if(!node.get("preset").asText().equals("ddbj")) { //shirokane, ssh (SGE)の時
											listStr = ConnectSsh.getSshCmdResult(node, keyFilePath, "cd "+workdir+"; qstat -j `cat save_jid.txt`|awk '$1==\"error\"{system(\"qdel '`cat save_jid.txt`' >> log.txt 2>&1\"); e=1} END{if(e==1){print 0}else{print NR}}'");
												//channelExec.setCommand("cd "+workdir+"; qstat -j `cat save_jid.txt`|grep job_number|wc -l");
										}else { //ddbjの時
											listStr = ConnectSsh.getSshCmdResult2StepSession(node, keyFilePath, "cd "+workdir+"; if [ -s save_jid.txt ]; then squeue -j `cat save_jid.txt` 2> /dev/null|tail -n+2|wc -l; else echo The job was rejected. It is likely that the requested amount of resources was too large. >> log.txt; echo 0; fi");
												//channelExec.setCommand("ssh a001 \"cd "+workdir+"; squeue -j `cat save_jid.txt` 2> /dev/null|tail -n+2|wc -l\"");
										}
										}
										//jobRunningStatusが0以外になったら異常終了判断
										int jobRunningStatus = 0;
										try {
											//save_pid.txtのjidで調べた結果の行数が0ならば異常終了
											if(listStr.get(0).equals("0")) {
												jobRunningStatus = 1;
											}
										}catch(Exception e2) {
											e2.printStackTrace();
										}
										returnCode = jobRunningStatus;
										
									}else {
										try {
											if(node.get("preset").asText().equals("WSL")) {
												Process process = Runtime.getRuntime().exec("bash -c 'ps -p `cat save_pid.txt`'", null, new File(savedOutputFolder+"/"+tempJobNode.id+"/results/"));
											returnCode = process.waitFor();
											}else { //Mac & Linux & Linux (SGE)
												//「`cat ...`」などという書き方がexecの中で直接は使えないようなので、2回に分ける
												Process process = Runtime.getRuntime().exec("cat save_pid.txt", null, new File(savedOutputFolder+"/"+tempJobNode.id+"/results/"));
												InputStream inputStream = process.getInputStream();
												BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
												String line;
												//一行だけ読み取る
												line = reader.readLine();
												returnCode = process.waitFor(); //終了待ち

												//System.out.println("line: "+line);
												process = Runtime.getRuntime().exec("ps -p "+line, null, new File(savedOutputFolder+"/"+tempJobNode.id+"/results/"));
												returnCode = process.waitFor();
											}

										} catch (Exception e) {
											System.err.println(e);
										}
									}
									if(returnCode != 0) {
										System.out.println("The job was terminated abnormally.");
									if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")
											&& !node.get("preset").asText().equals("Linux") && !node.get("preset").asText().equals("Linux (SGE)")) {
										recursiveFolderDownload(workdir, saveFolder.toString()+"/"+"results", channelSftp);
										//System.out.println("debug: "+node.get("checkdelete").asText());
										if(node.get("checkdelete").asText().equals("true")) {
											lsFolderRemove(workdir, channelSftp);
										}
									}

									String exitSatusString = "aborted";
									String tempString = new String(exitSatusString);
									Platform.runLater( () -> jobNodes.set( jobNodes.indexOf(tempJobNode),new JobNode(tempJobNode.id, tempString, tempJobNode.desc) ) );
									Platform.runLater( () -> saveJobList() );
									}
								}

							}catch (Exception e2) {
								System.out.println("download error: "+workdir);
							}

							if(!node.get("preset").asText().equals("WSL") && !node.get("preset").asText().equals("Mac")
									&& !node.get("preset").asText().equals("Linux") && !node.get("preset").asText().equals("Linux (SGE)")) {
								channelSftp.exit();
								channelSftp.getSession().disconnect();
								//System.out.println("Close SFTP");
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

			//切り替える前に現在の設定を覚えさせる
			Map<String, String> tempSettingtMap = new LinkedHashMap<String,  String>();
                tempSettingtMap.put("hostname",hostname.getText());
                tempSettingtMap.put("port",port.getText());
                tempSettingtMap.put("user",user.getText());
                tempSettingtMap.put("password",password.getText());
                tempSettingtMap.put("privatekey",privatekey.getText());
                tempSettingtMap.put("workfolder",workfolder.getText());
                tempSettingtMap.put("imagefolder",imagefolder.getText());
                if(checkdelete.isSelected()==true) {
			tempSettingtMap.put("checkdelete", "true");
                }else {
			tempSettingtMap.put("checkdelete", "false");
                }
                tempSettingtMap.put("changed","T");
                settings.put(((RadioButton) oldToggle).getText(), tempSettingtMap);

                //切り替え準備
			hostname.setDisable(false);
			port.setDisable(false);
				password.setDisable(false);
				privatekey.setDisable(false);
				user.setDisable(false);
				password.setDisable(false);
				imagefolder.setDisable(false);
				workfolder.setDisable(false);
				checkdelete.setDisable(false);

				if(settings.get(mode).get("changed").compareTo("T")==0) { //過去にそのpresetを表示させた場合はその値を呼び出す
					hostname.setText(settings.get(mode).get("hostname"));
					port.setText(settings.get(mode).get("port"));
					user.setText(settings.get(mode).get("user"));
					password.setText(settings.get(mode).get("password"));
					privatekey.setText(settings.get(mode).get("privatekey"));
					workfolder.setText(settings.get(mode).get("workfolder"));
					imagefolder.setText(settings.get(mode).get("imagefolder"));
					if(settings.get(mode).get("checkdelete").compareTo("true")==0) {
						checkdelete.setSelected(true);
					}else {
						checkdelete.setSelected(false);
					}
					switch(mode) {
					case "ssh":
						break;
					case "ssh (SGE)":
						break;
					case "ddbj":
						//password.setDisable(true);
						//password.setText("");
						break;
					case "shirokane":
						//password.setDisable(true);
						//password.setText("");
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
						checkdelete.setDisable(true);
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
						checkdelete.setDisable(true);
						break;
					case "Linux":
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
						workfolder.setDisable(true);
						workfolder.setText("");
						checkdelete.setDisable(true);
						break;
					case "Linux (SGE)":
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
						workfolder.setDisable(true);
						workfolder.setText("");
						checkdelete.setDisable(true);
						break;
					default:
						System.out.println("no preset value");
					}

				}else {
					switch(mode) {
					case "ssh":
						if(settings.get("ssh (SGE)").get("changed").compareTo("T")==0) {
							hostname.setText(settings.get("ssh (SGE)").get("hostname"));
							port.setText(settings.get("ssh (SGE)").get("port"));
							user.setText(settings.get("ssh (SGE)").get("user"));
							password.setText(settings.get("ssh (SGE)").get("password"));
							privatekey.setText(settings.get("ssh (SGE)").get("privatekey"));
							workfolder.setText(settings.get("ssh (SGE)").get("workfolder"));
							imagefolder.setText(settings.get("ssh (SGE)").get("imagefolder"));
							if(settings.get("ssh (SGE)").get("checkdelete").compareTo("true")==0) {
								checkdelete.setSelected(true);
							}else {
								checkdelete.setSelected(false);
							}
						}else {
							hostname.setText("");
							port.setText("22");
							workfolder.setText("work");
							imagefolder.setText("$HOME/img");
							checkdelete.setSelected(true);
						}
						break;
					case "ssh (SGE)":
						if(settings.get("ssh").get("changed").compareTo("T")==0) {
							hostname.setText(settings.get("ssh").get("hostname"));
							port.setText(settings.get("ssh").get("port"));
							user.setText(settings.get("ssh").get("user"));
							password.setText(settings.get("ssh").get("password"));
							privatekey.setText(settings.get("ssh").get("privatekey"));
							workfolder.setText(settings.get("ssh").get("workfolder"));
							imagefolder.setText(settings.get("ssh").get("imagefolder"));
							if(settings.get("ssh").get("checkdelete").compareTo("true")==0) {
								checkdelete.setSelected(true);
							}else {
								checkdelete.setSelected(false);
							}
						}else {
							hostname.setText("");
							port.setText("22");
							workfolder.setText("work");
							imagefolder.setText("$HOME/img");
							checkdelete.setSelected(true);
						}
						break;
					case "ddbj":
						hostname.setText("gw.ddbj.nig.ac.jp");
						port.setText("22");
						//password.setDisable(true);
						//password.setText("");
						workfolder.setText("work");
						imagefolder.setText("$HOME/img");
						checkdelete.setSelected(true);
						break;
					case "shirokane":
						hostname.setText("slogin.hgc.jp");
						port.setText("22");
						//password.setDisable(true);
						//password.setText("");
						workfolder.setText("work");
						imagefolder.setText("$HOME/img");
						checkdelete.setSelected(true);
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
						checkdelete.setDisable(true);
						checkdelete.setSelected(false);
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
						checkdelete.setSelected(false);
						break;
					case "Linux":
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
						workfolder.setDisable(true);
						workfolder.setText("");
						checkdelete.setDisable(true);
						break;
					case "Linux (SGE)":
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
						workfolder.setDisable(true);
						workfolder.setText("");
						checkdelete.setDisable(true);
						break;
					default:
						System.out.println("no preset value");
					}
				}
		}
		});

        Tooltip label_privatekey_tooltip = new Tooltip();
        label_privatekey_tooltip.setText(label_privatekey.getText());
	    label_privatekey.setTooltip(label_privatekey_tooltip);
        Tooltip label_workfolder_tooltip = new Tooltip();
        label_workfolder_tooltip.setText(label_workfolder.getText());
	    label_workfolder.setTooltip(label_workfolder_tooltip);
        Tooltip label_imagefolder_tooltip = new Tooltip();
        label_imagefolder_tooltip.setText(label_imagefolder.getText());
	    label_imagefolder.setTooltip(label_imagefolder_tooltip);
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
                    (new OutputStreamWriter(new FileOutputStream(PPSetting.getBaseDir()+"jobs.json"),"UTF-8")));

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

	//「$scriptdir/」で書かれた子スクリプトを探す
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

        //「PP_DO_CHILD」、「PP_ENV_CHILD」で書かれた子スクリプトを探す
        scriptcontString = readAll(scriptFolderPath+"/"+scriptPathString);
        scriptcontStrings = scriptcontString.split("\n");
        for(String scripti: scriptcontStrings) {
		 String searchString = "PP_DO_CHILD";
             // searchStringが含まれる位置を見つける
             int index = scripti.indexOf(searchString);
             if (index != -1) {
                 // searchStringが見つかった場合、その文字列以降の部分を抜き出し、先頭の空白文字列を消す
                 String result = scripti.substring(index + searchString.length()).replaceAll("^\\s+", "");;
                 // スペースが最初に出てくる位置を見つける
                 int spaceIndex = result.indexOf(' ');
                 if (spaceIndex != -1) {
                     // スペースの前の文字列を切り出す
                     result = result.substring(0, spaceIndex);
                 }
			if(!scriptcontList.contains(result)) {
				scriptcontList.add(result);
				searchScript(scriptFolderPath, result, scriptcontList);
			}
             }

             searchString = "PP_ENV_CHILD";
             // searchStringが含まれる位置を見つける
             index = scripti.indexOf(searchString);
             if (index != -1) {
                 // searchStringが見つかった場合、その文字列以降の部分を抜き出し、先頭の空白文字列を消す
                 String result = scripti.substring(index + searchString.length()).replaceAll("^\\s+", "");;
                 // スペースが最初に出てくる位置を見つける
                 int spaceIndex = result.indexOf(' ');
                 if (spaceIndex != -1) {
                     // スペースの前の文字列を切り出す
                     result = result.substring(0, spaceIndex);
                 }
			if(!scriptcontList.contains(result)) {
				scriptcontList.add(result);
				searchScript(scriptFolderPath, result, scriptcontList);
			}

             }
        }

    }

    String getStatusByWorkId(int workid) {
	String reString="";
	for(JobNode jNode : jobNodes) {
		if(jNode.id.equals(String.valueOf(workid))){
			reString=jNode.status;
		}
	}
	return reString;
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
			Files.createLink(Paths.get(link+"/"+Paths.get(target).getFileName()), Paths.get(target).toAbsolutePath());
		}catch (Exception e) {
			System.err.println(e.getMessage());
			try {
				Files.copy(Paths.get(target), Paths.get(link+"/"+Paths.get(target).getFileName()));
			} catch (IOException e1) {
				// TODO 自動生成された catch ブロック
				e1.printStackTrace();
			}
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
		//System.out.println( channelSftp.stat(channelSftp.realpath(sourcePath + "/" + item.getFilename())).isDir());
            if (!item.getAttrs().isDir()) { // Check if it is a file (not a directory).
			System.out.println("download information... "+sourcePath + "/" + item.getFilename());
		//System.out.println("sftp size:  "+item.getAttrs().getSize());
		//System.out.println("local size: "+new File(destinationPath + "/" + item.getFilename()).length());
		//System.out.println("is Symbolic Link: "+Files.isSymbolicLink(new File(destinationPath + "/" + item.getFilename()).toPath()));
                if (!(new File(destinationPath + "/" + item.getFilename())).exists()
                        || (item.getAttrs().getMTime() > Long.valueOf(new File(destinationPath + "/" + item.getFilename()).lastModified() / (long) 1000).intValue())  // Download only if changed later.
                        || (item.getAttrs().getSize() > new File(destinationPath + "/" + item.getFilename()).length()) //ファイルサイズがリモートより小さいならダウンロード(途中でダウンロードが失敗した場合を想定)
                  ) {
			if(!(Files.isSymbolicLink(new File(destinationPath + "/" + item.getFilename()).toPath()))) { //ダウンロード先がシンボリックリンクならばダウンロードしない

				System.out.println("downloading... "+sourcePath + "/" + item.getFilename());
				//System.out.println( channelSftp.realpath(sourcePath + "/" + item.getFilename()));
				if(!channelSftp.stat(channelSftp.realpath(sourcePath + "/" + item.getFilename())).isDir()) { //for symbolic link
					channelSftp.get(channelSftp.realpath(sourcePath + "/" + item.getFilename()),
							destinationPath + "/" + item.getFilename()); // Download file from source (source filename, destination filename).
				}else {
					new File(destinationPath + "/" + item.getFilename()).mkdirs(); // Empty folder copy.
					recursiveFolderDownload(sourcePath + "/" + item.getFilename(),
							destinationPath + "/" + item.getFilename(), channelSftp); // Enter found folder on server to read its contents and create locally.
				}
			}
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
    public static boolean containsUnicode(String str) {
	for(int i = 0 ; i < str.length() ; i++) {
		char ch = str.charAt(i);
		Character.UnicodeBlock unicodeBlock = Character.UnicodeBlock.of(ch);

		if (Character.UnicodeBlock.HIRAGANA.equals(unicodeBlock))
			return true;

		if (Character.UnicodeBlock.KATAKANA.equals(unicodeBlock))
			return true;

		if (Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS.equals(unicodeBlock))
			return true;

		if (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(unicodeBlock))
			return true;

		if (Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION.equals(unicodeBlock))
			return true;
	}
	return false;
    }

    public void showSourceCodeWindow(String scriptName, String sourceCode) {
        Stage newStage = new Stage();
        newStage.setTitle("[Source code]: "+scriptName);
        TextArea textArea = new TextArea(); // Reverted to TextArea
        textArea.setEditable(false);
        if (sourceCode != null) {
            textArea.setText(sourceCode);
        } else {
            textArea.setText("");
        }
        Scene scene = new Scene(textArea, 600, 400); // Set a default size for the window
        // newStage.setOnCloseRequest removed as it was for 'cleanup.unsubscribe()'
        newStage.setScene(scene);
        newStage.show();
    }

    // Removed computeHighlighting method

    public void handleShowSourceCode(String scriptName) {
        try {
            String scriptFolderPath = new PPSetting().get("scriptfolder");
            String scriptPath = Paths.get(scriptFolderPath, scriptName).toString();
            String sourceCode = readAll(scriptPath); // Assuming readAll can throw IOException
            showSourceCodeWindow(scriptName, sourceCode);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not read script file");
            alert.setContentText("The file " + scriptName + " could not be read.\n" + e.getMessage());
            alert.showAndWait();
        } catch (Exception e) { // Catch other potential exceptions from PPSetting
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error retrieving script folder path");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }
}
