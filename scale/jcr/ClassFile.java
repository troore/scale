package scale.jcr;

import java.io.*;
import java.util.zip.*;
import scale.common.Stack;
import java.util.Enumeration;

import scale.common.*;

/**
 * This class is used to both represent a Java class file and to read
 * that class file.
 * <p>
 * $Id: ClassFile.java,v 1.15 2007-10-04 19:58:15 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class ClassFile
{
  /**
   * The access masks.
   */
  public static final int ACC_PUBLIC       = 0x0001;
  public static final int ACC_PRIVATE      = 0x0002;
  public static final int ACC_PROTECTED    = 0x0004;
  public static final int ACC_STATIC       = 0x0008;
  public static final int ACC_FINAL        = 0x0010;
  public static final int ACC_SUPER        = 0x0020;
  public static final int ACC_SYNCHRONIZED = 0x0020;
  public static final int ACC_VOLATILE     = 0x0040;
  public static final int ACC_TRANSIENT    = 0x0080;
  public static final int ACC_NATIVE       = 0x0100;
  public static final int ACC_INTERFACE    = 0x0200;
  public static final int ACC_ABSTRACT     = 0x0400;

  private static char    pathSeparator = System.getProperty("path.separator").charAt(0);
  private static char    fileSeparator = System.getProperty("file.separator").charAt(0);
  private static HashMap<String, ZipFile> zips          = null; /* The jar and/or zip files opened. */
  /**
   * A running counter of the number of Java class files read.
   */
  public static int classesRead = 0;


  private int             magic;
  private int             minorVersion;
  private int             majorVersion;
  private int             accessFlags;
  private int             thisClass;
  private int             superClass;
  private int[]           interfaces;
  private CPInfo[]        constantPool;
  private FieldInfo[]     fields;
  private MethodInfo[]    methods;
  private AttributeInfo[] attributes;

  /**
   * Read in the specified Java class file.
   * @exception scale.common.InvalidException if the class file was
   * not read successfully
   */
  public ClassFile(String name) throws scale.common.InvalidException
  {
    DataInputStream reader = CFReader(name);

    try {
      magic             = reader.readInt();
      minorVersion      = reader.readUnsignedShort();
      majorVersion      = reader.readUnsignedShort();

      int constantPoolCount = reader.readUnsignedShort();

      constantPool = new CPInfo[constantPoolCount];
      constantPool[0] = null;
      for (int i = 1; i < constantPoolCount; i++) {
        CPInfo c = CPInfo.read(this, reader);
        constantPool[i] = c;
        if ((c instanceof LongCPInfo) || (c instanceof DoubleCPInfo))
          i++;
      }

      accessFlags     = reader.readUnsignedShort();
      thisClass       = reader.readUnsignedShort();
      superClass      = reader.readUnsignedShort();

      int interfacesCount = reader.readUnsignedShort();

      interfaces = new int[interfacesCount];
      for (int i = 0; i < interfacesCount; i++)
        interfaces[i] = reader.readUnsignedShort();

      int fieldsCount = reader.readUnsignedShort();

      fields = new FieldInfo[fieldsCount];
      for (int i = 0; i < fieldsCount; i++)
        fields[i] = FieldInfo.read(this, reader);

      int methodsCount = reader.readUnsignedShort();

      methods = new MethodInfo[methodsCount];
      for (int i = 0; i < methodsCount; i++)
        methods[i] = MethodInfo.read(this, reader);

      int attributesCount = reader.readUnsignedShort();

      attributes = new AttributeInfo[attributesCount];
      for (int i = 0; i < attributesCount; i++)
        attributes[i] = AttributeInfo.read(this, reader);

      classesRead++;

      reader.close();
    } catch (java.io.IOException ex) {
      ex.printStackTrace();
      throw new scale.common.InvalidException("Problem reading class file " + name);
    }
  }

  private DataInputStream CFReader(String className) throws scale.common.InvalidException
  {
    return CFReader((System.getProperty("java.class.path") +
                     pathSeparator +
                     System.getProperty("sun.boot.class.path")),
                    className);
  }

  private DataInputStream CFReader(String classPath,
                                   String className) throws scale.common.InvalidException
  {
    Vector<String> paths = new Vector<String>(3);

    // Split up the class path into paths.

    while (true) {
      int index = classPath.indexOf(pathSeparator);

      if (index < 0) {
        paths.addElement(classPath);
        break;
      }

      paths.addElement(classPath.substring(0, index));

      classPath = classPath.substring(index + 1);
    }

    // Go through each path looking for this class.

    int n = paths.size();
    for (int i = 0; i < n; i++) {
      String path = paths.elementAt(i);

      if (path.endsWith(".zip") || path.endsWith(".jar")) {
        String fileName = className.replace('.', '/') + ".class";

        if (zips == null)
          zips = new HashMap<String, ZipFile>(3);

        ZipFile  zipFile = zips.get(path);
        if (zipFile == null) {
          try {
            zipFile = new ZipFile(path);
            zips.put(path, zipFile);
          } catch(IOException e) {
            continue;
          }
        }
        ZipEntry entry   = zipFile.getEntry(fileName);

        if (entry == null) {
          continue;
        } else {
          try {
            DataInputStream s = new DataInputStream(zipFile.getInputStream(entry));
            //              System.out.println(path + "/" + fileName);
            return s;
          } catch(IOException e) {
            System.out.println("** " + path);
            e.printStackTrace();
            continue;
          }
        }
      } else { // Try loading class directly.
        StringBuffer buf = new StringBuffer(path);

        if (path.charAt(path.length() - 1) != fileSeparator)
          buf.append(fileSeparator);

        buf.append(className.replace('.', fileSeparator));
        buf.append(".class");

        String fullPath = buf.toString();

        try {
          File f = new File(fullPath);

          DataInputStream s = new DataInputStream(new FileInputStream(f));
          //      System.out.println(fullPath);
          return s;
        } catch(IOException e) {
          continue;
        }
      }
    }
    throw new scale.common.InvalidException("Class file " + className + " not found.");
  }

  /**
   * Return the magic field of the class file.
   */
  public int getMagic()
  {
    return magic;
  }

  /**
   * Return the minorVersion field of the class file.
   */
  public int getMinorVersion()
  {
    return minorVersion;
  }

  /**
   * Return the majorVersion field of the class file.
   */
  public int getMajorVersion()
  {
    return majorVersion;
  }

  /**
   * Return the accessFlags field of the class file.
   */
  public int getAccessFlags()
  {
    return accessFlags;
  }

  /**
   * Return the thisClass field of the class file.
   */
  public int getThisClass()
  {
    return thisClass;
  }

  /**
   * Return the superClass field of the class file.
   */
  public int getSuperClass()
  {
    return superClass;
  }

  /**
   * Return the number of constant pool entries.
   */
  public int numCPEntries()
  {
    return constantPool.length;
  }

  /**
   * Return the constant pool entry specified.
   */
  public CPInfo getCP(int index)
  {
    return constantPool[index];
  }

  /**
   * Return the name of entry in the constant pool.
   * @param index is an index into the constant pool of a Utf-8 string
   */
  public String getName(int index)
  {
    return ((Utf8CPInfo) getCP(index)).getString();
  }

  /**
   * Return the String from the constant pool.
   * @param index is an index into the constant pool of a Utf-8 string
   */
  public String getString(int index)
  {
    return ((Utf8CPInfo) getCP(index)).getString();
  }

  /**
   * Return the interface index specified.
   */
  public int getInterface(int index)
  {
    return interfaces[index];
  }

  /**
   * Return an array of interface indexs into the constant pool.
   */
  public int[] getInterfaces()
  {
    return interfaces.clone();
  }

  /**
   * Return the field entry specified.
   */
  public FieldInfo getField(int index)
  {
    return fields[index];
  }

  /**
   * Return an array of the FieldInfo structures for the class.
   */
  public FieldInfo[] getFields()
  {
    return fields.clone();
  }

  /**
   * Return the method entry specified.
   */
  public MethodInfo getMethod(int index)
  {
    return methods[index];
  }

  /**
   * Return an array of the MethodInfo structures for the class.
   */
  public MethodInfo[] getMethods()
  {
    return methods.clone();
  }

  /**
   * Return the attribute entry specified.
   */
  public AttributeInfo getAttribute(int index)
  {
    return attributes[index];
  }

  /**
   * Return an array of the AttributeInfo structures for the class.
   */
  public AttributeInfo[] getAttributes()
  {
    return attributes.clone();
  }

  /**
   * Adds all the class names referenced by this class to the stack.
   */
  public void addRefClasses(Stack<String> wl)
  {
    if (superClass != 0) {
      int nameIndex = ((ClassCPInfo) constantPool[superClass]).getNameIndex();
      wl.push(((Utf8CPInfo) constantPool[nameIndex]).getString());
    }
    for (int i = 0; i < constantPool.length; i++)
      if (constantPool[i] instanceof ClassCPInfo) {
        int    nameIndex = ((ClassCPInfo) constantPool[i]).getNameIndex();
        String s         = ((Utf8CPInfo) constantPool[nameIndex]).getString();
        if (s.startsWith("[")) { // Skip arrays.
          continue;
        }
        wl.push(s);
      }

    for (int i = 0; i < methods.length; i++) {
      MethodInfo mi = methods[i];
      int nameIndex = mi.getDescriptorIndex();
      String s = ((Utf8CPInfo) constantPool[nameIndex]).getString();
      getJavaTypes(s, wl);
    }

    for (int i = 0; i < fields.length; i++) {
      FieldInfo fi = fields[i];
      int nameIndex = fi.getDescriptorIndex();
      String s = ((Utf8CPInfo) constantPool[nameIndex]).getString();
      getJavaTypes(s, wl);
    }
  }

  private void getJavaTypes(String descriptor, Stack<String> wl)
  {
    int     index = 0;

    while (index < descriptor.length()) {
      char c = descriptor.charAt(index++);
      switch (c) {
      case '(':
      case ')':
      case '[':
      case 'V':
      case 'B':
      case 'C':
      case 'D':
      case 'F':
      case 'I':
      case 'J':
      case 'S':
      case 'Z':
        break;
      case 'L':
        int n = descriptor.indexOf(';', index);
        wl.push(descriptor.substring(index, n));
        index = n;
        break;
      }
    }
  }

  /**
   * Closes any zip or jar files opened to read any class.
   */
  public static void closeZipFiles()
  {
    if (zips == null)
      return;

    Enumeration<String> ez = zips.keys();
    while (ez.hasMoreElements()) {
      String name = ez.nextElement();
      System.out.println("/* Closing " + name + " */");
      ZipFile zf = zips.get(name);
      try {
        zf.close();
      } catch(java.io.IOException ex) {
      }
    }
    zips = null;
  }
}
