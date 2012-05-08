2012-5-5
Symbol table mechanism of Scale
There are mainly three classes when it comes with Scale's symbols: Symtab, SymtabScope and SymtabEntry. We can grasp their essentials through scan their fields.
1.Symtab
/**
 * A class to represent a program's symbol table.
 *
 * A symbol table comprises many scopes arranged in a tree.  Each
 * scope has a unique identification (the scope number) and a local
 * symbol table.  A local symbol table is a dictionary which pairs
 * identifiers with entries.  A single "entries" is a list of
 * entry nodes, where each node represents a single declared
 * entity.  We maintain a list of entry nodes because an identifier
 * could conceivably be used to name multiple entities even within
 * a single scope.  Though not currently used, we do assume that
 * with a scope, each use of a single identifier names a different
 * kind of entity.
 */
public final class Symtab
{
	/**
	 * The root of the scope tree has depth zero.  The currentDepth
	 * field should always hold depth of currently active scope.
	 */
	private SymtabScope                       rootScope;    // The root of the nested scopes.
	private SymtabScope                       currentScope; // The current scope.
	private HashMap<Declaration, SymtabScope> declScope;    // Map from Declaration to scope.
	private int                               currentDepth; // The current depth of the nested scopes.
	
	/**
	 * Here thousands of lines of codes for methods have been omitted. 
	 */
}
Symtab is the superitendent of mechanism. <rootScope>, <currentScope> and <currentDepth> are manifest by their annotations respectively. <declScope> is used to collect all the Declararions of the translation unit and their corresponding scope, then combines them as two-tuples ans stucks these pairs into a HashMap.

2.SymtabScope
/**
 * This class represents a single, local scope (e.g., for a procedure
 * or local block).
 *
 * The primary function of a scope is to maintain a table of
 * (identifier, decl) pairs.  A list of the entries in the order they
 * were added to the scope is also maintained for use when C code is
 * generated.
*/

public class SymtabScope
{

  private HashMap<String, Vector<SymtabEntry>> localSymtab; // This scope's local symbol table.
  private SymtabScope              outerScope;  // Points to the outer scope containg this scope.
  private Vector<SymtabScope>      children;    // A vector of SymtabScope nodes.
  private Vector<SymtabEntry>      order;       // The order that symbols are added.
  private int                      scopeNumber; // This field is this scope's unique identifier.
  private int                      scopeDepth;  // The depth of this scope in the scope tree.

  /**
   * methods. 
   */
}
Using a HashMap, <localSymtab> contains all the symbols of the current scope in which there may be several declarations to an individual symbol name(string). In order to make the fetch of all SymtabEntries to the same string symbol name a constant-time operation, <localSymtab> maps a string to a Vector<SymtabEntry>. In the SymtabScope tree, <outerScope> is parent of current scope, while <children> are its children. <order> collects all the SymtabEntries in the current scope. As to <scopeNumber> and <scopeDepth>, you know it.

3.SymtabEntry
/**
 * This class represents an entry in the symbol table.
 *
 * A symbol table entry is a {@link scale.clef.decl.Declaration declataion}
 * and a link to its {@link scale.clef.symtab.SymtabScope scope}.
 */

public final class SymtabEntry
{
  /**
   * The scope for this symbol.
   */
  private SymtabScope scope;
  /**
   * The declaration for this symbol.
   */
  private Declaration declaration;

  /**
   * methods. 
   */
}

Next, I will use one code snippet to illustrate how symbol table works in the process of AST construction.
/* code snippet begin*/
//scope0
int a = 0;
int b = 1;

int a = 10;

extern int c;

int d = &b;

void main ()
{
	//scope1
	int a = 2;
	int a = 20;	

	int b = 3;
	
	printf ("%x\n", d);
	{
		//scope2
		int b = 4;
		printf ("%x\n", &b);
		{
			//scope3
			int a = 5;
			extern int a;
			extern int a;
			a = 6;
		}
		{
			//scope3
			extern int b;
			int h = b;
			int b = 7;
			//b = 7;
			printf ("%x\n", &b);
			printf ("%d\n", b);
			printf ("%d\n", h);
		}
	}
}
/* code snippet end*/

