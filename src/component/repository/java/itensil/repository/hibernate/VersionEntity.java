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
package itensil.repository.hibernate;

import itensil.repository.DefaultNodeVersion;
import itensil.repository.NodeVersion;
import itensil.repository.NodeProperties;
import itensil.repository.PropertyHelper;
import itensil.util.Check;
import itensil.io.HibernateUtil;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Query;


/**
 * @author ggongaware@itensil.com
 *
 */
public class VersionEntity extends DefaultNodeVersion implements NodeProperties {
	
	private static Logger log = Logger.getLogger(VersionEntity.class);

    private NodeEntity nodeEntity;
    private Set<ContentEntity> contentEntities;
    private long id;
    private String davContentType;
    private String davContentLang;
    private Integer davContentLen;
    private String davEtag;
    private Date davLastMod;
    
    private String irModifier;
    private String irDescription;
    private String irKeywords;
    private String irTags;
    private String irStyle;

    private long ieRecordId;
    private String ieBrowse0;
    private String ieBrowse1;
    private String ieBrowse2;
    private String ieBrowse3;
    private String ieBrowse4;
    private String ieBrowse5;
    private String ieBrowse6;
    private String ieBrowse7;
    private String ieBrowse8;
    private String ieBrowse9;
    private String ieBrowseA;
    private String ieBrowseB;
    
    private String cust1Ns;
    private String cust1Val;
    private String cust2Ns;
    private String cust2Val;
    private String cust3Ns;
    private String cust3Val;
    private String cust4Ns;
    private String cust4Val;
    
    public VersionEntity() {
    	this.isDefault = false;
    }

    public VersionEntity(NodeVersion version) {
        super(version.getNumber(), version.getLabel(), version.isDefault());
    }
    
    public void initNew() {
    	contentEntities = new HashSet<ContentEntity>();
    	davLastMod = new Date();
    }

    public NodeEntity getNodeEntity() {
        return nodeEntity;
    }

    public void setNodeEntity(NodeEntity nodeEntity) {
        this.nodeEntity = nodeEntity;
    }

    void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public QName[] getNames() {
    	ArrayList<QName> qns = new ArrayList<QName>();
    	
    	if (getDavContentType() != null) qns.add(PropertyHelper.defaultQName("getcontenttype"));
    	if (getDavContentLang() != null) qns.add(PropertyHelper.defaultQName("getcontentlanguage"));
    	if (getDavContentLen() != null) qns.add(PropertyHelper.defaultQName("getcontentlength"));
    	if (getDavEtag() != null) qns.add(PropertyHelper.defaultQName("getetag"));
    	if (getDavLastMod() != null) qns.add(PropertyHelper.defaultQName("getlastmodified"));
    	
    	if (getIrModifier() != null) qns.add(PropertyHelper.itensilQName("modifier"));
    	if (getIrDescription() != null) qns.add(PropertyHelper.itensilQName("description"));
    	if (getIrKeywords() != null) qns.add(PropertyHelper.itensilQName("keywords"));
    	if (getIrTags() != null) qns.add(PropertyHelper.itensilQName("tags"));
    	if (getIrStyle() != null) qns.add(PropertyHelper.itensilQName("style"));
    	
    	if (getIeRecordId() > 0) qns.add(PropertyHelper.itensilEntityQName("recordId"));
    	if (getIeBrowse0() != null) qns.add(PropertyHelper.itensilEntityQName("browse0"));
    	if (getIeBrowse1() != null) qns.add(PropertyHelper.itensilEntityQName("browse1"));
    	if (getIeBrowse2() != null) qns.add(PropertyHelper.itensilEntityQName("browse2"));
    	if (getIeBrowse3() != null) qns.add(PropertyHelper.itensilEntityQName("browse3"));
    	if (getIeBrowse4() != null) qns.add(PropertyHelper.itensilEntityQName("browse4"));
    	if (getIeBrowse5() != null) qns.add(PropertyHelper.itensilEntityQName("browse5"));
    	if (getIeBrowse6() != null) qns.add(PropertyHelper.itensilEntityQName("browse6"));
    	if (getIeBrowse7() != null) qns.add(PropertyHelper.itensilEntityQName("browse7"));
    	if (getIeBrowse8() != null) qns.add(PropertyHelper.itensilEntityQName("browse8"));
    	if (getIeBrowse9() != null) qns.add(PropertyHelper.itensilEntityQName("browse9"));
    	if (getIeBrowseA() != null) qns.add(PropertyHelper.itensilEntityQName("browseA"));
    	if (getIeBrowseB() != null) qns.add(PropertyHelper.itensilEntityQName("browseB"));
    	
    	if (getCust1Ns() != null) qns.add(QName.valueOf(getCust1Ns()));
    	if (getCust2Ns() != null) qns.add(QName.valueOf(getCust2Ns()));
    	if (getCust3Ns() != null) qns.add(QName.valueOf(getCust3Ns()));
    	if (getCust4Ns() != null) qns.add(QName.valueOf(getCust4Ns()));
    	
        return qns.toArray(new QName[qns.size()]);
    }

