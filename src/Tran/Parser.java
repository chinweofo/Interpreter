package Tran;
import AST.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*      Questions
* 1.
* */

public class Parser {
    private final TranNode top;
    private TokenManager tokenManager;

    public Parser(TranNode top, List<Token> tokens) {
        this.top = top;
        this.tokenManager = new TokenManager(tokens);
    }

    //Tran = ( Class | Interface ) - | = try both Class and Interface
    public void Tran() throws SyntaxErrorException {
        while(!tokenManager.done()){
            //ParseInterface();
            if(tokenManager.peek(0).get().getType() == Token.TokenTypes.INTERFACE){
                top.Interfaces.add(ParseInterface().get());
            }
            if(tokenManager.peek(0).get().getType() == Token.TokenTypes.CLASS){
                top.Classes.add(ParseClass().get());
            }
        }

    }

                                                        /*Parser 1*/

    //Around 8 lines of code
    //>= 1 newlines, otherwise < 1 throw new exception error
    private void requireNewLine() throws SyntaxErrorException {
        boolean foundOne = false;
        //Consume at least one NEWLINE
        while (tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()) {
            foundOne = true;
        }
        //Ensure at least one NEWLINE was found or the next token is DEDENT
        if (foundOne || tokenManager.peek(0).orElseThrow().getType() == Token.TokenTypes.DEDENT) {
            return; //just end
        } else {
            throw new SyntaxErrorException("Expected at least one new line", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
    }

    //Around 20 lines of code
    //Interface = "interface" IDENTIFIER NEWLINE INDENT MethodHeader* DEDENT
    private Optional<InterfaceNode>ParseInterface() throws SyntaxErrorException {
        //"interface"
        if(tokenManager.matchAndRemove(Token.TokenTypes.INTERFACE).isEmpty()){
            return Optional.empty();
        }
        //IDENTIFIER
        var interfaceToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(interfaceToken.isEmpty()){
            return Optional.empty();
        }
        InterfaceNode interfaceNode= new InterfaceNode();
        interfaceNode.name = interfaceToken.get().getValue(); //gets string as a token

        //NEWLINE
        requireNewLine();

        //INDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Indent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //MethodHeader
        Optional<MethodHeaderNode> methodHeader = ParseMethodHeader();

        while(methodHeader.isPresent()){
            interfaceNode.methods.add(methodHeader.get());
            methodHeader = ParseMethodHeader();
        }
        
        //DEDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Dedent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        if(!tokenManager.done() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
            requireNewLine();
        }

        return Optional.of(interfaceNode);
    }

    //Around 10 lines
    //MethodHeader = IDENTIFIER "(" ParameterVariableDeclarations ")" (":" ParameterVariableDeclarations)? NEWLINE
    private Optional<MethodHeaderNode> ParseMethodHeader() throws SyntaxErrorException {
        //IDENTIFIER (method name) - "x("
        var methodHeaderToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(methodHeaderToken.isEmpty()){
            return Optional.empty();
        }
        MethodHeaderNode methodHeader = new MethodHeaderNode();
        methodHeader.name = methodHeaderToken.get().getValue(); //gets string as a token


        //"("
        if(tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()){
            throw new SyntaxErrorException("Missing left parenthesis token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //ParameterVariableDeclarations
        var paramVariableDeclarations = ParseParameterVariableDeclarations();
// methodHeadr.params = paramVariableDeclarations

        methodHeader.parameters = paramVariableDeclarations;
        //")"
        if(tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()){
            throw new SyntaxErrorException("Missing right parenthesis token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //":"
        if(tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()){
            paramVariableDeclarations = ParseParameterVariableDeclarations();
            if(!paramVariableDeclarations.isEmpty()){
                methodHeader.returns = paramVariableDeclarations;
            }
            else{
                throw new SyntaxErrorException("Expected parameter variable declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }
        //NEWLINE
        requireNewLine();

        return Optional.of(methodHeader);
    }

    //Around 10 lines
    //ParameterVariableDeclarations =  ParameterVariableDeclaration  ("," ParameterVariableDeclaration)*
    //if token is returned then get both otherwise return optional empty                                ----Fix
    private List<VariableDeclarationNode> ParseParameterVariableDeclarations() throws SyntaxErrorException {
        ArrayList<VariableDeclarationNode> newParam = new ArrayList<>();
        var checkParseParam = ParseParameterVariableDeclaration();
        while(checkParseParam.isPresent()){
            newParam.add(checkParseParam.get());
            if(tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
                checkParseParam = ParseParameterVariableDeclaration();
            }
            return newParam;
        }
        return newParam;
    }

    //Around 8 lines
    //ParameterVariableDeclaration = IDENTIFIER IDENTIFIER
    private Optional <VariableDeclarationNode> ParseParameterVariableDeclaration() throws SyntaxErrorException {
        //IDENTIFIER (method name)
        var typeVar = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(typeVar.isEmpty()){
            return Optional.empty();
        }
        VariableDeclarationNode paramVarDeclaration = new VariableDeclarationNode();
        paramVarDeclaration.type = typeVar.get().getValue(); //gets string as a token
        //IDENTIFIER
        var nameVar = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(nameVar.isEmpty()){
            return Optional.empty();
        }
        paramVarDeclaration.name = nameVar.get().getValue(); //gets string as a token
        return Optional.of(paramVarDeclaration);
    }

                                                    /*Parser 2*/

    //Around 45 lines
    //Class =  "class" IDENTIFIER ( "implements" IDENTIFIER ( "," IDENTIFIER )* )? NEWLINE INDENT ( Constructor | MethodDeclaration | Member )* DEDENT
    private Optional<ClassNode> ParseClass() throws SyntaxErrorException {
        // "class"
        if(tokenManager.matchAndRemove(Token.TokenTypes.CLASS).isEmpty()){
            return Optional.empty();
        }
        //IDENTIFIER
        var classToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(classToken.isEmpty()){
            return Optional.empty();
        }
        ClassNode classNode = new ClassNode();
        classNode.name = classToken.get().getValue(); //gets string as a token

        //( "implements" IDENTIFIER ( "," IDENTIFIER )* )?                                  --is this handling correct?
        //"implements"
        if(tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent()){
            String in_name = tokenManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
            classNode.interfaces.add(in_name);
            while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
                classNode.interfaces.add(tokenManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue());
            }
        }

        //NEWLINE
        requireNewLine();

        //INDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Indent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        //( Constructor | MethodDeclaration | Member )*                                     --is this handling correct?
        while(tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD || tokenManager.peek(0).get().getType() == Token.TokenTypes.CONSTRUCT){
            if(tokenManager.peek(0).get().getType() == Token.TokenTypes.CONSTRUCT){ //peeking too much
                classNode.constructors.add(ParseConstructor().get());
            }
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
                var methodDec = ParseMethodDeclaration();
                if(methodDec.isPresent()){
                    classNode.methods.add(methodDec.get());
                }
            }
            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){
                var memberDec = ParseMember();
                if(memberDec.isPresent()){
                    classNode.members.add(memberDec.get());
                }
            }
        }
        //DEDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Indent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        if(!tokenManager.done() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
            requireNewLine();
        }
        return Optional.of(classNode);
    }

    //About 12 lines
    //Constructor = "construct" "(" ParameterVariableDeclarations ")" NEWLINE MethodBody
    private Optional <ConstructorNode> ParseConstructor() throws SyntaxErrorException {
        //"construct"
        if(tokenManager.matchAndRemove(Token.TokenTypes.CONSTRUCT).isEmpty()){
            return Optional.empty();
        }
        //"("
        if(tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()){
            throw new SyntaxErrorException("Missing left Parenthesis token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        //ParameterVariableDeclarations
        var paramVariableDeclarations = ParseParameterVariableDeclarations();
        ConstructorNode constNode = new ConstructorNode();
        while(!paramVariableDeclarations.isEmpty()){
            constNode.parameters.add(paramVariableDeclarations.get(0));
            paramVariableDeclarations = ParseParameterVariableDeclarations();
        }
        //")"
        if(tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()){
            throw new SyntaxErrorException("Missing right parenthesis token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        //NEWLINE
        requireNewLine();

        //MethodBody
        //var methodDec = ParseMethodBody();
        Optional<MethodDeclarationNode> bodyStatement = ParseMethodBody();
        if(bodyStatement.isPresent()){
            constNode.statements.addAll(bodyStatement.get().statements);
        }

        return Optional.of(constNode);
    }

    //Around 18 lines
    //MethodDeclaration = "private"? "shared"? MethodHeader NEWLINE MethodBody
    private Optional <MethodDeclarationNode> ParseMethodDeclaration() throws SyntaxErrorException {
        MethodDeclarationNode methodDec = new MethodDeclarationNode();
        //private?
        if (tokenManager.matchAndRemove(Token.TokenTypes.PRIVATE).isPresent()) {
            methodDec.isPrivate = true;
        }
        //shared?
        if (tokenManager.matchAndRemove(Token.TokenTypes.SHARED).isPresent()) {
            methodDec.isShared = true;
        }
        //MethodHeader
        Optional<MethodHeaderNode> methodHeader = ParseMethodHeader();
        if(methodHeader.isPresent()) {
            methodDec.name = String.valueOf(methodHeader.get());
        }
        //methodDec.name = methodHeader.get().name;

        //NEWLINE
        //requireNewLine();

        //MethodBody
        //var methodBodyDec = ParseMethodBody();
        Optional<MethodDeclarationNode> bodyStatement = ParseMethodBody();
        if(bodyStatement.isPresent()){
            methodDec.statements.addAll(bodyStatement.get().statements);
        }
        return Optional.of(methodDec);
    }

    //MethodBody = INDENT ( VariableDeclarations )*  Statement* DEDENT
    //contains all the code within one indentation level of the method
    private Optional <MethodDeclarationNode> ParseMethodBody() throws SyntaxErrorException {
        MethodDeclarationNode methodDec = new MethodDeclarationNode();
        //INDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Indent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        //(VariableDeclarations)*                                                                   -- is this correct?
        Optional<VariableDeclarationNode> newVarDec = ParseVariableDeclarations();
        while (newVarDec.isPresent()) {
            methodDec.locals.add(newVarDec.get());
            newVarDec = ParseVariableDeclarations();  // Keep parsing until no more variables are found
        }

        //Statement*                                                                                -- is this correct?
        Optional<StatementNode> methodStatement = ParseStatement();
        while (methodStatement.isPresent()) {
            methodDec.statements.add(methodStatement.get());
            methodStatement = ParseStatement();  // Keep parsing until no more statements are found
        }
        //DEDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Dedent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        return Optional.of(methodDec);
    }

    //Around 15 lines
    //Statements = INDENT Statement*  DEDENT
    private List<StatementNode> ParseStatements() throws SyntaxErrorException {
        //INDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.INDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Indent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        //Statement*
        var newStatement = ParseStatement(); //create new statement
        ArrayList<StatementNode> methodStatementList = new ArrayList<>();
        while(newStatement.isPresent()){
            //add to list of statements
            methodStatementList.add(newStatement.get());
        }
        //DEDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Dedent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        return methodStatementList;
    }

    //Around 5 lines
    //Statement = If | Loop | MethodCall | Assignment
    //should I make this private void instead of StatementNode?
    private Optional <StatementNode> ParseStatement() throws SyntaxErrorException {
        //If
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.IF){
            Optional<IfNode> ifNode = ParseIf();
            return Optional.of(ifNode.get());
        }
        //Loop
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.LOOP){
            Optional<LoopNode> loopNode = ParseLoop();
            return Optional.of(loopNode.get());
        }
        return Optional.empty();
    }

    //Around 18 lines
    //If = "if" BoolExpTerm NEWLINE Statements ("else" NEWLINE (Statement | Statements))?
    private Optional <IfNode> ParseIf() throws SyntaxErrorException {
        IfNode ifNode = new IfNode();
        //"if"
        if(tokenManager.matchAndRemove(Token.TokenTypes.IF).isEmpty()){
            return Optional.empty();
        }

        //BoolExpTerm
        var presentBoolExpTerm = ParseBoolExpTerm();
        if(presentBoolExpTerm.isEmpty()){
            throw new SyntaxErrorException("If statements must have a BoolExpTerm", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        ifNode.condition = presentBoolExpTerm.get();

        //NEWLINE
        requireNewLine();

        //Statements
        ArrayList<StatementNode> ifStatementsList = new ArrayList<>(); //StatementNode doesn't have a list feature
        var ifStatements = ParseStatements();
        while(!ifStatements.isEmpty()){
            ifStatementsList.add(ifStatements.get(0));
        }
        ifNode.statements = ifStatementsList;

        //("else" NEWLINE (Statement | Statements))?
        if(tokenManager.matchAndRemove(Token.TokenTypes.ELSE).isPresent()){
            //NEWLINE
            requireNewLine();
            ElseNode elseNode = new ElseNode();
            elseNode.statements = ParseStatements(); // Parse statements for the else block
            ifNode.elseStatement = Optional.of(elseNode);
        }else {
            ifNode.elseStatement = Optional.empty();
        }
        return Optional.of(ifNode);
    }

    //Around 12 lines
    //Loop = "loop" (VariableReference "=" )?  ( BoolExpTerm ) NEWLINE Statements
    private Optional <LoopNode> ParseLoop() throws SyntaxErrorException {
        LoopNode loopNode = new LoopNode();
        //"if"
        if(tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isEmpty()){
            return Optional.empty();
        }
        var newVarRef = ParseVariableReferenceNode();
        if(newVarRef.isPresent()){
            if(tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()){
                throw new SyntaxErrorException("Variable Reference Node must have an assignment", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            loopNode.assignment = newVarRef;
        }
        //NEWLINE
        requireNewLine();

        //Statements
        loopNode.statements = ParseStatements();

        return Optional.of(loopNode);
    }

    //BoolExpTerm = MethodCallExpression | (Expression ( "==" | "!=" | "<=" | ">=" | ">" | "<" ) Expression) | VariableReference
    private Optional <BooleanOpNode> ParseBoolExpTerm(){
        BooleanOpNode boolExpTerm = new BooleanOpNode();
        return Optional.of(boolExpTerm);
    }

    //VariableReference = IDENTIFIER
    private Optional <VariableReferenceNode> ParseVariableReferenceNode() throws SyntaxErrorException {
        //IDENTIFIER
        VariableReferenceNode varRefNode = new VariableReferenceNode();
        var varRefToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(varRefToken.isEmpty()){
            return Optional.empty();
        }
        varRefNode.name = varRefToken.get().getValue(); //gets string as a token

        return Optional.of(varRefNode);
    }

    //About 15 lines
    //Member = VariableDeclarations
    private Optional <MemberNode> ParseMember() throws SyntaxErrorException {
        var getVarDec = ParseVariableDeclarations();
        if (getVarDec.isEmpty()) {
            return Optional.empty();
        }
        MemberNode memberNode = new MemberNode();
        memberNode.declaration = getVarDec.get();
        return Optional.of(memberNode);
    }

    //VariableDeclarations =  IDENTIFIER VariableNameValue ("," VariableNameValue)* NEWLINE
    private Optional <VariableDeclarationNode> ParseVariableDeclarations() throws SyntaxErrorException {
        // IDENTIFIER
        var typeToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if (typeToken.isEmpty()) {
            return Optional.empty();
        }
        VariableDeclarationNode varDecNode = new VariableDeclarationNode();
        varDecNode.type = typeToken.get().getValue();  // Set type

        Optional<VariableDeclarationNode> varNameValue = ParseVariableNameValue();
        if (varNameValue.isEmpty()) {
            throw new SyntaxErrorException("Expected variable name after type", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //name and initializer
        varDecNode.name = varNameValue.get().name;
        varDecNode.initializer = varNameValue.get().initializer;

        //("," VariableNameValue)*
        while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
            varNameValue = ParseVariableNameValue();
            if (varNameValue.isEmpty()) {
                throw new SyntaxErrorException("Expected variable declaration after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }
        //NEWLINE
        requireNewLine();
        return Optional.of(varDecNode);
    }

    //VariableNameValue = IDENTIFIER ( "=" Expression)?
    private Optional <VariableDeclarationNode> ParseVariableNameValue() throws SyntaxErrorException {
        VariableDeclarationNode varDecNode= new VariableDeclarationNode();
        var varDecToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(varDecToken.isEmpty()){
            return Optional.empty();
        }
        varDecNode.name = varDecToken.get().getValue();
        if (tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()) {
            var expression = ParseExpression();
            if (expression.isEmpty()) {
                throw new SyntaxErrorException("Expected expression after '=' in variable declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            varDecNode.initializer = expression;
        }
        return Optional.of(varDecNode);
    }

    //Expression = Term ( ("+"|"-") Term )*
    private Optional <ExpressionNode> ParseExpression() throws SyntaxErrorException {
        //ExpressionNode expressionNode = new ExpressionNode();
        ExpressionNode expressionNode = null;
        return Optional.of(expressionNode);
    }

}
/*

*/

 /*
        while(tokenManager.matchAndRemove(Token.TokenTypes.IMPLEMENTS).isPresent()){
           String in_name = tokenManager.matchAndRemove(Token.TokenTypes.WORD).get().getValue();
           classNode.interfaces.add(in_name);
           if(tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
               classNode.interfaces.add(in_name);
           }
        }
*/
