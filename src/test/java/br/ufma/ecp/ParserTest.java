package br.ufma.ecp;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

public class ParserTest extends TestSupport {

    @Test
    public void testParseLetSimple() {
        var input = "let string = 20;";
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseLet();
        System.out.println(parser.XMLOutput());
    }

    
    @Test
    public void testParseLet() {
        var input = "let square = Square.new(0, 0, 30);";
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseLet();
        var expectedResult =  """
        <letStatement>
        <keyword> let </keyword>
        <identifier> square </identifier>
        <symbol> = </symbol>
        <expression>
          <term>
            <identifier> Square </identifier>
            <symbol> . </symbol>
            <identifier> new </identifier>
            <symbol> ( </symbol>
            <expressionList>
              <expression>
                <term>
                  <integerConstant> 0 </integerConstant>
                </term>
              </expression>
              <symbol> , </symbol>
              <expression>
                <term>
                  <integerConstant> 0 </integerConstant>
                </term>
              </expression>
              <symbol> , </symbol>
              <expression>
                <term>
                  <integerConstant> 30 </integerConstant>
                </term>
              </expression>
            </expressionList>
            <symbol> ) </symbol>
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
    public void testParseIf() {
        var input = "if (direction = 1) { do square.moveUp(); }";
        var expectedResult = """
            <ifStatement>
            <keyword> if </keyword>
            <symbol> ( </symbol>
            <expression>
              <term>
                <identifier> direction </identifier>
              </term>
              <symbol> = </symbol>
              <term>
                <integerConstant> 1 </integerConstant>
              </term>
            </expression>
            <symbol> ) </symbol>
            <symbol> { </symbol>
            <statements>
              <doStatement>
                <keyword> do </keyword>
                <identifier> square </identifier>
                <symbol> . </symbol>
                <identifier> moveUp </identifier>
                <symbol> ( </symbol>
                <expressionList>
                </expressionList>
                <symbol> ) </symbol>
                <symbol> ; </symbol>
              </doStatement>
            </statements>
            <symbol> } </symbol>
          </ifStatement>
                """;

        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseIf();
        var result = parser.XMLOutput();
        expectedResult = expectedResult.replaceAll("  ", "");
        result = result.replaceAll("\r", ""); // no codigo em linux não tem o retorno de carro
        assertEquals(expectedResult, result);
    }

    @Test
    public void arrayTest () {
        var input = """
            class Main {
                function void main () {
                    var Array v;
                    let v[2] = v[3] + 42;
                    return;
                }
            }
            """;;
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parser();
        String actual = parser.VMOutput();
        String expected = """
            function Main.main 1
            push constant 2
            push local 0
            add
            push constant 3
            push local 0
            add
            pop pointer 1
            push that 0
            push constant 42
            add
            pop temp 0
            pop pointer 1
            push temp 0
            pop that 0
            push constant 0
            return        
                """;
        assertEquals(expected, actual);
    }

    @Test
    public void callFunctionTest() {

        var input = """
            class Main {
                function int soma (int x, int y) {
                       return  x + y;
                }
               
                function void main () {
                       var int d;
                       let d = Main.soma(4,5);
                       return;
                 }
               
               }
            """;;
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parser();


        String actual = parser.VMOutput();
        String expected = """
            function Main.null 0
            add
            return
            function Main.null 1
            push constant 4
            push constant 5
            call Main.soma 2
            push constant 0
            return
                """;
        assertEquals(expected, actual);
 
 
    }
    
    @Test
    public void methodTest () {
        var input = """
            class Main {
                function void main () {
                    var Point p;
                    var int x;
                    let p = Point.new (10, 20);
                    let x = p.getX();
                    return;
                }
            }
            """;;
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parser();
        String actual = parser.VMOutput();
        String expected = """
            function Main.null 2
            push constant 10
            push constant 20
            call Point.new 2
            push local 0
            call Point.getX 1
            push constant 0
            return
                """;
        assertEquals(expected, actual);
    }
    @Test
    public void testLet () {
        var input = """
            class Main {
            
              function void main () {
                  var int x;
                  let x = 42;
                  return;
              }
            }
            """;;
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parser();
        String actual = parser.VMOutput();
        String expected = """
            function Main.null 1
            push constant 42
            push constant 0
            return
                """;
        assertEquals(expected, actual);
    }

    @Test
    public void testParseDo() {
        var input = "do Sys.wait(5);";
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseDo();

        var expectedResult = """
            <doStatement>
            <keyword> do </keyword>
            <identifier> Sys </identifier>
            <symbol> . </symbol>
            <identifier> wait </identifier>
            <symbol> ( </symbol>
            <expressionList>
              <expression>
                <term>
                  <integerConstant> 5 </integerConstant>
                </term>
              </expression>
            </expressionList>
            <symbol> ) </symbol>
            <symbol> ; </symbol>
          </doStatement>
                """;
        var result = parser.XMLOutput();
        expectedResult = expectedResult.replaceAll("  ", "");
        result = result.replaceAll("\r", ""); // no codigo em linux não tem o retorno de carro
        assertEquals(expectedResult, result);
    }


    @Test
    public void testParseClassVarDec() {
        var input = "field Square square;";
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseClassVarDec();
        var expectedResult = """
            <classVarDec>
            <keyword> field </keyword>
            <identifier> Square </identifier>
            <identifier> square </identifier>
            <symbol> ; </symbol>
          </classVarDec>
                """;

        var result = parser.XMLOutput();
        expectedResult = expectedResult.replaceAll("  ", "");
        result = result.replaceAll("\r", ""); // no codigo em linux não tem o retorno de carro
        assertEquals(expectedResult, result);
    }

    @Test
    public void testParseSubroutineDec() {
        var input = """
            constructor Square new(int Ax, int Ay, int Asize) {
                let x = Ax;
                let y = Ay;
                let size = Asize;
                do draw();
                return this;
             }
                """;;
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseSubroutineDec();
        var expectedResult = """
            <subroutineDec>
            <keyword> constructor </keyword>
            <identifier> Square </identifier>
            <identifier> new </identifier>
            <symbol> ( </symbol>
            <parameterList>
              <keyword> int </keyword>
              <identifier> Ax </identifier>
              <symbol> , </symbol>
              <keyword> int </keyword>
              <identifier> Ay </identifier>
              <symbol> , </symbol>
              <keyword> int </keyword>
              <identifier> Asize </identifier>
            </parameterList>
            <symbol> ) </symbol>
            <subroutineBody>
              <symbol> { </symbol>
              <statements>
                <letStatement>
                  <keyword> let </keyword>
                  <identifier> x </identifier>
                  <symbol> = </symbol>
                  <expression>
                    <term>
                      <identifier> Ax </identifier>
                    </term>
                  </expression>
                  <symbol> ; </symbol>
                </letStatement>
                <letStatement>
                  <keyword> let </keyword>
                  <identifier> y </identifier>
                  <symbol> = </symbol>
                  <expression>
                    <term>
                      <identifier> Ay </identifier>
                    </term>
                  </expression>
                  <symbol> ; </symbol>
                </letStatement>
                <letStatement>
                  <keyword> let </keyword>
                  <identifier> size </identifier>
                  <symbol> = </symbol>
                  <expression>
                    <term>
                      <identifier> Asize </identifier>
                    </term>
                  </expression>
                  <symbol> ; </symbol>
                </letStatement>
                <doStatement>
                  <keyword> do </keyword>
                  <identifier> draw </identifier>
                  <symbol> ( </symbol>
                  <expressionList>
                  </expressionList>
                  <symbol> ) </symbol>
                  <symbol> ; </symbol>
                </doStatement>
                <returnStatement>
                  <keyword> return </keyword>
                  <expression>
                    <term>
                      <keyword> this </keyword>
                    </term>
                  </expression>
                  <symbol> ; </symbol>
                </returnStatement>
              </statements>
              <symbol> } </symbol>
            </subroutineBody>
          </subroutineDec>
                """;

        var result = parser.XMLOutput();
        expectedResult = expectedResult.replaceAll("  ", "");
        result = result.replaceAll("\r", ""); // no codigo em linux não tem o retorno de carro
        assertEquals(expectedResult, result);
    }

    
    @Test
    public void testParserWithLessSquareGame() throws IOException {
        var input = fromFile("ExpressionLessSquare/SquareGame.jack");
        var expectedResult =  fromFile("ExpressionLessSquare/SquareGame.xml");

        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parser();
        var result = parser.XMLOutput();
        expectedResult = expectedResult.replaceAll("  ", "");
        assertEquals(expectedResult, result);
    }     

    @Test
    public void testParserWithSquareGame() throws IOException {
        var input = fromFile("Square/SquareGame.jack");
        var expectedResult =  fromFile("Square/SquareGame.xml");

        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parser();
        var result = parser.XMLOutput();
        expectedResult = expectedResult.replaceAll("  ", "");
        assertEquals(expectedResult, result);
    }

    @Test
    public void testParserWithSquare() throws IOException {
        var input = fromFile("Square/Square.jack");
        var expectedResult =  fromFile("Square/Square.xml");

        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parser();
        var result = parser.XMLOutput();
        expectedResult = expectedResult.replaceAll("  ", "");
        assertEquals(expectedResult, result);
    } 

    @Test
    public void testInt () {
        var input = """
            10
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseExpression();
        String actual = parser.VMOutput();
        String expected = """
                push constant 10       
                    """;
            assertEquals(expected, actual);
    }
    @Test
    public void testSimpleExpression () {
        var input = """
            10 + 30
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseExpression();
        String actual = parser.VMOutput();
        String expected = """
                push constant 10
                push constant 30
                add       
                    """;
            assertEquals(expected, actual);
    }

    @Test
    public void testLiteralString () {
        var input = """
            "OLA"
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseExpression();
        String actual = parser.VMOutput();
        String expected = """
                push constant 3
                call String.new 1
                push constant 79
                call String.appendChar 2
                push constant 76
                call String.appendChar 2
                push constant 65
                call String.appendChar 2
                    """;
            assertEquals(expected, actual);
    }
    @Test
    public void testFalse () {
        var input = """
            false
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseExpression();
        String actual = parser.VMOutput();
        String expected = """
                push constant 0       
                    """;
            assertEquals(expected, actual);
    }

    @Test
    public void testNull () {
        var input = """
            null
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseExpression();
        String actual = parser.VMOutput();
        String expected = """
                push constant 0       
                    """;
            assertEquals(expected, actual);
    }
    @Test
    public void testNot () {
        var input = """
            ~ false
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseExpression();
        String actual = parser.VMOutput();
        String expected = """
                push constant 0   
                not    
                    """;
            assertEquals(expected, actual);
    }
    @Test
    public void testReturn () {
        var input = """
            return;
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseStatement();
        String actual = parser.VMOutput();
        String expected = """
                push constant 0
                return       
                    """;
            assertEquals(expected, actual);
    }

