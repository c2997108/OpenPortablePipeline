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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

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

        	ImageView imageView1;
			Image iconImage1;
        	Button stopButton = new Button("Stop");
    		switch(jNode.status) {
    			case "preparing":
    				iconImage1 = new Image("file:image/pato_lamp_on_blue.png", 30, 30, false, false);
    				imageView1 = new ImageView(iconImage1);
    				break;
    			case "running":
    				iconImage1 = new Image("file:image/pato_lamp_on_blue.png", 30, 30, false, false);
    				imageView1 = new ImageView(iconImage1);
    				break;
    			case "finished":
    				iconImage1 = new Image("file:image/pato_lamp_on_green.png", 30, 30, false, false);
    				imageView1 = new ImageView(iconImage1);
    				stopButton.setDisable(true);
    				break;
    			default:
    				iconImage1 = new Image("file:image/pato_lamp_on_red.png", 30, 30, false, false);
    				imageView1 = new ImageView(iconImage1);
    				stopButton.setDisable(true);
    		}

        	//hbox1.getChildren().add(label2);
        	TextField label3 = new TextField(jNode.desc);
        	label3.focusedProperty().addListener(new ChangeListener<Boolean>() {
        		@Override
        		public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        			if(!newValue) {
        				System.out.println(label3.getText() + " out focus");
        				label3.setTooltip(new Tooltip(label3.getText()));

        				for(JobNode jobNode: JobWindowController.jobNodes) {
        					if(jobNode.id.equals(jNode.id)) {
                				if(!label3.getText().equals(jobNode.desc)) {
                					System.out.println("changed");
                					jobNode.desc = label3.getText();
                    				JobWindowController.saveJobList();
                				}
        					}
        				}
        			}
        		}
			});
        	Button button1 = new Button("", new ImageView(new Image("file:image/computer_folder.png", 20, 20, false, false)));
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
						if(node.get("preset").asText().equals("shirokane")||node.get("preset").asText().equals("ddbj")||node.get("preset").asText().equals("direct (SGE)")) {
							ChannelSftp chsftp = ConnectSsh.getSftpChannel(node, outputdir);
							chsftp.get(workdir+"/"+"save_jid.txt", outputdir+"/results/"+"save_jid.txt");

							List<String> lines = Files.readAllLines(Paths.get(outputdir+"/results/"+"save_jid.txt"), StandardCharsets.UTF_8);
		        	    	int jobId = Integer.valueOf(lines.get(0).split(" ")[2]);

		        	    	ChannelExec chexec = ConnectSsh.getSshChannel(node, outputdir);
		        	    	chexec.setCommand("if [ `find "+ workdir +" |grep /qsub.log$|wc -l` != 0 ];then cat `find . |grep /qsub.log$`; fi|awk '{print $3}'|xargs qdel; qdel "+jobId);
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
						}else if(node.get("preset").asText().equals("WSL")) {
							List<String> lines = Files.readAllLines(Paths.get(outputdir+"/results/"+"save_pid.txt"), StandardCharsets.UTF_8);
		        	    	int jobId = Integer.valueOf(lines.get(0));
		        	    	Runtime.getRuntime().exec("bash.exe -c 'kill -9 "+jobId+"'");
						}else if(node.get("preset").asText().equals("Mac")) {
							List<String> lines = Files.readAllLines(Paths.get(outputdir+"/results/"+"save_pid.txt"), StandardCharsets.UTF_8);
		        	    	int jobId = Integer.valueOf(lines.get(0));
		        	    	Runtime.getRuntime().exec("kill -9 "+jobId);
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
        	Button deleteButton = new Button("", new ImageView(new Image("file:image/gomibako_full.png", 20, 20, false, false)));

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
        	if(!(jNode.status.equals("finished")||jNode.status.equals("aborted")||jNode.status.equals("cancelled"))) {
        		deleteButton.setDisable(true);
        	}
        	
        	HBox hBox = new HBox(20d);
        	//hBox.setPrefWidth(500);
        	//HBox.setHgrow(label3, Priority.ALWAYS);
        	//HBox.setHgrow(label2, Priority.ALWAYS);
        	//HBox.setHgrow(button1, Priority.ALWAYS);
        	//label3.setMaxWidth(Double.MAX_VALUE);
        	label3.setMinWidth(580);
        	label3.setMaxWidth(580);
        	label3.setTooltip(new Tooltip(jNode.desc));
        	label1.setMinWidth(30d);
        	label2.setMinWidth(60d);
        	button1.setMinWidth(30d);
        	button1.setTooltip(new Tooltip("open results"));
        	stopButton.setMinWidth(60d);
        	deleteButton.setMinWidth(30d);
        	deleteButton.setTooltip(new Tooltip("delete"));
            hBox.getChildren().add(label1);
            hBox.getChildren().add(label3);
            hBox.getChildren().add(imageView1);
            hBox.getChildren().add(label2);
            hBox.getChildren().add(stopButton);
            hBox.getChildren().add(deleteButton);
            hBox.getChildren().add(button1);
        	setGraphic(hBox);
        }

	}

}
