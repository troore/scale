package scale.j2s;

import java.util.Enumeration;
import scale.jcr.*;
import scale.common.*;
import scale.clef.*;
import scale.callGraph.*;
import scale.clef.decl.*;
import scale.clef.type.*;
import scale.clef.expr.*;

/**
 * This class maintains the global (inter-class) information used
 * while converting Java byte codes to Scribble.
 * <p>
 * $Id: Java2Scribble.java,v 1.29 2007-10-04 19:58:13 burrill Exp $
 * <p>
 * Copyright 2008 by the
 * <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 */
public class Java2Scribble
{
  public static final int CLASSTYPE     = 0;
  public static final int INTERFACETYPE = 1;
  public static final int ARRAYTYPE     = 2;

  private static HashMap<String, TypeDecl>      arrayMap;   // Map from array    name to array type declaration.
  private static HashMap<String, ClassStuff>    classMap;   // Map from class    name to class stuff.
  private static HashMap<String, ProcedureDecl> procedures; // Map from method   name to ProcedureDecl
  private static HashMap<String, String>        names;      // Map from original name to fixed name.
  private static HashMap<String, VariableDecl>  stringMap;  // Map from string to variable defining it.

  private HashMap<String, Declaration> topGlobals; // Declarations to add at the top level.
  private Vector<Declaration>          topInOrder; // Declarations to add at the top level in order of declaration.
  private UniqueName    un;         // For generating string initializer variable names.

  private Type          classType;           // The class structure.
  private TypeDecl      classTypeDecl;
  private Type          classTypeRef;
  private Type          classpType;          // Pointer to the class structure.

  private Type          interfaceType;       // The interface structure.
  private TypeDecl      interfaceTypeDecl;
  private Type          interfaceTypeRef;
  private Type          interfacepType;      // Pointer to the interface structure.

  private Type          arrayHeaderType;     // The array header structure.
  private TypeDecl      arrayHeaderTypeDecl;
  private Type          arrayHeaderTypeRef;
  private Type          arrayHeaderpType;    // Pointer to the array header structure.

  private Type          vtableType;          // Generic vtable structure.
  private TypeDecl      vtableTypeDecl;
  private Type          vtableTypeRef;
  private Type          vtablepType;         // Pointer to the generic vtable structure.

  private Type          exceptionEntryType;     // Generic exceptionEntry structure.
  private TypeDecl      exceptionEntryTypeDecl;
  private Type          exceptionEntryTypeRef;
  private Type          exceptionEntrypType;    // Pointer to the generic exceptionEntry structure.

  private ProcedureType ftnType;             // Generic function type.
  private TypeDecl      ftnTypeDecl;
  private Type          ftnTypeRef;
  private Type          ftnpType;
  private Type          cFtnpType;

  /**
   * The Scale representation for the Java byte type.
   */
  public static final IntegerType byteType = SignedIntegerType.create(8);
  /**
   * The Scale representation for the Java short type.
   */
  public static final IntegerType shortType = SignedIntegerType.create(16);
  /**
   * The Scale representation for the Java int type.
   */
  public static final IntegerType intType = SignedIntegerType.create(32);
  /**
   * The Scale representation for the Java long type.
   */
  public static final IntegerType longType = SignedIntegerType.create(64);
  /**
   * The Scale representation for the Java float type.
   */
  public static final FloatType floatType = FloatType.create(32);
  /**
   * The Scale representation for the Java double type.
   */
  public static final FloatType doubleType = FloatType.create(64);
  /**
   * The Scale representation for the Java char type.
   */
  public static final CharacterType charType = CharacterType.create(CharacterType.cUnicode);
  /**
   * The Scale representation for a pointer to the Java char type.
   */
  public static final PointerType charpType = PointerType.create(charType);
  /**
   * The Scale representation for a pointer to the Java char type.
   */
  public static final PointerType cCharpType = PointerType.create(RefType.create(charType, RefAttr.Const));
  /**
   * The Scale representation for a pointer to the Java int type.
   */
  public static final PointerType intpType = PointerType.create(intType);
  /**
   * The Scale representation for the unspecified pointer type.
   */
  public static final PointerType voidp = PointerType.create(VoidType.type);
  /**
   * Map from Java type specifier (int) to the Scale type.
   */
  public static final Type[] typeMap = {
    voidp, null, null, null,
    BooleanType.type, charType, floatType,
    doubleType, byteType, shortType, intType,
    longType};

  /**
   * A procedure that constructs a String from an array of shorts.
   */
  public ProcedureDecl createStringProc;  
  /**
   * Of type pointer to the String class.
   */
  public Type stringpType;       
  /**
   * A procedure that finds the address of an interface method.
   */
  public ProcedureDecl findIntMethodProc; 
  /**
   * A procedure that creates an exception.
   */
  public ProcedureDecl makeExceptionProc;
  /**
   * A procedure that maps an exception to an index into the methods exception table.
   */
  public ProcedureDecl lookupExceptionProc;
  /**
   * A procedure that checks if a is an instance of b
   */
  public ProcedureDecl instanceOfProc;
  /**
   * The global exception variable.  This variable is checked after each method call.
   * If it is non-null, an exception has occured.
   */
  public VariableDecl globalExceptionVariable;
  /**
   * The literal for -1.
   */
  public final IntLiteral intm1;
  /**
   * The literal for 0.
   */
  public final IntLiteral int0;
  /**
   * The literal for 1.
   */
  public final IntLiteral int1;
  /**
   * The literal for 2.
   */
  public final IntLiteral int2;
  /**
   * The literal for 3.
   */
  public final IntLiteral int3;
  /**
   * The literal for 4.
   */
  public final IntLiteral int4;
  /**
   * The literal for 5.
   */
  public final IntLiteral int5;
  /**
   * The literal for 0.0.
   */
  public final FloatLiteral float0;
  /**
   * The literal for 1.0.
   */
  public final FloatLiteral float1;
  /**
   * The literal for 2.0.
   */
  public final FloatLiteral float2;
  /**
   * The literal for null.
   */
  public final IntLiteral nil;

