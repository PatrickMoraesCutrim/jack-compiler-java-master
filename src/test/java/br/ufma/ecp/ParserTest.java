package br.ufma.ecp;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

public class ParserTest extends TestSupport {

    @Test
    public void testParseLetSimple() {
        var input = "let var1 = 10+20;";
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseLet();
				var expectedResult =  """
	     <letStatement>
        <keyword> let </keyword>
        <identifier> var1 </identifier>
        <symbol> = </symbol>
        <expression>
          <term>
          <integerConstant> 10 </integerConstant>
          </term>
          <symbol> + </symbol>
          <term>
          <integerConstant> 20 </integerConstant>
          </term>
          </expression>
        <symbol> ; </symbol>
      </letStatement> 
				""";
        var result = parser.XMLOutput();
        expectedResult = expectedResult.replaceAll("  ", "");
        result = result.replaceAll("\r", ""); // no codigo em linux não tem o retorno de carro
        assertEquals(expectedResult, result);
    }


    @Test
    public void testParseExpressionSimple() {
        var input = "10+20";
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseExpression();
        
        var expectedResult =  """
          <expression>
          <term>
          <integerConstant> 10 </integerConstant>
          </term>
          <symbol> + </symbol>
          <term>
          <integerConstant> 20 </integerConstant>
          </term>
          </expression>
          """;
              
          var result = parser.XMLOutput();
          result = result.replaceAll("\r", ""); 
          expectedResult = expectedResult.replaceAll("  ", "");
          assertEquals(expectedResult, result);    

    }   
   

}