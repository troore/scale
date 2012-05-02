// $ANTLR 2.7.4: "c99.g" -> "C99Lexer.java"$

package scale.frontend.c;

import java.io.InputStream;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.TokenStreamRecognitionException;
import antlr.CharStreamException;
import antlr.CharStreamIOException;
import antlr.ANTLRException;
import java.io.Reader;
import java.util.Hashtable;
import antlr.CharScanner;
import antlr.InputBuffer;
import antlr.ByteBuffer;
import antlr.CharBuffer;
import antlr.Token;
import antlr.CommonToken;
import antlr.RecognitionException;
import antlr.NoViableAltForCharException;
import antlr.MismatchedCharException;
import antlr.TokenStream;
import antlr.ANTLRHashString;
import antlr.LexerSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.SemanticException;

import java.util.Enumeration;
import scale.common.*;
import scale.clef.LiteralMap;
import scale.clef.expr.Literal;
import scale.clef.expr.IntLiteral;
import scale.clef.expr.FloatLiteral;
import scale.clef.expr.StringLiteral;
import scale.clef.type.*;
import scale.clef.decl.*;
import scale.clef.symtab.*;
import scale.callGraph.*;

/** 
 * This class reads in a chacater stream and outputs tokens in the C99 language.
 * <p>
 * $Id: c99.g,v 1.144 2007-09-20 18:49:43 burrill Exp $
 * <p>
 * Copyright 2007 by the <a href="http://ali-www.cs.umass.edu/">Scale Compiler Group</a>,<br>
 * <a href="http://www.cs.umass.edu/">Department of Computer Science</a><br>
 * <a href="http://www.umass.edu/">University of Massachusetts</a>,<br>
 * Amherst MA. 01003, USA<br>
 * All Rights Reserved.<br>
 * <p>
 */

