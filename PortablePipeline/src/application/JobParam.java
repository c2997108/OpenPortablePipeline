package application;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * ジョブを投入したときに指定した項目1つ分（入力ファイル欄またはオプション欄）。
 * jobs.jsonに保存し、ジョブ一覧で項目ごとに独立したテキストボックスとして表示する。
 */
public class JobParam {

    /** 入力欄を表す種別。 */
    public static final String KIND_INPUT = "input";
    /** オプション欄を表す種別。 */
    public static final String KIND_OPTION = "option";

    /** 項目のID。input_1、opt_c など。 */
    public String id;
    /** KIND_INPUT か KIND_OPTION。 */
    public String kind;
    /** 実行画面に出ていた項目の説明。 */
    public String label;
    /** 入力欄はファイルの一覧、オプション欄は値1つ。 */
    public List<String> values;

    public JobParam() {
        this.id = "";
        this.kind = "";
        this.label = "";
        this.values = new ArrayList<>();
    }

    public JobParam(String id, String kind, String label, List<String> values) {
        this.id = id == null ? "" : id;
        this.kind = kind == null ? "" : kind;
        this.label = label == null ? "" : label;
        this.values = values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    /** 1行にまとめた値。従来のジョブ説明文と同じ形式（カンマ区切り）。 */
    public String joinedValues() {
        return values == null ? "" : String.join(",", values);
    }

    /** 値が1つも入っていない項目か。 */
    @JsonIgnore
    public boolean isEmpty() {
        if (values == null || values.isEmpty()) {
            return true;
        }
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @JsonIgnore
    public boolean isInput() {
        return KIND_INPUT.equals(kind);
    }
}