  public Java2Scribble()
  {
    topGlobals  = new HashMap<String, Declaration>(203);
    topInOrder  = new Vector<Declaration>(200);
    un          = new UniqueName("__sinit_");
    intm1       = LiteralMap.put(-1, intType);
    int0        = LiteralMap.put(0, intType);
    int1        = LiteralMap.put(1, intType);
    int2        = LiteralMap.put(2, intType);
    int3        = LiteralMap.put(3, intType);
    int4        = LiteralMap.put(4, intType);
    int5        = LiteralMap.put(5, intType);
    float0      = LiteralMap.put(0.0, floatType);
    float1      = LiteralMap.put(1.0, floatType);
    float2      = LiteralMap.put(2.0, floatType);
    nil         = LiteralMap.put(0, voidp);
    defineStructures();
    defineFunctions();
  }

  public void convertClass(ClassStuff cs, CallGraph cg)
  {
    ClassFile    cf      = cs.cf;
    MethodInfo[] methods = cf.getMethods();

    cs.include = true;
    addTopGlobal(cs.td);

    cg.recordRoutine(createStringProc);
    cg.recordRoutine(findIntMethodProc);

    cs.setClassDecl(makeClassObject(cs));
    createVtable(cs);

    for (int mi = 0; mi < methods.length; mi++) {
      MethodInfo    method     = methods[mi];
      String        mName      = cf.getName(method.getNameIndex());
      int           acc        = method.getAccessFlags();
      String        descriptor = ((Utf8CPInfo) cf.getCP(method.getDescriptorIndex())).getString();
      ClassCPInfo   ccpi       = (ClassCPInfo) cf.getCP(cf.getThisClass());
      String        m          = genMethodName(cf.getName(ccpi.getNameIndex()), mName, descriptor);
      ProcedureDecl pd         = getProcedureDecl(cs.type, m, descriptor, 0 != (acc & ClassFile.ACC_STATIC));
      pd.setVisibility((0 != (acc & ClassFile.ACC_PRIVATE)) ? Visibility.FILE : Visibility.GLOBAL);

      ScribbleGen codeGen = new ScribbleGen(this, cs, pd, cg);
      codeGen.generate(method);

      if (mName.equals("main")) {
	cg.recordRoutine(pd);
        cg.setMain(pd);
      }
    }
  }

  /**
   * Return the Java type specifier for the Scale type.  Index 0 is
   * used for all address types.
   */
  public int getTypeSpecifier(Type type)
  {
    Type t = type.getCoreType();
    for (int i = 0; i < typeMap.length; i++)
      if (t == typeMap[i])
        return i;
    return 0;
  }

  /**
   * Return an enumeration of all the classes.
   * @see ClassStuff
   */
  public Enumeration<ClassStuff> getClasses()
  {
    if (classMap == null)
      return new EmptyEnumeration<ClassStuff>();

    return classMap.elements();
  }

  /**
   * Return the top level Declaration for the specified name.
   */
  public Declaration getGlobal(String name)
  {
    return topGlobals.get(name);
  }

  /**
   * Return the class information for the specified class.
   * @param cname is the name of the class
   */
  public ClassStuff getClass(String cname)
  {
    if (classMap == null)
      classMap = new HashMap<String, ClassStuff>(203);

    String     fixed = fixName(cname);
    ClassStuff cs    = classMap.get(fixed);
    if (cs == null) {
      try {
        if (Debug.debug(1))
          System.out.println("Reading " + cname);
        ClassFile  cf = new ClassFile(cname);
        cs = new ClassStuff(fixed, cf);
      } catch(scale.common.InvalidException ex) {
        ex.printStackTrace();
        System.exit(1);
      }
      classMap.put(fixed, cs);
      defineClass(cs);
    }
    return cs;
  }

  /**
   * Convert from Java method access specifier to Scale access
   * specifier.
   */
  public Accessibility getAccess(int accessFlags)
  {
    Accessibility acc = Accessibility.PRIVATE;
    if (0 != (accessFlags & ClassFile.ACC_PUBLIC))
      acc = Accessibility.PUBLIC;
    else if (0 != (accessFlags & ClassFile.ACC_PROTECTED))
      acc = Accessibility.PROTECTED;
    return acc;
  }

  /**
   * Create the VariableDecl for the static field of a Java class.
   */
  private VariableDecl defineGlobalVar(ClassFile cf, FieldInfo field, String vname, boolean isRoot)
  {
    int    access       = field.getAccessFlags();
    Object initialValue = null;

    if (isRoot) {
      AttributeInfo[] attributes = field.getAttributes();
      for (int j = 0; j < attributes.length; j++) {
        AttributeInfo attribute = attributes[j];
        if (attribute instanceof ConstantValueAttribute) {
          initialValue = getLiteral(cf, ((ConstantValueAttribute) attribute).getConstantValueIndex());
        }
      }
    }

    Type vt = getClefType(cf.getString(field.getDescriptorIndex()));

    if ((access & ClassFile.ACC_FINAL) != 0)
      vt = RefType.create(vt, RefAttr.Const);

    VariableDecl vd = new VariableDecl(vname, vt, (Expression) initialValue);

    if (isRoot) {
      if ((access & ClassFile.ACC_PUBLIC) == 0)
        vd.setVisibility(Visibility.FILE);
    } else
      vd.setVisibility(Visibility.EXTERN);

    return vd;
  }

