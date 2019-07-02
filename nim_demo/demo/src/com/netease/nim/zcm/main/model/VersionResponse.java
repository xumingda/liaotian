package com.netease.nim.zcm.main.model;




/**
 * @作者: 许明达
 * @创建时间: 2016-3-23下午15:43:20
 * @版权: 特速版权所有
 * @描述: 封装服务器返回列表的参数
 * @更新人:
 * @更新时间:
 * @更新内容: TODO
 */
public class VersionResponse {
	/** 服务器响应码 */
	public String code;
	/** 服务器返回消息 */
	public String msg;
	public  MyData data;
	public class MyData{
		public String id;
		public String platform;
		public String url;
		public String version;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getPlatform() {
			return platform;
		}

		public void setPlatform(String platform) {
			this.platform = platform;
		}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}



		@Override
		public String toString() {
			return "MyData{" +
					", id='" + id + '\'' +
					", platform='" + platform + '\'' +
					", url='" + url + '\'' +
					", version='" + version + '\'' +
					'}';
		}
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public MyData getData() {
		return data;
	}

	public void setData(MyData data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "VersionResponse{" +
				"code='" + code + '\'' +
				", msg='" + msg + '\'' +
				", data=" + data +
				'}';
	}
}
