package itensil.workflow.activities.signals;

import java.io.Serializable;

public enum SignalType implements Serializable {
	ACTIVITY_ALERT(001),
	SIGNAL_ALERT(002),
	SIGNAL_INTERPROCESS_DEF1(003);

	//public static final String ACTIVITY_ALERT_STRINGVALUE = ACTIVITY_ALERT.toString();
	public static final String SIGNAL_ALERT_STRINGVALUE = SIGNAL_ALERT.toString();
	public static final String SIGNAL_INTERPROCESS_DEF1_STRINGVALUE = SIGNAL_INTERPROCESS_DEF1.toString();

    private final int type; 

    SignalType(int type) {
        this.type = type;
    }
    private int type()   { return type; }
}

