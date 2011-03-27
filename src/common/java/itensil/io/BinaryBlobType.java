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
package itensil.io;

import java.io.Serializable;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.type.Type;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

public class BinaryBlobType implements CompositeUserType {

	public Class returnedClass() {
		return Hibernate.BINARY.getReturnedClass();
	}

	public boolean equals(Object x, Object y) {
		return Hibernate.BINARY.isEqual(x, y);
	}

	public Object deepCopy(Object value) {
		if (value == null)
			return null;

		byte[] bytes = (byte[]) value;
		byte[] result = new byte[bytes.length];
		System.arraycopy(bytes, 0, result, 0, bytes.length);

		return result;
	}

	public boolean isMutable() {
		return Hibernate.BINARY.isMutable();
	}

	public int hashCode(Object x) throws HibernateException {
		return Hibernate.BINARY.getHashCode(x, null);
	}

	public Object assemble(Serializable cached, SessionImplementor session,
			Object owner) throws HibernateException {

		return Hibernate.BINARY.assemble(cached, session, owner);

	}

	public Serializable disassemble(Object value, SessionImplementor session)
			throws HibernateException {
		return Hibernate.BINARY.disassemble(value, session, null);
	}

	public String[] getPropertyNames() {
		return new String[] { "value" };
	}

	public Type[] getPropertyTypes() {
		return new Type[] { Hibernate.BLOB };
	}

	public Object getPropertyValue(Object component, int property)
			throws HibernateException {
		return component;
	}

	public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException, SQLException {

		Blob blob = (Blob) Hibernate.BLOB.nullSafeGet(rs, names, session, owner);
		if (blob == null) return null; 
		else return blob.getBytes(1, (int)blob.length());
	}

	public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) 
		throws HibernateException, SQLException {

		if (value == null)
			Hibernate.BLOB.nullSafeSet(st, null, index, session);
		else {

			Blob blob = Hibernate.createBlob((byte[]) value);
			Hibernate.BLOB.nullSafeSet(st, blob, index, session);
		}
	}

	public Object replace(Object original, Object target, SessionImplementor session, Object owner) 
		throws HibernateException {

		return Hibernate.BINARY.replace(original, target, session, owner, null);
	}

	public void setPropertyValue(Object component, int property, Object value)
			throws HibernateException {
	}

}
