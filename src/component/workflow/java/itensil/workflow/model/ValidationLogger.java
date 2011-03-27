/*
 * Copyright 2004-2007 by Itensil, Inc.,
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information
 * of Itensil, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Itensil.
 */
package itensil.workflow.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class ValidationLogger {

	int errors;
	int warns;
	int infos;
	
	ArrayList<ValidationRecord> records;
	HashSet<String> checks;
	
	public ValidationLogger() {
		errors = warns = infos = 0;
		records = new ArrayList<ValidationRecord>();
		checks = new HashSet<String>();
	}
	
	public void addCheck(String opt) {
		checks.add(opt);
	}
	
	public boolean check(String opt) {
		return checks.contains(opt);
	}
	
	public void error(BasicElement source, String type, String message) {
		log(ValidationLevel.ERROR, source, type, message);
	}
	
	public void warn(BasicElement source, String type, String message) {
		log(ValidationLevel.WARN, source, type, message);
	}
	
	public void info(BasicElement source, String type, String message) {
		log(ValidationLevel.INFO, source, type, message);
	}
	
	public void log(ValidationLevel level, BasicElement source, String type, String message) {
		switch (level) {
			case ERROR: errors++; break;
			case WARN: warns++; break;
			default: infos++;
		}
		records.add(new ValidationRecord(level, source, type, message));
	}
	
	public boolean isValid() {
		return errors == 0;
	}
	
	public Collection<ValidationRecord> getRecords() {
		return records;
	}

	public int getErrors() {
		return errors;
	}

	public int getInfos() {
		return infos;
	}

	public int getWarns() {
		return warns;
	}
	
}
