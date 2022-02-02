package edu.ufl.cise.plc.test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.plc.CompilerComponentFactory;
import edu.ufl.cise.plc.ILexer;
import edu.ufl.cise.plc.IToken;
import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.LexicalException;

import java.util.Arrays;


public class LexerTests {

	ILexer getLexer(String input) throws LexicalException {
		 return CompilerComponentFactory.getLexer(input);
	}
	
	//makes it easy to turn output on and off (and less typing than System.out.println)
	static final boolean VERBOSE = true;
	void show(Object obj) {
		if(VERBOSE) {
			System.out.println(obj);
		}
	}
	
	//check that this token has the expected kind
	void checkToken(IToken t, Kind expectedKind) {
		assertEquals(expectedKind, t.getKind());
	}
		
	//check that the token has the expected kind and position
	void checkToken(IToken t, Kind expectedKind, int expectedLine, int expectedColumn){
		assertEquals(expectedKind, t.getKind());
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
	}

	void checkReserved(IToken t, Kind expectedKind, String expectedName, int expectedLine, int expectedColumn){
		assertEquals(expectedName, t.getText());
		assertEquals(expectedKind, t.getKind());
		assertEquals(expectedLine, t.getSourceLocation().line());
		assertEquals(expectedColumn, t.getSourceLocation().column());
	}
	
	//check that this token is an IDENT and has the expected name
	void checkIdent(IToken t, String expectedName){
		assertEquals(Kind.IDENT, t.getKind());
		assertEquals(expectedName, t.getText());
	}
	
	//check that this token is an IDENT, has the expected name, and has the expected position
	void checkIdent(IToken t, String expectedName, int expectedLine, int expectedColumn){
		checkIdent(t,expectedName);
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
	}
	
	//check that this token is an INT_LIT with expected int value
	void checkInt(IToken t, int expectedValue) {
		assertEquals(Kind.INT_LIT, t.getKind());
		assertEquals(expectedValue, t.getIntValue());	
	}


	void checkBoolean(IToken t, boolean expectedValue){
		assertEquals(Kind.BOOLEAN_LIT, t.getKind());
		assertEquals(expectedValue, t.getBooleanValue());
	}