    public String getValue(QName name) {
    	
    	String lp = name.getLocalPart();
    	if (PropertyHelper.DEFAULT_QNAMESPACE.equals(name.getNamespaceURI())) {
    		
    		if (		"getlastmodified".equals(lp)) {
    			Date dt = getDavLastMod();
    			return dt != null ? PropertyHelper.dateString(dt) : null;
    		} else if (	"getcontentlength".equals(lp)) {
    			Integer ii = getDavContentLen();
    			return ii != null ? ii.toString() : null;
    		} else if (	"getetag".equals(lp)) {
    			return getDavEtag();
    		} else if (	"getcontenttype".equals(lp)) {
    			return getDavContentType();
    		} else if (	"getcontentlanguage".equals(lp)) {
    			return getDavContentLang();
    		} 
    		
    	} else if (PropertyHelper.ITENSIL_QNAMESPACE.equals(name.getNamespaceURI())) {
    		
    		if ( 		"modifier".equals(lp)) {
    			return getIrModifier();
    		} else if ( "tags".equals(lp)) {
    			return getIrTags();
    		} else if ( "style".equals(lp)) {
    			return getIrStyle();
    		} else if ( "description".equals(lp)) {
    			return getIrDescription();
    		} else if ( "keywords".equals(lp)) {
    			return getIrKeywords();
    		}
    		
    	}else if (PropertyHelper.ITENSIL_ENTITY_QNAMESPACE.equals(name.getNamespaceURI())) {
    		
    		if	(		"recordId".equals(lp)) {
    			return String.valueOf(getIeRecordId());
    		} else if ( "browse0".equals(lp)) {
    			return getIeBrowse0();
    		} else if ( "browse1".equals(lp)) {
    			return getIeBrowse1();
    		} else if ( "browse2".equals(lp)) {
    			return getIeBrowse2();
    		} else if ( "browse3".equals(lp)) {
    			return getIeBrowse3();
    		} else if ( "browse4".equals(lp)) {
    			return getIeBrowse4();
    		} else if ( "browse5".equals(lp)) {
    			return getIeBrowse5();
    		} else if ( "browse6".equals(lp)) {
    			return getIeBrowse6();
    		} else if ( "browse7".equals(lp)) {
    			return getIeBrowse7();
    		} else if ( "browse8".equals(lp)) {
    			return getIeBrowse8();
    		} else if ( "browse9".equals(lp)) {
    			return getIeBrowse9();
    		} else if ( "browseA".equals(lp)) {
    			return getIeBrowseA();
    		} else if ( "browseB".equals(lp)) {
    			return getIeBrowseB();
    		} 
    	}
    	
    	String qnVal = name.toString();
    	
		if (qnVal.equals(getCust1Ns())) {
			return getCust1Val();
		} else if (qnVal.equals(getCust2Ns())) {
			return getCust2Val();
		} else if (qnVal.equals(getCust3Ns())) {
			return getCust3Val();
		} else if (qnVal.equals(getCust4Ns())) {
			return getCust4Val();
		}
		return null;

    }

    public String getValue(String localName) {
        return getValue(new QName(PropertyHelper.DEFAULT_QNAMESPACE, localName));
    }

