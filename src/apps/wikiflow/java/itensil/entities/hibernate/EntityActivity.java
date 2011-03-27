package itensil.entities.hibernate;

import java.io.Serializable;
import java.util.Date;

import itensil.repository.hibernate.NodeEntity;
import itensil.workflow.activities.state.Activity;

public class EntityActivity implements Serializable {
	
	Activity activity;
	String entityId;
	String name;
	long recordId;
	Date createTime;
	
	public EntityActivity() {
		
	}
	
	public void initNew() {
		createTime = new Date();
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getEntityId() {
		return entityId;
	}

	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getRecordId() {
		return recordId;
	}

	public void setRecordId(long recordId) {
		this.recordId = recordId;
	}

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((activity == null) ? 0 : activity.hashCode());
		result = PRIME * result + ((entityId == null) ? 0 : entityId.hashCode());
		result = PRIME * result + ((name == null) ? 0 : name.hashCode());
		result = PRIME * result + (int) (recordId ^ (recordId >>> 32));
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
		final EntityActivity other = (EntityActivity) obj;
		if (activity == null) {
			if (other.activity != null)
				return false;
		} else if (!activity.equals(other.activity))
			return false;
		if (entityId == null) {
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (recordId != other.recordId)
			return false;
		return true;
	}

}