	void checkFloat(IToken t, float expectedValue){
		assertEquals(Kind.FLOAT_LIT, t.getKind());
		assertEquals(expectedValue, t.getFloatValue());
	}
	//check that this token  is an INT_LIT with expected int value and position
	void checkInt(IToken t, int expectedValue, int expectedLine, int expectedColumn) {
		checkInt(t,expectedValue);
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());		
	}
	
	//check that this token is the EOF token
	void checkEOF(IToken t) {
		checkToken(t, Kind.EOF);
	}
	
	
	//The lexer should add an EOF token to the end.
	@Test
	void testEmpty() throws LexicalException {
		String input = "";
		show(input);
		ILexer lexer = getLexer(input);
		checkEOF(lexer.next());
	}
	
	//A couple of single character tokens
	@Test
	void testSingleChar0() throws LexicalException {
		String input = """
				+       
				-           
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.PLUS, 0,0);
		checkToken(lexer.next(), Kind.MINUS, 1,0);
		checkEOF(lexer.next());
	}
	
	//comments should be skipped
	@Test
	void testComment0() throws LexicalException {
		//Note that the quotes around "This is a string" are passed to the lexer.  
		String input = """
				"This is a string"
				#this is a comment
				*
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 0,0);
		checkToken(lexer.next(), Kind.TIMES, 2,0);
		checkEOF(lexer.next());
	}
	
	//Example for testing input with an illegal character 
	@Test
	void testError0() throws LexicalException {
		String input = """
				abc
				@
				""";
		show(input);
		ILexer lexer = getLexer(input);
		//this check should succeed
		checkIdent(lexer.next(), "abc");
		//this is expected to throw an exception since @ is not a legal 
		//character unless it is part of a string or comment
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
		});
	}
	
	//Several identifiers to test positions
	@Test
	public void testIdent0() throws LexicalException {
		String input = """
				abc
				  def
				     ghi
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "abc", 0,0);
		checkIdent(lexer.next(), "def", 1,2);
		checkIdent(lexer.next(), "ghi", 2,5);
		checkEOF(lexer.next());
	}

	public void testIdent1() throws LexicalException {

	}
	
	@Test
	public void testEquals0() throws LexicalException {
		String input = """
				= == ===
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(),Kind.ASSIGN,0,0);
		checkToken(lexer.next(),Kind.EQUALS,0,2);
		checkToken(lexer.next(),Kind.EQUALS,0,5);
		checkToken(lexer.next(),Kind.ASSIGN,0,7);
		checkEOF(lexer.next());
	}
	
	@Test
	public void testIdenInt() throws LexicalException {
		String input = """
				a123 456b
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.next(), "a123", 0,0);
		checkInt(lexer.next(), 456, 0,5);
		checkIdent(lexer.next(), "b",0,8);
		checkEOF(lexer.next());
		}
	
	
	//example showing how to handle number that are too big.
	@Test
	public void testIntTooBig() throws LexicalException {
		String input = """
				42
				99999999999999999999999999999999999999999999999999999999999999999999999
				""";
		ILexer lexer = getLexer(input);
		checkInt(lexer.next(),42);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();			
		});
	}

	//Custom tests
	@Test
	public void testSingleChars() throws LexicalException{
		String input = """
				+       
				-
				*/  
				[;]          
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.PLUS, 0,0);
		checkToken(lexer.next(), Kind.MINUS, 1,0);
		checkToken(lexer.next(), Kind.TIMES, 2, 0);
		checkToken(lexer.next(), Kind.DIV, 2, 1);
		checkToken(lexer.next(), Kind.LSQUARE, 3, 0);
		checkToken(lexer.next(), Kind.SEMI, 3, 1);
		checkToken(lexer.next(), Kind.RSQUARE, 3, 2);
		checkEOF(lexer.next());
	}

	@Test
	public void testTabs() throws LexicalException{
		String input = """
				+	
				*lmao>>>>>
				[->=
				]
				= == lol
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.PLUS, 0, 0);
		checkToken(lexer.next(), Kind.TIMES, 1, 0);
		checkToken(lexer.next(), Kind.IDENT, 1, 1);
		checkToken(lexer.next(), Kind.RANGLE, 1, 5);
		checkToken(lexer.next(), Kind.RANGLE, 1, 7);
		checkToken(lexer.next(), Kind.GT, 1, 9);
		checkToken(lexer.next(), Kind.LSQUARE, 2, 0);
		checkToken(lexer.next(), Kind.RARROW, 2, 1);
		checkToken(lexer.next(), Kind.ASSIGN, 2, 3);
		checkToken(lexer.next(), Kind.RSQUARE, 3, 0);
		checkToken(lexer.next(), Kind.ASSIGN, 4, 0);
		checkToken(lexer.next(), Kind.EQUALS, 4, 2);
		checkIdent(lexer.next(), "lol", 4,5);
		checkEOF(lexer.next());
	}

	@Test
	public void testLengthTwoTokens() throws LexicalException{
		String input = """
				->>
				==
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.RARROW, 0, 0);
		checkToken(lexer.next(), Kind.GT, 0, 2);
		checkToken(lexer.next(), Kind.EQUALS, 1, 0);
		checkEOF(lexer.next());
	}

	@Test
	public void testEqualsAndAssign() throws LexicalException{
		String input = """
				= == === = 
				=====
				= = = ==
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(),Kind.ASSIGN,0,0);
		checkToken(lexer.next(),Kind.EQUALS,0,2);
		checkToken(lexer.next(),Kind.EQUALS,0,5);
		checkToken(lexer.next(),Kind.ASSIGN,0,7);
		checkToken(lexer.next(), Kind.ASSIGN, 0, 9);
		checkToken(lexer.next(), Kind.EQUALS, 1, 0);
		checkToken(lexer.next(), Kind.EQUALS, 1, 2);
		checkToken(lexer.next(), Kind.ASSIGN, 1, 4);
		checkToken(lexer.next(), Kind.ASSIGN, 2, 0);
		checkToken(lexer.next(), Kind.ASSIGN, 2, 2);
		checkToken(lexer.next(), Kind.ASSIGN, 2, 4);
		checkToken(lexer.next(), Kind.EQUALS, 2, 6);
		checkEOF(lexer.next());
	}

	@Test
	public void testMinusAndRArrows() throws LexicalException{
		String input = """
				->=->==-> -		
				==--->!!=>->
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.RARROW, 0, 0);
		checkToken(lexer.next(), Kind.ASSIGN, 0, 2);
		checkToken(lexer.next(), Kind.RARROW, 0, 3);
		checkToken(lexer.next(), Kind.EQUALS, 0, 5);
		checkToken(lexer.next(), Kind.RARROW, 0, 7);
		checkToken(lexer.next(), Kind.MINUS, 0, 10);
		checkToken(lexer.next(), Kind.EQUALS, 1, 0);
		checkToken(lexer.next(), Kind.MINUS, 1, 2);
		checkToken(lexer.next(), Kind.MINUS, 1, 3);
		checkToken(lexer.next(), Kind.RARROW, 1, 4);
		checkToken(lexer.next(), Kind.BANG, 1, 6);
		checkToken(lexer.next(), Kind.NOT_EQUALS, 1,7);
		checkToken(lexer.next(), Kind.GT, 1, 9);
		checkToken(lexer.next(), Kind.RARROW, 1, 10);
		checkEOF(lexer.next());
	}

	@Test
	public void testBang() throws LexicalException{
		String input = """
				!=
				!!
				!=!
				!!=>>>=<-<<<
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.NOT_EQUALS, 0, 0);
		checkToken(lexer.next(), Kind.BANG, 1, 0);
		checkToken(lexer.next(), Kind.BANG, 1, 1);
		checkToken(lexer.next(), Kind.NOT_EQUALS, 2, 0);
		checkToken(lexer.next(), Kind.BANG, 2, 2);
		checkToken(lexer.next(), Kind.BANG, 3, 0 );
		checkToken(lexer.next(), Kind.NOT_EQUALS, 3, 1);
		checkToken(lexer.next(), Kind.RANGLE, 3, 3);
		checkToken(lexer.next(), Kind.GE, 3, 5);
		checkToken(lexer.next(), Kind.LARROW, 3,7);
		checkToken(lexer.next(), Kind.LANGLE, 3, 9);
		checkToken(lexer.next(), Kind.LT, 3, 11);
		checkEOF(lexer.next());
	}

	@Test
	public void testIdent() throws LexicalException{
		String input = """
				if else
				this
				sampleToken = number
				""";
		show(input);
		ILexer lexer = getLexer(input);

		checkToken(lexer.next(), Kind.KW_IF, 0, 0);
		checkToken(lexer.next(), Kind.KW_ELSE, 0, 3);
		checkToken(lexer.next(), Kind.IDENT, 1, 0);
		checkToken(lexer.next(), Kind.IDENT, 2, 0);
		checkToken(lexer.next(), Kind.ASSIGN, 2, 12);
		checkToken(lexer.next(), Kind.IDENT, 2, 14);
		checkEOF(lexer.next());
	}

	@Test
	public void testExceptions() throws LexicalException{
		String input = """
				00
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.INT_LIT, 0, 0);
		checkToken(lexer.next(), Kind.INT_LIT, 0, 1);
		checkEOF(lexer.next());
	}

	@Test
	public void testIdentNames() throws LexicalException{
		String input = """
				if else
				this
				sampleToken = number
				getGreen()
				ifelse
				N_0
				string water = i
				""";
		show(input);
		ILexer lexer = getLexer(input);

		checkReserved(lexer.next(), Kind.KW_IF,"if", 0,0);
		checkReserved(lexer.next(), Kind.KW_ELSE,"else", 0,3);
		checkReserved(lexer.next(), Kind.IDENT,"this", 1,0);
		checkReserved(lexer.next(), Kind.IDENT,"sampleToken", 2,0);
		checkReserved(lexer.next(), Kind.ASSIGN,"=", 2,12);
		checkReserved(lexer.next(), Kind.IDENT, "number", 2,14);
		checkReserved(lexer.next(), Kind.COLOR_OP, "getGreen", 3, 0);
		checkReserved(lexer.next(), Kind.LPAREN,"(", 3, 8);
		checkReserved(lexer.next(), Kind.RPAREN, ")", 3, 9);
		checkReserved(lexer.next(), Kind.IDENT, "ifelse", 4, 0);
		checkReserved(lexer.next(), Kind.IDENT, "N_0", 5, 0);
		checkReserved(lexer.next(), Kind.TYPE, "string", 6, 0);
		checkReserved(lexer.next(), Kind.IDENT, "water", 6, 7);
		checkReserved(lexer.next(), Kind.ASSIGN, "=", 6, 13);
		checkReserved(lexer.next(), Kind.IDENT, "i", 6, 15);
		checkEOF(lexer.next());
	}

	@Test
	public void testNum() throws LexicalException{
		String input = """
				4035
				34 93
				359
				""";
		show(input);
		ILexer lexer = getLexer(input);

		checkToken(lexer.next(), Kind.INT_LIT, 0, 0);
		checkToken(lexer.next(), Kind.INT_LIT, 1, 0);
		checkToken(lexer.next(), Kind.INT_LIT, 1, 3);
		checkToken(lexer.next(), Kind.INT_LIT, 2, 0);
		checkEOF(lexer.next());
	}

	@Test
	public void testFloat() throws LexicalException{
		String input = """
				4.035
				3.4 9.3
				3.59
				""";
		show(input);
		ILexer lexer = getLexer(input);

		checkToken(lexer.next(), Kind.FLOAT_LIT, 0, 0);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 1, 0);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 1, 4);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 2, 0);
		checkEOF(lexer.next());
	}

	@Test
	public void testZeroFloat() throws LexicalException{
		String input = """
				0.035
				0.4 0.3
				0.59
				""";
		show(input);
		ILexer lexer = getLexer(input);

		checkToken(lexer.next(), Kind.FLOAT_LIT, 0, 0);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 1, 0);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 1, 4);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 2, 0);
		checkEOF(lexer.next());
	}

	@Test
	public void testSampleNumber() throws LexicalException{
		String input = """
				00.15
				""";
		show(input);
		ILexer lexer = getLexer(input);

		checkToken(lexer.next(), Kind.INT_LIT, 0, 0);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 0, 1);
	}

	@Test
	public void intToFloat() throws LexicalException{
		String input = """
				30.035
				20.4 0.3
				70.59
				#
				00.15
				""";
		show(input);
		ILexer lexer = getLexer(input);

		checkToken(lexer.next(), Kind.FLOAT_LIT, 0, 0);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 1, 0);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 1, 5);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 2, 0);
		checkToken(lexer.next(), Kind.INT_LIT, 4, 0);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 4, 1);
		checkEOF(lexer.next());
	}

	@Test
	public void dotInvalid() throws LexicalException{
		String input = """
				..
				""";
		show(input);
		ILexer lexer = getLexer(input);

		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}
	String getASCII(String s) {
		int[] ascii = new int[s.length()];
		for (int i = 0; i != s.length(); i++) {
			ascii[i] = s.charAt(i);
		}
		return Arrays.toString(ascii);
	}

	@Test
	public void testComment() throws LexicalException{
		String input = """
				int x = 10; # Shouldn't register this
				x++;
					#kajsdfpoi34jf9823jh 4 98ph3 mnljknjdopqwj09812389*()U)()(*I)(&*^*&(%^%*#$^&%@%^&@#R&@%$&^%@#&^%@$&U
				# float y = 123.
				float y = 3.24;
				# akljsdhflkjasdhfjkabsdfjknasdfkhjnakljsdfhnlakjsdfhnajslkdfnsldfj
				boolean testPass = true;
				
				""";

		show(input);
		ILexer lexer = getLexer(input);
		checkIdent(lexer.peek(), "x", 0, 4);
		checkToken(lexer.next(), Kind.TYPE, 0, 0);
		checkToken(lexer.next(), Kind.IDENT, 0, 4);
		checkToken(lexer.next(), Kind.ASSIGN, 0, 6);
		checkToken(lexer.next(), Kind.INT_LIT, 0, 8);
		checkToken(lexer.next(), Kind.SEMI, 0, 10);
		checkToken(lexer.next(), Kind.IDENT, 1, 0);
		checkToken(lexer.next(), Kind.PLUS, 1, 1);
		checkToken(lexer.next(), Kind.PLUS, 1, 2);
		checkToken(lexer.next(), Kind.SEMI, 1, 3);
		checkToken(lexer.next(), Kind.TYPE, 4,0);
		checkToken(lexer.next(), Kind.IDENT, 4, 6);
		checkToken(lexer.next(), Kind.ASSIGN, 4, 8);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 4, 10);
		checkToken(lexer.next(), Kind.SEMI, 4, 14);
		checkToken(lexer.next(), Kind.TYPE, 6, 0);
		checkToken(lexer.next(), Kind.IDENT, 6, 8);
		checkToken(lexer.next(), Kind.ASSIGN, 6, 17);
		checkToken(lexer.next(), Kind.BOOLEAN_LIT, 6, 19);
		checkToken(lexer.next(), Kind.SEMI, 6, 23);
		checkEOF(lexer.next());
	}

	@Test
	public void testNumberLogic() throws LexicalException{
		String input = """
				1.kjadshfkjadshf
				""";
		show(input);
		ILexer lexer = getLexer(input);
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
		});
	}

	@Test
	public void testZeroLogin() throws LexicalException {
		String input = """
				0aqweqwe
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.INT_LIT, 0, 0);
		checkToken(lexer.next(), Kind.IDENT, 0, 1);
		checkEOF(lexer.next());
	}
	@Test
	public void testEscapeSequences0() throws LexicalException {
		String input = "\"\\b \\t \\n \\f \\r \"";
		show(input);
		show("input chars= " + getASCII(input));
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		show("getStringValueChars=     " + getASCII(val));
		String expectedStringValue = "\b \t \n \f \r ";
		show("expectedStringValueChars=" + getASCII(expectedStringValue));
		assertEquals(expectedStringValue, val);
		String text = t.getText();
		show("getTextChars=     " +getASCII(text));
		String expectedText = "\"\\b \\t \\n \\f \\r \"";
		show("expectedTextChars="+getASCII(expectedText));
		assertEquals(expectedText,text);
	}

	@Test
	public void testValues() throws LexicalException {
			String input = """
					0.035
					0.4 0.3
					0.59
					12312adsfafds
					false
					""";

			ILexer lexer = getLexer(input);
			checkFloat(lexer.next(), 0.035f);
			checkFloat(lexer.next(), 0.4f);
			checkFloat(lexer.next(), 0.3f);
			checkFloat(lexer.next(), 0.59f);
			checkInt(lexer.next(), 12312);
			checkIdent(lexer.next(), "adsfafds");
			checkBoolean(lexer.next(), false);
			checkEOF(lexer.next());
		}
	@Test
	public void testEscapeSequences1() throws LexicalException {
		String input = "   \" ...  \\\"  \\\'  \\\\  \"";
		show(input);
		show("input chars= " + getASCII(input));
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		show("getStringValueChars=     " + getASCII(val));
		String expectedStringValue = " ...  \"  \'  \\  ";
		show("expectedStringValueChars=" + getASCII(expectedStringValue));
		assertEquals(expectedStringValue, val);
		String text = t.getText();
		show("getTextChars=     " +getASCII(text));
		String expectedText = "\" ...  \\\"  \\\'  \\\\  \""; //almost the same as input, but white space is omitted
		show("expectedTextChars="+getASCII(expectedText));
		assertEquals(expectedText,text);
	}

	@Test
	public void unclosedString() throws LexicalException {
		String input = """
                "good"
                "test

                """;
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 0);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}

	@Test
	public void emptyString() throws LexicalException {
		String input = """
                ""
                """;
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 0);
		checkEOF(lexer.next());
	}

	@Test
	public void invalidFloat() throws LexicalException {
		String input = """
                1.23.45
                """;
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 0, 0);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}

	@Test
	public void testZeros() throws LexicalException {
		String input = """
				00000
				""";
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.INT_LIT, 0, 0);
		checkToken(lexer.next(), Kind.INT_LIT, 0, 1);
		checkToken(lexer.next(), Kind.INT_LIT, 0, 2);
		checkToken(lexer.next(), Kind.INT_LIT, 0, 3);
		checkToken(lexer.next(), Kind.INT_LIT, 0, 4);
		checkEOF(lexer.next());

	}
}
