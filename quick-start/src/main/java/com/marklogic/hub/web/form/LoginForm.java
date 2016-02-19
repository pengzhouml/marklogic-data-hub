package com.marklogic.hub.web.form;

import java.util.ArrayList;
import java.util.List;

import com.marklogic.hub.model.DomainModel;

public class LoginForm extends BaseForm {

	private String mlHost;
	private String mlRestPort;
	private String mlUsername;
	private String mlPassword;
	private String userPluginDir;
	private String mlcpHomeDir;
	private boolean serverVersionAccepted;
	private boolean installed;
	private boolean loggedIn;
    private List<DomainModel> domains = new ArrayList<DomainModel>();
	private DomainModel selectedDomain;

	public String getMlHost() {
		return mlHost;
	}

	public void setMlHost(String mlHost) {
		this.mlHost = mlHost;
	}

	public String getMlRestPort() {
		return mlRestPort;
	}

	public void setMlRestPort(String restPort) {
		this.mlRestPort = restPort;
	}

	public String getMlUsername() {
		return mlUsername;
	}

	public void setMlUsername(String mlUsername) {
		this.mlUsername = mlUsername;
	}

	public String getMlPassword() {
		return mlPassword;
	}

	public void setMlPassword(String mlPassword) {
		this.mlPassword = mlPassword;
	}

	public boolean isServerVersionAccepted() {
		return serverVersionAccepted;
	}

	public void setServerVersionAccepted(boolean serverVersionAccepted) {
		this.serverVersionAccepted = serverVersionAccepted;
	}

	public String getMlcpHomeDir() {
	  return mlcpHomeDir;
	}

	public void setMlcpHomeDir(String mlcpHomeDir) {
	  this.mlcpHomeDir = mlcpHomeDir;
	}

	public boolean isInstalled() {
		return installed;
	}

	public void setInstalled(boolean installed) {
		this.installed = installed;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
	}

	public String getUserPluginDir() {
		return userPluginDir;
	}

	public void setUserPluginDir(String userPluginDir) {
		this.userPluginDir = userPluginDir;
	}

	public List<DomainModel> getDomains() {
		return domains;
	}

	public void setDomains(List<DomainModel> domains) {
		this.domains = domains;
	}
	
	public DomainModel getSelectedDomain() {
		return selectedDomain;
	}

	public void setSelectedDomain(DomainModel selectedDomain) {
		this.selectedDomain = selectedDomain;
	}

	public void selectDomain(String domainName) {
	    if (domains != null) {
	        for (DomainModel domain : domains) {
	            if (domain.getDomainName().equals(domainName)) {
	                setSelectedDomain(domain);
	            }
	        }
	    }
	}
	
	public void refreshSelectedDomain() {
	    if (selectedDomain != null) {
	        selectDomain(selectedDomain.getDomainName());
	    }
	}
}
