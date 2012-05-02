package scale.clef;

import scale.clef.type.*;

/**
 * Predicate class for visit pattern of Clef Types.
 * <p>
 * $Id: TypePredicate.java,v 1.22 2007-05-10 16:48:01 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * @see scale.clef.Predicate
 */

public interface TypePredicate
{
  public void visitAggregateType(AggregateType n); 
  public void visitAllocArrayType(AllocArrayType n); 
  public void visitArrayType(ArrayType n); 
  public void visitAtomicType(AtomicType n); 
  public void visitBooleanType(BooleanType n); 
  public void visitBound(Bound n); 
  public void visitCharacterType(CharacterType n); 
  public void visitComplexType(ComplexType n); 
  public void visitCompositeType(CompositeType n); 
  public void visitEnumerationType(EnumerationType n); 
  public void visitFixedArrayType(FixedArrayType n); 
  public void visitFloatType(FloatType n); 
  public void visitIncompleteType(IncompleteType n); 
  public void visitIntegerType(IntegerType n); 
  public void visitSignedIntegerType(SignedIntegerType n); 
  public void visitUnsignedIntegerType(UnsignedIntegerType n); 
  public void visitFortranCharType(FortranCharType n); 
  public void visitNumericType(NumericType n); 
  public void visitPointerType(PointerType n); 
  public void visitProcedureType(ProcedureType n); 
  public void visitRaise(Raise n); 
  public void visitRaiseWithObject(RaiseWithObject n); 
  public void visitRaiseWithType(RaiseWithType n); 
  public void visitRealType(RealType n); 
  public void visitRecordType(RecordType n); 
  public void visitRefType(RefType n); 
  public void visitType(Type n); 
  public void visitUnionType(UnionType n); 
  public void visitVoidType(VoidType n); 
}