  /**
   * Return the VariableDecl for the static field of a Java class.
   * @param cs specifies the class with the static field
   * @param fname is the name of the field
   */
  public VariableDecl getGlobalVar(ClassStuff cs, String fname)
  {
    String       cname = cs.name;
    String       vname = cname + '_' + fname;
    VariableDecl vd    = (VariableDecl) getGlobal(vname);

    if (vd != null)
      return vd;

    boolean     isRoot = cs.include;
    ClassFile   cf     = cs.cf;
    FieldInfo[] fields = cf.getFields();

    for (int i = 0; i < fields.length; i++) {
      FieldInfo field  = fields[i];
      int       access = field.getAccessFlags();
      String    fName  = cf.getName(field.getNameIndex());

      if ((access & ClassFile.ACC_STATIC) == 0)
        continue;

      if (fName.equals(fname)) {
        vd = defineGlobalVar(cf, field, vname, isRoot);
        break;
      }
    }

    assert (vd != null) : "Static variable " + vname + " not found.";

    addTopGlobal(vd);

    return vd;
  }

  /**
   * Return the Scale Type for the Java class.
   */
  private TypeDecl defineClass(ClassStuff cs)
  {
    String     fixed = cs.name;
    TypeDecl   td    = cs.td;
    ClassStuff scs   = null;

    if (td.getType().getCoreType() != null)
      return td;

    ClassFile         cf         = cs.cf;
    Vector<FieldDecl> cFields    = new Vector<FieldDecl>(10);
    int               superClass = cf.getSuperClass();
    boolean           isRoot     = cs.include;

    if (superClass != 0) {
      scs  = getClass(cf.getName(((ClassCPInfo) cf.getCP(superClass)).getNameIndex()));
      cs.setSuperClass(scs);

      TypeDecl   tn   = scs.td;
      RecordType base = tn.getType().getCoreType().returnRecordType();
      int        l    = base.numFields();
      for (int i = 0; i < l; i++) {
        FieldDecl fd = base.getField(i);
        cFields.addElement(fd);
      }
    } else {
      FieldDecl vt = new FieldDecl("vtable", vtablepType);
      cFields.addElement(vt);
    }

    FieldInfo[] fields = cf.getFields();
    for (int i = 0; i < fields.length; i++) {
      FieldInfo field  = fields[i];
      int       access = field.getAccessFlags();
      String    fName  = cf.getName(field.getNameIndex());

      if ((access & ClassFile.ACC_STATIC) != 0) { // Define global variable
        if (isRoot)
          getGlobalVar(cs, fName);
      } else { // define class field
        Type      type = getClefType(cf.getString(field.getDescriptorIndex()));

        if ((access & ClassFile.ACC_FINAL) != 0)
          type = RefType.create(type, RefAttr.Const);

        FieldDecl fd = new FieldDecl(fName, type); // Defer initialization to the <init> method.

        fd.setAccessibility(getAccess(access));

        cFields.addElement(fd);
      }
    }

    int            access    = cf.getAccessFlags();
    Type           clefClass = RecordType.create(cFields);
    RefType        rt        = td.getType().returnRefType();
    IncompleteType ct        = rt.getRefTo().returnIncompleteType();

    if (ct != null)
      ct.setCompleteType(RefType.create(clefClass, td));
    else
      throw new scale.common.InternalError("Class already defined " + fixed);

    return td;
  }

  /**
   * Specify that the Declaration is a top level declaration so that
   * it is processed by Clef2C later.
   */
  public void addTopGlobal(Declaration d)
  {
    Object o = topGlobals.put(d.getName(), d);
    if (o == null)
      topInOrder.addElement(d);
  }

  /**
   * Return the special top level Declarations.
   */
  public Enumeration<Declaration> getTopDecls()
  {
    return topInOrder.elements();
  }

  /**
   * Return the name with characters replaced with '_'.
   */
  private String fixName(String name)
  {
    if (names == null)
      names = new HashMap<String, String>(203);

    String n = names.get(name);
    if (n == null) {
      StringBuffer buf = new StringBuffer(name);
      int          len = buf.length();
      for (int i = 0; i <len; i++)
        switch (buf.charAt(i)) {
        case '/':
        case '(':
        case ')':
        case '<':
        case '>':
        case ';':
        case '$':
        case '.': buf.setCharAt(i, '_'); break;
        case '[': buf.setCharAt(i, 'X'); break;
        default: break;
        }
      n = buf.toString();
      names.put(name, n);
    }
    return n;
  }

