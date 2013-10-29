package org.chibitomo.logger;

public class Logger {

	private java.util.logging.Logger logger;

	public Logger(java.util.logging.Logger logger) {
		this.logger = logger;
	}

	public void info(String msg) {
		logger.info(msg);
	}

	public void error(Exception e) {
		e.printStackTrace();
	}
}
