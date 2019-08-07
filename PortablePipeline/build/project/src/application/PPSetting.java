package application;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class PPSetting {

    JsonNode node;
	public PPSetting() throws JsonProcessingException, IOException{
		// TODO 自動生成されたコンストラクター・スタブ
        ObjectMapper mapper = new ObjectMapper();
        node = mapper.readTree(new File("settings.json"));
	}
	public PPSetting(String setting_file) throws JsonProcessingException, IOException{
		// TODO 自動生成されたコンストラクター・スタブ
        ObjectMapper mapper = new ObjectMapper();
        node = mapper.readTree(new File(setting_file));
	}

	public String get(String param) {
		if(node.get(param)==null) {
			return "";
		}else {
			return node.get(param).asText();
		}
	}

}