  /**
   * Each class has a virtual function transfer vector (vtable)
   * associated with it.  Each class instance has a pointer to this
   * vector.  Thus, the instance address can be used to access the
   * transfer vector to obtain the address of a class method.  The
   * transfer vector contains addresses only for public or protected,
   * non-static methods.
   * <p>
   * The structure of the vtable is as follows:
   * <pre>
   * struct VTABLE {
   *   const CLASS    *class;          // The class structure for this class
   *   int             interfaceCount; // The number of interfaces implemented by this class
   *   INTERFACEENTRY *interfaces;     // A list of the interfaces implemented by this class
   *   void           *methods[0]      // The addresses of the virtual methods - there is at least one.
   * };
   * <pre>
   * Each INTERFACEENTRY records a different interface:
   * <pre>
   * struct INTERFACEENTRY {
   *   int          hash;             // A hash of the interface name
   *   const CLASS *class;            // The class structure for this interface
   *   void        *((methods *)[0]); // A pointer to a table of method addresses
   * } ;
   * <pre>
   * The CLASS structure is defined as
   * <pre>
   * struct CLASS {
   *   const unsigned short *name;
   *   const struct CLASS   *super;
   *   byte           kind;
   * };
   * </pre>
   * Note - all the addresses that occur in the array pointed to by
   * the methods field are also in the VTABLE instance for the class.
   */
  public void defineStructures()
  {
    // Define a generic pointer to a procedure

    ftnType     = ProcedureType.create(VoidType.type, new Vector<FormalDecl>(0), null);
    ftnTypeDecl = new TypeDecl("FTN", null);
    ftnTypeRef  = RefType.create(ftnType, ftnTypeDecl);
    ftnpType    = PointerType.create(ftnTypeRef);
    cFtnpType   = PointerType.create(RefType.create(ftnTypeRef, RefAttr.Const));

    ftnTypeDecl.setType(ftnTypeRef);

    // Define the Class structure

    IncompleteType inc = new IncompleteType();
    classType     = inc;
    classTypeDecl = new TypeDecl("CLASSENTRY", null);
    classTypeRef  = RefType.create(classType, classTypeDecl);
    classpType    = PointerType.create(classTypeRef);

    FieldDecl className   = new FieldDecl("name",  RefType.create(cCharpType, RefAttr.Const));
    FieldDecl classSuper  = new FieldDecl("super", RefType.create(classpType, RefAttr.Const));
    FieldDecl classKind   = new FieldDecl("kind",  RefType.create(byteType,   RefAttr.Const));
    Vector<FieldDecl> classFields = new Vector<FieldDecl>(3);

    classFields.addElement(className);
    classFields.addElement(classSuper);
    classFields.addElement(classKind);

    inc.setCompleteType(RecordType.create(classFields));

    classTypeDecl.setType(classTypeRef);
    addTopGlobal(classTypeDecl);

    // Define the INTERFACEENTRY structure

    Type ht = RefType.create(intType, RefAttr.Const);
    Type ct = RefType.create(classpType, RefAttr.Const);
    Type mt = RefType.create(PointerType.create(ftnpType), RefAttr.Const);

    FieldDecl hashValue       = new FieldDecl("hash",    ht);
    FieldDecl interfaceClass  = new FieldDecl("class",   ct);
    FieldDecl methodp         = new FieldDecl("methods", mt);
    Vector<FieldDecl> interfaceFields = new Vector<FieldDecl>(3);

    interfaceFields.addElement(hashValue);
    interfaceFields.addElement(interfaceClass);
    interfaceFields.addElement(methodp);

    interfaceType     = RecordType.create(interfaceFields);
    interfaceTypeDecl = new TypeDecl("INTERFACEENTRY", null);
    interfaceTypeRef  = RefType.create(interfaceType, interfaceTypeDecl);
    interfacepType    = PointerType.create(interfaceTypeRef);

    interfaceTypeDecl.setType(interfaceTypeRef);
    addTopGlobal(interfaceTypeDecl);

    // Define the ARRAYHEADER structure

    arrayHeaderType     = makeArrayHeaderType(voidp);
    arrayHeaderTypeDecl = new TypeDecl("ARRAYHEADER", null);
    arrayHeaderTypeRef  = RefType.create(arrayHeaderType, arrayHeaderTypeDecl);
    arrayHeaderpType    = PointerType.create(arrayHeaderTypeRef);

    arrayHeaderTypeDecl.setType(arrayHeaderTypeRef);
    addTopGlobal(arrayHeaderTypeDecl);

    // Define the generic VTABLE structure

    vtableType     = makeVTableType(1);
    vtableTypeDecl = new TypeDecl("VTABLE", null);
    vtableTypeRef  = RefType.create(vtableType, vtableTypeDecl);
    vtablepType    = PointerType.create(vtableTypeRef);

    vtableTypeDecl.setType(vtableTypeRef);
    addTopGlobal(vtableTypeDecl);

    // Define the generic EXCEPTIONENTRY structure

    FieldDecl beginPc   = new FieldDecl("beginPc",   RefType.create(intType,    RefAttr.Const));
    FieldDecl endPc     = new FieldDecl("endPc",     RefType.create(intType,    RefAttr.Const));
    FieldDecl catchType = new FieldDecl("catchType", RefType.create(classpType, RefAttr.Const));
    Vector<FieldDecl> exceptionFields = new Vector<FieldDecl>(3);

    exceptionFields.addElement(beginPc);
    exceptionFields.addElement(endPc);
    exceptionFields.addElement(catchType);

    exceptionEntryType     = RecordType.create(exceptionFields);
    exceptionEntryTypeDecl = new TypeDecl("EXCEPTIONENTRY", null);
    exceptionEntryTypeRef  = RefType.create(exceptionEntryType, exceptionEntryTypeDecl);
    exceptionEntrypType    = PointerType.create(exceptionEntryTypeRef);

    exceptionEntryTypeDecl.setType(exceptionEntryTypeRef);
    addTopGlobal(exceptionEntryTypeDecl);
  }

  /**
   * Create the Declarations for various runtime procedures needed.
   */
  public void defineFunctions()
  {
    // Create the procedure that creates Strings.

    ClassStuff jlo = getClass("java/lang/Object");
    addTopGlobal(jlo.td);
    ClassStuff jls = getClass("java/lang/String");
    addTopGlobal(jls.td);

    stringpType = jls.type;

    FormalDecl target = new FormalDecl("target", PointerType.create(stringpType));
    FormalDecl source = new FormalDecl("source", cCharpType);

    Vector<FormalDecl> formals = new Vector<FormalDecl>(2);
    formals.addElement(target);
    formals.addElement(source);

    ProcedureType pt = ProcedureType.create(VoidType.type, formals, null);
    createStringProc = new ProcedureDecl("__createString", pt);
    addTopGlobal(createStringProc);
    createStringProc.setVisibility(Visibility.EXTERN);

    // Create the procedure that finds interface methods

    Vector<FormalDecl> iformals = new Vector<FormalDecl>(3);
    iformals.addElement(new FormalDecl("index", intType));
    iformals.addElement(new FormalDecl("hashcode", intType));
    iformals.addElement(new FormalDecl("vtable", vtablepType));

    ProcedureType ipt = ProcedureType.create(voidp, iformals, null);
    findIntMethodProc = new ProcedureDecl("__findIntMethod", ipt);
    addTopGlobal(findIntMethodProc);
    findIntMethodProc.setVisibility(Visibility.EXTERN);

    // Create the procedure that makes exceptions

    Vector<FormalDecl> eformals = new Vector<FormalDecl>(1);
    eformals.addElement(new FormalDecl("class", classpType));

    ProcedureType ept = ProcedureType.create(voidp, eformals, null);
    makeExceptionProc = new ProcedureDecl("__makeException", ept);
    addTopGlobal(makeExceptionProc);
    makeExceptionProc.setVisibility(Visibility.EXTERN);

    // Create the procedure that looks up exceptions

    Vector<FormalDecl> lformals = new Vector<FormalDecl>(3);
    lformals.addElement(new FormalDecl("pc", intType));
    lformals.addElement(new FormalDecl("exception", voidp));
    lformals.addElement(new FormalDecl("exceptionTab", exceptionEntrypType));

    ProcedureType lpt = ProcedureType.create(voidp, lformals, null);
    lookupExceptionProc = new ProcedureDecl("__lookupException", lpt);
    addTopGlobal(lookupExceptionProc);
    lookupExceptionProc.setVisibility(Visibility.EXTERN);

    // Create the global exception variable

    globalExceptionVariable = new VariableDecl("__globalExceptionVariable", voidp);
    globalExceptionVariable.setVisibility(Visibility.EXTERN);
    addTopGlobal(globalExceptionVariable);

    // Create the procedure that determines instanceof

    Vector<FormalDecl> ioformals = new Vector<FormalDecl>(3);
    ioformals.addElement(new FormalDecl("instance", vtablepType));
    ioformals.addElement(new FormalDecl("target", vtablepType));

    ProcedureType iopt = ProcedureType.create(intType, ioformals, null);
    instanceOfProc = new ProcedureDecl("__instanceof", iopt);
    addTopGlobal(instanceOfProc);
    instanceOfProc.setVisibility(Visibility.EXTERN);
  }