    public void setValue(QName name, String value) {
    	String lp = name.getLocalPart();
    	if (PropertyHelper.DEFAULT_QNAMESPACE.equals(name.getNamespaceURI())) {
    		
    		if (		"getlastmodified".equals(lp)) {
    			setDavLastMod(Check.isEmpty(value) ? null : PropertyHelper.parseDate(value));
    			return;
    		} else if (	"getcontentlength".equals(lp)) {
    			setDavContentLen(Check.isEmpty(value) ?  null : Integer.valueOf(value));
    			return;
    		} else if (	"getetag".equals(lp)) {
    			setDavEtag(value);
    			return;
    		} else if (	"getcontenttype".equals(lp)) {
    			setDavContentType(value);
    			return;
    		} else if (	"getcontentlanguage".equals(lp)) {
    			setDavContentLang(value);
    			return;
    		} 
    		
    	} else if (PropertyHelper.ITENSIL_QNAMESPACE.equals(name.getNamespaceURI())) {
    		
    		if ( 		"modifier".equals(lp)) {
    			setIrModifier(value);
    			return;
    		} else if ( "tags".equals(lp)) {
    			setIrTags(value);
    			return;
    		} else if ( "style".equals(lp)) {
    			setIrStyle(value);
    			return;
    		} else if ( "description".equals(lp)) {
    			setIrDescription(value);
    			return;
    		} else if ( "keywords".equals(lp)) {
    			setIrKeywords(value);
    			return;
    		}
    	} else if (PropertyHelper.ITENSIL_ENTITY_QNAMESPACE.equals(name.getNamespaceURI())) {
    		
    		if	(		"recordId".equals(lp)) {
    			setIeRecordId(Check.isEmpty(value) ? 0 : Long.parseLong(value));
    			return;
    		} else if ( "browse0".equals(lp)) {
    			setIeBrowse0(value);
    			return;
    		} else if ( "browse1".equals(lp)) {
    			setIeBrowse1(value);
    			return;
    		} else if ( "browse2".equals(lp)) {
    			setIeBrowse2(value);
    			return;
    		} else if ( "browse3".equals(lp)) {
    			setIeBrowse3(value);
    			return;
    		} else if ( "browse4".equals(lp)) {
    			setIeBrowse4(value);
    			return;
    		} else if ( "browse5".equals(lp)) {
    			setIeBrowse5(value);
    			return;
    		} else if ( "browse6".equals(lp)) {
    			setIeBrowse6(value);
    			return;
    		} else if ( "browse7".equals(lp)) {
    			setIeBrowse7(value);
    			return;
    		} else if ( "browse8".equals(lp)) {
    			setIeBrowse8(value);
    			return;
    		} else if ( "browse9".equals(lp)) {
    			setIeBrowse9(value);
    			return;
    		} else if ( "browseA".equals(lp)) {
    			setIeBrowseA(value);
    			return;
    		} else if ( "browseB".equals(lp)) {
    			setIeBrowseB(value);
    			return;
    		} 
    	}
    	
    	String qnVal = name.toString();
    	
		if (qnVal.equals(getCust1Ns())) {
			if (value == null) setCust1Ns(null);
			setCust1Val(value);
			return;
		} else if (qnVal.equals(getCust2Ns())) {
			if (value == null) setCust2Ns(null);
			setCust2Val(value);
			return;
		} else if (qnVal.equals(getCust3Ns())) {
			if (value == null) setCust3Ns(null);
			setCust3Val(value);
			return;
		} else if (qnVal.equals(getCust4Ns())) {
			if (value == null) setCust4Ns(null);
			setCust4Val(value);
			return;
		}
		
		if (value == null) return;
		
		if (getCust1Ns() == null) {
			setCust1Ns(qnVal);
			setCust1Val(value);
			return;
		} else if (getCust2Ns() == null) {
			setCust2Ns(qnVal);
			setCust2Val(value);
			return;
		} else if (getCust3Ns() == null) {
			setCust3Ns(qnVal);
			setCust3Val(value);
			return;
		} else if (getCust4Ns() == null) {
			setCust4Ns(qnVal);
			setCust4Val(value);
			return;
		}
		
		log.warn("Could not add property: " + qnVal);
    }


