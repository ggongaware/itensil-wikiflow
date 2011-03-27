package itensil.scripting;

import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.WrappedException;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.RhinoException;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ScriptError extends Exception {

    public ScriptError(JavaScriptException jse) {
       super("[JavaScript] " + jse.getMessage());
    }
    public ScriptError(EvaluatorException jse) {
       super("[JavaScript] " + jse.getMessage());
    }

    public ScriptError(WrappedException we) {
       super("[JavaScript-Java] " + we.getMessage());
    }

    public ScriptError(RhinoException re) {
        super("[JavaScript] " + re.getMessage());
    }

}