  /**
   * Return the Type for an array with the specified element type.
   */
  private Type makeArrayHeaderType(Type elementType)
  {
    Bound         b = Bound.create(int0, int0);
    Vector<Bound> r = new Vector<Bound>(1);

    r.addElement(b);

    Type      at     = FixedArrayType.create(r, elementType);
    FieldDecl aClass = new FieldDecl("class",       RefType.create(classpType, RefAttr.Const));
    FieldDecl len    = new FieldDecl("length",      RefType.create(intType, RefAttr.Const));
    FieldDecl et     = new FieldDecl("elementType", RefType.create(voidp, RefAttr.Const));
    FieldDecl elems  = new FieldDecl("array",       at);
    Vector<FieldDecl> fields = new Vector<FieldDecl>(3);

    fields.addElement(aClass);
    fields.addElement(len);
    fields.addElement(et);
    fields.addElement(elems);

    return RecordType.create(fields);
  }

  /**
   * Create the virtual table for the specified class.
   */
  private void createVtable(ClassStuff cs)
  {
    ClassFile cf = cs.cf;
    if (0 != (ClassFile.ACC_ABSTRACT & cf.getAccessFlags()))
      return; // if interface

    String       vn = cs.name + "_vtable";
    VariableDecl vd = null;

    Type     vt = makeVTableType(cs.numMethods());
    TypeDecl td = new TypeDecl(vn + "_t", null);
    Type     rt = RefType.create(vt, td);

    td.setType(rt);
    addTopGlobal(td);

    Expression init = makeVTableInit(cs, rt, makeInterfaces(cs));
    vd   = new VariableDecl(vn, rt, init);

    addTopGlobal(vd);

    cs.setVTableDecl(vd);
  }

  /**
   * Return the variable defining the class' CLASSENTRY.
   * @param name is the name of the class
   */
  public VariableDecl getClassDecl(String name)
  {
    return getClassDecl(getClass(name));
  }

  /**
   * Return the variable defining the class' CLASSENTRY.
   * @param ecs is the ClassStuff for the class
   */
  public VariableDecl getClassDecl(ClassStuff ecs)
  {
    VariableDecl cvd = ecs.getClassDecl(classTypeRef);

    addTopGlobal(cvd);

    return cvd;
  }

  /**
   * Create the variable and its initialization for a class.
   * @param cs is the ClassStuff for this class
   */
  private VariableDecl makeClassObject(ClassStuff cs)
  {
    boolean interf = ((cs.cf.getAccessFlags() & ClassFile.ACC_INTERFACE) != 0);
    return makeClassObject(cs.name,
                           interf ? INTERFACETYPE : CLASSTYPE,
                           cs.getSuperClass());
  }

  /**
   * Create the variable and its initialization for a class'
   * CLASSENTRY variable.
   * @param cname is the name of the class
   * @param classType specifies (0 class), (1 interface), (2 array)
   * @param superClass is this class' super class
   */
  private VariableDecl makeClassObject(String cname, int classType, ClassStuff superClass)
  {
    String       name  = cname + "_class";
    Vector<Object> initc = new Vector<Object>(3);
    VariableDecl initv = makeStringInit(name);
    Type         ivt   = PointerType.create(initv.getType());
    Literal      sc    = nil;

    if (superClass != null) {
      Declaration cd = getClassDecl(superClass);
      Type        ct = PointerType.create(cd.getType());
      sc = new AddressLiteral(ct, cd);
    }

    initc.addElement(new AddressLiteral(ivt, initv));
    initc.addElement(sc);
    initc.addElement(getIntLiteral(classType));

    AggregationElements init = new AggregationElements(classTypeRef, initc);
    VariableDecl        vd   = new VariableDecl(name, classTypeRef, init);

    addTopGlobal(vd);

    return vd;
  }

