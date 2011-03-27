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

public class ValidationRecord {

	ValidationLevel level;
	BasicElement source;
	String type;
	String message;
	
	public ValidationRecord(ValidationLevel level, BasicElement source, String type, String message) {
		this.level = level;
		this.source = source;
		this.type = type;
		this.message = message;
	}

	public ValidationLevel getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

	public BasicElement getSource() {
		return source;
	}

	public String getType() {
		return type;
	}
	
}
