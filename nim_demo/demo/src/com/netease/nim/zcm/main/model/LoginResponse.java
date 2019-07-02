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
public class LoginResponse {
	/** 服务器响应码 */
	public String res;
	/** 服务器返回消息 */
	public String errmsg;
	public  MyData data;
	public  VersionData data1;
	public class MyData{
		public String token;

		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}

		@Override
		public String toString() {
			return "MyData{" +
					"token='" + token + '\'' +
					'}';
		}
	}
	public class VersionData{
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
					"id='" + id + '\'' +
					", platform='" + platform + '\'' +
					", url='" + url + '\'' +
					", version='" + version + '\'' +
					'}';
		}
	}

	public String getRes() {
		return res;
	}

	public void setRes(String res) {
		this.res = res;
	}

	public String getMsg() {
		return errmsg;
	}

	public void setMsg(String msg) {
		this.errmsg = msg;
	}

	public MyData getData() {
		return data;
	}

	public void setData(MyData data) {
		this.data = data;
	}

	public VersionData getData1() {
		return data1;
	}

	public void setData1(VersionData data1) {
		this.data1 = data1;
	}

	@Override
	public String toString() {
		return "LoginResponse{" +
				"res='" + res + '\'' +
				", errmsg='" + errmsg + '\'' +
				", data=" + data +
				", data1=" + data1 +
				'}';
	}
}
