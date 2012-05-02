package scale.j2s;

import java.util.Enumeration;
import scale.jcr.*;
import scale.common.*;
import scale.clef.decl.*;
import scale.clef.type.*;

/**
 * This class associates various information about a Java class.
 * <p>
 * $Id: ClassStuff.java,v 1.14 2007-01-04 17:01:16 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ClassStuff
{
  /**
   * The "fixed" name for the class.
   */
  public String name = null;    
  /**
   * The Java class file for the class.
   */
  public ClassFile cf = null;      
  /**
   * The Clef declaration for the class.
   */
  public TypeDecl td = null;      
  /**
   * The Clef type of the class (pointer type).
   */
  public PointerType type = null; 
  /**
   * Variable defining class vtable.
   */
  private VariableDecl vd = null;   
  /**
   * Variable defining the class variable.
   */
  private VariableDecl cvd = null;   
  /**
   * Information about the vtable entries.
   */
  private Vector<CM> vtable = null;  
  /**
   * Information about the interfaces implemented by the class.
   */
  private Vector<String> interfaces;
  /**
   * This class' super class.
   */
  private ClassStuff scs = null;

  /**
   * True if the code for this class should be converted to Scribble.
   */
  public boolean include = false;

  /**
   * @param name is the <i>fixed</i> name for the class.
   * @param cf is the class' class file structure
   */
  public ClassStuff(String name, ClassFile cf)
  {
    this.name = name;
    this.cf   = cf;
    this.td   = new TypeDecl(name, null);

    IncompleteType inct = new IncompleteType();
    Type           rt   = RefType.create(inct, this.td);

    this.td.setType(rt);

    this.type = PointerType.create(rt);
  }

  /**
   * Return the VariableDecl for the class' virtual table.
   */
  public VariableDecl getVTableDecl(Type vtableType)
  {
    if (vd == null) {
      vd   = new VariableDecl(name + "_vtable", vtableType);
      vd.setVisibility(Visibility.EXTERN);
    }
    return vd;
  }

  /**
   * Specify the VariableDecl for the class' virtual table.
   */
  public void setVTableDecl(VariableDecl vd)
  {
    this.vd = vd;
  }

  /**
   * Return the VariableDecl for the class' class structure.
   */
  public VariableDecl getClassDecl(Type classType)
  {
    if (cvd == null) {
      cvd   = new VariableDecl(name + "_class", classType);
      cvd.setVisibility(Visibility.EXTERN);
    }
    return cvd;
  }

  /**
   * Specify the VariableDecl for the class' class structure.
   */
  public void setClassDecl(VariableDecl vd)
  {
    this.cvd = vd;
  }

  /**
   * Create the vector of (class file, method name) pairs for methods
   * that should be in the class' virtual table.
   */
  private void makeVTable()
  {
    // Record the information for the virtual method pointer table

    vtable = new Vector<CM>(10);
    if (scs != null) {
      Enumeration<CM> ef = scs.getVirtualMethods();
      while (ef.hasMoreElements())
        vtable.addElement(ef.nextElement());
    }

    MethodInfo[] methods = cf.getMethods();
    for (int i = 0; i < methods.length; i++) {
      MethodInfo method = methods[i];
      String     mName  = cf.getName(method.getNameIndex());
      int        acc    = method.getAccessFlags();

      if ((mName.charAt(0) != '<') && (0 == (acc & ClassFile.ACC_PRIVATE)) && (0 == (acc & ClassFile.ACC_STATIC))) {
        String descriptor = ((Utf8CPInfo) cf.getCP(method.getDescriptorIndex())).getString();
        int    len        = vtable.size();
        CM     p          = new CM(cf, method);
        int    j;
        for (j = 0; j < len; j++) {
          CM         sp          = vtable.elementAt(j);
          ClassFile  scf         = sp.classFile;
          MethodInfo smethod     = sp.methodInfo;
          String     sdescriptor = ((Utf8CPInfo) scf.getCP(smethod.getDescriptorIndex())).getString();
          String     smName      = scf.getName(smethod.getNameIndex());
          if (smName.equals(mName) && sdescriptor.equals(descriptor))
            break;
        }
        if (j < len)
          vtable.setElementAt(p, j);
        else
          vtable.addElement(p);
      }
    }
  }

  /**
   * Return an enumeration of pairs of (class file, method name) for
   * the methods in the class' virtual method table.
   */
  public Enumeration<CM> getVirtualMethods()
  {
    if (vtable == null)
      makeVTable();
    return vtable.elements();
  }

  /**
   * Return the number of methods in the class' virtual method table.
   */
  public int numMethods()
  {
    if (vtable == null)
      makeVTable();
    return vtable.size();
  }

  /**
   * Make a vector of the Interface names used by the class.
   */
  private void makeInterfaces()
  {
    interfaces = new Vector<String>(10); // Record the information for the interface pointer table
    int[] ifa = cf.getInterfaces();
    for (int i = 0; i < ifa.length; i++) {
      String     icn = cf.getName(((ClassCPInfo) cf.getCP(ifa[i])).getNameIndex());
      interfaces.addElement(icn);
    }
  }

  /**
   * Return an enumeration of the names of the interfaces implemented
   * by the class.
   */
  public Enumeration<String> getInterfaces()
  {
    if (interfaces == null)
      makeInterfaces();
    return interfaces.elements();
  }

  /**
   * Return the number of interfaces implemented by the class.
   */
  public int numInterfaces()
  {
    if (interfaces == null)
      makeInterfaces();
    return interfaces.size();
  }

  /**
   * Specify this class' super class.
   */
  public void setSuperClass(ClassStuff scs)
  {
    this.scs = scs;
  }

  /**
   * Return this class' super class.
   */
  public ClassStuff getSuperClass()
  {
    return scs;
  }

  /**
   * Return the index in the class' virtual method table of the
   * specified method.
   */
  public int findVirtualMethod(String mName, String descriptor)
  {
    if (vtable == null)
      makeVTable();

    int n = vtable.size();
    for (int i = 0; i < n; i++) {
      CM         p    = vtable.elementAt(i);
      ClassFile  cf   = p.classFile;
      MethodInfo info = p.methodInfo;
      String     name = ((Utf8CPInfo) cf.getCP(info.getNameIndex())).getString();
      if (!name.equals(mName))
        continue;
      String     desc = ((Utf8CPInfo) cf.getCP(info.getDescriptorIndex())).getString();
      if (desc.equals(descriptor))
        return i;
    }
    return -1;
  }

  /**
   * Associate a class file with a method.
   */
  public static class CM
  {
    public ClassFile  classFile;
    public MethodInfo methodInfo;

    public CM(ClassFile classFile, MethodInfo methodInfo)
    {
      this.classFile = classFile;
      this.methodInfo = methodInfo;
    }
  }
}

