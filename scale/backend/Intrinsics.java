package scale.backend;

import scale.common.*;
import scale.clef.type.*;
import scale.score.expr.*;
import java.util.HashMap;

/**
 * This class represents a target independent implementation for
 * compiler intrinsics.
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA <br>All Rights Reserved. <br>
 * <p>
 * Intrinsics are invoked dynamically using Java reflection and are
 * expected to be found inside the callers Generator instance
 * (i.e. AlphaGenerator).  The method invoked to implement the
 * intrinsic is set at the time an intrinsic is installed.  By default
 * the method called is set to the same name as the external function
 * name being turned into an intrinsic.  See installIntrinsic() below
 * for more details.
 * <p>
 * The current implementation assumes that before a function call is
 * replaced with an intrinsic, the arguments to the function call have
 * been setup exactly as they would have been if the call took place.
 * This allows the method which implements an intrinsic to make
 * assumptions about where arguments reside.
 */
public abstract class Intrinsics 
{
  private static int createdCount = 0; // A count of all the instances of this class that were created.

  private static final String[] stats = { "created" };

  static {
    Statistics.register("scale.backend.Intrinsics", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created() 
  {
    return createdCount;
  }

  /**
   * This is a hook to the instance of Generator that instantiated this class.
   * This is used to invoke an intrinsic method dynamically through reflection.
   */
  private Generator gen;
   
  /**
   * This is the Generator class for reflection, ex. AlphaGenerator.class.  
   */
  private Class<Generator> c;

  /**
   * This is a hashmap of all installed intrinsics.
   */
  private HashMap<String, Prototype> intrinsics = new HashMap<String, Prototype>();

  /**
   * This represents a function prototype and is used to record the signature of
   * an intrinsic and the method that implements the intrinsic.
   */
  private static class Prototype 
  {
    String methodName;
    Type[] args;
    Type   rt;
    /**
     * @param args the parameters to the intrinsic
     * @param rt the return type
     */
    private Prototype(String methodName, Type[] args, Type rt) 
    {
      this.methodName = methodName;
      this.args       = args;
      this.rt         = rt;
    }
  }

  /**
   * Constructor used to instantiate a new intrinsic handler.
   * @param gen the instance of Generator that instantiated this class.
   * @param c the class type of Generator, ex. AlphaGenerator.class.
   */
  protected Intrinsics(Generator gen, Class<Generator> c) 
  {
    this.gen = gen;
    this.c   = c;
    createdCount++;
  }

  /**
   * Called by a backend wishing to install a compiler intrinsic.  This version
   * will set the internal method name to be the same as the external intrinsic.
   * ex. if __builtin_abs() is the external function name, then __builtin_abs()
   * will be the method called in Generator to implement the intrinsic. 
   * @param name the function name, i.e. "abs"
   * @param args the parameters to the function
   * @param rt the return type
   */
  protected void installIntrinsic(String name, Type[] args, Type rt) 
  {
    installIntrinsic(name, args, rt, name);
  }

  /**
   * Called by a backend wishing to install a compiler intrinsic.
   * This version will set the internal method name called to
   * implement the intrinsic to "methodName".  ex. if __builtin_abs()
   * is the external function name, and "absIntrinsic" is passed as
   * "methodName", absIntrinsic() will be called in Generator to
   * implement the intrinsic.
   * @param name the function name, i.e. "abs"
   * @param args the parameters to the function
   * @param rt the return type
   * @param methodName the name of the method that implements this
   * intrinsic in the compiler
   */
  protected void installIntrinsic(String name, Type[] args, Type rt, String methodName) 
  {
    intrinsics.put(name, new Prototype(methodName, args, rt));
  }
   
  /**
   * Determines if a function call has an equivalent compiler intrinsic.
   * @param name the function name, i.e. "abs"
   * @param args the parameters to the function
   * @param rt the return type
   * @return the method used to implement the intrinsic, or null if
   * there is no intrinsic
   */
  private Prototype getIntrinsic(String name, Expr[] args, Type rt) 
  {
    Prototype p = intrinsics.get(name);
    if (p == null)
      return null;

    // check that there are the same number of arguments
    if (args.length != p.args.length)
      return null;

    // check the that the type of each argument matches
    for (int i = 0; i < args.length; i++) {
      Type t1 = p.args[i];
      Type t2 = args[i].getCoreType();
      if (!t1.equivalent(t2)) {
        return null; 
      }
    }

    // check that the return types match
    if (!rt.equivalent(p.rt))
      return null;

    return p;
  }

  /**
   * Called by a backend to convert a function call into an intrinsic.
   * @param name the function name, i.e. "abs"
   * @param args the parameters to the function
   * @param rt the return type
   * @return true if the function call was converted into an intrinsic.
   */
  public boolean invokeIntrinsic(String name, Expr[] args, Type rt) 
  {
    /*Class[] parameterTypes = new Class[] {null};
      Object[] arguments = new Object[] {null};*/
    Class[]  parameterTypes = null;
    Object[] arguments      = null;
     
    // check if this function has an equivalent intrinsic
    Prototype p = getIntrinsic(name, args, rt);
    if (p == null)
      return false;
      
    String methodName = p.methodName; 
      
    try {
      java.lang.reflect.Method intrinsic = c.getMethod(methodName, parameterTypes);
      Object retObj = intrinsic.invoke(gen, arguments);
      if (retObj != null) {
        throw new scale.common.InternalError("Compiler intrinsics must not return a value!");
      }
      return true;
    } catch (java.lang.Exception ex) {
      System.out.println("** Cannot invoke intrinsic : " + name + " : " + ex);
    }
    return false;
  }

}
