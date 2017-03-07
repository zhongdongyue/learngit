package com.trendytech.tds.os.admin.model;

import org.codehaus.jackson.annotate.JsonProperty;

public class Usage {
	@JsonProperty("rgw.main")
	private Object rgwMain;

	public Object getRgwMain() {
		return rgwMain;
	}

	public void setRgwMain(Object rgwMain) {
		this.rgwMain = rgwMain;
	}
	
}