    public void setValue(String localName, String value) {
        setValue(new QName(PropertyHelper.DEFAULT_QNAMESPACE, localName), value);
    }

    public void remove(QName name) {
    	setValue(name, null);
    }

    public void remove(String localName) {
        remove(new QName(PropertyHelper.DEFAULT_QNAMESPACE, localName));
    }

    public NodeVersion getVersion() {
        return this;
    }

    public Map<QName,String> getPropertyMap() {
    	QName names[] = getNames();
    	HashMap<QName,String> propMap = new HashMap<QName,String>(names.length);
    	for (QName qn : names) {
    		propMap.put(qn, getValue(qn));
    	}
        return propMap;
    }

    public void replaceProperties(NodeProperties properties) {
    	
    	// clear current
    	setDavContentType(null);
    	setDavContentLang(null);
    	setDavContentLen(null);
    	setDavEtag(null);
    	setDavLastMod(null);
    	
    	setIrModifier(null);
    	setIrDescription(null);
    	setIrKeywords(null);
    	setIrTags(null);
    	setIrStyle(null);
    	
    	setIeRecordId(0);
    	setIeBrowse0(null);
    	setIeBrowse1(null);
    	setIeBrowse2(null);
    	setIeBrowse3(null);
    	setIeBrowse4(null);
    	setIeBrowse5(null);
    	setIeBrowse6(null);
    	setIeBrowse7(null);
    	setIeBrowse8(null);
    	setIeBrowse9(null);
    	setIeBrowseA(null);
    	setIeBrowseB(null);
    	
    	setCust1Ns(null);
    	setCust1Val(null);
    	setCust2Ns(null);
    	setCust2Val(null);
    	setCust3Ns(null);
    	setCust3Val(null);
    	setCust4Ns(null);
    	setCust4Val(null);
    	
    	//set 
    	for (QName qn : properties.getNames()) {
    		setValue(qn, properties.getValue(qn));
    	}
    	
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Set<ContentEntity> getContentEntities() {
        return contentEntities;
    }

    public void setContentEntities(Set<ContentEntity> contentEntities) {
        this.contentEntities = contentEntities;
    }

	public String getCust1Ns() {
		return cust1Ns;
	}

	public void setCust1Ns(String cust1Ns) {
		this.cust1Ns = Check.maxLength(cust1Ns, 255);
	}

	public String getCust1Val() {
		return cust1Val;
	}

	public void setCust1Val(String cust1Val) {
		this.cust1Val = Check.maxLength(cust1Val, 255);
	}

	public String getCust2Ns() {
		return cust2Ns;
	}

	public void setCust2Ns(String cust2Ns) {
		this.cust2Ns = Check.maxLength(cust2Ns, 255);
	}

	public String getCust2Val() {
		return cust2Val;
	}

	public void setCust2Val(String cust2Val) {
		this.cust2Val = Check.maxLength(cust2Val, 255);
	}

	public String getCust3Ns() {
		return cust3Ns;
	}

	public void setCust3Ns(String cust3Ns) {
		this.cust3Ns = Check.maxLength(cust3Ns, 255);
	}

	public String getCust3Val() {
		return cust3Val;
	}

	public void setCust3Val(String cust3Val) {
		this.cust3Val = Check.maxLength(cust3Val, 255);
	}

	public String getCust4Ns() {
		return cust4Ns;
	}

	public void setCust4Ns(String cust4Ns) {
		this.cust4Ns = Check.maxLength(cust4Ns, 255);
	}

	public String getCust4Val() {
		return cust4Val;
	}

	public void setCust4Val(String cust4Val) {
		this.cust4Val = Check.maxLength(cust4Val, 255);
	}

	public String getDavContentLang() {
		return davContentLang;
	}

	public void setDavContentLang(String davContentLang) {
		this.davContentLang = davContentLang;
	}

	public Integer getDavContentLen() {
		return davContentLen;
	}

	public void setDavContentLen(Integer davContentLen) {
		this.davContentLen = davContentLen;
	}

	public String getDavContentType() {
		return davContentType;
	}

	public void setDavContentType(String davContentType) {
		this.davContentType = davContentType;
	}

	public String getDavEtag() {
		return davEtag;
	}

	public void setDavEtag(String davEtag) {
		this.davEtag = davEtag;
	}

	public Date getDavLastMod() {
		return davLastMod;
	}

	public void setDavLastMod(Date davLastMod) {
		this.davLastMod = davLastMod;
	}

	public String getIrDescription() {
		return irDescription;
	}

	public void setIrDescription(String val) {
		if (val != null && val.length() > 250) {
    		this.irDescription = val.substring(0,250) + "...";
    	} else {
    		this.irDescription = val;
    	}
	}

	public String getIrKeywords() {
		return irKeywords;
	}

	public void setIrKeywords(String irKeywords) {
		this.irKeywords = Check.maxLength(irKeywords, 255);
	}

	public String getIrModifier() {
		return irModifier;
	}

	public void setIrModifier(String irModifier) {
		this.irModifier = irModifier;
	}

	public String getIrStyle() {
		return irStyle;
	}

	public void setIrStyle(String irStyle) {
		this.irStyle = Check.maxLength(irStyle, 255);
	}

	public String getIrTags() {
		return irTags;
	}

	public void setIrTags(String irTags) {
		this.irTags = irTags;
	}
	
	public String getIeBrowse0() {
		return ieBrowse0;
	}

	public void setIeBrowse0(String ieBrowse0) {
		this.ieBrowse0 = Check.maxLength(ieBrowse0, 255);
	}

	public String getIeBrowse1() {
		return ieBrowse1;
	}

	public void setIeBrowse1(String ieBrowse1) {
		this.ieBrowse1 = Check.maxLength(ieBrowse1, 255);
	}

	public String getIeBrowse2() {
		return ieBrowse2;
	}

	public void setIeBrowse2(String ieBrowse2) {
		this.ieBrowse2 = Check.maxLength(ieBrowse2, 255);
	}

	public String getIeBrowse3() {
		return ieBrowse3;
	}

	public void setIeBrowse3(String ieBrowse3) {
		this.ieBrowse3 = Check.maxLength(ieBrowse3, 255);
	}

	public String getIeBrowse4() {
		return ieBrowse4;
	}

	public void setIeBrowse4(String ieBrowse4) {
		this.ieBrowse4 = Check.maxLength(ieBrowse4, 255);
	}

	public String getIeBrowse5() {
		return ieBrowse5;
	}

	public void setIeBrowse5(String ieBrowse5) {
		this.ieBrowse5 = ieBrowse5;
	}

	public String getIeBrowse6() {
		return ieBrowse6;
	}

	public void setIeBrowse6(String ieBrowse6) {
		this.ieBrowse6 = Check.maxLength(ieBrowse6, 255);
	}

	public String getIeBrowse7() {
		return ieBrowse7;
	}

	public void setIeBrowse7(String ieBrowse7) {
		this.ieBrowse7 = Check.maxLength(ieBrowse7, 255);
	}

	public String getIeBrowse8() {
		return ieBrowse8;
	}

	public void setIeBrowse8(String ieBrowse8) {
		this.ieBrowse8 = Check.maxLength(ieBrowse8, 255);
	}

	public String getIeBrowse9() {
		return ieBrowse9;
	}

	public void setIeBrowse9(String ieBrowse9) {
		this.ieBrowse9 = Check.maxLength(ieBrowse9, 255);
	}

	public String getIeBrowseA() {
		return ieBrowseA;
	}

	public void setIeBrowseA(String ieBrowseA) {
		this.ieBrowseA = Check.maxLength(ieBrowseA, 255);
	}

	public String getIeBrowseB() {
		return ieBrowseB;
	}

	public void setIeBrowseB(String ieBrowseB) {
		this.ieBrowseB = Check.maxLength(ieBrowseB, 255);
	}

	public long getIeRecordId() {
		return ieRecordId;
	}

	public void setIeRecordId(long ieRecordId) {
		this.ieRecordId = ieRecordId;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + (int) (id ^ (id >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final VersionEntity other = (VersionEntity) obj;
		if (id != other.id)
			return false;
		return true;
	}
    
    
}
