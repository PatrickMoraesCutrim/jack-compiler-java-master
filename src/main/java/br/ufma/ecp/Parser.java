package br.ufma.ecp;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;
import br.ufma.ecp.SymbolTable.Kind;
import br.ufma.ecp.SymbolTable.Symbol;
import br.ufma.ecp.VmWriter.Command;
import br.ufma.ecp.VmWriter.Segment;
import br.ufma.ecp.token.*;



public class Parser {

    private static class ParseError extends RuntimeException {}
    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();
    private SymbolTable symbolTable;
    private int ifLabelNum;
    private int whileLabelNum;
    private VmWriter vmWriter;

    private String className; // nome da classe

    public Parser (byte[] input) {
        scan = new Scanner(input);
        symbolTable = new SymbolTable();
        vmWriter = new VmWriter();
        nextToken();        
    }

    public void nextToken () {
        currentToken = peekToken;
        peekToken = scan.nextToken();        
    }

    public void parse () {
        
    }

    // 'class' className '{' classVarDec* subroutineDec* '}'
    public void parseClass () {

        printNonTerminal("class");
        expectPeek(TokenType.CLASS);
        expectPeek(TokenType.IDENT);
        className = currentToken.lexeme;
        
        expectPeek(TokenType.LBRACE);

        while ( peekTokenIs(TokenType.STATIC) || peekTokenIs(TokenType.FIELD) ) {
            parseClassVarDec();
        }

        while (peekTokenIs(TokenType.FUNCTION) || peekTokenIs(TokenType.CONSTRUCTOR) || peekTokenIs(TokenType.METHOD)) {
            //parseSubroutineDec();
        }      
        
        expectPeek(TokenType.RBRACE);
        printNonTerminal("class");

        
    }
    
    
    // 'var' type varName ( ',' varName)* ';'
    void parseVarDec() {

        printNonTerminal("varDec");
        expectPeek(TokenType.VAR);
        SymbolTable.Kind kind = Kind.VAR;

        // 'int' | 'char' | 'boolean' | className
        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        String type = currentToken.lexeme;
        expectPeek(TokenType.IDENT);
        String name = currentToken.lexeme;
        symbolTable.define(name, type, kind);

        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);
            name = currentToken.lexeme;
            symbolTable.define(name, type, kind);

        }

        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/varDec");
    }

    // classVarDec → ( 'static' | 'field' ) type varName ( ',' varName)* ';'
    void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(TokenType.FIELD, TokenType.STATIC);

        SymbolTable.Kind kind = Kind.STATIC;
        if (currentTokenIs(TokenType.FIELD))
            kind = Kind.FIELD;

        // 'int' | 'char' | 'boolean' | className
        expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        String type = currentToken.lexeme;
        expectPeek(TokenType.IDENT);
        
        String name = currentToken.lexeme;
        symbolTable.define(name, type, kind);
        
        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            expectPeek(TokenType.IDENT);

            name = currentToken.lexeme;
            symbolTable.define(name, type, kind);

        }
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/classVarDec");
    }

    void parseSubroutineDec() {
        printNonTerminal("subroutineDec");

        ifLabelNum = 0;
        whileLabelNum = 0;
        symbolTable.startSubroutine();

        expectPeek(TokenType.CONSTRUCTOR, TokenType.FUNCTION, TokenType.METHOD);
        var subroutineType = currentToken.type;

        if (subroutineType == TokenType.METHOD) {
            symbolTable.define("this", className, Kind.ARG);
        }

        // 'int' | 'char' | 'boolean' | className
        expectPeek(TokenType.VOID, TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
        expectPeek(TokenType.IDENT);
        // **
        var functName = className + "." + currentToken.lexeme;
        // **

        expectPeek(TokenType.LPAREN);
        parseParameterList();
        expectPeek(TokenType.RPAREN);
        parseSubroutineBody(functName, subroutineType);

        printNonTerminal("/subroutineDec");
    }

     // ((type varName) ( ',' type varName)*)?
     void parseParameterList() {
        printNonTerminal("parameterList");
        SymbolTable.Kind kind = Kind.ARG;

        if (!peekTokenIs(TokenType.RPAREN)) // verifica se tem pelo menos uma expressao
        {
            expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
            String type = currentToken.lexeme;
            expectPeek(TokenType.IDENT);
            String name = currentToken.lexeme;
            symbolTable.define(name, type, kind);

            while (peekTokenIs(TokenType.COMMA)) {
                expectPeek(TokenType.COMMA);
                expectPeek(TokenType.INT, TokenType.CHAR, TokenType.BOOLEAN, TokenType.IDENT);
                type = currentToken.lexeme;

                expectPeek(TokenType.IDENT);
                name = currentToken.lexeme;

                symbolTable.define(name, type, kind);

            }
        }
        printNonTerminal("/parameterList");
    }

    // '{' varDec* statements '}'
    void parseSubroutineBody(String functName, TokenType subroutineType) {

        printNonTerminal("subroutineBody");
        expectPeek(TokenType.LBRACE);
        while (peekTokenIs(TokenType.VAR)) {
            parseVarDec();
        }
        var numlocals = symbolTable.varCont(Kind.VAR);
        vmWriter.writeFunction(functName, numlocals);
        if (subroutineType == TokenType.CONSTRUCTOR) {
            vmWriter.writePush(Segment.CONST, symbolTable.varCont(Kind.FIELD));
            vmWriter.writeCall("Memory.alloc", 1);
            vmWriter.writePop(Segment.POINTER, 0);
        }

        if (subroutineType == TokenType.METHOD) {
            vmWriter.writePush(Segment.ARG, 0);
            vmWriter.writePop(Segment.POINTER, 0);
        }

        parseStatements();
        expectPeek(TokenType.RBRACE);
        printNonTerminal("/subroutineBody");
    }
    void parseStatements() {
        printNonTerminal("statements");
        while (peekToken.type == TokenType.WHILE ||
                peekToken.type == TokenType.IF ||
                peekToken.type == TokenType.LET ||
                peekToken.type == TokenType.DO ||
                peekToken.type == TokenType.RETURN) {
            parseStatement();
        }

        printNonTerminal("/statements");
    }

    // letStatement | ifStatement | whileStatement | doStatement | returnStatement
    void parseStatement() {
        switch (peekToken.type) {
            case LET:
                parseLet();
                break;
            case WHILE:
                parseWhile();
                break;
            case IF:
                parseIf();
                break;
            case RETURN:
                parseReturn();
                break;
            case DO:
                parseDo();
                break;
            default:
                throw new Error("Expected a statement");
        }
    }

    void parseWhile() {
        printNonTerminal("whileStatement");
        // **
        var labelTrue = "WHILE_EXP" + whileLabelNum;
        var labelFalse = "WHILE_END" + whileLabelNum;
        whileLabelNum++;

        vmWriter.writeLabel(labelTrue);
        // **

        expectPeek(TokenType.WHILE);
        expectPeek(TokenType.LPAREN);
        parseExpression();
        // **
        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(labelFalse);

        expectPeek(TokenType.RPAREN);
        expectPeek(TokenType.LBRACE);
        parseStatements();
        // **
        vmWriter.writeGoto(labelTrue); // Go back to labelTrue and check condition
        vmWriter.writeLabel(labelFalse); // Breaks out of while loop because ~(condition) is true

        expectPeek(TokenType.RBRACE);
        printNonTerminal("/whileStatement");
    }

    // 'if' '(' expression ')' '{' statements '}' ( 'else' '{' statements '}' )?
    void parseIf() {
        printNonTerminal("ifStatement");

        var labelTrue = "IF_TRUE" + ifLabelNum;
        var labelFalse = "IF_FALSE" + ifLabelNum;
        var labelEnd = "IF_END" + ifLabelNum;

        ifLabelNum++;

        expectPeek(TokenType.IF);
        expectPeek(TokenType.LPAREN);
        parseExpression();
        expectPeek(TokenType.RPAREN);
        // **
        vmWriter.writeIf(labelTrue);
        vmWriter.writeGoto(labelFalse);
        vmWriter.writeLabel(labelTrue);

        // **

        expectPeek(TokenType.LBRACE);
        parseStatements();
        expectPeek(TokenType.RBRACE);

        if (peekTokenIs(TokenType.ELSE)) {
            vmWriter.writeGoto(labelEnd);
        }

        vmWriter.writeLabel(labelFalse);

        if (peekTokenIs(TokenType.ELSE)) {
            expectPeek(TokenType.ELSE);
            expectPeek(TokenType.LBRACE);
            parseStatements();
            expectPeek(TokenType.RBRACE);
            vmWriter.writeLabel(labelEnd);
        }

        printNonTerminal("/ifStatement");
    }

    void parseDo() {
        printNonTerminal("doStatement");
        expectPeek(TokenType.DO);
        expectPeek(TokenType.IDENT);
        parseSubroutineCall(); 
        expectPeek(TokenType.SEMICOLON);
        vmWriter.writePop(Segment.TEMP, 0);
        printNonTerminal("/doStatement");
    }

    void parseReturn() {
        printNonTerminal("returnStatement");
        expectPeek(TokenType.RETURN);

        if (!peekTokenIs(TokenType.SEMICOLON)) {
            parseExpression();
        } else {
            vmWriter.writePush(Segment.CONST, 0);
        }
        expectPeek(TokenType.SEMICOLON);

        vmWriter.writeReturn();
        printNonTerminal("/returnStatement");
    }

    void parseSubroutineCall() {
        // **
        var numArg = 0;
        var ident = currentToken.lexeme;
        var simbol = symbolTable.resolve(ident); // CLASS OR OBJECT
        var functName = ident + ".";
        // **
        // método da propria classe
        if (peekTokenIs(TokenType.LPAREN)) {
            expectPeek(TokenType.LPAREN);
            vmWriter.writePush(Segment.POINTER, 0);
            numArg = parseExpressionList() + 1;
            expectPeek(TokenType.RPAREN);
            functName = className + "." + ident;
        } else {
            // pode ser um metodo de um outro objeto ou uma função
            expectPeek(TokenType.DOT);
            expectPeek(TokenType.IDENT); // nome da função
            if (simbol != null) {
                // é o metodo
                functName = simbol.type() + "." + currentToken.lexeme;
                vmWriter.writePush(kindSegment2(simbol.kind()), simbol.index());
                numArg = 1;

            } else {
                functName += currentToken.lexeme; // se for uma função
            }
            expectPeek(TokenType.LPAREN);
            numArg += parseExpressionList();

            expectPeek(TokenType.RPAREN);
        }
        vmWriter.writeCall(functName, numArg);

    }

    private Segment kindSegment2(Kind kind) {
        if (kind == Kind.STATIC)
            return Segment.STATIC;
        if (kind == Kind.FIELD)
            return Segment.THIS;
        if (kind == Kind.VAR)
            return Segment.LOCAL;
        if (kind == Kind.ARG)
            return Segment.ARG;
        return null;

    }

    int parseExpressionList() {
        printNonTerminal("expressionList");
        var numArg = 0;

        if (!peekTokenIs(TokenType.RPAREN)) // verifica se tem pelo menos uma expressao
        {
            parseExpression();
            numArg = 1;
        }

        // procurando as outras
        while (peekTokenIs(TokenType.COMMA)) {
            expectPeek(TokenType.COMMA);
            parseExpression();
            numArg++;
        }

        printNonTerminal("/expressionList");
        return numArg;
    }

    private boolean isOperator(TokenType type) {
        return type.ordinal() >= PLUS.ordinal() && type.ordinal() <= EQ.ordinal();
    }
    

    void compileOperators(TokenType type) {
        System.out.println(type);
        if (type == TokenType.AST) {
            vmWriter.writeCall("Math.multiply", 2);
        } else if (type == TokenType.SLASH) {
            vmWriter.writeCall("Math.divide", 2);
        } else if (type == TokenType.PLUS) {
            vmWriter.writeArithmetic(Command.ADD);
        } else {
            vmWriter.writeArithmetic(typeOperator(type));
        }

    }

    private Command typeOperator(TokenType type) {
        if (type == TokenType.PLUS)
            return Command.ADD;
        if (type == TokenType.MINUS)
            return Command.SUB;
        if (type == TokenType.LT)
            return Command.LT;
        if (type == TokenType.GT)
            return Command.GT;
        if (type == TokenType.EQ)
            return Command.EQ;
        if (type == TokenType.AND)
            return Command.AND;
        if (type == TokenType.OR)
            return Command.OR;
        return null;

    }



    



    


    //Funções Auxiliares
    public String XMLOutput () {
        return xmlOutput.toString();
    }


    private void printNonTerminal (String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }

    boolean peekTokenIs (TokenType type) {
        return peekToken.type == type;
    }

    boolean currentTokenIs (TokenType type) {
        return currentToken.type == type;
    }

    private void expectPeek (TokenType... types) {
        for (TokenType type : types) {
            if (peekToken.type == type) {
                expectPeek(type);
                return;
            }
        }

       throw error(peekToken, "Expected a statement");

    }

    private void expectPeek (TokenType type) {
        if (peekToken.type == type) {
            nextToken();
            xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
        } else {
            throw error(peekToken, "Expected "+type.name());
        }
    }

    private static void report (int line, String where, String message) {
            System.err.println(
            "[line " + line + "] Error" + where + ": " + message);
    }

    private ParseError error (Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }
        return new ParseError();
    }

    
    void parseTerm() {
        printNonTerminal("term");
        switch (peekToken.type) {
          case NUMBER:
            expectPeek(TokenType.NUMBER);
            break;
          case STRING:
            expectPeek(TokenType.STRING);
            break;
          case FALSE:
          case NULL:
          case TRUE:
            expectPeek(TokenType.FALSE, TokenType.NULL, TokenType.TRUE);
            break;
          case THIS:
            expectPeek(TokenType.THIS);
            break;
          case IDENT:
            expectPeek(TokenType.IDENT);
            break;
          default:
            throw error(peekToken, "term expected");
        }
    
        printNonTerminal("/term");
      }


    static public boolean isOperator(String op) {
        return "+-*/<>=~&|".contains(op);
    }

    void parseExpression() {
        printNonTerminal("expression");
        parseTerm ();
        while (isOperator(peekToken.lexeme)) {
            expectPeek(peekToken.type);
            parseTerm();
        }
        printNonTerminal("/expression");
    }

    /**
     * 
     */
    void parseLet() {
        printNonTerminal("letStatement");
        expectPeek(TokenType.LET);
        expectPeek(TokenType.IDENT);

        if (peekTokenIs(TokenType.LBRACKET)) {
            expectPeek(TokenType.LBRACKET);
            parseExpression();
            expectPeek(TokenType.RBRACKET);
        }

        expectPeek(TokenType.EQ);
        parseExpression();
        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/letStatement");

    }



}