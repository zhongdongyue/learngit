/**
 * 
 */
package com.trendytech.tds.os.admin.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * @author Robin
 *
 */
public class Bucket implements Serializable {

	private static final long serialVersionUID = 2784926567432195397L;

	private String name = null;
	@JsonInclude(Include.NON_NULL)
	private Usage usage;
	
	/*public Usage getUsage() {
		return usage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}*/
	private Map<String, Object> params = new HashMap<String, Object>();
    
	public Usage getUsage() {
		return usage;
	}

	public void setUsage(Usage usage) {
		this.usage = usage;
	}
	public static class Usage{
		@JsonProperty("rgw.main")
		private SizeAndObject rgwMain;
		@JsonProperty("rgw.multimeta")
		private SizeAndObject rgwMultiMeta;

		public SizeAndObject getRgwMultiMeta() {
			return rgwMultiMeta;
		}

		public void setRgwMultiMeta(SizeAndObject rgwMultiMeta) {
			this.rgwMultiMeta = rgwMultiMeta;
		}

		public SizeAndObject getRgwMain() {
			return rgwMain;
		}

		public void setRgwMain(SizeAndObject rgwMain) {
			this.rgwMain = rgwMain;
		}
		
	}
	public static class SizeAndObject{
		private double size_kb;

		private double size_kb_actual;
		
		private double num_objects;

		public double getSize_kb() {
			return size_kb;
		}

		public void setSize_kb(double size_kb) {
			this.size_kb = size_kb;
		}

		public double getSize_kb_actual() {
			return size_kb_actual;
		}

		public void setSize_kb_actual(double size_kb_actual) {
			this.size_kb_actual = size_kb_actual;
		}

		public double getNum_objects() {
			return num_objects;
		}

		public void setNum_objects(double num_objects) {
			this.num_objects = num_objects;
		}
		
		
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Map<String, Object> getParams() {
		return params;
	}
	@JsonAnySetter
	public void setParams(String key, Object value) {
		params.put(key, value);
	}
}
