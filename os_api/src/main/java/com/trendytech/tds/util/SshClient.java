package com.trendytech.tds.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;

public class SshClient {
	private static Logger log = LoggerFactory.getLogger(SshClient.class);
	
    private Connection connection;
    
    private String ipaddr;
    
    private String output;
    
    private boolean connected = false;
    
    public SshClient() {
        connection = null;
    }
    
    public SshClient(String ip) {
    	connect(ip);
    }
    
    public SshClient(String ip, String user, String password) {
    	connect(ip, user, password);
    }
    
    public boolean isConnected() {
        return connected == true;
    }
    
    public int connect(String ip) {
    	ipaddr = ip;
        connection = new Connection(ip);
        try {
            connection.connect(null,
            		PropertiesUtil.getInt("sshClient.connectTimeout"), 
            		PropertiesUtil.getInt("sshClient.kexTimeout"));
            connected = connection.authenticateWithPassword(
            		PropertiesUtil.getString("cluster.host.user"),
            		PropertiesUtil.getString("cluster.host.password"));
            if (!connected) {
            	log.info("\"" + ip + "\" connect FAIL");
            } else {
            	log.info("\"" + ip + "\" connect OK");
            }
        } catch (IOException e) {
            log.error("\"" + ip + "\" connect exception", e);
            if (connection != null) {
                connection.close();
                connection = null;
            }
            return -1;
        }
        return 1;
    }
    
    public int connectCluster() {
    	ipaddr = PropertiesUtil.getString("cluster.host.ip");
        connection = new Connection(ipaddr);
        try {
            connection.connect(null,
            		PropertiesUtil.getInt("sshClient.connectTimeout"), 
            		PropertiesUtil.getInt("sshClient.kexTimeout"));
            connected = connection.authenticateWithPassword(
            		PropertiesUtil.getString("cluster.host.user"),
            		PropertiesUtil.getString("cluster.host.password"));
            if (!connected) {
            	log.info("\"" + ipaddr + "\" connect FAIL");
            	connection = null;
            	ipaddr = PropertiesUtil.getString("cluster.secondary.ip");
            	//备用连接
            	connection = new Connection(ipaddr);
                connection.connect(null,
                		PropertiesUtil.getInt("sshClient.connectTimeout"), 
                		PropertiesUtil.getInt("sshClient.kexTimeout"));
                connected = connection.authenticateWithPassword(
                		PropertiesUtil.getString("cluster.secondary.user"),
                		PropertiesUtil.getString("cluster.secondary.password"));
                //次链接成功判断
                if (!connected) {
                	log.info("\"" + ipaddr + "\" secondary cluster server connect FAIL");
                    
                } else {
                	//次连接成功
                	log.info("\"" + ipaddr + "\" secondary cluster server connect OK");
                }
            } else {
            	//主连接成功
            	log.info("\"" + ipaddr + "\" connect OK");
            }
        } catch (IOException e) {
            log.error("\"" + ipaddr + "\" connect exception", e);
            if (connection != null) {
                connection.close();
                connection = null;
            }
            if (!connected) {
            	log.info("\"" + ipaddr + "\" connect FAIL");
            	connection = null;
            	ipaddr = PropertiesUtil.getString("cluster.secondary.ip");
            	//备用连接
            	connection = new Connection(ipaddr);
                try {
					connection.connect(null,
							PropertiesUtil.getInt("sshClient.connectTimeout"), 
							PropertiesUtil.getInt("sshClient.kexTimeout"));
	                connected = connection.authenticateWithPassword(
	                		PropertiesUtil.getString("cluster.secondary.user"),
	                		PropertiesUtil.getString("cluster.secondary.password"));
	                //次链接成功判断
	                if (!connected) {
	                	log.info("\"" + ipaddr + "\" secondary cluster server connect FAIL");
	                    
	                } else {
	                	//次连接成功
	                	log.info("\"" + ipaddr + "\" secondary cluster server connect OK");
	                }
				} catch (IOException e1) {
		            log.error("\"" + ipaddr + "\" connect secondary cluster server exception", e);
		            if (connection != null) {
		                connection.close();
		                connection = null;
		            }
				}

            } else {
            	//主连接成功
            	log.info("\"" + ipaddr + "\" connect OK");
            }
            return -1;
        }
        return 1;
    }
    
    public int connect(String ip, String user, String password) {
    	ipaddr = ip;
        connection = new Connection(ip);
        try {
            connection.connect(null,
            		PropertiesUtil.getInt("sshClient.connectTimeout"), 
            		PropertiesUtil.getInt("sshClient.kexTimeout"));
            connected = connection.authenticateWithPassword(
            		user, password);
            if (!connected) {
            	log.info("\"" + ip + "\" connect FAIL");
            } else {
            	log.info("\"" + ip + "\" connect OK");
            }
        } catch (IOException e) {
            log.error("\"" + ip + "\" connect exception", e);
            if (connection != null) {
            	connection.close();
            	connection = null;
            }
            return -1;
        }
        return 1;
    }
    
    public void disconnect() {
        connection.close();
        connection = null;
        connected = false;
    }
    
    public int exec(String cmd) {
        BufferedReader breader = null;
        int err = 0;
        int cond = 0;
        InputStream os = null;
        Session sess = null;
        if (isConnected()) {
	        try {
	            sess = connection.openSession();
	            sess.requestDumbPTY();
	            sess.execCommand(cmd);
	            os = new StreamGobbler(sess.getStdout());
	            cond = sess.waitForCondition(ChannelCondition.EXIT_STATUS, 
	            		PropertiesUtil.getInt("sshClient.executeTimeout"));
	            if ((cond & ChannelCondition.EXIT_STATUS) != 0) {
	                StringBuilder sb = new StringBuilder();
	                String line;
	                breader = new BufferedReader(new InputStreamReader(os));
	                while ((line = breader.readLine()) != null) {
	                    sb.append(line + "\n");
	                }
	                output = sb.toString();
	                err = sess.getExitStatus();
	            }
	            else if ((cond & ChannelCondition.TIMEOUT) != 0) {
	            	log.info("Execute cmd \"" + cmd + "\" timeout");
	                output = "Execute cmd \"" + cmd + "\" timeout";
	                err = -1;
	            }
	            return err;
	        }
	        catch (IOException e) {
	        	log.error("IO Exception", e);
	            output = "IO Exception";
	            return -1;
	        }
	        catch (IllegalStateException e) {
	        	log.error("Illegal State Exception", e);
	        	output = "Illegal State Exception!";
	            return -1;
	        }
	        finally {
	            try {
	                if (breader != null)
	                    breader.close();
	                if (sess != null)
	                    sess.close();
	                if (os != null)
	                    os.close();
	            }
	            catch (IOException e) {
	            	output = "IO Exception";
	            	log.error("IO Exception", e);
	                e.printStackTrace();
	            }
	        }
        } else {
        	output = "Cannot be connected!";
        	log.info("Cannot be connected!");
        	return -1;
        }
    }
    
    public String getOutput() {
    	int objIndex = output.indexOf("{");
    	int arrayIndex = output.indexOf("[");
    	if (objIndex > -1 && objIndex < arrayIndex) {
			return output.substring(objIndex);
		} else if (arrayIndex > -1 && objIndex > arrayIndex) {
			return output.substring(arrayIndex);
		} else {
			return output;
		}
    }
    
    public String getIpaddr() {
    	return ipaddr;
    }
    
    
    @Override
	public String toString() {
		return "SshClient [connection=" + connection + ", ipaddr=" + ipaddr + ", output=" + output + ", connected="
				+ connected + "]";
	}
}
