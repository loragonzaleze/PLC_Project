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
	//check that this token is an FLOAT_LIT with expected float value

	//check that this token  is an FLOAT_LIT with expected float value and position
	void checkFloat(IToken t, float expectedValue, int expectedLine, int expectedColumn) {
		checkFloat(t,expectedValue);
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
	}

	void checkToken(IToken t, Kind expectedKind, String expectedText, int expectedLine, int expectedColumn){
		assertEquals(expectedKind, t.getKind());
		assertEquals(expectedText, t.getText());
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
	}

	void checkToken(IToken t, Kind expectedKind, int expectedLine, int expectedColumn, String expectedText){
		assertEquals(expectedKind, t.getKind());
		assertEquals(expectedText, t.getText());
		assertEquals(new IToken.SourceLocation(expectedLine,expectedColumn), t.getSourceLocation());
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

	void checkReserved(IToken t, Kind expectedKind, int expectedLine, int expectedColumn,  String expectedName){
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
				9999999999\r
				sfs
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
				"fakjsfkjahsdfkjasdjflhadsfhadsjf"
				""";
		show(input);
		ILexer lexer = getLexer(input);
		System.out.println("hello");
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
		checkToken(lexer.next(), Kind.TYPE, 0, 0);
		checkIdent(lexer.peek(), "x", 0, 4);
		checkToken(lexer.next(), Kind.IDENT, 0, 4);
		checkToken(lexer.next(), Kind.ASSIGN, 0, 6);
		checkInt(lexer.peek(), 10, 			0, 8);
		checkToken(lexer.next(), Kind.INT_LIT, 0, 8);
		checkToken(lexer.next(), Kind.SEMI, 0, 10);
		checkToken(lexer.next(), Kind.IDENT, 1, 0);
		checkToken(lexer.next(), Kind.PLUS, 1, 1);
		checkToken(lexer.next(), Kind.PLUS, 1, 2);
		checkToken(lexer.next(), Kind.SEMI, 1, 3);
		checkToken(lexer.next(), Kind.TYPE, 4,0);
		checkToken(lexer.next(), Kind.IDENT, 4, 6);
		checkToken(lexer.next(), Kind.ASSIGN, 4, 8);
		checkFloat(lexer.peek(), (float) 3.24,	4, 10);
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
	public void testQuiz3() throws LexicalException {
		String input = "\"abc\t09\n\"";
		show(input);
		show("input chars= " + getASCII(input));
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();

		String text = t.getText();
		show("getTextChars=     " +getASCII(text));
		String expectedText = "\"\\b \\t \\n \\f \\r \"";
		show("expectedTextChars="+getASCII(expectedText));
		assertEquals(expectedText,text);
	}

	@Test
	public void testQUiz() throws LexicalException {
		String input = "\"abc\t0n\"";
		show(input);
		show("input chars= " + getASCII(input));
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		System.out.println("hello");
	}

	@Test
	public void testQUiz2() throws LexicalException {
		String input = "REDYELLOWGREEN.";
		show(input);
		ILexer lexer = getLexer(input);
		IToken t = lexer.next();
		String val = t.getStringValue();
		System.out.println("hello");
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
	@Test
	void testAllSymbolTokens() throws LexicalException {
		String input = """
			&
			|
			/
			*
			+
			(
			)
			[
			]
			!=
			==
			>=
			<=
			>>
			<<
			<-
			->
			%
			^
			,
			;
			!
			=
			-
			<
			>	 
			""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.AND,		0, 0);
		checkToken(lexer.next(), Kind.OR,		1, 0);
		checkToken(lexer.next(), Kind.DIV,		2, 0);
		checkToken(lexer.next(), Kind.TIMES,	3, 0);
		checkToken(lexer.next(), Kind.PLUS,		4, 0);
		checkToken(lexer.next(), Kind.LPAREN,	5, 0);
		checkToken(lexer.next(), Kind.RPAREN,	6, 0);
		checkToken(lexer.next(), Kind.LSQUARE,    7, 0);
		checkToken(lexer.next(), Kind.RSQUARE,	8, 0);
		checkToken(lexer.next(), Kind.NOT_EQUALS,	9, 0);
		checkToken(lexer.next(), Kind.EQUALS,    	10, 0);
		checkToken(lexer.next(), Kind.GE,         11, 0);
		checkToken(lexer.next(), Kind.LE,         12, 0);
		checkToken(lexer.next(), Kind.RANGLE,     13, 0);
		checkToken(lexer.next(), Kind.LANGLE,     14, 0);
		checkToken(lexer.next(), Kind.LARROW,     15, 0);
		checkToken(lexer.next(), Kind.RARROW,     16, 0);
		checkToken(lexer.next(), Kind.MOD,        17, 0);
		checkToken(lexer.next(), Kind.RETURN,     18, 0);
		checkToken(lexer.next(), Kind.COMMA,      19, 0);
		checkToken(lexer.next(), Kind.SEMI,       20, 0);
		checkToken(lexer.next(), Kind.BANG,       21, 0);
		checkToken(lexer.next(), Kind.ASSIGN,     22, 0);
		checkToken(lexer.next(), Kind.MINUS,      23, 0);
		checkToken(lexer.next(), Kind.LT,		24, 0);
		checkToken(lexer.next(), Kind.GT,		25, 0);
		checkEOF(lexer.next());
	}
	@Test
	void testReservedWords() throws LexicalException {
		String input = """
			string CYAN
			int
			float
			boolean
			color
			image
			void
			getWidth
			getHeight
			getRed
			getGreen
			getBlue
			BLACK
			BLUE
			CYAN
			DARK_GRAY
			GRAY
			GREEN
			LIGHT_GRAY
			MAGENTA
			ORANGE
			PINK
			RED
			WHITE
			YELLOW
			true
			false
			if
			else
			fi
			write
			console	 
			""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.TYPE,		    0, 0, "string");
		checkToken(lexer.next(), Kind.COLOR_CONST,    0, 7, "CYAN");
		checkToken(lexer.next(), Kind.TYPE,		    1, 0, "int");
		checkToken(lexer.next(), Kind.TYPE,		    2, 0, "float");
		checkToken(lexer.next(), Kind.TYPE,		    3, 0, "boolean");
		checkToken(lexer.next(), Kind.TYPE,		    4, 0, "color");
		checkToken(lexer.next(), Kind.TYPE,		    5, 0, "image");
		checkToken(lexer.next(), Kind.KW_VOID,	    6, 0, "void");
		checkToken(lexer.next(), Kind.IMAGE_OP,	    7, 0, "getWidth");
		checkToken(lexer.next(), Kind.IMAGE_OP,	    8, 0, "getHeight");
		checkToken(lexer.next(), Kind.COLOR_OP,	    9, 0, "getRed");
		checkToken(lexer.next(), Kind.COLOR_OP,	    10, 0, "getGreen");
		checkToken(lexer.next(), Kind.COLOR_OP,	    11, 0, "getBlue");
		checkToken(lexer.next(), Kind.COLOR_CONST,	12, 0, "BLACK");
		checkToken(lexer.next(), Kind.COLOR_CONST,	13, 0, "BLUE");
		checkToken(lexer.next(), Kind.COLOR_CONST,	14, 0, "CYAN");
		checkToken(lexer.next(), Kind.COLOR_CONST,	15, 0, "DARK_GRAY");
		checkToken(lexer.next(), Kind.COLOR_CONST,	16, 0, "GRAY");
		checkToken(lexer.next(), Kind.COLOR_CONST,	17, 0, "GREEN");
		checkToken(lexer.next(), Kind.COLOR_CONST,	18, 0, "LIGHT_GRAY");
		checkToken(lexer.next(), Kind.COLOR_CONST,	19, 0, "MAGENTA");
		checkToken(lexer.next(), Kind.COLOR_CONST,	20, 0, "ORANGE");
		checkToken(lexer.next(), Kind.COLOR_CONST,	21, 0, "PINK");
		checkToken(lexer.next(), Kind.COLOR_CONST,	22, 0, "RED");
		checkToken(lexer.next(), Kind.COLOR_CONST,	23, 0, "WHITE");
		checkToken(lexer.next(), Kind.COLOR_CONST,	24, 0, "YELLOW");
		checkToken(lexer.next(), Kind.BOOLEAN_LIT,	25, 0, "true");
		checkToken(lexer.next(), Kind.BOOLEAN_LIT,	26, 0, "false");
		checkToken(lexer.next(), Kind.KW_IF,        27, 0, "if");
		checkToken(lexer.next(), Kind.KW_ELSE,	    28, 0, "else");
		checkToken(lexer.next(), Kind.KW_FI,	    29, 0, "fi");
		checkToken(lexer.next(), Kind.KW_WRITE,	    30, 0, "write");
		checkToken(lexer.next(), Kind.KW_CONSOLE,	31, 0, "console");
		checkEOF(lexer.next());
	}

	@Test
	void testIntFloatError() throws LexicalException {
		String input = """
			0.32
			00.15
			10.030.32
			""";
		show(input);
		ILexer lexer = getLexer(input);
		checkFloat(lexer.next(), (float) 0.32,	0, 0);
		checkInt(lexer.next(), 0, 			1, 0);
		checkFloat(lexer.next(), (float) 0.15,	1, 1);
		checkFloat(lexer.next(), (float) 10.030,	2, 0);
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
		});
	}

	@Test
	public void testStringErrorEOF() throws LexicalException {
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
	void testRandomInputs() throws LexicalException {
		String input = """
			abc
			00.4
			123
			_Name1
			_1@
			""";
		show(input);
		ILexer lexer = getLexer(input);
		//these checks should succeed
		checkIdent(lexer.next(), "abc");
		checkInt(lexer.next(), 0, 1,0);
		checkToken(lexer.peek(), Kind.FLOAT_LIT, 1, 1);
		checkToken(lexer.next(), Kind.FLOAT_LIT, 1, 1);
		checkToken(lexer.next(), Kind.INT_LIT, 2,0);
		checkIdent(lexer.next(), "_Name1", 3, 0);
		checkIdent(lexer.next(), "_1", 4, 0);
		//this is expected to throw an exception since @ is not a legal
		//character unless it is part of a string or comment
		assertThrows(LexicalException.class, () -> {
			@SuppressWarnings("unused")
			IToken token = lexer.next();
		});
	}


	@Test
	void testMany() throws LexicalException {
		String input = """
			[
			int a = 28.3 * 55.597;<>
			string _b1 = "testing \\nstring";
			boolean c$5 = true;
			]
			""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.LSQUARE, 0,0);
		checkToken(lexer.next(), Kind.TYPE, "int", 1,0);
		checkToken(lexer.next(), Kind.IDENT, "a", 1,4);
		checkToken(lexer.next(), Kind.ASSIGN, 1,6);
		checkToken(lexer.next(), Kind.FLOAT_LIT, "28.3", 1, 8);
		checkToken(lexer.next(), Kind.TIMES, 1,13);
		checkToken(lexer.next(), Kind.FLOAT_LIT, "55.597", 1, 15);
		checkToken(lexer.next(), Kind.SEMI, 1,21);
		checkToken(lexer.next(), Kind.LT);
		checkToken(lexer.next(), Kind.GT);
		checkToken(lexer.next(), Kind.TYPE, "string", 2,0);
		checkToken(lexer.next(), Kind.IDENT, "_b1", 2,7);
		checkToken(lexer.next(), Kind.ASSIGN, 2,11);
		IToken t = lexer.next();
		String val = t.getStringValue();
		show("getStringValueChars=     " + getASCII(val));
		String expectedStringValue = "testing \nstring";
		show("expectedStringValueChars=" + getASCII(expectedStringValue));
		assertEquals(expectedStringValue, val);
		String text = t.getText();
		show("getTextChars=     " +getASCII(text));
		String expectedText = "\"testing \\nstring\"";
		show("expectedTextChars="+getASCII(expectedText));
		assertEquals(expectedText,text);
		checkToken(lexer.next(), Kind.SEMI, 2,31);
		checkToken(lexer.next(), Kind.TYPE, "boolean", 3,0);
		checkToken(lexer.next(), Kind.IDENT, "c$5", 3,8);
		checkToken(lexer.next(), Kind.ASSIGN, 3,12);
		checkToken(lexer.next(), Kind.BOOLEAN_LIT, "true", 3,14);
		checkToken(lexer.next(), Kind.SEMI, 3,18);
		checkToken(lexer.next(), Kind.RSQUARE, 4,0);
		checkEOF(lexer.next());
	}

	@Test
	public void testCodeExample() throws LexicalException{
		String input = """
					string a = "hello\\\\nworld";
				   		int size = 11;
				   		string b = "";
				   		boolean display = true;
				   		
				 	 	for (int i = size - 1;i >= 0; i++) [
				 	 		b = b + a[i];
				 	 	]
				 
				   		if (display == true)
				   		print(b);
				 
				""";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.TYPE, 0, 0, "string");
		checkToken(lexer.next(), Kind.IDENT, 0, 7, "a");
		checkToken(lexer.next(), Kind.ASSIGN, 0, 9, "=");
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 11, "\"hello\\nworld\"");
		checkToken(lexer.next(), Kind.SEMI, 0, 25);

		checkToken(lexer.next(), Kind.TYPE, 1, 0, "int");
		checkToken(lexer.next(), Kind.IDENT, 1, 4, "size");
		checkToken(lexer.next(), Kind.ASSIGN, 1, 9, "=");
		checkInt(lexer.next(), 11, 1, 11);
		checkToken(lexer.next(), Kind.SEMI, 1, 13);

		checkToken(lexer.next(), Kind.TYPE);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.ASSIGN);
		checkToken(lexer.next(), Kind.STRING_LIT);
		checkToken(lexer.next(), Kind.SEMI);

		checkToken(lexer.next(), Kind.TYPE, 3, 0, "boolean");
		checkToken(lexer.next(), Kind.IDENT, 3, 8, "display");
		checkToken(lexer.next(), Kind.ASSIGN, 3, 16, "=");
		checkToken(lexer.next(), Kind.BOOLEAN_LIT, 3, 18, "true");
		checkToken(lexer.next(), Kind.SEMI, 3, 22);

		checkToken(lexer.next(), Kind.IDENT, 5, 0, "for");
		checkToken(lexer.next(), Kind.LPAREN);
		checkToken(lexer.next(), Kind.TYPE);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.ASSIGN);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.MINUS);
		checkInt(lexer.next(), 1);


		checkToken(lexer.next(), Kind.SEMI);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.GE);
		checkInt(lexer.next(), 0);
		checkToken(lexer.next(), Kind.SEMI);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.PLUS);
		checkToken(lexer.next(), Kind.PLUS);
		checkToken(lexer.next(), Kind.RPAREN);
		checkToken(lexer.next(), Kind.LSQUARE);

		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.ASSIGN);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.PLUS);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.LSQUARE);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.RSQUARE);
		checkToken(lexer.next(), Kind.SEMI);

		checkToken(lexer.next(), Kind.RSQUARE);

		checkToken(lexer.next(), Kind.KW_IF);
		checkToken(lexer.next(), Kind.LPAREN);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.EQUALS);
		checkToken(lexer.next(), Kind.BOOLEAN_LIT);
		checkToken(lexer.next(), Kind.RPAREN);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.LPAREN);
		checkToken(lexer.next(), Kind.IDENT);
		checkToken(lexer.next(), Kind.RPAREN);
		checkToken(lexer.next(), Kind.SEMI);

		checkEOF(lexer.next());
	}

	@Test
	public void testEOF1() throws LexicalException {
		String input = "#ThisEndsInEOF";
		show(input);
		ILexer lexer = getLexer(input);
		checkEOF(lexer.next());
	}
	@Test
	void testOnlyEmptyStringLit() throws LexicalException {
		String input = "\"\"";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 0, "\"\"");
	}
	@Test
	void testKeywordWithinIdent() throws LexicalException {
		String input = "stringVar\nifBlah\nREDfoo";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.IDENT, 0, 0);
		checkToken(lexer.next(), Kind.IDENT, 1, 0);
		checkToken(lexer.next(), Kind.IDENT, 2, 0);
	}

	@Test
	void testSlashes() throws LexicalException {
		String input = "\"Hello\\\\World\"";
		show(input);
		ILexer lexer = getLexer(input);
		IToken token = lexer.next();
		checkToken(token, Kind.STRING_LIT, 0, 0, "\"Hello\\\\World\"");
		String val = token.getStringValue();
		assertEquals("Hello\\World", val);
	}

	@Test
	public void testStringErrorEscape() throws LexicalException {
		String input = """
                "good"
                "test \\n nesting \\h"
   
                """;
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 0);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}

	@Test
	void testSingleDigitZero() throws LexicalException
	{
		String input = "0";
		show(input);
		ILexer lexer = getLexer(input);
		checkInt(lexer.next(), 0);
	}

	@Test
	void testSingleCharIdent() throws LexicalException
	{
		String input = "a";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.IDENT, 0, 0);
	}

	@Test
	public void testCommentEOF2() throws LexicalException {
		String input = "#";
		show(input);
		ILexer lexer = getLexer(input);
		checkEOF(lexer.next());
	}


	@Test
	void testIllegalFloat2() throws LexicalException
	{
		String input = "1.";
		show(input);
		ILexer lexer = getLexer(input);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}

	@Test
	void testIllegalDigitZero() throws LexicalException
	{
		// Java's Character::isDigit doesn't cut it -- it includes Unicode digits too
		String input = "\u0660";
		show(input);
		ILexer lexer = getLexer(input);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}
	@Test
	void testIllegalDigitInInt() throws LexicalException
	{
		// Java's Character::isDigit doesn't cut it -- it includes Unicode digits too
		String input = "\u0661\u0662\u0663";
		show(input);
		ILexer lexer = getLexer(input);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}

	@Test
	public void testIllegalEscape2() throws LexicalException
	{
		// Octal escapes are not processed in this language
		String input = """
        "\\123"
        """;
		show(input);
		ILexer lexer = getLexer(input);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}

	@Test
	void testPointerDereference() throws LexicalException
	{
		String input = "myObject->myProperty";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.IDENT, 0, 0);
		checkToken(lexer.next(), Kind.RARROW, 0, 8);
		checkToken(lexer.next(), Kind.IDENT, 0, 10);
	}

	@Test
	void testManyIdentsWithWhitespace() throws LexicalException
	{
		String input = "Did you ever hear the tragedy of Darth Plagueis \"the wise\"?";
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.IDENT, 0, 0);
		checkToken(lexer.next(), Kind.IDENT, 0, 4);
		checkToken(lexer.next(), Kind.IDENT, 0, 8);
		checkToken(lexer.next(), Kind.IDENT, 0, 13);
		checkToken(lexer.next(), Kind.IDENT, 0, 18);
		checkToken(lexer.next(), Kind.IDENT, 0, 22);
		checkToken(lexer.next(), Kind.IDENT, 0, 30);
		checkToken(lexer.next(), Kind.IDENT, 0, 33);
		checkToken(lexer.next(), Kind.IDENT, 0, 39);
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 48, "\"the wise\"");
		Exception e = assertThrows(LexicalException.class, () -> {
			// It’s not a story the Jedi would tell you
			lexer.next();
		});
	}

	@Test
	public void testEmptyStringThenEmptyString2() throws LexicalException
	{
		String input = "\"\" \"\"";
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 0, "\"\"");
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 3, "\"\"");
	}

	@Test
	public void testEmptyStringThenEmptyString1() throws LexicalException
	{
		String input = "\"\"\"\"";
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 0, "\"\"");
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 2, "\"\"");
	}

	@Test
	public void testEmptyStringThenIdent2() throws LexicalException
	{
		String input = "\"\" a";
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 0, "\"\"");
		checkToken(lexer.next(), Kind.IDENT, 0, 3, "a");
	}

	@Test
	void testIllegalUnicodeInIdent2() throws LexicalException
	{
		String input = "abc\u1F9D1\u200D\u1F373def"; // Face and frying pan connected with a zero-width joiner U+200D to make Chef
		show(input);
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.IDENT, 0, 0);
		Exception e = assertThrows(LexicalException.class, () -> {
			lexer.next();
		});
	}

	@Test
	public void multiLineString() throws LexicalException{
		String input = """
			string a = "test
			52
			as";
			a
			""";
		ILexer lexer = getLexer(input);
		checkToken(lexer.next(), Kind.TYPE, 0, 0, "string");
		checkIdent(lexer.next(), "a", 0, 7);
		checkToken(lexer.next(), Kind.ASSIGN, 0, 9);
		checkToken(lexer.next(), Kind.STRING_LIT, 0, 11, "\"test\n52\nas\"");
		checkToken(lexer.next(), Kind.SEMI, 2, 3);
		checkIdent(lexer.next(), "a", 3, 0);
	}


	@Test
	public void quiz2() throws LexicalException{
		String input = """
   			"hi\nbye\n"
			""";
		ILexer lexer = getLexer(input);
		IToken test = lexer.next();
		System.out.println("Prints this: ");
		String testStr = test.getText();
		String stringVal = test.getStringValue();
		System.out.println("Hello");
	}

	@Test
	public void quiz2Part1() throws LexicalException{
		String input = """
   			
			""";
		ILexer lexer = getLexer(input);
		IToken test = lexer.next();
		System.out.println("Prints this: ");
		System.out.println("Hello");
	}

}
