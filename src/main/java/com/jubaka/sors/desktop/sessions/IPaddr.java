package com.jubaka.sors.desktop.sessions;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.*;

import com.jubaka.sors.desktop.factories.ClassFactory;
import com.jubaka.sors.desktop.protocol.application.HTTP;


public class IPaddr extends Observable implements Serializable, CustomObserver {


	private ClassFactory myFactory;
	private List<Session> newSessionsForCC = Collections.synchronizedList(new ArrayList<>());
	private boolean remoteObserver = false;

	private InetAddress addr;
	private String dnsName= new String("not resolved");
	private Subnet net;
	private Long dataDown = (long) 0;
	private Long dataUp = (long) 0;
	private Date activated = new Date();

	private Integer inputActiveCount=0;
	private Integer inputCount=0;
	private Integer outputActiveCount=0;
	private Integer outputCount=0;
	private Integer activeCount=0;
	private Integer savedCount=0;
	private Integer notifyLimit = 1024; // GUI will change after counters increase on more than this val 
	private Integer notifyValue = 0;
	

//	private static HashMap<String, IPaddr> storage = new HashMap<String, IPaddr>();




	public synchronized Long getDataDown() {
		return dataDown;
	}
	public synchronized Long getDataUp() {
		return dataUp;
	}
	
	public void setActivated(Date date) {
		activated=date;
	}
	public Date getActivated(){
		return activated;
	}

	public List<Session> getNewSessionsForCC() {
		List<Session> item = new ArrayList<>(newSessionsForCC);
		clearNewSessionsList();
		return item;
	}

	public void clearNewSessionsList() {
		newSessionsForCC.clear();
	}
	public IPaddr(ClassFactory myFactory, Branch br, InetAddress addr, Subnet net) {
		this.addr = addr;
		this.net = net;
		GetHostName hName = new GetHostName(this);
		hName.resolve();
		ObserverTimer ipTimer = ObserverTimer.getinstance();
		ipTimer.addObserver(this);
		myFactory.getSesionInstance(br.getId()).addIpInst(addr.getHostAddress(),this);
		this.myFactory = myFactory;
	}

	public Integer getBrachId() {
		return net.getId();
	}
	
	public boolean isIn(Session ses) {
		if (ses.getSrcIP() == this) return false;
		if (ses.getDstIP() == this) return true;
		return false;
	}

	public boolean isRemoteObserver() {
		return remoteObserver;
	}

	public void setRemoteObserver(boolean remoteObserver) {
		this.remoteObserver = remoteObserver;
		System.out.println("set remote observer "+remoteObserver);
	}
	
	public void clearAll() {
		dataDown=(long)0;
		dataUp=(long)0;
		inputCount=0;
		outputCount=0;
		activeCount=0;
		savedCount=0;
	}

	public synchronized void updateCounters(Long len, boolean receive) {
		
		if (receive) {
			dataDown = dataDown+len;
			net.incrementInputData(len);
			setChanged();
				
		
		} else {
			dataUp = dataUp+len;
			net.incrementOutputData(len);
			setChanged();
		}
	/*	if (receive) {
			Long length = len + receiveLimitBuf;
			if (length > 1024) {
				Long res = length / 1024;
				dataDown = dataDown + res;
				net.incrementInputData(res.intValue());
				receiveLimitBuf = (length - (res * 1024));
				setChanged();
				notifyObservers();
			} else  receiveLimitBuf = length;
		} else {
			Long length = len + sendLimitBuf;
			if (length > 1024) {
				Long res = length / 1024;
				dataUp = dataUp + res;
				net.incrementOutputData(res.intValue());
				sendLimitBuf = length - (res * 1024);
				setChanged();
				notifyObservers();
			} else  sendLimitBuf = length;
			
		}
		
		*/
		
		
	}

	
	public Subnet getNet() {
		return net;
	}

	public void moveToSubnet(Subnet newNet) {
		net = newNet;
	}
	public InetAddress getAddr() {
		return addr;
	}
	
