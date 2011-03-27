package itensil.security;

public class RelativeGroup implements Group {
	
	public static String UID = "AAAAAAAAAAAArelat000";
	
	public static String gid;
	
	public RelativeGroup() {
		this(0);
	}
	
	public RelativeGroup(int index) {
		gid = String.format(UID.substring(0, 17) + "%1$03d", index);
	}

	public String getGroupId() {
		return UID;
	}

	public String getSimpleName() {
		return "[Relative]";
	}

	public long timeStamp() {
		return 0;
	}

	public String getName() {
		return "[Relative]";
	}

    public static boolean isRelative(Group grp) {
        return grp.getGroupId().startsWith(UID.substring(0, 17));
    }
    
}
