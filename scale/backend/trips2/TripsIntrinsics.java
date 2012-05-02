package scale.backend.trips2;

import scale.common.*;
import scale.backend.*;
import scale.clef.type.*;
import scale.score.expr.*;
import java.util.HashMap;


/**
 * This class represents Trips intrinsic functions.
 * <p>
 * Copyright 2006 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 * @author Aaron Smith
 */
public class TripsIntrinsics extends Intrinsics
{
  private static int createdCount = 0; /* A count of all the instances of this class that were created. */

  private static final String[] stats = {"created"};

  static
  {
    Statistics.register("scale.backend.trips2.TripsIntrinsics", stats);
  }

  /**
   * Return the number of instances of this class created.
   */
  public static int created()
  {
    return createdCount;
  }
  
  /**
   * This represents a void* type.
   */
  private Type vpt = PointerType.create(VoidType.type);
  
  /**
   * This represents a const void* type.
   */
  private Type cvpt = PointerType.create(RefType.create(VoidType.type, RefAttr.Const));
  
  /**
   * This represents an int (32-bit) type.
   */
  private Type it = scale.clef.type.SignedIntegerType.create(32);
  
  /**
   * This represents a char type.
   */
  private Type cpt = PointerType.create(CharacterType.create(CharacterType.cAnsi));
  
  /**
   * Constructor used to install intrinsics.  
   * @param gen the instance of the generator that will call these intrinsics
   * @param c the generator's class, ex. Trips2Generator.class
   */
  public TripsIntrinsics(Generator gen, Class<Generator> c)
  {
    super(gen, c);
    
    Type[] t1 = {vpt, it, it}; 
    //installIntrinsic("__builtin_memset", t1, vpt);	/* void *memset(void *s, int c, size_t n); */
    
    Type[] t2 = {cvpt, cvpt, it};
    //installIntrinsic("__builtin_memcmp", t2, it);		/* int memcmp(const void *s1, const void *s2, size_t n); */
    //installIntrinsic("__builtin_memcpy", t2, vpt);	/* void *memcpy(void *s1, const void *s2, size_t n); */
    
    Type[] t3 = {cpt, cpt};
    //installIntrinsic("__builtin_strcpy", t3, cpt); 	/* char *strcpy(char *s1, const char *s2); */
    
    Type[] t4 = {it};
    installIntrinsic("__builtin_abs", t4, it);		/* int abs (int n);  */
    
    createdCount++;
  }
}
