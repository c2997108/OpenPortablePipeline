package application;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class JobCell extends ListCell<JobNode> {
	//private HBox hbox1;
	//public JobCell() {
	//	hbox1 = new HBox();
	//}

	@Override
    protected void updateItem(JobNode jNode, boolean empty) {

        super.updateItem(jNode, empty);

        if(empty) {
        	setText(null);
        	setGraphic(null);
        }else {
            //System.out.println(jNode.id);


        	Label label1 = new Label(jNode.id);
        	//hbox1.getChildren().add(label1);
        	Label label2 = new Label(jNode.status);
        	//hbox1.getChildren().add(label2);
        	Label label3 = new Label("status: "+jNode.desc);
        	Button button1 = new Button("open results");
        	try {
        		String outputdir = new PPSetting().get("outputfolder");
        		button1.setOnAction((ActionEvent e) -> {
        			System.out.println(jNode.id);
        			final String OS_NAME = System.getProperty("os.name").toLowerCase();
        			try {
        				Runtime rt = Runtime.getRuntime();
        				String cmd = "";
        				if(OS_NAME.startsWith("windows")) {
        					cmd = "explorer "+outputdir+"\\"+jNode.id+"\\results";
        				}else if(OS_NAME.startsWith("mac")) {
        					cmd = "open -a Finder "+outputdir+"/"+jNode.id+"/results";
        				}else if(OS_NAME.startsWith("linux")) {
        					cmd = "natilus "+outputdir+"/"+jNode.id+"/results";
        				}
        				System.out.println(cmd);
        				rt.exec(cmd);
        			} catch (Exception e2) {
        				e2.printStackTrace();
        			}
        		});
        	}catch(Exception e) {
        		e.printStackTrace();
        	}
        	Button stopButton = new Button("Stop");
        	try {
        		stopButton.setOnAction(ae -> {
        			System.out.println("stopping: "+jNode.id);
        			try {
						PPSetting mainSetting = new PPSetting();
                		String outputfolder = mainSetting.get("outputfolder");
                		String workfolder = mainSetting.get("workfolder");
                		String workdir = workfolder+"/"+jNode.id;
                		String outputdir = outputfolder + "/" + jNode.id;
						JsonNode node = new ObjectMapper().readTree(new File(outputdir+"/"+"settings.json"));
						if(node.get("preset").asText().equals("shirokane")) {
							ChannelSftp chsftp = ConnectSsh.getSftpChannel(node, outputdir);
							chsftp.get(workdir+"/"+"save_jid.txt", outputdir+"/results/"+"save_jid.txt");
							
							List<String> lines = Files.readAllLines(Paths.get(outputdir+"/results/"+"save_jid.txt"), StandardCharsets.UTF_8);
		        	    	int jobId = Integer.valueOf(lines.get(0).split(" ")[2]);
		        	    	
		        	    	ChannelExec chexec = ConnectSsh.getSshChannel(node, outputdir);
		        	    	chexec.setCommand("qdel "+jobId);
		                    chexec.connect();

		                    BufferedInputStream bin = null;
		                    //コマンド実行
		                    try {
		                    	bin = new BufferedInputStream(chexec.getInputStream());
		                    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bin, StandardCharsets.UTF_8));

		                    	String data;
		                    	while ((data = bufferedReader.readLine()) != null) {
		                    		System.out.println(data);
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
		                        JobWindowController.recursiveFolderDownload(workdir, outputdir+"/"+"results", chsftp);
        	                    JobWindowController.lsFolderRemove(workdir, chsftp);
    							
		                    }
		                    chexec.disconnect();
    	                    chsftp.exit();
						}else if(node.get("preset").asText().equals("direct")) {
							ChannelSftp chsftp = ConnectSsh.getSftpChannel(node, outputdir);
							chsftp.get(workdir+"/"+"save_pid.txt", outputdir+"/results/"+"save_pid.txt");
							
							List<String> lines = Files.readAllLines(Paths.get(outputdir+"/results/"+"save_pid.txt"), StandardCharsets.UTF_8);
		        	    	int jobId = Integer.valueOf(lines.get(0));
		        	    	
		        	    	ChannelExec chexec = ConnectSsh.getSshChannel(node, outputdir);
		        	    	chexec.setCommand("kill -9 "+jobId);
		                    chexec.connect();

		                    BufferedInputStream bin = null;
		                    //コマンド実行
		                    try {
		                    	bin = new BufferedInputStream(chexec.getInputStream());
		                    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bin, StandardCharsets.UTF_8));

		                    	String data;
		                    	while ((data = bufferedReader.readLine()) != null) {
		                    		System.out.println(data);
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
		                        JobWindowController.recursiveFolderDownload(workdir, outputdir+"/"+"results", chsftp);
        	                    JobWindowController.lsFolderRemove(workdir, chsftp);
    							
		                    }
		                    chexec.disconnect();
    	                    chsftp.exit();
						}
					} catch (Exception e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					} finally {

					    ObservableList<JobNode> jobNodes = JobWindowController.jobNodes;
						Platform.runLater( () -> {
							jobNodes.set( jobNodes.indexOf(jNode),new JobNode(jNode.id, "cancelled", jNode.desc) );
							JobWindowController.saveJobList();
						} );
                 		
					}
        		});
        	}catch (Exception e) {
			}
        	Button deleteButton = new Button("Delete");
        	try {
        		deleteButton.setOnAction(ae -> {
				    ObservableList<JobNode> jobNodes = JobWindowController.jobNodes;
					Platform.runLater( () -> {
						jobNodes.remove(jNode);
						JobWindowController.saveJobList();
					} );
        		});
        	}catch (Exception e) {
			}

        	HBox hBox = new HBox(20d);
        	hBox.setPrefWidth(500);
        	HBox.setHgrow(label3, Priority.ALWAYS);
        	//HBox.setHgrow(label2, Priority.ALWAYS);
        	//HBox.setHgrow(button1, Priority.ALWAYS);
        	//label3.setMaxWidth(Double.MAX_VALUE);
        	label3.setMaxWidth(500);
        	label1.setMinWidth(30d);
        	label2.setMinWidth(60d);
            hBox.getChildren().add(label1);
            hBox.getChildren().add(label3);
            hBox.getChildren().add(label2);
            hBox.getChildren().add(stopButton);
            hBox.getChildren().add(deleteButton);
            hBox.getChildren().add(button1);
        	setGraphic(hBox);
        }

	}

}