  /**
   * Create the initialization for a class' VTABLE.
   * @param cs is the ClassStuff for this class
   * @param vt is nonsense
   * @param initInterface is the address of the class' interface entries
   */
  private Expression makeVTableInit(ClassStuff cs, Type vt, Literal initInterface)
  {
    Type        ct     = cs.type;
    int         length = cs.numMethods();
    Vector<Object> initc  = new Vector<Object>(length + 3);
    Declaration cd     = getClassDecl(cs);
    Type        pc     = PointerType.create(cd.getType());

    initc.addElement(new AddressLiteral(pc, cd));
    initc.addElement(getIntLiteral(cs.numInterfaces()));
    initc.addElement(initInterface);

    Enumeration<ClassStuff.CM> em = cs.getVirtualMethods();
    while (em.hasMoreElements()) {
      ClassStuff.CM p          = em.nextElement();
      ClassFile     cf         = p.classFile;
      MethodInfo    method     = p.methodInfo;
      String        descriptor = ((Utf8CPInfo) cf.getCP(method.getDescriptorIndex())).getString();
      String        mName      = cf.getName(method.getNameIndex());
      ClassCPInfo   cinfo      = (ClassCPInfo) cf.getCP(cf.getThisClass());
      String        cName      = cf.getName(cinfo.getNameIndex());
      ClassStuff    rc         = getClass(cName);
      String        m          = genMethodName(cName, mName, descriptor);
      ProcedureDecl pd         = getProcedureDecl(rc.type, m, descriptor, false);
      Literal       ev         = new AddressLiteral(PointerType.create(rc.type), pd);

      initc.addElement(ev);

      addTopGlobal(pd);
    }
    return new AggregationElements(vt, initc);
  }

  /**
   * Each INTERFACEENTRY records a different interface:
   * <pre>
   * typedef struct {
   *   int hash;               // A hash of the interface name
   *   char *name;             // The name of the interface
   *   void *((methods *)[0]); // A pointer to a table of method addresses
   * } INTERFACEENTRY;
   * <pre>
   */
  private Literal makeInterfaces(ClassStuff cs)
  {
    int numInterfaces = cs.numInterfaces();

    if (numInterfaces == 0)
      return nil;

    String        vn     = cs.name + "_interfaces";
    Vector<Bound> r      = new Vector<Bound>(1);
    Expression    length = getIntLiteral(numInterfaces - 1);
    Bound         b      = Bound.create(int0, length);

    r.addElement(b);

    Type                vt   = FixedArrayType.create(r, interfaceTypeRef);
    Vector<Object>      init = new Vector<Object>(numInterfaces);
    Enumeration<String> ei   = cs.getInterfaces();

    while (ei.hasMoreElements()) {
      String         icn   = ei.nextElement();
      ClassStuff     ics   = getClass(icn);
      Vector<Object> inite = new Vector<Object>(3);
      VariableDecl   initv = getClassDecl(ics);

      inite.addElement(getIntLiteral(icn.hashCode()));
      inite.addElement(new AddressLiteral(PointerType.create(initv.getType()), initv));
      inite.addElement(makeInterfaceArray(cs, ics));

      init.addElement(new AggregationElements(VoidType.type, inite)); /***************/
    }

    VariableDecl vd = new VariableDecl(vn, vt, new AggregationElements(vt, init));
    addTopGlobal(vd);
    vd.setVisibility(Visibility.FILE);

    return new AddressLiteral(PointerType.create(vt), vd);
  }

  private Literal makeInterfaceArray(ClassStuff cs, ClassStuff ics)
  {
    int numMethods = cs.numMethods();

    if (numMethods == 0)
      return nil;

    Vector<Object> initc = new Vector<Object>(numMethods);
    Type           ct    = cs.type;
    int            i     = 0;
    Enumeration<ClassStuff.CM> em = cs.getVirtualMethods();

    while (em.hasMoreElements()) {
      ClassStuff.CM p          = em.nextElement();
      ClassFile     cf         = p.classFile;
      MethodInfo    method     = p.methodInfo;
      String        descriptor = ((Utf8CPInfo) cf.getCP(method.getDescriptorIndex())).getString();
      String        mName      = cf.getName(method.getNameIndex());
      String        in         = cs.name;
      Type          ict        = ct;

      if ((cf.getAccessFlags() & ClassFile.ACC_INTERFACE) == 0) {
        in = cf.getName(((ClassCPInfo) cf.getCP(cf.getThisClass())).getNameIndex());
        ClassStuff rcs = getClass(in);
        ict = rcs.type;
      }

      String        iName      = genMethodName(in, mName, descriptor);
      ProcedureDecl pd         = getProcedureDecl(ict, iName, descriptor, false);
      Literal       ev         = new AddressLiteral(PointerType.create(pd.getSignature()), pd);
      initc.addElement(ev);
      i++;
    }

    String        vn     = cs.name + "_" + ics.name;
    Vector<Bound> r      = new Vector<Bound>(1);
    Expression    length = getIntLiteral(numMethods - 1);
    Bound         b      = Bound.create(int0, length);

    r.addElement(b);

    Type         vt   = FixedArrayType.create(r, cFtnpType);
    VariableDecl vd   = new VariableDecl(vn, vt, new AggregationElements(vt, initc));

    vd.setVisibility(Visibility.FILE);

    addTopGlobal(vd);
    return new AddressLiteral(PointerType.create(vt), vd);
  }

  /**
   * Create a specific VTABLE type for the specified number of virtual
   * methods.
   * @param size specifies the number oif virtual methods for this
   * class
   */
  private Type makeVTableType(int size)
  {
    Vector<Bound> r      = new Vector<Bound>(1);
    Expression    length = getIntLiteral(size - 1);
    Bound         b      = Bound.create(int0, length);

    r.addElement(b);

    Type at = FixedArrayType.create(r, ftnpType);

    // Define the VTABLE structure

    Type ct  = RefType.create(classpType, RefAttr.Const);
    Type ict = RefType.create(intType, RefAttr.Const);
    Type it  = RefType.create(PointerType.create(interfaceTypeRef), RefAttr.Const);

    Vector<FieldDecl> vtableFields   = new Vector<FieldDecl>(4);
    FieldDecl classClass     = new FieldDecl("class",          ct);
    FieldDecl interfaceCount = new FieldDecl("interfaceCount", ict);
    FieldDecl interfaces     = new FieldDecl("interfaces",     it);
    FieldDecl vt             = new FieldDecl("methods",        at);

    vtableFields.addElement(classClass);
    vtableFields.addElement(interfaceCount);
    vtableFields.addElement(interfaces);
    vtableFields.addElement(vt);

    return RecordType.create(vtableFields);
  }

