package application;

import java.util.ArrayList;
import java.util.List;

public class JobNode {
	public String id;
	public String status;
	/** ジョブ一覧に出る見出し。利用者が自由に書き換えられる。 */
	public String desc;
	/** 実行したスクリプト名。 */
	public String script;
	/** 投入時に指定した入力ファイルとオプション。この変更より前のジョブでは空になる。 */
	public List<JobParam> params;
	/** 投入日時。"yyyy-MM-dd HH:mm:ss" 形式。分からないときは空。 */
	public String submittedAt;
	/** 終了日時。"yyyy-MM-dd HH:mm:ss" 形式。実行中や分からないときは空。 */
	public String finishedAt;
	public JobNode() {
		this.id="";
		this.status="";
		this.desc="";
		this.script="";
		this.params=new ArrayList<>();
		this.submittedAt="";
		this.finishedAt="";
	}
	public JobNode(String id, String status) {
		this(id, status, "", "", null);
	}
	public JobNode(String id, String status, String desc) {
		this(id, status, desc, "", null);
	}
	public JobNode(String id, String status, String desc, String script, List<JobParam> params) {
		this.id = id;
		this.status = status;
		this.desc = desc;
		this.script = script == null ? "" : script;
		this.params = params == null ? new ArrayList<>() : new ArrayList<>(params);
		this.submittedAt = "";
		this.finishedAt = "";
	}
	/** すべての内容をそのまま写した複製。 */
	public JobNode(JobNode other) {
		this(other.id, other.status, other.desc, other.script, other.params);
		this.submittedAt = other.submittedAt == null ? "" : other.submittedAt;
		this.finishedAt = other.finishedAt == null ? "" : other.finishedAt;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	/** 投入日時を入れた自分自身を返す（組み立て時に続けて書けるようにするため）。 */
	public JobNode submittedNow() {
		this.submittedAt = PortablePipelineCommon.nowStamp();
		return this;
	}
	/** 状態だけを変えた複製を返す。指定項目や日時の記録は引き継ぐ。 */
	public JobNode withStatus(String newStatus) {
		JobNode copy = new JobNode(this);
		copy.status = newStatus;
		return copy;
	}
	/** 終了状態に変えた複製を返す。終了日時が空のときだけ記録する。 */
	public JobNode withFinished(String newStatus, String stamp) {
		JobNode copy = withStatus(newStatus);
		if (copy.finishedAt == null || copy.finishedAt.isEmpty()) {
			copy.finishedAt = stamp == null ? "" : stamp;
		}
		return copy;
	}
	/**
	 * 一覧の絞り込み用。空白区切りの語をすべて含むジョブだけを残す（大文字小文字は区別しない）。
	 * 対象はID、状態、見出し、スクリプト名、入力ファイルのパス、オプションの名前と値、投入日時と終了日時。
	 */
	public boolean matchesSearch(String search) {
		if (search == null) {
			return true;
		}
		String[] terms = search.trim().toUpperCase().split("[\\s\\u3000]+");
		String haystack = searchText().toUpperCase();
		for (String term : terms) {
			if (!term.isEmpty() && !haystack.contains(term)) {
				return false;
			}
		}
		return true;
	}
	private String searchText() {
		StringBuilder builder = new StringBuilder();
		for (String field : new String[] { id, status, desc, script, submittedAt, finishedAt }) {
			builder.append(field == null ? "" : field).append('\n');
		}
		if (params != null) {
			for (JobParam param : params) {
				if (param == null) {
					continue;
				}
				builder.append(param.id).append('\n').append(param.label).append('\n');
				if (param.values != null) {
					for (String value : param.values) {
						builder.append(value == null ? "" : value).append('\n');
					}
				}
			}
		}
		return builder.toString();
	}
	/** 見出しと指定項目を1行にまとめた文字列。項目の記録がないジョブでは見出しだけになる。 */
	public String summary() {
		StringBuilder builder = new StringBuilder(desc == null ? "" : desc);
		if (params != null) {
			for (JobParam param : params) {
				builder.append(" ").append(param.id).append(":").append(param.joinedValues());
			}
		}
		return builder.toString();
	}
}
