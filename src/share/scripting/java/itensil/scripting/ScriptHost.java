package itensil.scripting;

import org.mozilla.javascript.*;

import itensil.security.SecurityAssociation;
import itensil.security.User;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author ggongaware@itensil.com
 *
 */
public class ScriptHost implements Serializable {

    protected ScriptableObject scope;
    protected String name;
    protected HashMap<String, Object> defines;
    protected HostJSObject hostObj;

    protected transient Context ctx;
    
    protected static Script bootScript; 


    /**
     * @param name - of this host
     */
    
    public ScriptHost(String name) {
    	this(name, SecurityAssociation.getUser());
    }
    
    public ScriptHost(String name, Principal user) {

        initContext();
        scope = ctx.initStandardObjects();
        this.name = name;
        defines = new HashMap<String, Object>();
        hostObj = new HostJSObject(user);
        scope.defineProperty(
            "Host", hostObj,
            ScriptableObject.READONLY | ScriptableObject.PERMANENT);
        boot();
    }

    /**
     * Define a variable in the root scope
     * @param name
     * @param value
     */
    public void defineObject(String name, Object value) {
        defines.put(name, value);
        scope.defineProperty(
            name, value,
            ScriptableObject.READONLY | ScriptableObject.PERMANENT);
    }

    /**
     * @param script
     * @return
     * @throws ScriptError
     */
    public Object evaluate(String script) throws ScriptError {

        try {
            return ctx.evaluateString(scope, script, name, 0, null);
        } catch (JavaScriptException e) {
            throw new ScriptError(e);
        } catch (WrappedException we) {
            throw new ScriptError(we);
        } catch (EvaluatorException ee) {
            throw new ScriptError(ee);
        } catch (EcmaError ec) {
            throw new ScriptError(ec);
        }
    }

    /**
     * @param script
     * @return
     * @throws ScriptError
     */
    public boolean evaluateToBoolean(String script) throws ScriptError {
        return Context.toBoolean(evaluate(script));
    }

    /**
     * @param script
     * @return
     * @throws ScriptError
     */
    public double evaluateToNumber(String script) throws ScriptError {
        return Context.toNumber(evaluate(script));
    }


    /**
     * @param script
     * @return
     * @throws ScriptError
     */
    public String evaluateToString(String script) throws ScriptError {
        return Context.toString(evaluate(script));
    }

    /**
     * Attempt to clean out user defined variables
     */
    public void reset() {
        scope = ctx.initStandardObjects();
        scope.defineProperty(
            "Host", hostObj,
            ScriptableObject.READONLY | ScriptableObject.PERMANENT);
        boot();
        Iterator itr = defines.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry ent = (Map.Entry)itr.next();
            scope.defineProperty(
                (String)ent.getKey(), ent.getValue(),
                ScriptableObject.READONLY | ScriptableObject.PERMANENT);
        }
    }

    protected void initContext() {
        ctx = Context.getCurrentContext();
        if (ctx != null) {
        	Context.exit();
        }
        	
        ctx = Context.enter();
        // Use pure interpreter mode to allow for
        // observeInstructionCount(Context, int) to work
        ctx.setClassShutter(new Shutter());
    }

    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        in.defaultReadObject();
        initContext();
    }
    
    private void boot() {
    	if (bootScript == null) {
    		synchronized (ScriptHost.class) {
    			if (bootScript == null) {
			    	try {
			    		bootScript = ctx.compileReader(
								new InputStreamReader(
										ScriptHost.class.getResourceAsStream("boot.jsc")), "boot.jsc", 0, null);
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}
    			}
    		}
    	}
    	bootScript.exec(ctx, scope);
    }

    protected static class Shutter implements ClassShutter {

        // deny all non-provided java access
        public boolean visibleToScripts(String cls) {
        	if ("org.mozilla.javascript.EcmaError".equals(cls)) return true;
        	if ("org.mozilla.javascript.WrappedException".equals(cls)) return true;
        	if ("java.lang.String".equals(cls)) return true;
        	if ("java.lang.Object".equals(cls)) return true;
        	if ("com.sun.org.apache.xerces.internal.dom.DocumentImpl".equals(cls)) return true;
        	if (cls.startsWith("org.dom4j.")) return true;
        	if (cls.startsWith("itensil.") && cls.endsWith("Exception")) return true;
		if (cls.startsWith("com.hp.hpl.jena.")) return true;
            return false;
        }
    }
    
    public static class ItensilContext extends Context {
    	long expireTime;
    	
    	public void extendExpireTime(long addMillis) {
    		expireTime += addMillis;
    	}

    }
    
    protected static class ItensilContextFactory extends ContextFactory {
    	
    	// Override makeContext()
        protected Context makeContext()
        {
        	ItensilContext cx = new ItensilContext();
            // Use pure interpreter mode to allow for
            // observeInstructionCount(Context, int) to work
            cx.setOptimizationLevel(-1);
            // Make Rhino runtime to call observeInstructionCount
            // each 1000 bytecode instructions
            cx.setInstructionObserverThreshold(1000);
            return cx;
        }
        
        // Override observeInstructionCount(Context, int)
        protected void observeInstructionCount(Context cx, int instructionCount)
        {
        	ItensilContext icx = (ItensilContext)cx;
            long currentTime = System.currentTimeMillis();
            if (currentTime  > icx.expireTime) {
                
                // it is time to stop the script.
                // Throw Error instance to ensure that script will never
                // get control back through catch or finally.
                throw new Error("Script running too long");
            }
            if (instructionCount > 200000) {
            	throw new Error("Too many script calls");
            }
        }


    	
    	// Override doTopCall(Callable, Context, Scriptable scope, Scriptable thisObj, Object[] args)
        protected Object doTopCall(Callable callable,
                                   Context cx, Scriptable scope,
                                   Scriptable thisObj, Object[] args)
        {
        	ItensilContext icx = (ItensilContext)cx;
        	// Expire after 10 seconds Context creation time:
        	icx.expireTime = System.currentTimeMillis() + (10*1000);

            return super.doTopCall(callable, cx, scope, thisObj, args);
        }

    }
    
    static {
        // Initialize GlobalFactory with custom factory
        ContextFactory.initGlobal(new ItensilContextFactory());
    }
    

}