	public synchronized Long getDataDown(Long onTime) {
		Long resultCount = (long) 0;
		Set<Session> set = getInputActiveSessions(onTime);
		for (Session ses : set) {
			resultCount+=ses.getSrcDataLen(0, onTime);
		}
		
		set = getInputStoredSessions(onTime);
		for (Session ses : set) {
			resultCount+=ses.getSrcDataLen(0, onTime);
		}
		
		set = getOutputActiveSessions(onTime);
		for (Session ses : set) {
			resultCount+=ses.getDstDataLen(0, onTime);
		}
		
		set = getOutputStoredSessions(onTime);
		for (Session ses : set) {
			resultCount+=ses.getDstDataLen(0, onTime);
		}
		
		return resultCount;
	}
	
	public synchronized Long getDataUp(Long onTime) {
		Long resultCount = (long) 0;
		Set<Session> set = getInputActiveSessions(onTime);
		for (Session ses : set) {
			resultCount+=ses.getDstDataLen(0, onTime);
		}
		
		set = getInputStoredSessions(onTime);
		for (Session ses : set) {
			resultCount+=ses.getDstDataLen(0, onTime);
		}
		
		set = getOutputActiveSessions(onTime);
		for (Session ses : set) {
			resultCount+=ses.getSrcDataLen(0, onTime);
		}
		
		set = getOutputStoredSessions(onTime);
		for (Session ses : set) {
			resultCount+=ses.getSrcDataLen(0, onTime);
		}
		
		return resultCount;
	}
	
	public Session getSessionByPacket(int tcpSrcPort,int tcpDstPort, IPaddr dstAddr) {



		
		Set<Session> inputActive = getInputActiveSessions();
		Set<Session> outputActive = getOutputActiveSessions();
		
		for (Session ses : inputActive) {
			if (ses.getSrcIP()==dstAddr & ses.getSrcP() == tcpDstPort) {
				if (ses.getDstP() == tcpSrcPort) return ses;
			}
		}
		for (Session ses : outputActive) {
			if (ses.getDstIP()==dstAddr & ses.getDstP() == tcpDstPort) {
				if (ses.getSrcP() == tcpSrcPort) return ses;
			}
		}

		return null;
	}
	
	public List<HTTP> getHTTPs() {
		Set<Session> sessions = net.getInputActiveSes(this);
		List<HTTP> resultSet = new ArrayList<HTTP>();
		for (Session ses : sessions) {
			for (HTTP packet : ses.getHTTPList()) {
				resultSet.add(packet);
			}
		}
		
		sessions = net.getInputStoredSes(this);
		for (Session ses : sessions) {
			for (HTTP packet : ses.getHTTPList()) {
				resultSet.add(packet);
			}
		}
		
		sessions = net.getOutputActiveSes(this);
		for (Session ses : sessions) {
			for (HTTP packet : ses.getHTTPList()) {
				resultSet.add(packet);
			}
		}
		
		sessions = net.getOutputStoredSes(this);
		for (Session ses : sessions) {
			for (HTTP packet : ses.getHTTPList()) {
				resultSet.add(packet);
			}
		}
		return resultSet;
	}

	public void closeAllDataSavers() {
		for (Session ses : getInputActiveSessions()){
			ses.getDataSaver().closeConnection();
		}
		for (Session ses : getInputStoredSessions()){
			ses.getDataSaver().closeConnection();
		}
		for (Session ses : getOutputActiveSessions()){
			ses.getDataSaver().closeConnection();
		}
		for (Session ses : getOutputStoredSessions()){
			ses.getDataSaver().closeConnection();
		}
	}
	
	public Set<Session> getOutputActiveSessions(Long onTime) {
		Set<Session> resultSet = new HashSet<Session>();
		Set<Session> set = net.getOutputActiveSes(this);
		Date closedDate;
		Date startDate;
		for (Session ses : set) {
			startDate = ses.getEstablished();
			if (startDate.getTime()<onTime) {
				resultSet.add(ses);
			}
		}
		
		set = net.getOutputStoredSes(this);
		for (Session ses : set) {
			closedDate = ses.getClosed(); 
			startDate = ses.getEstablished();
			if (startDate.getTime()<onTime & closedDate.getTime() > onTime) {
				resultSet.add(ses);
			}
		}
		return resultSet;
	}
	
	
	public Set<Session> getOutputStoredSessions(Long onTime) {
		Set<Session> resultSet = new HashSet<Session>();
		Set<Session> set;
		Date closedDate;
		Date startDate;

		set = net.getOutputStoredSes(this);
		for (Session ses : set) {
			closedDate = ses.getClosed(); 
			startDate = ses.getEstablished();
			if (startDate.getTime()<onTime & closedDate.getTime() <= onTime) {
				resultSet.add(ses);
			}
		}
		return resultSet;
	}
	
