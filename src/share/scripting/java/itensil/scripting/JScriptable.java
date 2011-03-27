package itensil.scripting;

import org.mozilla.javascript.Scriptable;

/**
 * @author ggongaware@itensil.com
 *
 */
public interface JScriptable {
    public Scriptable getScriptable() throws ScriptError;
}
