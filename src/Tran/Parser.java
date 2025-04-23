package Tran;
import AST.*;

import java.beans.Expression;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/*      Questions
* 2. Fix ParseBoolExpTerm()
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
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
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
        return Optional.empty();
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

        //( "implements" IDENTIFIER ( "," IDENTIFIER )* )?
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
        while(tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD || tokenManager.peek(0).get().getType() == Token.TokenTypes.CONSTRUCT || tokenManager.peek(0).get().getType() == Token.TokenTypes.PRIVATE || tokenManager.peek(0).get().getType() == Token.TokenTypes.SHARED){
            Optional <ConstructorNode> getConstruct = ParseConstructor();
            if(getConstruct.isPresent()){
                classNode.constructors.add(getConstruct.get());
            }

            Optional <MethodDeclarationNode> methodDec = ParseMethodDeclaration();
            if(methodDec.isPresent()){
                classNode.methods.add(methodDec.get());
            }

            if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.WORD)){
                Optional <MemberNode> memberDec = ParseMember();
                if(memberDec.isPresent()){
                    classNode.members.add(memberDec.get());
                }
            }
        }

        if(!tokenManager.done() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
            requireNewLine();
        }

        //DEDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Dedent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
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
        /*
        if(methodHeader.isPresent()) {
            methodDec.name = methodHeader.get().name;
            methodDec.parameters = methodHeader.get().parameters;
            methodDec.returns = methodHeader.get().returns;
        }*/
        if(methodHeader.isEmpty()){
            return Optional.empty();
        }
        methodDec.name = methodHeader.get().name;
        methodDec.parameters = methodHeader.get().parameters;
        methodDec.returns = methodHeader.get().returns;


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
        //(VariableDeclarations)*
        Optional<VariableDeclarationNode> newVarDec = ParseVariableDeclarations();
        while (newVarDec.isPresent()) {
            methodDec.locals.add(newVarDec.get());
            newVarDec = ParseVariableDeclarations();  //Keep parsing until no more variables are found
        }

        //Statement*
        Optional<StatementNode> methodStatement = ParseStatement();
        while (methodStatement.isPresent()) {
            methodDec.statements.add(methodStatement.get());
            if(!tokenManager.done() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
                requireNewLine();
            }
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
            if(!tokenManager.done() && tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
                requireNewLine();
            }
        }

        //DEDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Dedent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        return methodStatementList;
    }

    //Around 5 lines
    //Statement = If | Loop | MethodCall | Assignment
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

    //Around 30 lines - ExpressionNode here is good
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
                    break;
                //"!="
                case NOTEQUAL:
                    op = CompareNode.CompareOperations.ne;
                    break;
                // "<="
                case LESSTHANEQUAL:
                    op = CompareNode.CompareOperations.le;
                    break;
                //">="
                case GREATERTHANEQUAL:
                    op = CompareNode.CompareOperations.ge;
                    break;
                //"<"
                case LESSTHAN:
                    op = CompareNode.CompareOperations.lt;
                    break;
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

    //About 13 lines
    //Expression = Term ( ("+"|"-") Term )*
    private Optional <ExpressionNode> ParseExpression() throws SyntaxErrorException {
        //var expNode = ParseVariableReference().get();
        MathOpNode mathOpNode = new MathOpNode();

        //Term
        Optional<ExpressionNode> expTermLeft = ParseTerm();
        if (expTermLeft.isEmpty()) {
            return Optional.empty();
        }

        Optional <Token> peekType = tokenManager.peek(0);
        while (peekType.isPresent()) {
            Token.TokenTypes tokenType = peekType.get().getType();
            MathOpNode.MathOperations op = null;

            //("+"| "-")
            switch(tokenType){
                //"+"
                case PLUS:
                    op = MathOpNode.MathOperations.add;
                    break;
                case MINUS:
                    op = MathOpNode.MathOperations.subtract;
            }
            if(op != null){
                tokenManager.matchAndRemove(tokenType);
                //Term
                var expTermRight = ParseTerm();
                if(expTermRight.isEmpty()){
                    throw new SyntaxErrorException("Expected right side of the expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                mathOpNode.op = op;
                mathOpNode.left = expTermLeft.get();
                mathOpNode.right = expTermRight.get();

                peekType = tokenManager.peek(0);
                if(peekType.get().getType() == Token.TokenTypes.PLUS || peekType.get().getType() == Token.TokenTypes.MINUS) {
                    Token.TokenTypes tokenType2 = peekType.get().getType();
                    MathOpNode.MathOperations op2 = null;
                    MathOpNode newMathOpNode2 = new MathOpNode();
                    switch(tokenType2){
                        //"+"
                        case PLUS:
                            op2 = MathOpNode.MathOperations.add;
                            break;
                        case MINUS:
                            op2 = MathOpNode.MathOperations.subtract;
                    }
                    tokenManager.matchAndRemove(tokenType2);
                    newMathOpNode2.op = op2;
                    newMathOpNode2.left = mathOpNode;
                    newMathOpNode2.right = ParseTerm().get();

                    return Optional.of(newMathOpNode2);
                }

                return Optional.of(mathOpNode);
            }
            return Optional.of(expTermLeft.get());
        }
        return Optional.of(expTermLeft.get());
    }

                                                    /* Parser 3*/

    //MethodCallExpression =  (IDENTIFIER ".")? IDENTIFIER "(" (Expression ("," Expression )* )? ")"
    private Optional <MethodCallExpressionNode> ParseMethodCallExpression() throws SyntaxErrorException {
        MethodCallExpressionNode mceNode = new MethodCallExpressionNode();

        //(IDENTIFIER ".")?
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT)){
            var mceObjectName = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if(mceObjectName.isEmpty()){
                return Optional.empty();
            }
            var mceDotRemove = tokenManager.matchAndRemove(Token.TokenTypes.DOT);
            if(mceDotRemove.isEmpty()){
                return Optional.empty();
            }
            mceNode.objectName = Optional.of(mceObjectName.get().getValue());
        }
        else{
            mceNode.objectName = Optional.empty();
        }

        //IDENTIFIER
        if(!tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){//mceMethodName.isEmpty()
            return Optional.empty();
        }
        var mceMethodName = tokenManager.matchAndRemove(Token.TokenTypes.WORD);

        mceNode.methodName = mceMethodName.get().getValue(); //gets string as a token

        //"("
        if(tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()){
            throw new SyntaxErrorException("Missing left parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //(Expression ("," Expression )* )?
        Optional<ExpressionNode> assnExpression = ParseExpression();
        if (assnExpression.isPresent()) {
            mceNode.parameters.add(assnExpression.get());
            while(tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
                assnExpression = ParseExpression();
                if(assnExpression.isEmpty()){
                    throw new SyntaxErrorException("Expected expression after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                mceNode.parameters.add(assnExpression.get());
            }
        }

        //")"
        if(tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()){
            throw new SyntaxErrorException("Missing right parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        return Optional.of(mceNode);
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
            while (tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()) {
                varRefMethodCall = ParseVariableReference();
                methodCSNode.returnValues.add(varRefMethodCall.get());
            }

            /*while(tokenManager.nextTwoTokensMatch(Token.TokenTypes.COMMA, Token.TokenTypes.WORD)){
                varRefMethodCall = ParseVariableReference();
                if (varRefMethodCall.isEmpty()) {
                    throw new SyntaxErrorException("Expected Variable Reference after ','", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                methodCSNode.returnValues.add(varRefMethodCall.get());
            }*/

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

                                                 /* Parser 4 */

    //both Term and Factor are <ExpressionNode>

    //Term = Factor ( ("*"|"/"|"%") Factor )* - uses MathOpNode
    private Optional<ExpressionNode> ParseTerm() throws SyntaxErrorException {
        MathOpNode mathOpNode = new MathOpNode();

        //Factor
        Optional<ExpressionNode> termFactorLeft = ParseFactor();
        if (termFactorLeft.isEmpty()) {
            return Optional.empty();
        }
        mathOpNode.left = termFactorLeft.get();

        Optional<Token> peekType = tokenManager.peek(0);

        while (peekType.isPresent()) {
            MathOpNode.MathOperations op = null;
            Token.TokenTypes tokenType = peekType.get().getType();
            //("*"| "-/"|"%")
            switch(tokenType){
                //"*"
                case TIMES:
                    op = MathOpNode.MathOperations.multiply;
                    break;
                //"/"
                case DIVIDE:
                    op = MathOpNode.MathOperations.divide;
                    break;
                //"%"
                case MODULO:
                    op = MathOpNode.MathOperations.modulo;
            }

            if(op != null){
                tokenManager.matchAndRemove(tokenType);
                //Term
                var termFactorRight = ParseFactor();
                if(termFactorRight.isEmpty()){
                    throw new SyntaxErrorException("Expected right side of the expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                }
                mathOpNode.op = op;
                mathOpNode.left = termFactorLeft.get();
                mathOpNode.right = termFactorRight.get();

                peekType = tokenManager.peek(0);
                if(peekType.get().getType() == Token.TokenTypes.TIMES || peekType.get().getType() == Token.TokenTypes.MODULO || peekType.get().getType() == Token.TokenTypes.DIVIDE) {
                    Token.TokenTypes tokenType2 = peekType.get().getType();
                    MathOpNode.MathOperations op2 = null;
                    MathOpNode newMathOpNode2 = new MathOpNode();
                    switch(tokenType2){
                        //"*"
                        case TIMES:
                            op2 = MathOpNode.MathOperations.multiply;
                            break;
                        //"/"
                        case DIVIDE:
                            op2 = MathOpNode.MathOperations.divide;
                            break;
                        //"%"
                        case MODULO:
                            op2 = MathOpNode.MathOperations.modulo;
                    }
                    tokenManager.matchAndRemove(tokenType2);
                    newMathOpNode2.op = op2;
                    newMathOpNode2.left = mathOpNode;
                    newMathOpNode2.right = ParseFactor().get();

                    return Optional.of(newMathOpNode2);
                }

                return Optional.of(mathOpNode);
            }
            return Optional.of(termFactorLeft.get());
        }
        return Optional.of(termFactorLeft.get());
    }

    //About 54 lines
    //Factor = NUMBER | VariableReference |  STRINGLITERAL | CHARACTERLITERAL | MethodCallExpression | "(" Expression ")" | "new" IDENTIFIER "(" (Expression ("," Expression )*)? ")"
    private Optional<ExpressionNode> ParseFactor() throws SyntaxErrorException {
        //NUMBER
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.NUMBER){
            NumericLiteralNode numLitNode = new NumericLiteralNode();
            var currentNum = tokenManager.matchAndRemove(Token.TokenTypes.NUMBER);
            if(currentNum.isEmpty()){
                throw new SyntaxErrorException("Expected a Number", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            numLitNode.value = Float.parseFloat(currentNum.get().getValue());
            return Optional.of(numLitNode);
        }



        //STRINGLITERAL
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.QUOTEDSTRING){
            StringLiteralNode strLitNode = new StringLiteralNode();
            var currentStr = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDSTRING);
            if(currentStr.isEmpty()){
                throw new SyntaxErrorException("Expected a string", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            strLitNode.value = currentStr.get().getValue();
            return Optional.of(strLitNode);
        }

        //CHARACTERLITERAL
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.QUOTEDCHARACTER){
            CharLiteralNode charLitNode = new CharLiteralNode();
            var currentChar = tokenManager.matchAndRemove(Token.TokenTypes.QUOTEDCHARACTER);
            if(currentChar.isEmpty()){
                throw new SyntaxErrorException("Expected a character", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            charLitNode.value = currentChar.get().getValue().charAt(0);
            return Optional.of(charLitNode);
        }

        //MethodCallExpression
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.DOT) || tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
            var methodCallExp = ParseMethodCallExpression();
            if(methodCallExp.isEmpty()){
                throw new SyntaxErrorException("Expected a Method Call Expression", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            return Optional.of(methodCallExp.get());
        }

        //"(" Expression ")"
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.LPAREN){
            //"("
            if(tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()){
                throw new SyntaxErrorException("Missing left parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            //Expression
            Optional<ExpressionNode> boolExpLeft = ParseExpression();
            if(boolExpLeft.isEmpty()){
                throw new SyntaxErrorException("Expected boolean expression in loop", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            //")"
            if(tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()){
                throw new SyntaxErrorException("Missing right parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            return Optional.of(boolExpLeft.get());
        }

        //VariableReference - MOVE BEFORE EXPRESSION TOO - try
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD){
            Optional <VariableReferenceNode> varRefNode = ParseVariableReference();
            if (varRefNode.isEmpty()) {
                throw new SyntaxErrorException("Expected a variable reference", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            return Optional.of(varRefNode.get());
        }

        //"new" IDENTIFIER "(" (Expression ("," Expression )*)? ")"
        //"new"
        if(tokenManager.matchAndRemove(Token.TokenTypes.NEW).isPresent()){
            NewNode newNode = new NewNode();
            //IDENTIFIER
            var newIdentifier = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
            if(newIdentifier.isEmpty()){
                return Optional.empty();
            }
            newNode.className = newIdentifier.get().getValue();

            //"("
            if(tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()){
                throw new SyntaxErrorException("Missing left parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }

            //(Expression ("," Expression)*)?
            Optional<ExpressionNode> expr = ParseExpression();
            if (expr.isPresent()) {
                newNode.parameters.add(expr.get());
                while(tokenManager.matchAndRemove(Token.TokenTypes.COMMA).isPresent()){
                    expr = ParseExpression();
                    if(expr.isEmpty()){
                        throw new SyntaxErrorException("Expected expression after comma", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
                    }
                    newNode.parameters.add(expr.get());
                }
            }

            //")"
            if(tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()){
                throw new SyntaxErrorException("Missing right parenthesis", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
            }
            return Optional.of(newNode);
        }

        return Optional.empty();
    }


}
/*

Assignment 6

MethodCallExpression() : 30 lines
Expression(): 13 lines
Term(): 15 lines
Factor() : 54 lines



private Optional<MethodHeaderNode> ParseMethodHeader() throws SyntaxErrorException {
        //IDENTIFIER (method name) - "x("
        if(tokenManager.nextTwoTokensMatch(Token.TokenTypes.WORD, Token.TokenTypes.LPAREN)){
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
        return Optional.empty();
    }






* */