At the beginning, parser news a <rootScope> and sets <currentScope> to be <currentScope>, then parses "int a = 0;", code action is "decl = defineDecl(declId, dtype, sc, t)", 'declId' is the string name "a", dtype is declaration type, here it is "int", sc is storage type, t is next token, here is ";". In beginning of defineDecl, we should focus on 2 variables:
(1) SymtabScope cp = (sc == cExtern) ? st.getRootScope() : st.getCurrentScope();
(2) Vector<SymtabEntry> ents = st.lookupSymbol (name);
For cp, if sc is sort of cExtern, cp is global scope, that is <rootScope>, or else it is assigned to <currentScope>, which means that if current scope is local scope, extern variables declared in current scope will be added to local symbol table, even if it share the same string name with some other local variable. 
By Symtab's method lookupSymbol, parser collects all the Vector<SymtabEntry> having the keywords string name and dumps them into one Vector<SymtabEntry>, then ents is born.
Now that "a" is the first declaration, parser hardly do anything but news a VariableDecl and adds it into entries in the local symbol table, sets its value to be '0'. So is "int b = 1;".

When parser encounters "int a = 10;", it has to deal with redefination. In the method defineDecl, it detects there exists another 'a' with the same type and scope, so it returns the declararion of the previous 'a' and resets the value of this declaration to be '10'. We can say when parsing "int a = 10;", parser omits the declaration of this variable and snatches its value to give it to the first variable with the same string name and scope. Due to storage type "extern", after constructing a Declaration for variable "c", Declaration's two methods setVisibility and setResidency set this "c"'s two attributes vis and res to EXTERN and MEMORY respectively. 
I will skip "int d = &b;" which is meant to test. Compared with Scale, gcc regards redefination as illegal and doesn't have the error-tolerant mechanism like Scale.

Parser still uses defineDecl to define a declaration for procedure "main", and following this defination, it news a weird scope which seems vacant and I haven't figured out what it is meant to do, because we will soon see a genuine scope for "main" as a child of the fake scope mentioned right now on entering a compoundStatement module. Then parser begins to parse declarations and statements in scope1, it defines a declaration for the "a" in "int a = 2;" as with the "a" in scope0 and adds it into the local symbol table in scope1, what's different is that after initialize "a" with value "2", in procedure processDeclarations, it wear away this value with null and constructs a EvalStmt for operation "a = 2" and adds it in the statements set of the BlockStmt. So I guess that in local scope, parser translates a defination with initializition into a declaration and an assignment operation. Still, defineDecl return the reference of "a" within "int a = 2;" because of its redefination check mechanism, afterwards, parser constructs another EvalStmt for "a = 20", hence, it simply translates the redeclaration "int a = 20;" as an assignment. Then I will skip "int b = 3;" and "printf ("%x\n", d);", "int b = 4" and "printf ("%x\n", &b);" in scope2,  which is also for test, step directly into scope3.

In scope3, parser will put declaration for "a" in "int a = 5;" and the first "extern int a" in one single Entries of current scope with the latter SymtabEntry for "extern int a" lying after that for "int a = 5;" physically, and filter the second "extern int a;". For the "extern"'s sake, when parser deals with "extern int a;", it gets "a"'s declaration from <rootScope> rather than newing a declaration for it. And then, parser news a SymtabEntry for this declaration and current scope, puts it in the Entries mentioned above. When "a = 6;" comes, this declaration of "a" is the one from "int a = 5", which seems unreasonable because "extern int a;" is nearer to "a = 6;" than "int a = 5;". But actually, that's right, redeclaration should have been incorrect and not be allowed, as the case in gcc, Scale syntaxly allows this to happen but sematicly chooses the first declaration of the same string name as the only declaration, putting others as spare tires after it. In another scope3, you will see it more clearly by means of a couple of "prinf"s.

In the second scope3, parser firstly news a SymtabEntry for "extern int b", of which, declaration comes from "int b = 1;" of rootScope, scope is current scope; for "h" only lives in the current scope, for "int h = b;", parser news a SymtabEntry in local symbol table and a EvalStmt of which declaration for "h" and "b" are two components, and the value of "b" is "1" so far. The function of "int b = 7" is to change the value of "b" to "7", as "b" has the global attibute, instead of constructing a EvalStmt for "int b = 7;", parser just changes "b"'s initial value as a new initializer. So, of the following "printf"s, the second and third will both output value "7". However, if you replace "int b = 7;" with "b = 7;", the second print "7" without a question whereas the third get "1" which is "b"'s first initializer, because for "b = 7" which parser translates into an EvalStmt. In conclusion, two prerequisites make something like "Type variable = xxx;" an initializer: the Type quality and the global atribute.

Finally, Scale compiler needs the support of antlr package as its Lexer and Parser are generated by antlr, so donnot forget to appoint your CLASSPATH variable to include antlr.jar.