package application;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class JobCell extends ListCell<JobNode> {
	//private HBox hbox1;
	//public JobCell() {
	//	hbox1 = new HBox();
	//}

    String ppBinDir = System.getProperty("PP_BIN_DIR");

    /** カテゴリごとのアイコン。セルは頻繁に作り直されるので一度読んだものを使い回す。 */
    private static final Map<String, Image> CATEGORY_ICONS = new HashMap<String, Image>();

    /**
     * Analysis Scriptsの一覧と同じ規則でカテゴリのアイコンを返す（image/<カテゴリ>.png、無ければ image/pipe_icon.png）。
     * スクリプト名は記録があればそれを、無ければ見出しの先頭の語（古いジョブは見出しがスクリプト名で始まる）を使う。
     * カテゴリが分からないときは画像なしのImageViewを返し、幅だけ確保して行の位置をそろえる。
     */
    private ImageView buildScriptIcon(JobNode jNode) {
        ImageView view = new ImageView();
        view.setFitWidth(24);
        view.setFitHeight(24);
        String scriptName = jNode.script != null && !jNode.script.isEmpty() ? jNode.script : firstWord(jNode.desc);
        int tildeIndex = scriptName.indexOf('~');
        if(tildeIndex <= 0) {
            return view;
        }
        String category = scriptName.substring(0, tildeIndex);
        Image icon = CATEGORY_ICONS.get(category);
        if(icon == null) {
            try {
                icon = new Image(PortablePipelineCommon.fileUrl(ppBinDir, "image/" + category + ".png"));
                if(icon.isError()) {
                    icon = new Image(PortablePipelineCommon.fileUrl(ppBinDir, "image/pipe_icon.png"));
                }
            }catch(Exception e) {
                icon = null;
            }
            if(icon != null && !icon.isError()) {
                CATEGORY_ICONS.put(category, icon);
            }
        }
        if(icon != null && !icon.isError()) {
            view.setImage(icon);
            Tooltip.install(view, new Tooltip(category));
        }
        return view;
    }

    private static String firstWord(String text) {
        if(text == null) {
            return "";
        }
        String trimmed = text.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }
    
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
        	Button stopButton = new Button("", new ImageView(new Image(PortablePipelineCommon.fileUrl(ppBinDir, "image/stop.png"), 20, 20, false, false)));
        	stopButton.setId("stopButton");
        	stopButton.setTooltip(new Tooltip("stop"));
    		switch(jNode.status) {
    			case "preparing":
    				iconImage1 = new Image(PortablePipelineCommon.fileUrl(ppBinDir, "image/pato_lamp_on_blue.png"), 30, 30, false, false);
    				imageView1 = new ImageView(iconImage1);
    				break;
    			case "running":
    				iconImage1 = new Image(PortablePipelineCommon.fileUrl(ppBinDir, "image/pato_lamp_on_blue.png"), 30, 30, false, false);
    				imageView1 = new ImageView(iconImage1);
    				break;
    			case "finished":
    				iconImage1 = new Image(PortablePipelineCommon.fileUrl(ppBinDir, "image/pato_lamp_on_green.png"), 30, 30, false, false);
    				imageView1 = new ImageView(iconImage1);
    				stopButton.setDisable(true);
    				break;
    			default:
    				iconImage1 = new Image(PortablePipelineCommon.fileUrl(ppBinDir, "image/pato_lamp_on_red.png"), 30, 30, false, false);
    				imageView1 = new ImageView(iconImage1);
    				stopButton.setDisable(true);
    		}

        	//hbox1.getChildren().add(label2);
        	// 記録のあるジョブはスクリプト名をラベルで出す。記録のない古いジョブ（jobs.jsonにscriptが無い）は
        	// これまでどおり見出しを書き換えられるテキストボックスのままにする
        	boolean hasRecord = jNode.script != null && !jNode.script.isEmpty();
        	Region label3;
        	if(hasRecord) {
        		Label scriptLabel = new Label(jNode.script);
        		scriptLabel.setTooltip(new Tooltip(jNode.script));
        		label3 = scriptLabel;
        	}else {
        		TextField descField = new TextField(jNode.desc);
        		descField.setTooltip(new Tooltip(jNode.desc));
        		descField.focusedProperty().addListener(new ChangeListener<Boolean>() {
        			@Override
        			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        				if(!newValue) {
        					System.out.println(descField.getText() + " out focus");
        					descField.setTooltip(new Tooltip(descField.getText()));

        					for(JobNode jobNode: JobWindowController.jobNodes) {
        						if(jobNode.id.equals(jNode.id)) {
        							if(!descField.getText().equals(jobNode.desc)) {
        								System.out.println("changed");
        								jobNode.desc = descField.getText();
        								JobWindowController.saveJobList();
        							}
        						}
        					}
        				}
        			}
        		});
        		label3 = descField;
        	}
        	Button button1 = new Button("", new ImageView(new Image(PortablePipelineCommon.fileUrl(ppBinDir, "image/computer_folder.png"), 20, 20, false, false)));
        	button1.setId("resultsButton");
        	try {
        		String outputdir = PPSetting.getBaseDir() + new PPSetting().get("outputfolder");
        		button1.setOnAction((ActionEvent e) -> {
        			System.out.println(jNode.id);
        			final String OS_NAME = System.getProperty("os.name").toLowerCase();
        			try {
				// 絶対パスを正規化して引数1つとして渡す。スペースや非ASCII文字が入っていても問題ない
				String resultsPath = new File(outputdir, jNode.id + File.separator + "results").getAbsoluteFile().toPath().normalize().toString();
				List<String> cmd;
				if(OS_NAME.startsWith("windows")) {
					cmd = List.of("explorer.exe", resultsPath);
				}else if(OS_NAME.startsWith("mac")) {
					cmd = List.of("open", "-a", "Finder", resultsPath);
				}else {
					cmd = List.of("xdg-open", resultsPath);
				}
				System.out.println(String.join(" ", cmd));
				try {
					new ProcessBuilder(cmd).start();
				} catch (IOException ioe) {
					if(OS_NAME.startsWith("linux")) {
						new ProcessBuilder("nautilus", resultsPath).start();
					}else {
						throw ioe;
					}
				}
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
				if(JobWindowController.instance != null) {
					JobWindowController.instance.requestJobCancellation(jNode.id);
				}
				try {
						PPSetting mainSetting = new PPSetting();
                		String outputfolder = PPSetting.getBaseDir() + mainSetting.get("outputfolder");
                		String workfolder = mainSetting.get("workfolder");
                		String workdir = workfolder+"/"+jNode.id;
                		String outputdir = outputfolder + "/" + jNode.id;
                		String keyFilePath = outputfolder+"/"+jNode.id+"/"+"id_rsa";
						JsonNode node = new ObjectMapper().readTree(new File(outputdir+"/"+"settings.json"));
						if(node.get("preset").asText().equals("shirokane") || node.get("preset").asText().equals("ddbj") || node.get("preset").asText().equals("ssh (SGE)")) {
							ChannelSftp chsftp = ConnectSsh.getSftpChannel(node, outputdir);
							chsftp.get(workdir+"/"+"save_jid.txt", outputdir+"/results/"+"save_jid.txt");

							List<String> lines = Files.readAllLines(Paths.get(outputdir+"/results/"+"save_jid.txt"), StandardCharsets.UTF_8);
		        	    	int jobId = Integer.valueOf(lines.get(0));

		                    BufferedInputStream bin = null;
		                    //コマンド実行
		                    try {
		                    	List<String> listStr;
			        	    	//ChannelExec chexec = ConnectSsh.getSshChannel(node, outputdir);
								if(!node.get("preset").asText().equals("ddbj")) {
									listStr = ConnectSsh.getSshCmdResult(node, keyFilePath, "if [ `find "+ workdir +" |grep /qsub.log$|wc -l` != 0 ];then cat `find . |grep /qsub.log$`; fi|awk '{print $3}'|xargs qdel; qdel "+jobId+" 2>&1");
				        	    	//chexec.setCommand("if [ `find "+ workdir +" |grep /qsub.log$|wc -l` != 0 ];then cat `find . |grep /qsub.log$`; fi|awk '{print $3}'|xargs qdel; qdel "+jobId);
								}else { //ddbjの時 基本的に分散処理はしないのでjob本体を終了させて終了
									listStr = ConnectSsh.getSshCmdResult2StepSession(node, keyFilePath, "scancel "+jobId+" 2>&1");
				        	    	//chexec.setCommand("ssh a001 scancel "+jobId);
								}
								listStr.forEach(item -> System.out.println(item));
			                    //chexec.connect();

		                    	//bin = new BufferedInputStream(chexec.getInputStream());
		                    	//BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bin, StandardCharsets.UTF_8));

		                    	//String data;
		                    	//while ((data = bufferedReader.readLine()) != null) {
		                    	//	System.out.println(data);
		                    	//}
		                    	//// 最後にファイルを閉じてリソースを開放する
		                    	//bufferedReader.close();
			                    //chexec.disconnect();
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
								if(node.get("checkdelete").asText().equals("true")) {
	        	                    JobWindowController.lsFolderRemove(workdir, chsftp);
								}

		                    }
    	                    chsftp.exit();
						}else if(node.get("preset").asText().equals("ssh")) {
							ChannelSftp chsftp = ConnectSsh.getSftpChannel(node, outputdir);
							chsftp.get(workdir+"/"+"save_pid.txt", outputdir+"/results/"+"save_pid.txt");

							List<String> lines = Files.readAllLines(Paths.get(outputdir+"/results/"+"save_pid.txt"), StandardCharsets.UTF_8);
		        	    	int jobId = Integer.valueOf(lines.get(0));

		        	    	ChannelExec chexec = ConnectSsh.getSshChannel(node, outputdir);
		        	    	chexec.setCommand("kill "+jobId);
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
								if(node.get("checkdelete").asText().equals("true")) {
	        	                    JobWindowController.lsFolderRemove(workdir, chsftp);
								}


		                    }
		                    chexec.disconnect();
    	                    chsftp.exit();
						}else if(node.get("preset").asText().equals("WSL")) {
							List<String> lines = Files.readAllLines(Paths.get(outputdir+"/results/"+"save_pid.txt"), StandardCharsets.UTF_8);
		        	    	int jobId = Integer.valueOf(lines.get(0));
		        	    	Runtime.getRuntime().exec("bash.exe -c 'kill "+jobId+"'");
						}else if(node.get("preset").asText().equals("Mac") || node.get("preset").asText().equals("Linux") || node.get("preset").asText().equals("Linux (SGE)")) {
							List<String> lines = Files.readAllLines(Paths.get(outputdir+"/results/"+"save_pid.txt"), StandardCharsets.UTF_8);
		        	    	int jobId = Integer.valueOf(lines.get(0));
		        	    	Runtime.getRuntime().exec("kill "+jobId);
						}
					} catch (Exception e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					} finally {

					    ObservableList<JobNode> jobNodes = JobWindowController.jobNodes;
						Platform.runLater( () -> {
							jobNodes.set( jobNodes.indexOf(jNode), jNode.withFinished("cancelled", PortablePipelineCommon.nowStamp()) ); // 指定項目や日時の記録は引き継ぐ
							JobWindowController.saveJobList();
						} );

					}
        		});
        	}catch (Exception e) {
			}
        	Button deleteButton = new Button("", new ImageView(new Image(PortablePipelineCommon.fileUrl(ppBinDir, "image/gomibako_full.png"), 20, 20, false, false)));
        	deleteButton.setId("deleteButton");

        	try {
        		deleteButton.setOnAction(ae -> {
				    ObservableList<JobNode> jobNodes = JobWindowController.jobNodes;
					Platform.runLater( () -> {
						jobNodes.remove(jNode);
						JobWindowController.saveJobList();
						try {
							PPSetting mainSetting = new PPSetting();
	                		String outputfolder = PPSetting.getBaseDir() + mainSetting.get("outputfolder");
	                		String outputdir = outputfolder + "/" + jNode.id;

	                        Path target = Paths.get(outputdir);   // ① 対象ディレクトリ

	                            /*  
	                             * ② まず深い階層から順にファイル → ディレクトリの順で削除する
	                             *    Files.walk() は Stream<Path> を返すので、
	                             *    reverseOrder で子 → 親の順序に並べ替えるのがポイント
	                             */
	                            try (var stream = Files.walk(target)) {
	                                stream
	                                    .sorted(Comparator.reverseOrder())
	                                    .forEach(path -> {
	                                        try {
	                                            Files.delete(path);
	                                        } catch (IOException e) {
	                                            // 削除に失敗した箇所をログに出すなど
	                                            System.err.println("Cannot delete: " + path + " (" + e + ")");
	                                        }
	                                    });
	                            }

	                            System.out.println(outputdir + " was deleted.");

						} catch (IOException e) {
							e.printStackTrace();
						}
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
        	label1.setMinWidth(30d);
        	label2.setMinWidth(60d);
        	button1.setMinWidth(30d);
        	button1.setTooltip(new Tooltip("open results"));
        	stopButton.setMinWidth(30d);
        	deleteButton.setMinWidth(30d);
        	deleteButton.setTooltip(new Tooltip("delete"));
        	// 再実行: Analysis Scriptsタブを開いて、同じスクリプト・入力・オプションを実行画面に入れる
        	Button rerunButton = new Button("", new ImageView(new Image(PortablePipelineCommon.fileUrl(ppBinDir, "image/rerun.png"), 20, 20, false, false)));
        	rerunButton.setId("rerunButton");
        	rerunButton.setMinWidth(30d);
        	boolean canRerun = jNode.script != null && !jNode.script.isEmpty() && jNode.params != null && !jNode.params.isEmpty();
        	rerunButton.setDisable(!canRerun);
        	rerunButton.setTooltip(new Tooltip(canRerun
        			? "open Analysis Scripts with the same inputs and options"
        			: "no record of inputs and options (job submitted before this feature)"));
        	rerunButton.setOnAction(ae -> {
        		if(JobWindowController.instance == null) {
        			return;
        		}
        		String problem = JobWindowController.instance.rerunJob(jNode);
        		if(problem != null && !problem.isEmpty()) {
        			Alert alert = new Alert(Alert.AlertType.WARNING, problem);
        			alert.setHeaderText(null);
        			alert.showAndWait();
        		}
        	});
            hBox.getChildren().add(buildScriptIcon(jNode));
            hBox.getChildren().add(label1);
            hBox.getChildren().add(label3);
            hBox.getChildren().add(imageView1);
            hBox.getChildren().add(label2);
            hBox.getChildren().add(stopButton);
            hBox.getChildren().add(rerunButton);
            hBox.getChildren().add(deleteButton);
            hBox.getChildren().add(button1);

        	HBox timePane = buildTimePane(jNode);
        	GridPane paramPane = buildParamPane(jNode);
        	if(timePane == null && paramPane == null) {
        		setGraphic(hBox);
        	}else {
        		VBox vBox = new VBox(4d);
        		vBox.getChildren().add(hBox);
        		if(timePane != null) {
        			vBox.getChildren().add(timePane);
        		}
        		if(paramPane != null) {
        			vBox.getChildren().add(paramPane);
        		}
        		setGraphic(vBox);
        	}
        }

	}

	/**
	 * 投入日時と終了日時の行。どちらも分からないジョブではnullを返し、従来どおり1行だけ表示する。
	 */
	private HBox buildTimePane(JobNode jNode) {
		String submitted = jNode.submittedAt == null ? "" : jNode.submittedAt;
		String finished = jNode.finishedAt == null ? "" : jNode.finishedAt;
		if(submitted.isEmpty() && finished.isEmpty()) {
			return null;
		}
		Label submittedLabel = new Label("submitted: " + (submitted.isEmpty() ? "-" : submitted));
		submittedLabel.setTooltip(new Tooltip("ジョブを投入した日時"));
		Label finishedLabel = new Label("finished: " + (finished.isEmpty() ? "-" : finished));
		finishedLabel.setTooltip(new Tooltip("ジョブが終了した日時"));
		HBox box = new HBox(24d, submittedLabel, finishedLabel);
		box.setPadding(new Insets(0d, 0d, 0d, 34d));
		return box;
	}

	/**
	 * 投入時に指定した入力ファイルとオプションを、項目ごとに独立したテキストボックスで並べる。
	 * 記録のない古いジョブや、値が空の項目しかない場合はnullを返し、従来どおり1行だけ表示する。
	 */
	private GridPane buildParamPane(JobNode jNode) {
		if(jNode.params == null || jNode.params.isEmpty()) {
			return null;
		}
		GridPane grid = new GridPane();
		grid.setHgap(8d);
		grid.setVgap(3d);
		grid.setPadding(new Insets(0d, 0d, 4d, 34d));
		ColumnConstraints idColumn = new ColumnConstraints();
		idColumn.setMinWidth(80d);
		idColumn.setPrefWidth(80d);
		ColumnConstraints descColumn = new ColumnConstraints();
		descColumn.setMinWidth(170d);
		descColumn.setPrefWidth(170d);
		ColumnConstraints valueColumn = new ColumnConstraints();
		valueColumn.setMinWidth(300d);
		valueColumn.setPrefWidth(560d);
		valueColumn.setHgrow(Priority.ALWAYS);
		grid.getColumnConstraints().addAll(idColumn, descColumn, valueColumn);

		int row = 0;
		for(JobParam param : jNode.params) {
			if(param == null || param.isEmpty()) {
				continue; // 指定しなかった任意の入力欄などは並べない
			}
			Label idLabel = new Label(param.id);
			idLabel.setTooltip(new Tooltip(param.id));
			Label descLabel = new Label(param.label);
			descLabel.setMaxWidth(170d);
			if(param.label != null && !param.label.isEmpty()) {
				descLabel.setTooltip(new Tooltip(param.label));
			}
			grid.add(idLabel, 0, row);
			grid.add(descLabel, 1, row);
			grid.add(buildValueBox(param), 2, row);
			row++;
		}
		return row == 0 ? null : grid;
	}

	/** 値の表示用テキストボックス。ファイルが複数のときは1行に1つずつ並べる。 */
	private Region buildValueBox(JobParam param) {
		List<String> values = param.values;
		if(values.size() <= 1) {
			TextField field = new TextField(values.isEmpty() ? "" : values.get(0));
			field.setEditable(false); // 実行時の記録なので書き換えさせない（コピーはできる）
			field.setMaxWidth(Double.MAX_VALUE);
			field.setTooltip(new Tooltip(field.getText()));
			return field;
		}
		TextArea area = new TextArea(String.join("\n", values));
		area.setEditable(false);
		area.setWrapText(false);
		area.setPrefRowCount(Math.min(values.size(), 4));
		area.setMaxWidth(Double.MAX_VALUE);
		area.setPrefHeight(Math.min(values.size(), 4) * 20d + 12d);
		area.setTooltip(new Tooltip(values.size() + " files"));
		return area;
	}

}