    @Test
    public void testIf () {
        var input = """
            if (false) {
                return 10;
            } else {
                return 20;
            }
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseStatement();
        String actual = parser.VMOutput();
        String expected = """
            push constant 0
            if-goto IF_TRUE0
            goto IF_FALSE0
            label IF_TRUE0
            push constant 10
            return
            goto IF_END0
            label IF_FALSE0
            push constant 20
            return
            label IF_END0 
                    """;
            assertEquals(expected, actual);
    }

    @Test
    public void testReturnExpr () {
        var input = """
            return 10;
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseStatement();
        String actual = parser.VMOutput();
        String expected = """
                push constant 10
                return       
                    """;
            assertEquals(expected, actual);
    }

    @Test
    public void testWhile () {
        var input = """
            while (false) {
                return 10;
            } 
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseStatement();
        String actual = parser.VMOutput();
        String expected = """
            label WHILE_EXP0
            push constant 0
            not
            if-goto WHILE_END0
            push constant 10
            return
            goto WHILE_EXP0
            label WHILE_END0
                    """;
            assertEquals(expected, actual);
    }

    @Test
    public void testSimpleFunctions () {
        var input = """
            class Main {
 
                function int soma (int x, int y) {
                        return  30;
                 }
                
                 function void main () {
                        var int d;
                        return;
                  }
                
                }
            """;;
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parser();
        String actual = parser.VMOutput();
        String expected = """
            function Main.null 0
            push constant 30
            return
            function Main.null 1
            push constant 0
            return    
                """;
        assertEquals(expected, actual);
    }

    @Test
    public void testMinus () {
        var input = """
            - 10
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseExpression();
        String actual = parser.VMOutput();
        String expected = """
                push constant 10   
                neg    
                    """;
            assertEquals(expected, actual);
    }


    @Test
    public void testTrue () {
        var input = """
            true
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseExpression();
        String actual = parser.VMOutput();
        String expected = """
                push constant 0
                not       
                    """;
            assertEquals(expected, actual);
    }


    @Test
    public void testThis () {
        var input = """
            this
            """;
        
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parseExpression();
        String actual = parser.VMOutput();
        String expected = """
                push pointer 0
                    """;
            assertEquals(expected, actual);
    }

    @Test
    public void testSimpleFunctionWithVar () {
        var input = """
            class Main {

                 function int funcao () {
                        var int d;
                        return d;
                  }
                
                }
            """;;
        var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
        parser.parser();
        String actual = parser.VMOutput();
        String expected = """
            function Main.null 1
            return
            """;
        assertEquals(expected, actual);
    }

    @Test
    public void testVarDeclaration() {
      var input = """
          class Point {
            field int x, y;
            constructor Point new(int Ax, int Ay) {
              var int Ax;
  
              let x = Ax;
              let y = Ay;
              return this;
           }
          }
          """;
          
      var parser = new Parser(input.getBytes(StandardCharsets.UTF_8));
      //parser.parser();
      var result = parser.XMLOutput();
      System.out.println(result);
    }

    
}