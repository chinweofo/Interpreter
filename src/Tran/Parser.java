package Tran;
import AST.*;

import java.beans.Expression;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


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
    //if token is returned then get both otherwise return optional empty
    private List<VariableDeclarationNode> ParseParameterVariableDeclarations() throws SyntaxErrorException {
        ArrayList<VariableDeclarationNode> newParam = new ArrayList<>();
        var checkParseParam = ParseParameterVariableDeclaration();
        while(checkParseParam.isPresent()){
            newParam.add(checkParseParam.get());
            if(tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
                checkParseParam = ParseParameterVariableDeclaration();
            }
            else{
                return newParam;
            }
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
        //( Constructor | MethodDeclaration | Member )*
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
            methodDec.name = methodHeader.get().name;
            methodDec.parameters = methodHeader.get().parameters;
            methodDec.returns = methodHeader.get().returns;
        }
        //methodDec.name = methodHeader.get().name;

        //NEWLINE
        //requireNewLine();

        //MethodBody
        //var methodBodyDec = ParseMethodBody();
        Optional<MethodDeclarationNode> bodyStatement = ParseMethodBody();
        if(bodyStatement.isPresent()){
            methodDec.statements.addAll(bodyStatement.get().statements);
            methodDec.locals.addAll(bodyStatement.get().locals);
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
            newVarDec = ParseVariableDeclarations();  //Keep parsing until no more variables are found
        }

        //Statement*                                                                                -- is this correct?
        Optional<StatementNode> methodStatement = ParseStatement();
        while (methodStatement.isPresent()) {
            methodDec.statements.add(methodStatement.get());
            methodStatement = ParseStatement();  //Keep parsing until no more statements are found
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
            newStatement = ParseStatement();
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
        //check for methodCall or assignment using disambiguate()
        return disambiguate();
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
        Optional<ExpressionNode> presentBoolExpTerm = ParseBoolExpTerm();
        if(presentBoolExpTerm.isEmpty()){
            throw new SyntaxErrorException("If statements must have a BoolExpTerm", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        ifNode.condition = presentBoolExpTerm.get();

        //NEWLINE
        requireNewLine();

        //Statements
        ifNode.statements = ParseStatements();

        //("else" NEWLINE (Statement | Statements)) ?
        if(tokenManager.matchAndRemove(Token.TokenTypes.ELSE).isPresent()){
            //NEWLINE
            requireNewLine();
            //(Statement | Statements)
            ElseNode elseNode = new ElseNode();
            Optional<StatementNode> statement = ParseStatement();
            if (statement.isPresent()) {
                elseNode.statements = new ArrayList<>();
                elseNode.statements.add(statement.get());
            } else {
                elseNode.statements = ParseStatements();
            }

            ifNode.elseStatement = Optional.of(elseNode);

        }else{
            return Optional.of(ifNode);
        }

        return Optional.of(ifNode);
    }

    //Around 12 lines
    //Loop = "loop" (VariableReference "=" )?  ( BoolExpTerm ) NEWLINE Statements
    private Optional <LoopNode> ParseLoop() throws SyntaxErrorException {
        LoopNode loopNode = new LoopNode();
        //"loop"
        if(tokenManager.matchAndRemove(Token.TokenTypes.LOOP).isEmpty()){
            return Optional.empty();
        }

        //if next two are word then assign
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)){
            Optional<VariableReferenceNode> newVarRef = ParseVariableReference();
            if(newVarRef.isEmpty()){
                throw new SyntaxErrorException("Expected a Variable Reference", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            if(tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()){
                throw new SyntaxErrorException("Expected '=' after variable in loop assignment", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            loopNode.assignment = newVarRef;
        }

        //BoolExpTerm
        Optional<ExpressionNode> boolExpTerm = ParseBoolExpTerm();
        if (boolExpTerm.isEmpty()) {
            throw new SyntaxErrorException("Expected boolean expression in loop", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        loopNode.expression = boolExpTerm.get();

        //NEWLINE
        requireNewLine();

        //Statements
        loopNode.statements = ParseStatements();

        return Optional.of(loopNode);
    }

    //Around 30 lines
    //BoolExpTerm = MethodCallExpression | (Expression ( "==" | "!=" | "<=" | ">=" | ">" | "<" ) Expression) | VariableReference
    private Optional <ExpressionNode> ParseBoolExpTerm() throws SyntaxErrorException {
        //MethodCallExpression
        Optional<MethodCallExpressionNode> methodCall = ParseMethodCallExpression();
        if (methodCall.isPresent()) {
            return Optional.of(methodCall.get());
        }

        //Expression
        Optional<ExpressionNode> boolExpLeft = ParseExpression();
        if(boolExpLeft.isEmpty()){
            return Optional.empty();
        }

        Optional <Token> peekType = tokenManager.peek(0);
        if (peekType.isPresent()) {
            //use switch cases statements to try all in the CompareNode class
            Token.TokenTypes tokenType = peekType.get().getType();
            CompareNode.CompareOperations op = null;
            switch(tokenType){
                //"=="
                case EQUAL:
                    op = CompareNode.CompareOperations.eq;
                //"!="
                case NOTEQUAL:
                    op = CompareNode.CompareOperations.ne;
                // "<="
                case LESSTHANEQUAL:
                    op = CompareNode.CompareOperations.le;
                //">="
                case GREATERTHANEQUAL:
                    op = CompareNode.CompareOperations.ge;
                //"<"
                case LESSTHAN:
                    op = CompareNode.CompareOperations.lt;
                //">"
                case GREATERTHAN:
                    op = CompareNode.CompareOperations.gt;
            }
            if(op != null){
                tokenManager.matchAndRemove(tokenType);
                //Expression
                var boolExpRight = ParseExpression();
                if(boolExpRight.isEmpty()){
                    throw new SyntaxErrorException("Expected right side of the expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                CompareNode compareNode = new CompareNode();
                compareNode.op = op;
                compareNode.left = boolExpLeft.get();
                compareNode.right = boolExpRight.get();

                return Optional.of(compareNode);
            }
        }

        //VariableReference
        var varRef = ParseVariableReference();
        if (varRef.isPresent()) {
            return Optional.of(varRef.get());
        }

        return Optional.empty();
    }

    //VariableReference = IDENTIFIER
    private Optional <VariableReferenceNode> ParseVariableReference() throws SyntaxErrorException {
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
        VariableDeclarationNode varDecNode = new VariableDeclarationNode();

        var checkType = tokenManager.peek(0).get().getType();
        if(!tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){
            //var tokenType = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            return Optional.empty();
        }
        varDecNode.type = tokenManager.peek(0).get().getValue();

        Optional<VariableDeclarationNode> varNameValue = ParseVariableNameValue();
        if (varNameValue.isEmpty()) {
            return Optional.empty();
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
        VariableDeclarationNode varDecNode = new VariableDeclarationNode();
        var varDecType = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(varDecType.isEmpty()){
            return Optional.empty();
        }
        varDecNode.type = varDecType.get().getValue();

        var varDecName = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(varDecName.isEmpty()){
            return Optional.empty();
        }
        varDecNode.name = varDecName.get().getValue();
        if (tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isPresent()) {
            //var expression = ParseExpression();
            Optional<ExpressionNode> expression = ParseExpression();
            if (expression.isEmpty()) {
                //throw new SyntaxErrorException("Expected expression after '=' in variable declaration", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                return Optional.empty();
            }
            varDecNode.initializer = expression;
        }
        return Optional.of(varDecNode);
    }

    //Expression = Term ( ("+"|"-") Term )*
    private Optional <ExpressionNode> ParseExpression() throws SyntaxErrorException {
        //ExpressionNode expressionNode = new ExpressionNode();
        //ExpressionNode expressionNode = null;
        var expNode = ParseVariableReference().get();
        return Optional.of(expNode);
    }

                                                    /* Parser 3*/

    //MethodCallExpression =  (IDENTIFIER ".")? IDENTIFIER "(" (Expression ("," Expression )* )? ")"
    //stub out MethodCallExpression() to return Optional.empty() for now
    private Optional <MethodCallExpressionNode> ParseMethodCallExpression() throws SyntaxErrorException {
        MethodCallExpressionNode methodCallExpNode = new MethodCallExpressionNode();
        return Optional.empty();
    }

    private Optional<StatementNode> disambiguate() throws SyntaxErrorException {
        //First call MethodCallExpression() - if there is a value, call methodCallStatementNode and return it
        Optional <MethodCallExpressionNode> methodCallExpNode = ParseMethodCallExpression();
        if(methodCallExpNode.isPresent()){
            MethodCallStatementNode methodCallStmtNode = new MethodCallStatementNode(methodCallExpNode.get());
            return Optional.of(methodCallStmtNode);
        }

        //use next two tokens match
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.COMMA)){
            Optional<MethodCallStatementNode> methodCall = ParseMethodCall();
            if (methodCall.isEmpty()) {
                throw new SyntaxErrorException("Expected a Method Call", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            return Optional.of(methodCall.get());
        }
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.ASSIGN)){
            Optional <AssignmentNode> assnNode = ParseAssignment();
            if (assnNode.isEmpty()) {
                throw new SyntaxErrorException("Expected an Assignment Call", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            return Optional.of(assnNode.get());
        }

        return Optional.empty();
    }


    //MethodCall = (VariableReference ( "," VariableReference )* "=")? MethodCallExpression NEWLINE
    private Optional<MethodCallStatementNode> ParseMethodCall() throws SyntaxErrorException {
        MethodCallStatementNode methodCSNode = new MethodCallStatementNode();

        //(VariableReference ( "," VariableReference )* "=")?
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD){
            Optional <VariableReferenceNode> varRefMethodCall = ParseVariableReference();
            if (varRefMethodCall.isEmpty()) {
                return Optional.empty();
            }
            methodCSNode.returnValues.add(varRefMethodCall.get());
            while(tokenManager.nextTwoTokensMatch(Token.TokenTypes.COMMA, Token.TokenTypes.WORD)){
                varRefMethodCall = ParseVariableReference();
                if (varRefMethodCall.isEmpty()) {
                    throw new SyntaxErrorException("Expected Variable Reference after ',", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                methodCSNode.returnValues.add(varRefMethodCall.get());
            }

            //"="
            if(tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()){
                throw new SyntaxErrorException("Expected '=' after variable in loop assignment", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
        }

        //MethodCallExpression
        Optional<MethodCallExpressionNode> methodCallExp = ParseMethodCallExpression();
        if (methodCallExp.isEmpty()) {
            throw new SyntaxErrorException("Expected a Method Call Expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        MethodCallExpressionNode methodExprNode = methodCallExp.get();
        methodCSNode.objectName = methodExprNode.objectName;
        methodCSNode.methodName = methodExprNode.methodName;
        methodCSNode.parameters = methodExprNode.parameters;

        //NEWLINE
        requireNewLine();

        return Optional.of(methodCSNode);
    }

    //Assignment = VariableReference "=" Expression NEWLINE
    private Optional<AssignmentNode> ParseAssignment() throws SyntaxErrorException {
        AssignmentNode assignmentNode = new AssignmentNode();

        //VariableReference
        //var varRef = ParseVariableReference();
        Optional<VariableReferenceNode> varRef = ParseVariableReference();
        if(varRef.isEmpty()){
            throw new SyntaxErrorException("Expected a Variable Reference", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            //return Optional.empty();
        }
        assignmentNode.target = varRef.get();

        // "="
        if(tokenManager.matchAndRemove(Token.TokenTypes.ASSIGN).isEmpty()){
            throw new SyntaxErrorException("Missing Assignment token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //Expression
        Optional<ExpressionNode> assnExpression = ParseExpression();
        if(assnExpression.isEmpty()){
            throw new SyntaxErrorException("Expected an Expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            //return Optional.empty();
        }
        assignmentNode.expression = assnExpression.get();

        //NEWLINE
        requireNewLine();

        return Optional.of(assignmentNode);
    }



}
