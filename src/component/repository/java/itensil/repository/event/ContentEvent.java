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
package itensil.repository.event;

import java.io.Serializable;

import itensil.repository.NodeContent;
import itensil.repository.RepositoryNode;

public class ContentEvent implements Serializable {
	
	public enum Type {CREATE, UPDATE, REMOVE};

	RepositoryNode node;
	NodeContent content;
	Type type;
	
	public ContentEvent(RepositoryNode node, NodeContent content,Type type) {
		this.node = node;
		this.content = content;
		this.type = type;
	}

	/**
	 * @return null on type == REMOVE
	 */
	public NodeContent getContent() {
		return content;
	}

	public RepositoryNode getNode() {
		return node;
	}

	public Type getType() {
		return type;
	}

}