public class C99Lexer extends antlr.CharScanner implements C99ParserTokenTypes, TokenStream
 {

  private CallGraph cg;
  private CPreprocessor reader;
  private C99Parser parser;
  private boolean   longLongSubtype = false;
  private boolean   longSubtype     = false;
  private boolean   unsignedSubtype = false;
  private boolean   floatSubtype    = false;
  private boolean   intType         = false;
  private boolean   realType        = true;

  public void setCallGraph(CallGraph cg)
  {
    this.cg = cg;
  }

  public void setReader(CPreprocessor reader)
  {
    this.reader = reader;
  }

  public void setParser(C99Parser parser)
  {
    this.parser = parser;
  }

  public void newline()
  {
    setLine(reader.getLineNumber() + 1);
    setColumn(1);
  }

  private void resetSubtypes()
  {
    longSubtype = false;
    longLongSubtype = false;
    unsignedSubtype = false;
    floatSubtype = false;
    intType = false;
    realType = true;
  }

  private int specifyHexFloat()
  {
    if (longSubtype)
      return (HexLongDoubleValue);
    if (floatSubtype)
      return (HexFloatValue);
    return (HexDoubleValue);
  }

  private int specifyHexInt()
  {
    if (unsignedSubtype) {
      if (longLongSubtype)
        return (HexUnsignedLongLongIntValue);
      if (longSubtype)
        return (HexUnsignedLongIntValue);
      return (HexUnsignedIntValue);
    }

    if (longLongSubtype)
      return (HexLongLongIntValue);
    if (longSubtype)
      return (HexLongIntValue);
    return (HexIntValue);
  }

  private int specifyFloat()
  {
    if (longSubtype)
      return (LongDoubleValue);
    if (floatSubtype)
      return (FloatValue);
    return (DoubleValue);
  }

  private int specifyInt()
  {
    if (unsignedSubtype) {
      if (longLongSubtype)
        return (UnsignedLongLongIntValue);
      if (longSubtype)
        return (UnsignedLongIntValue);
      return
        (UnsignedIntValue);
    }

    if (longLongSubtype)
      return (LongLongIntValue);
    if (longSubtype)
      return (LongIntValue);
    return (IntValue);
  }
public C99Lexer(InputStream in) {
	this(new ByteBuffer(in));
}
public C99Lexer(Reader in) {
	this(new CharBuffer(in));
}
public C99Lexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public C99Lexer(LexerSharedInputState state) {
	super(state);
	caseSensitiveLiterals = true;
	setCaseSensitive(true);
	literals = new Hashtable();
	literals.put(new ANTLRHashString("extern", this), new Integer(16));
	literals.put(new ANTLRHashString("case", this), new Integer(6));
	literals.put(new ANTLRHashString("short", this), new Integer(27));
	literals.put(new ANTLRHashString("break", this), new Integer(5));
	literals.put(new ANTLRHashString("while", this), new Integer(42));
	literals.put(new ANTLRHashString("typeof", this), new Integer(36));
	literals.put(new ANTLRHashString("inline", this), new Integer(21));
	literals.put(new ANTLRHashString("unsigned", this), new Integer(38));
	literals.put(new ANTLRHashString("const", this), new Integer(8));
	literals.put(new ANTLRHashString("float", this), new Integer(17));
	literals.put(new ANTLRHashString("return", this), new Integer(26));
	literals.put(new ANTLRHashString("__builtin_va_arg", this), new Integer(51));
	literals.put(new ANTLRHashString("sizeof", this), new Integer(30));
	literals.put(new ANTLRHashString("__CLONE_beta", this), new Integer(53));
	literals.put(new ANTLRHashString("va_arg", this), new Integer(50));
	literals.put(new ANTLRHashString("__signed__", this), new Integer(29));
	literals.put(new ANTLRHashString("do", this), new Integer(12));
	literals.put(new ANTLRHashString("__label__", this), new Integer(35));
	literals.put(new ANTLRHashString("__alignof__", this), new Integer(47));
	literals.put(new ANTLRHashString("__volatile", this), new Integer(41));
	literals.put(new ANTLRHashString("__asm", this), new Integer(125));
	literals.put(new ANTLRHashString("typedef", this), new Integer(34));
	literals.put(new ANTLRHashString("__const", this), new Integer(9));
	literals.put(new ANTLRHashString("__asm__", this), new Integer(123));
	literals.put(new ANTLRHashString("if", this), new Integer(20));
	literals.put(new ANTLRHashString("double", this), new Integer(13));
	literals.put(new ANTLRHashString("volatile", this), new Integer(40));
	literals.put(new ANTLRHashString("union", this), new Integer(37));
	literals.put(new ANTLRHashString("_Imaginary", this), new Integer(45));
	literals.put(new ANTLRHashString("register", this), new Integer(24));
	literals.put(new ANTLRHashString("auto", this), new Integer(4));
	literals.put(new ANTLRHashString("goto", this), new Integer(19));
	literals.put(new ANTLRHashString("enum", this), new Integer(15));
	literals.put(new ANTLRHashString("_Complex", this), new Integer(44));
	literals.put(new ANTLRHashString("int", this), new Integer(22));
	literals.put(new ANTLRHashString("for", this), new Integer(18));
	literals.put(new ANTLRHashString("char", this), new Integer(7));
	literals.put(new ANTLRHashString("default", this), new Integer(11));
	literals.put(new ANTLRHashString("__builtin_offsetof", this), new Integer(52));
	literals.put(new ANTLRHashString("static", this), new Integer(31));
	literals.put(new ANTLRHashString("_Bool", this), new Integer(43));
	literals.put(new ANTLRHashString("continue", this), new Integer(10));
	literals.put(new ANTLRHashString("__restrict", this), new Integer(121));
	literals.put(new ANTLRHashString("struct", this), new Integer(32));
	literals.put(new ANTLRHashString("restrict", this), new Integer(25));
	literals.put(new ANTLRHashString("signed", this), new Integer(28));
	literals.put(new ANTLRHashString("else", this), new Integer(14));
	literals.put(new ANTLRHashString("void", this), new Integer(39));
	literals.put(new ANTLRHashString("switch", this), new Integer(33));
	literals.put(new ANTLRHashString("__inline__", this), new Integer(48));
	literals.put(new ANTLRHashString("__inline", this), new Integer(49));
	literals.put(new ANTLRHashString("long", this), new Integer(23));
	literals.put(new ANTLRHashString("__extension__", this), new Integer(46));
	literals.put(new ANTLRHashString("asm", this), new Integer(124));
}

public Token nextToken() throws TokenStreamException {
	Token theRetToken=null;
tryAgain:
	for (;;) {
		Token _token = null;
		int _ttype = Token.INVALID_TYPE;
		resetText();
		try {   // for char stream error handling
			try {   // for lexical error handling
				switch ( LA(1)) {
				case '\t':  case '\n':  case '\r':  case ' ':
				{
					mWS(true);
					theRetToken=_returnToken;
					break;
				}
				case '.':  case '0':  case '1':  case '2':
				case '3':  case '4':  case '5':  case '6':
				case '7':  case '8':  case '9':
				{
					mConstant(true);
					theRetToken=_returnToken;
					break;
				}
				case '\'':
				{
					mCharacterConstant(true);
					theRetToken=_returnToken;
					break;
				}
				case '"':
				{
					mStringLit(true);
					theRetToken=_returnToken;
					break;
				}
				case '{':
				{
					mLBrace(true);
					theRetToken=_returnToken;
					break;
				}
				case '}':
				{
					mRBrace(true);
					theRetToken=_returnToken;
					break;
				}
				case '[':
				{
					mLBracket(true);
					theRetToken=_returnToken;
					break;
				}
				case ']':
				{
					mRBracket(true);
					theRetToken=_returnToken;
					break;
				}
				case '(':
				{
					mLParen(true);
					theRetToken=_returnToken;
					break;
				}
				case ')':
				{
					mRParen(true);
					theRetToken=_returnToken;
					break;
				}
				case '~':
				{
					mComp(true);
					theRetToken=_returnToken;
					break;
				}
				case '?':
				{
					mQMark(true);
					theRetToken=_returnToken;
					break;
				}
				case ';':
				{
					mSemi(true);
					theRetToken=_returnToken;
					break;
				}
				case ',':
				{
					mComma(true);
					theRetToken=_returnToken;
					break;
				}
				default:
					if ((LA(1)=='<') && (LA(2)=='<') && (LA(3)=='=')) {
						mLSAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='>') && (LA(2)=='>') && (LA(3)=='=')) {
						mRSAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='%') && (LA(2)==':') && (LA(3)=='%')) {
						mMCMColon(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='L') && (LA(2)=='\'')) {
						mWideCharacterConstant(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='L') && (LA(2)=='"')) {
						mWideStringLiteral(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='-') && (LA(2)=='>')) {
						mSelect(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='+') && (LA(2)=='+')) {
						mInc(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='-') && (LA(2)=='-')) {
						mDec(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (LA(2)=='<') && (true)) {
						mLShift(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='>') && (LA(2)=='>') && (true)) {
						mRShift(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (LA(2)=='=')) {
						mLEqual(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='>') && (LA(2)=='=')) {
						mGEqual(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='=') && (LA(2)=='=')) {
						mEqual(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='!') && (LA(2)=='=')) {
						mNEqual(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='&') && (LA(2)=='&')) {
						mAndCond(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='|') && (LA(2)=='|')) {
						mOrCond(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='*') && (LA(2)=='=')) {
						mMultAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='/') && (LA(2)=='=')) {
						mSlashAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='%') && (LA(2)=='=')) {
						mModAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='+') && (LA(2)=='=')) {
						mPlusAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='-') && (LA(2)=='=')) {
						mSubAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='&') && (LA(2)=='=')) {
						mAndAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='^') && (LA(2)=='=')) {
						mXorAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='|') && (LA(2)=='=')) {
						mOrAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='#') && (LA(2)=='#')) {
						mDHatch(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (LA(2)==':')) {
						mLColon(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)==':') && (LA(2)=='>')) {
						mRColon(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (LA(2)=='%')) {
						mLMod(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='>') && (LA(2)=='%')) {
						mRMod(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='%') && (LA(2)==':') && (true)) {
						mMColon(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='#') && (LA(2)=='l')) {
						mPPLine(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='#') && (LA(2)=='i')) {
						mStdArg(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='#') && (LA(2)=='e')) {
						mPPError(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='#') && (LA(2)=='w')) {
						mPPWarning(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='#') && (LA(2)=='p')) {
						mPragma(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='&') && (true)) {
						mAnd(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='*') && (true)) {
						mMult(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='+') && (true)) {
						mPlus(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='-') && (true)) {
						mSub(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='!') && (true)) {
						mNot(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='/') && (true)) {
						mSlash(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='%') && (true)) {
						mMod(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='<') && (true)) {
						mLAngle(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='>') && (true)) {
						mRAngle(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='^') && (true)) {
						mXor(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='|') && (true)) {
						mOr(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)==':') && (true)) {
						mColon(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='=') && (true)) {
						mAssign(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='#') && (true)) {
						mHatch(true);
						theRetToken=_returnToken;
					}
					else if ((_tokenSet_0.member(LA(1))) && (true)) {
						mIdentifier(true);
						theRetToken=_returnToken;
					}
				else {
					if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}
				else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				}
				if ( _returnToken==null ) continue tryAgain; // found SKIP token
				_ttype = _returnToken.getType();
				_returnToken.setType(_ttype);
				return _returnToken;
			}
			catch (RecognitionException e) {
				throw new TokenStreamRecognitionException(e);
			}
		}
		catch (CharStreamException cse) {
			if ( cse instanceof CharStreamIOException ) {
				throw new TokenStreamIOException(((CharStreamIOException)cse).io);
			}
			else {
				throw new TokenStreamException(cse.getMessage());
			}
		}
	}
}

	public final void mWS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = WS;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case '\t':
		{
			match('\t');
			break;
		}
		case '\r':
		{
			match('\r');
			if ( inputState.guessing==0 ) {
				newline();
			}
			break;
		}
		case ' ':
		{
			match(' ');
			break;
		}
		case '\n':
		{
			match('\n');
			if ( inputState.guessing==0 ) {
				newline();
			}
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			_ttype = Token.SKIP;
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mOctalDigit(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OctalDigit;
		int _saveIndex;
		
		matchRange('0','7');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mOctalConstant(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OctalConstant;
		int _saveIndex;
		
		match('0');
		{
		_loop295:
		do {
			if (((LA(1) >= '0' && LA(1) <= '7'))) {
				mOctalDigit(false);
			}
			else {
				break _loop295;
			}
			
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mNonzeroDigit(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NonzeroDigit;
		int _saveIndex;
		
		matchRange('1','9');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexadecimalDigit(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexadecimalDigit;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			matchRange('0','9');
			break;
		}
		case 'a':  case 'b':  case 'c':  case 'd':
		case 'e':  case 'f':
		{
			matchRange('a','f');
			break;
		}
		case 'A':  case 'B':  case 'C':  case 'D':
		case 'E':  case 'F':
		{
			matchRange('A','F');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mIntegerSuffix(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = IntegerSuffix;
		int _saveIndex;
		
		if ((LA(1)=='L'||LA(1)=='l') && (LA(2)=='L'||LA(2)=='l') && (LA(3)=='U'||LA(3)=='u')) {
			mLongLongUnsignedSuffix(false);
			if ( inputState.guessing==0 ) {
				unsignedSubtype = true; longLongSubtype = true;
			}
		}
		else if ((LA(1)=='U'||LA(1)=='u') && (LA(2)=='L'||LA(2)=='l') && (LA(3)=='L'||LA(3)=='l')) {
			mUnsignedLongLongSuffix(false);
			if ( inputState.guessing==0 ) {
				unsignedSubtype = true; longLongSubtype = true;
			}
		}
		else if ((LA(1)=='L'||LA(1)=='l') && (LA(2)=='L'||LA(2)=='l') && (true)) {
			mLongLongSuffix(false);
			if ( inputState.guessing==0 ) {
				longLongSubtype = true;
			}
		}
		else if ((LA(1)=='L'||LA(1)=='l') && (LA(2)=='U'||LA(2)=='u')) {
			mLongUnsignedSuffix(false);
			if ( inputState.guessing==0 ) {
				unsignedSubtype = true; longSubtype = true;
			}
		}
		else if ((LA(1)=='U'||LA(1)=='u') && (LA(2)=='L'||LA(2)=='l') && (true)) {
			mUnsignedLongSuffix(false);
			if ( inputState.guessing==0 ) {
				unsignedSubtype = true; longSubtype = true;
			}
		}
		else if ((LA(1)=='L'||LA(1)=='l') && (true)) {
			mLongSuffix(false);
			if ( inputState.guessing==0 ) {
				longSubtype     = true;
			}
		}
		else if ((LA(1)=='U'||LA(1)=='u') && (true)) {
			mUnsignedSuffix(false);
			if ( inputState.guessing==0 ) {
				unsignedSubtype = true;
			}
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mLongLongSuffix(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LongLongSuffix;
		int _saveIndex;
		
		if ((LA(1)=='l') && (LA(2)=='l')) {
			match("ll");
		}
		else if ((LA(1)=='l') && (LA(2)=='L')) {
			match("lL");
		}
		else if ((LA(1)=='L') && (LA(2)=='l')) {
			match("Ll");
		}
		else if ((LA(1)=='L') && (LA(2)=='L')) {
			match("LL");
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mLongLongUnsignedSuffix(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LongLongUnsignedSuffix;
		int _saveIndex;
		
		if ((LA(1)=='l') && (LA(2)=='l') && (LA(3)=='u')) {
			match("llu");
		}
		else if ((LA(1)=='l') && (LA(2)=='l') && (LA(3)=='U')) {
			match("llU");
		}
		else if ((LA(1)=='L') && (LA(2)=='L') && (LA(3)=='u')) {
			match("LLu");
		}
		else if ((LA(1)=='L') && (LA(2)=='L') && (LA(3)=='U')) {
			match("LLU");
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mLongSuffix(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LongSuffix;
		int _saveIndex;
		
		switch ( LA(1)) {
		case 'l':
		{
			match("l");
			break;
		}
		case 'L':
		{
			match("L");
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mLongUnsignedSuffix(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LongUnsignedSuffix;
		int _saveIndex;
		
		if ((LA(1)=='l') && (LA(2)=='u')) {
			match("lu");
		}
		else if ((LA(1)=='l') && (LA(2)=='U')) {
			match("lU");
		}
		else if ((LA(1)=='L') && (LA(2)=='u')) {
			match("Lu");
		}
		else if ((LA(1)=='L') && (LA(2)=='U')) {
			match("LU");
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mUnsignedLongLongSuffix(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UnsignedLongLongSuffix;
		int _saveIndex;
		
		if ((LA(1)=='u') && (LA(2)=='l')) {
			match("ull");
		}
		else if ((LA(1)=='U') && (LA(2)=='l')) {
			match("Ull");
		}
		else if ((LA(1)=='u') && (LA(2)=='L')) {
			match("uLL");
		}
		else if ((LA(1)=='U') && (LA(2)=='L')) {
			match("ULL");
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mUnsignedLongSuffix(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UnsignedLongSuffix;
		int _saveIndex;
		
		if ((LA(1)=='u') && (LA(2)=='l')) {
			match("ul");
		}
		else if ((LA(1)=='U') && (LA(2)=='l')) {
			match("Ul");
		}
		else if ((LA(1)=='u') && (LA(2)=='L')) {
			match("uL");
		}
		else if ((LA(1)=='U') && (LA(2)=='L')) {
			match("UL");
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mUnsignedSuffix(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UnsignedSuffix;
		int _saveIndex;
		
		switch ( LA(1)) {
		case 'u':
		{
			match("u");
			break;
		}
		case 'U':
		{
			match("U");
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mExponentPart(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ExponentPart;
		int _saveIndex;
		
		switch ( LA(1)) {
		case 'e':
		{
			match('e');
			mExponent(false);
			break;
		}
		case 'E':
		{
			match('E');
			mExponent(false);
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mExponent(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Exponent;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case '+':
		{
			match('+');
			break;
		}
		case '-':
		{
			match('-');
			break;
		}
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		mDigitSequence(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mDigitSequence(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DigitSequence;
		int _saveIndex;
		
		{
		int _cnt311=0;
		_loop311:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				mDigit(false);
			}
			else {
				if ( _cnt311>=1 ) { break _loop311; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt311++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mDigit(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Digit;
		int _saveIndex;
		
		matchRange('0','9');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mBinaryExponentPart(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = BinaryExponentPart;
		int _saveIndex;
		
		switch ( LA(1)) {
		case 'P':
		{
			match('P');
			mExponent(false);
			break;
		}
		case 'p':
		{
			match('p');
			mExponent(false);
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mFloatingSuffix(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FloatingSuffix;
		int _saveIndex;
		
		switch ( LA(1)) {
		case 'f':
		{
			match('f');
			if ( inputState.guessing==0 ) {
				floatSubtype = true;
			}
			break;
		}
		case 'F':
		{
			match('F');
			if ( inputState.guessing==0 ) {
				floatSubtype = true;
			}
			break;
		}
		case 'l':
		{
			match('l');
			if ( inputState.guessing==0 ) {
				longSubtype = true;
			}
			break;
		}
		case 'L':
		{
			match('L');
			if ( inputState.guessing==0 ) {
				longSubtype = true;
			}
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexDoubleValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexDoubleValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexFloatValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexFloatValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexLongDoubleValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexLongDoubleValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexUnsignedIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexUnsignedIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexUnsignedLongIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexUnsignedLongIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexUnsignedLongLongIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexUnsignedLongLongIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexLongIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexLongIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexLongLongIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexLongLongIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mDoubleValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DoubleValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mFloatValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FloatValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mLongDoubleValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LongDoubleValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mUnsignedIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UnsignedIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mUnsignedLongIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UnsignedLongIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mUnsignedLongLongIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UnsignedLongLongIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = IntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mLongIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LongIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mLongLongIntValue(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LongLongIntValue;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mConstant(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Constant;
		int _saveIndex;
		
		if ((LA(1)=='0') && (LA(2)=='X'||LA(2)=='x')) {
			{
			_saveIndex=text.length();
			mHexPrefix(false);
			text.setLength(_saveIndex);
			if ( inputState.guessing==0 ) {
				resetSubtypes();
			}
			{
			switch ( LA(1)) {
			case '.':
			{
				match('.');
				{
				int _cnt336=0;
				_loop336:
				do {
					if ((_tokenSet_1.member(LA(1)))) {
						mHexadecimalDigit(false);
					}
					else {
						if ( _cnt336>=1 ) { break _loop336; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
					}
					
					_cnt336++;
				} while (true);
				}
				mBinaryExponentPart(false);
				{
				if ((_tokenSet_2.member(LA(1)))) {
					_saveIndex=text.length();
					mFloatingSuffix(false);
					text.setLength(_saveIndex);
				}
				else {
				}
				
				}
				if ( inputState.guessing==0 ) {
					realType = true; _ttype = specifyHexFloat();
				}
				break;
			}
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':  case 'A':  case 'B':
			case 'C':  case 'D':  case 'E':  case 'F':
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':
			{
				{
				int _cnt339=0;
				_loop339:
				do {
					if ((_tokenSet_1.member(LA(1)))) {
						mHexadecimalDigit(false);
					}
					else {
						if ( _cnt339>=1 ) { break _loop339; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
					}
					
					_cnt339++;
				} while (true);
				}
				{
				switch ( LA(1)) {
				case '.':
				{
					match('.');
					{
					_loop342:
					do {
						if ((_tokenSet_1.member(LA(1)))) {
							mHexadecimalDigit(false);
						}
						else {
							break _loop342;
						}
						
					} while (true);
					}
					mBinaryExponentPart(false);
					{
					if ((_tokenSet_2.member(LA(1)))) {
						_saveIndex=text.length();
						mFloatingSuffix(false);
						text.setLength(_saveIndex);
					}
					else {
					}
					
					}
					if ( inputState.guessing==0 ) {
						realType = true; _ttype = specifyHexFloat();
					}
					break;
				}
				case 'P':  case 'p':
				{
					mBinaryExponentPart(false);
					{
					if ((_tokenSet_2.member(LA(1)))) {
						_saveIndex=text.length();
						mFloatingSuffix(false);
						text.setLength(_saveIndex);
					}
					else {
					}
					
					}
					if ( inputState.guessing==0 ) {
						realType = true; _ttype = specifyHexFloat();
					}
					break;
				}
				default:
					{
						{
						if ((_tokenSet_3.member(LA(1)))) {
							_saveIndex=text.length();
							mIntegerSuffix(false);
							text.setLength(_saveIndex);
						}
						else {
						}
						
						}
						if ( inputState.guessing==0 ) {
							intType = true; _ttype = specifyHexInt();
						}
					}
				}
				}
				break;
			}
			default:
			{
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
			}
			}
			}
			}
		}
		else if ((_tokenSet_4.member(LA(1))) && (true)) {
			{
			boolean synPredMatched367 = false;
			if ((((LA(1) >= '0' && LA(1) <= '9')) && (_tokenSet_5.member(LA(2))) && (_tokenSet_6.member(LA(3))))) {
				int _m367 = mark();
				synPredMatched367 = true;
				inputState.guessing++;
				try {
					{
					{
					int _cnt365=0;
					_loop365:
					do {
						if (((LA(1) >= '0' && LA(1) <= '9'))) {
							mDigit(false);
						}
						else {
							if ( _cnt365>=1 ) { break _loop365; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
						}
						
						_cnt365++;
					} while (true);
					}
					{
					switch ( LA(1)) {
					case 'e':
					{
						match('e');
						break;
					}
					case 'E':
					{
						match('E');
						break;
					}
					default:
					{
						throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
					}
					}
					}
					}
				}
				catch (RecognitionException pe) {
					synPredMatched367 = false;
				}
				rewind(_m367);
				inputState.guessing--;
			}
			if ( synPredMatched367 ) {
				{
				int _cnt369=0;
				_loop369:
				do {
					if (((LA(1) >= '0' && LA(1) <= '9'))) {
						mDigit(false);
					}
					else {
						if ( _cnt369>=1 ) { break _loop369; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
					}
					
					_cnt369++;
				} while (true);
				}
				mExponentPart(false);
				if ( inputState.guessing==0 ) {
					resetSubtypes();
				}
				{
				if ((_tokenSet_2.member(LA(1)))) {
					_saveIndex=text.length();
					mFloatingSuffix(false);
					text.setLength(_saveIndex);
				}
				else {
				}
				
				}
				if ( inputState.guessing==0 ) {
					realType = true; _ttype = specifyFloat();
				}
			}
			else {
				boolean synPredMatched348 = false;
				if (((LA(1)=='.') && ((LA(2) >= '0' && LA(2) <= '9')))) {
					int _m348 = mark();
					synPredMatched348 = true;
					inputState.guessing++;
					try {
						{
						match('.');
						mDigit(false);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched348 = false;
					}
					rewind(_m348);
					inputState.guessing--;
				}
				if ( synPredMatched348 ) {
					match('.');
					{
					int _cnt350=0;
					_loop350:
					do {
						if (((LA(1) >= '0' && LA(1) <= '9'))) {
							mDigit(false);
						}
						else {
							if ( _cnt350>=1 ) { break _loop350; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
						}
						
						_cnt350++;
					} while (true);
					}
					{
					if ((LA(1)=='E'||LA(1)=='e')) {
						mExponentPart(false);
					}
					else {
					}
					
					}
					if ( inputState.guessing==0 ) {
						resetSubtypes();
					}
					{
					if ((_tokenSet_2.member(LA(1)))) {
						_saveIndex=text.length();
						mFloatingSuffix(false);
						text.setLength(_saveIndex);
					}
					else {
					}
					
					}
					if ( inputState.guessing==0 ) {
						realType = true; _ttype = specifyFloat();
					}
				}
				else {
					boolean synPredMatched356 = false;
					if ((((LA(1) >= '0' && LA(1) <= '9')) && (_tokenSet_4.member(LA(2))) && (true))) {
						int _m356 = mark();
						synPredMatched356 = true;
						inputState.guessing++;
						try {
							{
							{
							int _cnt355=0;
							_loop355:
							do {
								if (((LA(1) >= '0' && LA(1) <= '9'))) {
									mDigit(false);
								}
								else {
									if ( _cnt355>=1 ) { break _loop355; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
								}
								
								_cnt355++;
							} while (true);
							}
							match('.');
							}
						}
						catch (RecognitionException pe) {
							synPredMatched356 = false;
						}
						rewind(_m356);
						inputState.guessing--;
					}
					if ( synPredMatched356 ) {
						{
						int _cnt358=0;
						_loop358:
						do {
							if (((LA(1) >= '0' && LA(1) <= '9'))) {
								mDigit(false);
							}
							else {
								if ( _cnt358>=1 ) { break _loop358; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
							}
							
							_cnt358++;
						} while (true);
						}
						match('.');
						{
						_loop360:
						do {
							if (((LA(1) >= '0' && LA(1) <= '9'))) {
								mDigit(false);
							}
							else {
								break _loop360;
							}
							
						} while (true);
						}
						{
						if ((LA(1)=='E'||LA(1)=='e')) {
							mExponentPart(false);
						}
						else {
						}
						
						}
						if ( inputState.guessing==0 ) {
							resetSubtypes();
						}
						{
						if ((_tokenSet_2.member(LA(1)))) {
							_saveIndex=text.length();
							mFloatingSuffix(false);
							text.setLength(_saveIndex);
						}
						else {
						}
						
						}
						if ( inputState.guessing==0 ) {
							realType = true; _ttype = specifyFloat();
						}
					}
					else {
						boolean synPredMatched378 = false;
						if (((LA(1)=='.') && (LA(2)=='.'))) {
							int _m378 = mark();
							synPredMatched378 = true;
							inputState.guessing++;
							try {
								{
								match("...");
								}
							}
							catch (RecognitionException pe) {
								synPredMatched378 = false;
							}
							rewind(_m378);
							inputState.guessing--;
						}
						if ( synPredMatched378 ) {
							match("...");
							if ( inputState.guessing==0 ) {
								_ttype = Varargs;
							}
						}
						else if (((LA(1) >= '1' && LA(1) <= '9')) && (true) && (true)) {
							mNonzeroDigit(false);
							{
							_loop372:
							do {
								if (((LA(1) >= '0' && LA(1) <= '9'))) {
									mDigit(false);
								}
								else {
									break _loop372;
								}
								
							} while (true);
							}
							if ( inputState.guessing==0 ) {
								resetSubtypes();
							}
							{
							if ((_tokenSet_3.member(LA(1)))) {
								_saveIndex=text.length();
								mIntegerSuffix(false);
								text.setLength(_saveIndex);
							}
							else {
							}
							
							}
							if ( inputState.guessing==0 ) {
								intType = true; _ttype = specifyInt();
							}
						}
						else if ((LA(1)=='0') && (true) && (true)) {
							match('0');
							{
							_loop375:
							do {
								if (((LA(1) >= '0' && LA(1) <= '7'))) {
									mOctalDigit(false);
								}
								else {
									break _loop375;
								}
								
							} while (true);
							}
							if ( inputState.guessing==0 ) {
								resetSubtypes();
							}
							{
							if ((_tokenSet_3.member(LA(1)))) {
								_saveIndex=text.length();
								mIntegerSuffix(false);
								text.setLength(_saveIndex);
							}
							else {
							}
							
							}
							if ( inputState.guessing==0 ) {
								intType = true; _ttype = specifyInt();
							}
						}
						else {
							boolean synPredMatched380 = false;
							if (((LA(1)=='.') && (true))) {
								int _m380 = mark();
								synPredMatched380 = true;
								inputState.guessing++;
								try {
									{
									match('.');
									}
								}
								catch (RecognitionException pe) {
									synPredMatched380 = false;
								}
								rewind(_m380);
								inputState.guessing--;
							}
							if ( synPredMatched380 ) {
								match('.');
								if ( inputState.guessing==0 ) {
									_ttype = Dot;
								}
							}
							else {
								throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
							}
							}}}}
							}
						}
						else {
							throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
						}
						
						if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
							_token = makeToken(_ttype);
							_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
						}
						_returnToken = _token;
					}
					
	protected final void mHexPrefix(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexPrefix;
		int _saveIndex;
		
		if ((LA(1)=='0') && (LA(2)=='x')) {
			match("0x");
		}
		else if ((LA(1)=='0') && (LA(2)=='X')) {
			match("0X");
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mCCharSequence(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CCharSequence;
		int _saveIndex;
		
		{
		int _cnt383=0;
		_loop383:
		do {
			if ((_tokenSet_7.member(LA(1)))) {
				mCChar(false);
			}
			else {
				if ( _cnt383>=1 ) { break _loop383; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt383++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mCChar(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CChar;
		int _saveIndex;
		
		if ((LA(1)=='\\')) {
			mEscapeSequence(false);
		}
		else if ((_tokenSet_8.member(LA(1)))) {
			{
			match(_tokenSet_8);
			}
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mEscapeSequence(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = EscapeSequence;
		int _saveIndex;
		
		char x;
		
		
		match('\\');
		{
		switch ( LA(1)) {
		case '\'':
		{
			match('\'');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('\'');
			}
			break;
		}
		case '"':
		{
			match('"');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('"');
			}
			break;
		}
		case '?':
		{
			match('?');
			break;
		}
		case '\\':
		{
			match('\\');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('\\');
			}
			break;
		}
		case 'a':
		{
			match('a');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('\007');
			}
			break;
		}
		case 'b':
		{
			match('b');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('\b');
			}
			break;
		}
		case 'e':
		{
			match('e');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('\033');
			}
			break;
		}
		case 'f':
		{
			match('f');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('\f');
			}
			break;
		}
		case 'n':
		{
			match('n');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('\n');
			}
			break;
		}
		case 'r':
		{
			match('r');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('\r');
			}
			break;
		}
		case 't':
		{
			match('t');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('\t');
			}
			break;
		}
		case 'v':
		{
			match('v');
			if ( inputState.guessing==0 ) {
				text.setLength(_begin); text.append('\013');
			}
			break;
		}
		case 'x':
		{
			match('x');
			mHexadecimalDigit(false);
			{
			boolean synPredMatched390 = false;
			if (((_tokenSet_1.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true))) {
				int _m390 = mark();
				synPredMatched390 = true;
				inputState.guessing++;
				try {
					{
					mHexadecimalDigit(false);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched390 = false;
				}
				rewind(_m390);
				inputState.guessing--;
			}
			if ( synPredMatched390 ) {
				mHexadecimalDigit(false);
			}
			else if (((LA(1) >= '\u0000' && LA(1) <= '\ufffe')) && (true) && (true)) {
			}
			else {
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
			}
			
			}
			if ( inputState.guessing==0 ) {
				
				String str = new String(text.getBuffer(),_begin,text.length()-_begin);
				x=((char)Integer.parseInt(str.substring(2), 16)); text.setLength(_begin); text.append(x);
				
			}
			break;
		}
		case 'u':
		{
			match('u');
			mHexQuad(false);
			{
			boolean synPredMatched393 = false;
			if (((_tokenSet_1.member(LA(1))) && (_tokenSet_1.member(LA(2))) && (_tokenSet_1.member(LA(3))))) {
				int _m393 = mark();
				synPredMatched393 = true;
				inputState.guessing++;
				try {
					{
					mHexQuad(false);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched393 = false;
				}
				rewind(_m393);
				inputState.guessing--;
			}
			if ( synPredMatched393 ) {
				{
				mHexQuad(false);
				}
			}
			else if (((LA(1) >= '\u0000' && LA(1) <= '\ufffe')) && (true) && (true)) {
			}
			else {
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
			}
			
			}
			break;
		}
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		{
			mOctalDigit(false);
			{
			boolean synPredMatched397 = false;
			if ((((LA(1) >= '0' && LA(1) <= '7')) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true))) {
				int _m397 = mark();
				synPredMatched397 = true;
				inputState.guessing++;
				try {
					{
					mOctalDigit(false);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched397 = false;
				}
				rewind(_m397);
				inputState.guessing--;
			}
			if ( synPredMatched397 ) {
				mOctalDigit(false);
				{
				boolean synPredMatched400 = false;
				if ((((LA(1) >= '0' && LA(1) <= '7')) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true))) {
					int _m400 = mark();
					synPredMatched400 = true;
					inputState.guessing++;
					try {
						{
						mOctalDigit(false);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched400 = false;
					}
					rewind(_m400);
					inputState.guessing--;
				}
				if ( synPredMatched400 ) {
					mOctalDigit(false);
				}
				else if (((LA(1) >= '\u0000' && LA(1) <= '\ufffe')) && (true) && (true)) {
				}
				else {
					throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
				}
				
				}
			}
			else if (((LA(1) >= '\u0000' && LA(1) <= '\ufffe')) && (true) && (true)) {
			}
			else {
				throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
			}
			
			}
			if ( inputState.guessing==0 ) {
				
				String str = new String(text.getBuffer(),_begin,text.length()-_begin);
				x=(char)Integer.parseInt(str.substring(1), 8); text.setLength(_begin); text.append(x);
				
			}
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHexQuad(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HexQuad;
		int _saveIndex;
		
		mHexadecimalDigit(false);
		mHexadecimalDigit(false);
		mHexadecimalDigit(false);
		mHexadecimalDigit(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCharacterConstant(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CharacterConstant;
		int _saveIndex;
		
		_saveIndex=text.length();
		match('\'');
		text.setLength(_saveIndex);
		mCCharSequence(false);
		_saveIndex=text.length();
		match('\'');
		text.setLength(_saveIndex);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mWideCharacterConstant(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = WideCharacterConstant;
		int _saveIndex;
		
		_saveIndex=text.length();
		match('L');
		text.setLength(_saveIndex);
		_saveIndex=text.length();
		match('\'');
		text.setLength(_saveIndex);
		mCCharSequence(false);
		_saveIndex=text.length();
		match('\'');
		text.setLength(_saveIndex);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mStringLit(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = StringLit;
		int _saveIndex;
		
		_saveIndex=text.length();
		match('"');
		text.setLength(_saveIndex);
		{
		_loop405:
		do {
			if ((_tokenSet_9.member(LA(1)))) {
				mSChar(false);
			}
			else {
				break _loop405;
			}
			
		} while (true);
		}
		_saveIndex=text.length();
		match('"');
		text.setLength(_saveIndex);
		if ( inputState.guessing==0 ) {
			
			String str = new String(text.getBuffer(),_begin,text.length()-_begin);
			text.setLength(_begin); text.append(str+ "\0");
			
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSChar(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SChar;
		int _saveIndex;
		
		if ((LA(1)=='\\')) {
			mEscapeSequence(false);
		}
		else if ((_tokenSet_10.member(LA(1)))) {
			{
			match(_tokenSet_10);
			}
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mWideStringLiteral(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = WideStringLiteral;
		int _saveIndex;
		
		_saveIndex=text.length();
		match('L');
		text.setLength(_saveIndex);
		_saveIndex=text.length();
		match('"');
		text.setLength(_saveIndex);
		{
		_loop408:
		do {
			if ((_tokenSet_9.member(LA(1)))) {
				mSChar(false);
			}
			else {
				break _loop408;
			}
			
		} while (true);
		}
		_saveIndex=text.length();
		match('"');
		text.setLength(_saveIndex);
		if ( inputState.guessing==0 ) {
			
			String str = new String(text.getBuffer(),_begin,text.length()-_begin);
			text.setLength(_begin); text.append(str+ "\0");
			
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLBrace(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LBrace;
		int _saveIndex;
		
		match('{');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRBrace(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RBrace;
		int _saveIndex;
		
		match('}');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLBracket(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LBracket;
		int _saveIndex;
		
		match('[');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRBracket(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RBracket;
		int _saveIndex;
		
		match(']');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLParen(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LParen;
		int _saveIndex;
		
		match('(');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRParen(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RParen;
		int _saveIndex;
		
		match(')');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSelect(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Select;
		int _saveIndex;
		
		match("->");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mInc(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Inc;
		int _saveIndex;
		
		match("++");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDec(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Dec;
		int _saveIndex;
		
		match("--");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mAnd(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = And;
		int _saveIndex;
		
		match('&');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMult(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Mult;
		int _saveIndex;
		
		match('*');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPlus(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Plus;
		int _saveIndex;
		
		match('+');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSub(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Sub;
		int _saveIndex;
		
		match('-');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mComp(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Comp;
		int _saveIndex;
		
		match('~');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNot(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Not;
		int _saveIndex;
		
		match('!');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSlash(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Slash;
		int _saveIndex;
		
		match('/');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMod(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Mod;
		int _saveIndex;
		
		match('%');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLShift(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LShift;
		int _saveIndex;
		
		match("<<");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRShift(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RShift;
		int _saveIndex;
		
		match(">>");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLAngle(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LAngle;
		int _saveIndex;
		
		match('<');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRAngle(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RAngle;
		int _saveIndex;
		
		match('>');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLEqual(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LEqual;
		int _saveIndex;
		
		match("<=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mGEqual(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = GEqual;
		int _saveIndex;
		
		match(">=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mEqual(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Equal;
		int _saveIndex;
		
		match("==");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNEqual(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NEqual;
		int _saveIndex;
		
		match("!=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mXor(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Xor;
		int _saveIndex;
		
		match('^');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mAndCond(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = AndCond;
		int _saveIndex;
		
		match("&&");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mOrCond(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OrCond;
		int _saveIndex;
		
		match("||");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mOr(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Or;
		int _saveIndex;
		
		match('|');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mQMark(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = QMark;
		int _saveIndex;
		
		match('?');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mColon(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Colon;
		int _saveIndex;
		
		match(':');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSemi(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Semi;
		int _saveIndex;
		
		match(';');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Assign;
		int _saveIndex;
		
		match('=');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMultAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = MultAssign;
		int _saveIndex;
		
		match("*=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSlashAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SlashAssign;
		int _saveIndex;
		
		match("/=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mModAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ModAssign;
		int _saveIndex;
		
		match("%=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPlusAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PlusAssign;
		int _saveIndex;
		
		match("+=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSubAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SubAssign;
		int _saveIndex;
		
		match("-=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLSAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LSAssign;
		int _saveIndex;
		
		match("<<=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRSAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RSAssign;
		int _saveIndex;
		
		match(">>=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mAndAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = AndAssign;
		int _saveIndex;
		
		match("&=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mXorAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = XorAssign;
		int _saveIndex;
		
		match("^=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mOrAssign(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OrAssign;
		int _saveIndex;
		
		match("|=");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mComma(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Comma;
		int _saveIndex;
		
		match(',');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mHatch(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Hatch;
		int _saveIndex;
		
		match('#');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDHatch(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DHatch;
		int _saveIndex;
		
		match("##");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLColon(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LColon;
		int _saveIndex;
		
		match("<:");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRColon(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RColon;
		int _saveIndex;
		
		match(":>");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLMod(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LMod;
		int _saveIndex;
		
		match("<%");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRMod(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RMod;
		int _saveIndex;
		
		match(">%");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMColon(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = MColon;
		int _saveIndex;
		
		match("%:");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMCMColon(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = MCMColon;
		int _saveIndex;
		
		match("%:%:");
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPPLine(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PPLine;
		int _saveIndex;
		Token l=null;
		Token f=null;
		
		match("#line");
		{
		int _cnt466=0;
		_loop466:
		do {
			if ((LA(1)==' ')) {
				match(' ');
			}
			else {
				if ( _cnt466>=1 ) { break _loop466; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt466++;
		} while (true);
		}
		mLineNumber(true);
		l=_returnToken;
		{
		int _cnt468=0;
		_loop468:
		do {
			if ((LA(1)==' ')) {
				match(' ');
			}
			else {
				if ( _cnt468>=1 ) { break _loop468; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt468++;
		} while (true);
		}
		match('"');
		mText1(true);
		f=_returnToken;
		match('"');
		match('\n');
		if ( inputState.guessing==0 ) {
			newline();
		}
		if ( inputState.guessing==0 ) {
			
			setLine(Integer.parseInt(l.getText()) + 1);
			parser.setFilename(f.getText());
			setText(" ");
			_ttype = Token.SKIP;
			
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mLineNumber(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LineNumber;
		int _saveIndex;
		
		{
		int _cnt487=0;
		_loop487:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				mDigit(false);
			}
			else {
				if ( _cnt487>=1 ) { break _loop487; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt487++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mText1(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Text1;
		int _saveIndex;
		
		{
		_loop491:
		do {
			if ((_tokenSet_11.member(LA(1)))) {
				{
				match(_tokenSet_11);
				}
			}
			else {
				break _loop491;
			}
			
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mStdArg(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = StdArg;
		int _saveIndex;
		
		match("#include");
		{
		_loop471:
		do {
			if ((LA(1)==' ')) {
				match(' ');
			}
			else {
				break _loop471;
			}
			
		} while (true);
		}
		match('<');
		{
		_loop473:
		do {
			if ((LA(1)==' ')) {
				match(' ');
			}
			else {
				break _loop473;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case 's':
		{
			match("stdarg.h");
			break;
		}
		case 'v':
		{
			match("va_list.h");
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		{
		_loop476:
		do {
			if ((LA(1)==' ')) {
				match(' ');
			}
			else {
				break _loop476;
			}
			
		} while (true);
		}
		match('>');
		{
		_loop478:
		do {
			if ((LA(1)==' ')) {
				match(' ');
			}
			else {
				break _loop478;
			}
			
		} while (true);
		}
		match('\n');
		if ( inputState.guessing==0 ) {
			newline();
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPPError(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PPError;
		int _saveIndex;
		Token f=null;
		
		match("#error");
		{
		switch ( LA(1)) {
		case '\n':
		{
			match('\n');
			if ( inputState.guessing==0 ) {
				newline(); setText("");
			}
			break;
		}
		case ' ':
		{
			match(' ');
			mText2(true);
			f=_returnToken;
			match('\n');
			if ( inputState.guessing==0 ) {
				newline(); setText(f.getText());
			}
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mText2(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Text2;
		int _saveIndex;
		
		{
		_loop495:
		do {
			if ((_tokenSet_12.member(LA(1)))) {
				{
				match(_tokenSet_12);
				}
			}
			else {
				break _loop495;
			}
			
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPPWarning(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PPWarning;
		int _saveIndex;
		Token f=null;
		
		match("#warning");
		{
		switch ( LA(1)) {
		case '\n':
		{
			match('\n');
			if ( inputState.guessing==0 ) {
				newline(); setText("");
			}
			break;
		}
		case ' ':
		{
			match(' ');
			mText2(true);
			f=_returnToken;
			match('\n');
			if ( inputState.guessing==0 ) {
				newline(); setText(f.getText());
			}
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPragma(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Pragma;
		int _saveIndex;
		Token f=null;
		
		match("#pragma");
		{
		switch ( LA(1)) {
		case '\n':
		{
			match('\n');
			if ( inputState.guessing==0 ) {
				newline(); setText("");
			}
			break;
		}
		case ' ':
		{
			match(' ');
			mText2(true);
			f=_returnToken;
			match('\n');
			if ( inputState.guessing==0 ) {
				newline(); setText(f.getText());
			}
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mDot(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Dot;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mVarargs(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Varargs;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mAttributes(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Attributes;
		int _saveIndex;
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mIdentifier(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Identifier;
		int _saveIndex;
		
		boolean synPredMatched501 = false;
		if (((LA(1)=='_') && (LA(2)=='_') && (LA(3)=='a'))) {
			int _m501 = mark();
			synPredMatched501 = true;
			inputState.guessing++;
			try {
				{
				match("__attribute__");
				}
			}
			catch (RecognitionException pe) {
				synPredMatched501 = false;
			}
			rewind(_m501);
			inputState.guessing--;
		}
		if ( synPredMatched501 ) {
			match("__attribute__");
			{
			_loop503:
			do {
				if ((_tokenSet_13.member(LA(1)))) {
					mWS(false);
				}
				else {
					break _loop503;
				}
				
			} while (true);
			}
			match("((");
			mAtt(false);
			match("))");
			if ( inputState.guessing==0 ) {
				
				_ttype = Attributes;
				
			}
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true)) {
			mIdentifierNondigit(false);
			{
			_loop505:
			do {
				switch ( LA(1)) {
				case 'A':  case 'B':  case 'C':  case 'D':
				case 'E':  case 'F':  case 'G':  case 'H':
				case 'I':  case 'J':  case 'K':  case 'L':
				case 'M':  case 'N':  case 'O':  case 'P':
				case 'Q':  case 'R':  case 'S':  case 'T':
				case 'U':  case 'V':  case 'W':  case 'X':
				case 'Y':  case 'Z':  case '\\':  case '_':
				case 'a':  case 'b':  case 'c':  case 'd':
				case 'e':  case 'f':  case 'g':  case 'h':
				case 'i':  case 'j':  case 'k':  case 'l':
				case 'm':  case 'n':  case 'o':  case 'p':
				case 'q':  case 'r':  case 's':  case 't':
				case 'u':  case 'v':  case 'w':  case 'x':
				case 'y':  case 'z':
				{
					mIdentifierNondigit(false);
					break;
				}
				case '0':  case '1':  case '2':  case '3':
				case '4':  case '5':  case '6':  case '7':
				case '8':  case '9':
				{
					mDigit(false);
					break;
				}
				default:
				{
					break _loop505;
				}
				}
			} while (true);
			}
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		_ttype = testLiteralsTable(_ttype);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mAtt(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Att;
		int _saveIndex;
		
		int cnt = 0;
		
		
		{
		_loop509:
		do {
			if (((LA(1)==')') && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && ((LA(3) >= '\u0000' && LA(3) <= '\ufffe')))&&(cnt > 0)) {
				match(')');
				if ( inputState.guessing==0 ) {
					cnt--;
				}
			}
			else if ((LA(1)=='(')) {
				match('(');
				if ( inputState.guessing==0 ) {
					cnt++;
				}
			}
			else if ((_tokenSet_14.member(LA(1)))) {
				{
				match(_tokenSet_14);
				}
			}
			else {
				break _loop509;
			}
			
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mIdentifierNondigit(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = IdentifierNondigit;
		int _saveIndex;
		
		switch ( LA(1)) {
		case 'A':  case 'B':  case 'C':  case 'D':
		case 'E':  case 'F':  case 'G':  case 'H':
		case 'I':  case 'J':  case 'K':  case 'L':
		case 'M':  case 'N':  case 'O':  case 'P':
		case 'Q':  case 'R':  case 'S':  case 'T':
		case 'U':  case 'V':  case 'W':  case 'X':
		case 'Y':  case 'Z':  case '_':  case 'a':
		case 'b':  case 'c':  case 'd':  case 'e':
		case 'f':  case 'g':  case 'h':  case 'i':
		case 'j':  case 'k':  case 'l':  case 'm':
		case 'n':  case 'o':  case 'p':  case 'q':
		case 'r':  case 's':  case 't':  case 'u':
		case 'v':  case 'w':  case 'x':  case 'y':
		case 'z':
		{
			mNondigit(false);
			break;
		}
		case '\\':
		{
			mUniversalCharacterName(false);
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mNondigit(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = Nondigit;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '_':
		{
			match('_');
			break;
		}
		case 'a':  case 'b':  case 'c':  case 'd':
		case 'e':  case 'f':  case 'g':  case 'h':
		case 'i':  case 'j':  case 'k':  case 'l':
		case 'm':  case 'n':  case 'o':  case 'p':
		case 'q':  case 'r':  case 's':  case 't':
		case 'u':  case 'v':  case 'w':  case 'x':
		case 'y':  case 'z':
		{
			matchRange('a','z');
			break;
		}
		case 'A':  case 'B':  case 'C':  case 'D':
		case 'E':  case 'F':  case 'G':  case 'H':
		case 'I':  case 'J':  case 'K':  case 'L':
		case 'M':  case 'N':  case 'O':  case 'P':
		case 'Q':  case 'R':  case 'S':  case 'T':
		case 'U':  case 'V':  case 'W':  case 'X':
		case 'Y':  case 'Z':
		{
			matchRange('A','Z');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mUniversalCharacterName(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UniversalCharacterName;
		int _saveIndex;
		
		match("\\u");
		mHexQuad(false);
		{
		boolean synPredMatched516 = false;
		if (((_tokenSet_1.member(LA(1))) && (_tokenSet_1.member(LA(2))) && (_tokenSet_1.member(LA(3))))) {
			int _m516 = mark();
			synPredMatched516 = true;
			inputState.guessing++;
			try {
				{
				mHexQuad(false);
				}
			}
			catch (RecognitionException pe) {
				synPredMatched516 = false;
			}
			rewind(_m516);
			inputState.guessing--;
		}
		if ( synPredMatched516 ) {
			{
			mHexQuad(false);
			}
		}
		else {
		}
		
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	
	private static final long[] mk_tokenSet_0() {
		long[] data = new long[1025];
		data[1]=576460746263625726L;
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = new long[1025];
		data[0]=287948901175001088L;
		data[1]=541165879422L;
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = new long[1025];
		data[1]=17867063955520L;
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = new long[1025];
		data[1]=9024791442886656L;
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = new long[1025];
		data[0]=288019269919178752L;
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = new long[1025];
		data[0]=287948901175001088L;
		data[1]=137438953504L;
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = new long[1025];
		data[0]=287992881640112128L;
		data[1]=137438953504L;
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = new long[2048];
		data[0]=-549755814913L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = new long[2048];
		data[0]=-549755814913L;
		data[1]=-268435457L;
		for (int i = 2; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = new long[2048];
		data[0]=-17179869185L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = new long[2048];
		data[0]=-17179869185L;
		data[1]=-268435457L;
		for (int i = 2; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = new long[2048];
		data[0]=-17179870209L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = new long[2048];
		data[0]=-1025L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = new long[1025];
		data[0]=4294977024L;
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = new long[2048];
		data[0]=-3298534883329L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	
	}