  private Type makeArrayType(String descriptor)
  {
    int rank = 0;
    int index = 0;
    while (descriptor.charAt(index) == '[') {
      index++;
      rank++;
    }
    Type  elementType = getClefType(descriptor.substring(index));

    while (true) {
      elementType = makeArrayHeaderType(elementType);
      rank--;
      if (rank <= 0)
        return elementType;
      elementType = PointerType.create(elementType);
    }
  }

  private PointerType getArrayType(String descriptor)
  {
    if (arrayMap == null)
      arrayMap = new HashMap<String, TypeDecl>(203);

    TypeDecl td = arrayMap.get(descriptor);
    if (td == null) {
      Type at = makeArrayType(descriptor);

      td = new TypeDecl("_a" + fixName(descriptor), null);
      td.setType(RefType.create(at, td));
      addTopGlobal(td);
      arrayMap.put(descriptor, td);
    }
    PointerType t = PointerType.create(td.getType());
    return t;
  }

  /**
   * Return the array type for the specified Java type.
   * @param type is the Java type
   * @see scale.jcr.CodeAttribute
   */
  public Type getArrayType(int type)
  {
    return getArrayType("[" + CodeAttribute.typeSpecifier[type]);
  }

  /**
   * Return a vector of AST {@link scale.clef.type.Type Type}s
   * corresponding the the types specified in the descriptor.
   * @see scale.clef.type.Type
   */
  public Vector<Type> getClefTypes(String descriptor)
  {
    Vector<Type> types = new Vector<Type>(4);
    int          index = 0;

    while (index < descriptor.length()) {
      char c = descriptor.charAt(index++);
      switch (c) {
      case '(':
      case ')':
        break;
      case 'V':
        types.addElement(VoidType.type);
        break;
      case 'B':
        types.addElement(byteType);
        break;
      case 'C':
        types.addElement(charType);
        break;
      case 'D':
        types.addElement(doubleType);
        break;
      case 'F':
        types.addElement(floatType);
        break;
      case 'I':
        types.addElement(intType);
        break;
      case 'J':
        types.addElement(longType);
        break;
      case 'S':
        types.addElement(shortType);
        break;
      case 'Z':
        types.addElement(BooleanType.type);
        break;
      case 'L':
        int      n     = descriptor.indexOf(';', index);
        String   cname = descriptor.substring(index, n);
        ClassStuff cs = getClass(cname);
        types.addElement(cs.type);
        index = n + 1;
        break;
      case '[':
        int beg = index - 1;
        while (descriptor.charAt(index) == '[') index++;
        c = descriptor.charAt(index);
        if (c == 'L')
          index = descriptor.indexOf(';', index);
        index++;
        types.addElement(getArrayType(descriptor.substring(beg, index)));
        break;
      default:
        throw new scale.common.InternalError("Unrecognized descriptor " + descriptor);
      }
    }
    return types;
  }

  /**
   * Return a Clef Type corresponding the the type specified in the
   * descriptor.
   * @see scale.clef.type.Type
   */
  private Type getClefType(String descriptor)
  {
    int index = 0;

    char c = descriptor.charAt(index++);
    switch (c) {
    case 'B':
      return byteType;
    case 'C':
      return charType;
    case 'D':
      return doubleType;
    case 'F':
      return floatType;
    case 'I':
      return intType;
    case 'J':
      return longType;
    case 'S':
      return shortType;
    case 'Z':
      return BooleanType.type;
    case 'L':
      String     cname = descriptor.substring(index, descriptor.indexOf(';', index));
      ClassStuff cs    = getClass(cname);
      return cs.type;
    case '[':
      return getArrayType(descriptor.substring(index - 1));
    }
    throw new scale.common.InternalError("Unrecognized descriptor " + descriptor);
  }

  /**
   * Return the fully qualified name of a method.
   * @param className is the name of the method's class
   * @param mName is the name of the method
   * @param descriptor is the Java method descriptor
   */
  public String genMethodName(String className, String mName, String descriptor)
  {
    StringBuffer buf = new StringBuffer(fixName(className));
    buf.append('_');
    buf.append(fixName(mName));
    buf.append('_');
    buf.append(fixName(descriptor));
    return buf.toString();
  }

  /**
   * This method creates the Clef representation of a Java String as
   * an array of shorts.  The first element of the array is the length
   * of the string.
   * @return a Clef VariableDecl for the String
   */
  public VariableDecl makeStringInit(String s)
  {
    if (stringMap == null)
      stringMap = new HashMap<String, VariableDecl>(203);

    VariableDecl var = stringMap.get(s);
    if (var == null) {
      int            len    = s.length();
      Vector<Bound>  r      = new Vector<Bound>(1);
      Vector<Object> initc  = new Vector<Object>(len + 1);
      Literal        length = LiteralMap.put(len, charType);
      Bound          b      = Bound.create(int0, length);

      r.addElement(b);

      Type at = RefType.create(FixedArrayType.create(r, charType), RefAttr.Const);

      initc.addElement(length);
      for (int i = 0; i < len; i++) {
        int c  = s.charAt(i);
        initc.addElement(LiteralMap.put(c, charType));
      }

      var = new VariableDecl(un.genName(), at, new AggregationElements(at, initc));

      stringMap.put(s, var);

      var.setVisibility(Visibility.FILE);
      addTopGlobal(var);
    }
    return var;
  }

