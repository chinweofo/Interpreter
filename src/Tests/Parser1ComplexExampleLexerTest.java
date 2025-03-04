package Tests;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import Tran.*;
public class Parser1ComplexExampleLexerTest{

	@Test
	public void Parser1ComplexExampleLexerTest() throws Exception {
		var lexer = new Lexer(
			"interface complexExample\n"+
			"    simpleMethod() \n"+
			"    methodWithParam(number one)\n"+
			"    methodWithOneReturnValue() : number good\n"+
			"    methodWithManyParams(number one, string two, character three) \n"+
			"    methodWithManyParamsAndReturns(number one, string two, character three) : number four, string five, character six\n"+
			"\n"+
			"interface simpleExample\n"+
			"    doesItWork() \n"+
			"    yesItDoes() \n"+
			"    \n"+
			"" );
		var tokens = lexer.Lex();
		Assertions.assertEquals(70, tokens.size());
		Assertions.assertEquals(Token.TokenTypes.INTERFACE, tokens.get(0).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(1).getType());
		Assertions.assertEquals("complexExample", tokens.get(1).getValue());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(2).getType());
		Assertions.assertEquals(Token.TokenTypes.INDENT, tokens.get(3).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(4).getType());
		Assertions.assertEquals("simpleMethod", tokens.get(4).getValue());
		Assertions.assertEquals(Token.TokenTypes.LPAREN, tokens.get(5).getType());
		Assertions.assertEquals(Token.TokenTypes.RPAREN, tokens.get(6).getType());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(7).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(8).getType());
		Assertions.assertEquals("methodWithParam", tokens.get(8).getValue());
		Assertions.assertEquals(Token.TokenTypes.LPAREN, tokens.get(9).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(10).getType());
		Assertions.assertEquals("number", tokens.get(10).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(11).getType());
		Assertions.assertEquals("one", tokens.get(11).getValue());
		Assertions.assertEquals(Token.TokenTypes.RPAREN, tokens.get(12).getType());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(13).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(14).getType());
		Assertions.assertEquals("methodWithOneReturnValue", tokens.get(14).getValue());
		Assertions.assertEquals(Token.TokenTypes.LPAREN, tokens.get(15).getType());
		Assertions.assertEquals(Token.TokenTypes.RPAREN, tokens.get(16).getType());
		Assertions.assertEquals(Token.TokenTypes.COLON, tokens.get(17).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(18).getType());
		Assertions.assertEquals("number", tokens.get(18).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(19).getType());
		Assertions.assertEquals("good", tokens.get(19).getValue());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(20).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(21).getType());
		Assertions.assertEquals("methodWithManyParams", tokens.get(21).getValue());
		Assertions.assertEquals(Token.TokenTypes.LPAREN, tokens.get(22).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(23).getType());
		Assertions.assertEquals("number", tokens.get(23).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(24).getType());
		Assertions.assertEquals("one", tokens.get(24).getValue());
		Assertions.assertEquals(Token.TokenTypes.COMMA, tokens.get(25).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(26).getType());
		Assertions.assertEquals("string", tokens.get(26).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(27).getType());
		Assertions.assertEquals("two", tokens.get(27).getValue());
		Assertions.assertEquals(Token.TokenTypes.COMMA, tokens.get(28).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(29).getType());
		Assertions.assertEquals("character", tokens.get(29).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(30).getType());
		Assertions.assertEquals("three", tokens.get(30).getValue());
		Assertions.assertEquals(Token.TokenTypes.RPAREN, tokens.get(31).getType());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(32).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(33).getType());
		Assertions.assertEquals("methodWithManyParamsAndReturns", tokens.get(33).getValue());
		Assertions.assertEquals(Token.TokenTypes.LPAREN, tokens.get(34).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(35).getType());
		Assertions.assertEquals("number", tokens.get(35).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(36).getType());
		Assertions.assertEquals("one", tokens.get(36).getValue());
		Assertions.assertEquals(Token.TokenTypes.COMMA, tokens.get(37).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(38).getType());
		Assertions.assertEquals("string", tokens.get(38).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(39).getType());
		Assertions.assertEquals("two", tokens.get(39).getValue());
		Assertions.assertEquals(Token.TokenTypes.COMMA, tokens.get(40).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(41).getType());
		Assertions.assertEquals("character", tokens.get(41).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(42).getType());
		Assertions.assertEquals("three", tokens.get(42).getValue());
		Assertions.assertEquals(Token.TokenTypes.RPAREN, tokens.get(43).getType());
		Assertions.assertEquals(Token.TokenTypes.COLON, tokens.get(44).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(45).getType());
		Assertions.assertEquals("number", tokens.get(45).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(46).getType());
		Assertions.assertEquals("four", tokens.get(46).getValue());
		Assertions.assertEquals(Token.TokenTypes.COMMA, tokens.get(47).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(48).getType());
		Assertions.assertEquals("string", tokens.get(48).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(49).getType());
		Assertions.assertEquals("five", tokens.get(49).getValue());
		Assertions.assertEquals(Token.TokenTypes.COMMA, tokens.get(50).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(51).getType());
		Assertions.assertEquals("character", tokens.get(51).getValue());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(52).getType());
		Assertions.assertEquals("six", tokens.get(52).getValue());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(53).getType());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(54).getType());
		Assertions.assertEquals(Token.TokenTypes.DEDENT, tokens.get(55).getType());
		Assertions.assertEquals(Token.TokenTypes.INTERFACE, tokens.get(56).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(57).getType());
		Assertions.assertEquals("simpleExample", tokens.get(57).getValue());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(58).getType());
		Assertions.assertEquals(Token.TokenTypes.INDENT, tokens.get(59).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(60).getType());
		Assertions.assertEquals("doesItWork", tokens.get(60).getValue());
		Assertions.assertEquals(Token.TokenTypes.LPAREN, tokens.get(61).getType());
		Assertions.assertEquals(Token.TokenTypes.RPAREN, tokens.get(62).getType());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(63).getType());
		Assertions.assertEquals(Token.TokenTypes.WORD, tokens.get(64).getType());
		Assertions.assertEquals("yesItDoes", tokens.get(64).getValue());
		Assertions.assertEquals(Token.TokenTypes.LPAREN, tokens.get(65).getType());
		Assertions.assertEquals(Token.TokenTypes.RPAREN, tokens.get(66).getType());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(67).getType());
		Assertions.assertEquals(Token.TokenTypes.NEWLINE, tokens.get(68).getType());
		Assertions.assertEquals(Token.TokenTypes.DEDENT, tokens.get(69).getType());
	}
}