	public Set<Session> getInputActiveSessions(Long onTime) {
		Set<Session> resultSet = new HashSet<Session>();
		Set<Session> set = net.getInputActiveSes(this);
		Date closedDate;
		Date startDate;
		for (Session ses : set) {
			startDate = ses.getEstablished();
			if (startDate.getTime()<onTime) {
				resultSet.add(ses);
			}
		}
		
		set = net.getInputStoredSes(this);
		for (Session ses : set) {
			closedDate = ses.getClosed(); 
			startDate = ses.getEstablished();
			if (startDate.getTime()<onTime & closedDate.getTime() > onTime) {
				resultSet.add(ses);
			}
		}
		return resultSet;
		
	}
	public Set<Session> getInputStoredSessions(Long onTime) {
		Set<Session> resultSet = new HashSet<Session>();
		Set<Session> set;
		Date closedDate;
		Date startDate;

		set = net.getInputStoredSes(this);
		for (Session ses : set) {
			closedDate = ses.getClosed(); 
			startDate = ses.getEstablished();
			if (startDate.getTime()<onTime & closedDate.getTime() <= onTime) {
				resultSet.add(ses);
			}
		}
		return resultSet;
		
	}
	
	
	public Set<Session> getInputActiveSessions() {
		return net.getInputActiveSes(this);
	}
	
	public Set<Session> getInputStoredSessions() {
		return net.getInputStoredSes(this);
	}
	
	public Set<Session> getOutputActiveSessions() {
		return net.getOutputActiveSes(this);
	}
	
	public Set<Session> getOutputStoredSessions() {
		return net.getOutputStoredSes(this);
	}
	
	
	public synchronized Integer getInSessionCount() {
		return inputCount;
	}
	public synchronized Integer getActiveSesCount() {
		return activeCount;
	}
	public synchronized Integer getSavedSesCount() {
		return savedCount;
	}
	
	public synchronized Integer getOutSessionCount() {
		return outputCount;
	}

	public Integer getInputActiveCount() {
		return inputActiveCount;
	}

	public void setInputActiveCount(Integer inputActiveCount) {
		this.inputActiveCount = inputActiveCount;
	}

	public Integer getOutputActiveCount() {
		return outputActiveCount;
	}

	public void setOutputActiveCount(Integer outputActiveCount) {
		this.outputActiveCount = outputActiveCount;
	}



	public void  sessionClose(Session s) {
		net.onCloseConnection(s);
		net.onCloseConnection(s);
		activeCount--;
		savedCount++;
		setChanged();
		notifyObservers(s);
		
		
		
	}   
	public synchronized void handleIncomingSession(Session ses) {
		activeCount++;
		inputCount++;
		inputActiveCount++;
		net.save(ses);
		net.addIp(ses.getDstIP());
		setChanged();
	//	notifyObservers(ses);
		 newSessionsForCC.add(ses);
		
	}
	public synchronized void handleOutputSession(Session ses) {
		activeCount++;
		outputCount++;
		outputActiveCount++;
		net.save(ses);
		net.addIp(ses.getSrcIP());
		setChanged();
	//	notifyObservers(ses);
		if (remoteObserver) newSessionsForCC.add(ses);
		
	}
	public String getDnsName() {
		return dnsName;
	}
	public void setDnsName(String dnsName) {
		this.dnsName = dnsName;
	}

	public String toString() {
		return addr.getHostAddress();
	}
	public void removeSession(Session ses) {
		
		if (ses.getSrcIP()==this) {
			outputCount--;
			if (ses.closed == null) {
				activeCount--;
			} else {
				savedCount--;
			}
		} 
		if (ses.getDstIP()==this) {
			inputCount--;
			if (ses.closed == null) {
				activeCount--;
			} else {
				savedCount--;
			}
		}
		net.removeSession(ses);
		
	}
	
	@Override
	public void customUpdate() {
		notifyObservers();
		
	}
}