  /**
   * Create the run time exception table for a class' method.
   * @param cf is the ClassFile for the class
   * @param methodName is the name of the method
   * @param entries is the table of exception handlers
   */
  public VariableDecl makeExceptionTable(ClassFile cf, String methodName, ExceptionEntry[] entries)
  {
    String tn  = methodName + "_exceptions";
    int    len = entries.length;
    Bound  b   = Bound.create(int0, getIntLiteral(len - 1));
    Vector<Bound> r   = new Vector<Bound>(1);

    r.addElement(b);

    Type   at   = FixedArrayType.create(r, exceptionEntryTypeRef);
    Vector<Object> init = new Vector<Object>(len);

    for (int i = 0; i < len; i++) {
      ExceptionEntry ee    = entries[i];
      int            ct    = ee.catchType;
      Literal        tab   = nil;
      Vector<Object> inite = new Vector<Object>(3);

      if (ct != 0) {
        ClassCPInfo  cinfo = (ClassCPInfo) cf.getCP(ee.catchType);
        String       ename = cf.getName(cinfo.getNameIndex());
        VariableDecl vd    = getClassDecl(ename);
        tab = new AddressLiteral(PointerType.create(vd.getType()), vd);
      }

      inite.addElement(getIntLiteral(ee.startPc));
      inite.addElement(getIntLiteral(ee.endPc));
      inite.addElement(tab);

      init.addElement(new AggregationElements(VoidType.type, inite)); /*****************/
    }

    VariableDecl exVar = new VariableDecl(tn, at, new AggregationElements(at, init));
    addTopGlobal(exVar);
    return exVar;
  }

  /**
   * Return the Clef Literal for the specified constant value.
   * @param cf is the class file containing the constant
   * @param index is the index of the constant in the constant pool
   */
  public Object getLiteral(ClassFile cf, int index)
  {
    CPInfo cp = cf.getCP(index);
    switch (cp.getTag()) {
    case CPInfo.CONSTANT_Utf8:    return makeStringInit(((Utf8CPInfo)   cp).getString());
    case CPInfo.CONSTANT_Integer: return LiteralMap.put(((IntCPInfo)    cp).getValue(), intType);
    case CPInfo.CONSTANT_Float:   return LiteralMap.put(((FloatCPInfo)  cp).getValue(), floatType);
    case CPInfo.CONSTANT_Long:    return LiteralMap.put(((LongCPInfo)   cp).getValue(), longType);
    case CPInfo.CONSTANT_Double:  return LiteralMap.put(((DoubleCPInfo) cp).getValue(), doubleType);
    case CPInfo.CONSTANT_String:
      return makeStringInit(((Utf8CPInfo) cf.getCP(((StringCPInfo) cp).getStringIndex())).getString());
    }
    throw new scale.common.InternalError("Unrecognized constant " + cp);
  }

  /**
   * Return return a Clef Literal with the specified value.
   */
  public Literal getIntLiteral(int value)
  {
    return LiteralMap.put(value, intType);
  }

  /**
   * Return a Clef ProcedureType for the method.
   * @param ct is the type of the 'this' parameter
   * @param descriptor is the Java method descriptor
   */
  public ProcedureType createMethodType(Type ct, String descriptor, boolean isStatic)
  {
    Vector<Type>       types  = getClefTypes(descriptor);
    int                num    = types.size() - 1;
    Vector<FormalDecl> params = new Vector<FormalDecl>(num);
    if (!isStatic)
      params.addElement(new FormalDecl("this", voidp, ParameterMode.VALUE));

    for (int i = 0; i < num; i++) {
      String pname = "p" + i;
      Type   t     = types.elementAt(i);
      params.addElement(new FormalDecl(pname, t, ParameterMode.VALUE));
    }
    return ProcedureType.create(types.elementAt(num), params, null);
  }

  /**
   * Create a Clef ProcedureDecl for the specified method.
   * @param ct is the type of the 'this' parameter
   * @param name is the name of the method
   * @param descriptor is the Java method descriptor
   * @return a new ProcedureDecl
   */
  public ProcedureDecl getProcedureDecl(Type ct, String name, String descriptor, boolean isStatic)
  {
    if (procedures == null)
      procedures = new HashMap<String, ProcedureDecl>(203);

    ProcedureDecl pd = procedures.get(name);
    if (pd == null) {
      ProcedureType sig = createMethodType(ct, descriptor, isStatic);
      pd = new ProcedureDecl(name, sig);
      procedures.put(name, pd);
      pd.setVisibility(Visibility.EXTERN);
    }
    return pd;
  }

  /**
   * Return the named FieldDecl in the structure.
   */
  private FieldDecl findField(RecordType rt, String fname)
  {
    int l = rt.numFields();
    for (int i = 0; i < l; i++) {
      FieldDecl d = rt.getField(i);
      if (fname.equals(d.getName()))
        return d;
    }
    throw new scale.common.InternalError("Field " + fname + " not found in " + rt);
  }

  /**
   * Return the named FieldDecl in the structure.
   * @param cs specifies the class
   * @param fname specifies the name of the field
   */
  public FieldDecl findField(ClassStuff cs, String fname)
  {
    TypeDecl   td = cs.td;
    RecordType rt = td.getType().getCoreType().returnRecordType();
    if (!fname.equals("vtable"))
      addTopGlobal(td);
    return findField(rt, fname);
  }

  /**
   * Return the named FieldDecl in the array structure.
   */
  public FieldDecl getArrayField(String fname)
  {
    return findField(arrayHeaderType.getCoreType().returnRecordType(), fname);
  }

  /**
   * Return the named FieldDecl in the virtual table structure.
   */
  public FieldDecl getVTableField(String fname)
  {
    return findField(vtableType.getCoreType().returnRecordType(), fname);
  }

  /**
   * Return the virtual table VariableDecl for the class.
   */
  public VariableDecl getVTableDecl(ClassStuff cs)
  {
    return cs.getVTableDecl(vtableTypeRef);
  }

  /**
   * Return the generic array structure type.
   */
  public Type getArrayHeaderType()
  {
    return arrayHeaderTypeRef;
  }

  /**
   * Clean up for profiling statistics.
   */
  public static void cleanup()
  {
    arrayMap   = null;
    classMap   = null;
    procedures = null;
    names      = null;
    stringMap  = null;
  }
}
