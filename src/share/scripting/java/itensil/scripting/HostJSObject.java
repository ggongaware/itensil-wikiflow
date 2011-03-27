package itensil.scripting;

import java.security.Principal;

import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.ScriptableObject;


/**
 * @author ggongaware@itensil.com
 *
 */
public class HostJSObject extends ScriptableObject {
	
	Principal curUser;
	
	public HostJSObject(Principal curUser) {
		this.curUser = curUser;
		String funcs[] = {"getCurrentUser"};
        try {
            this.defineFunctionProperties(
                funcs,
                HostJSObject.class,
                ScriptableObject.PERMANENT |
                ScriptableObject.READONLY);
            
            this.defineProperty("version", "1.7", READONLY | PERMANENT);
            
        } catch (RhinoException e) {
            e.printStackTrace();
        }
        sealObject();
	}

    public String getClassName() {
        return "Host";
    }

    public Principal getCurrentUser() {
        return curUser;
    }

}
