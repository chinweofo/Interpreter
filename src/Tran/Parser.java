/*
*               Questions
* 2. ParseInterface()
*   What would I be returning?
* 3. ParseParameterVariableDeclarationS
*   Is the formatting of the method correct?
*
*   Am I handling the method correctly?
*   Can I use peek here as well?
* 4. ParseParameterVariableDeclaration()
*   How do I write the IDENTIFIER part?
*   What would I be returning?
* 5. Tran()
*   How would I fix the quit and change nothing part?
*
*
*/

package Tran;
import AST.*;

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
        //ParseInterface();
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.INTERFACE){
            top.Interfaces.add(ParseInterface().get());
        }
    }

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
        /*if(tokenManager.matchAndRemove(Token.TokenTypes.NEWLINE).isPresent()){
            requireNewLine();
        }*/
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
            requireNewLine();
        }
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

        /*
        while(tokenManager.peek(0).get().getType() == Token.TokenTypes.WORD){
            methodHeader = ParseMethodHeader();
            interfaceNode.methods.add(methodHeader.get());
        }*/

        //DEDENT
        if(tokenManager.matchAndRemove(Token.TokenTypes.DEDENT).isEmpty()){
            throw new SyntaxErrorException("Missing Dedent Token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        return Optional.of(interfaceNode);
    }

    //Around 10 lines
    //MethodHeader = IDENTIFIER "(" ParameterVariableDeclarations ")" (":" ParameterVariableDeclarations)? NEWLINE
    Optional<MethodHeaderNode> ParseMethodHeader() throws SyntaxErrorException {
        //IDENTIFIER (method name)
        var methodHeaderToken = tokenManager.matchAndRemove(Token.TokenTypes.WORD);
        if(methodHeaderToken.isEmpty()){
            return Optional.empty();
        }
        MethodHeaderNode methodHeader = new MethodHeaderNode();
        methodHeader.name = methodHeaderToken.get().getValue(); //gets string as a token

        //"("
        if(tokenManager.matchAndRemove(Token.TokenTypes.LPAREN).isEmpty()){
            throw new SyntaxErrorException("Missing token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }

        //ParameterVariableDeclarations
        var paramVariableDeclarations = ParseParameterVariableDeclarations();

        while(!paramVariableDeclarations.isEmpty()){
            methodHeader.parameters.add(paramVariableDeclarations.get(0));
            paramVariableDeclarations = ParseParameterVariableDeclarations();
        }

        //")"
        if(tokenManager.matchAndRemove(Token.TokenTypes.RPAREN).isEmpty()){
            throw new SyntaxErrorException("Missing token", tokenManager.getCurrentLine(), tokenManager.getCurrentColumnNumber());
        }
        //":"
        if(tokenManager.matchAndRemove(Token.TokenTypes.COLON).isPresent()){
            List<VariableDeclarationNode> varDecNode = ParseParameterVariableDeclarations();
            //if(varDecNode.isEmpty()){
                //return Optional.empty();
            //}
            while(!varDecNode.isEmpty()){
                methodHeader.returns.add(varDecNode.get(0));
                varDecNode = ParseParameterVariableDeclarations();
            }
        }
        //NEWLINE
        if(tokenManager.peek(0).get().getType() == Token.TokenTypes.NEWLINE){
            requireNewLine();
        }

        return Optional.of(methodHeader);
    }

    //Around 10 lines
    //ParameterVariableDeclarations =  ParameterVariableDeclaration  ("," ParameterVariableDeclaration)*
    //if token is returned then get both otherwise return optional empty                                ----Fix
    private List<VariableDeclarationNode> ParseParameterVariableDeclarations() throws SyntaxErrorException {
        ArrayList<VariableDeclarationNode> newParam = new ArrayList<>();
        //ParameterVariableDeclaration - initial
        //                                                                                  ----Fix

        var checkParseParam = ParseParameterVariableDeclaration();
        while(!checkParseParam.isEmpty()){
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
    Optional <VariableDeclarationNode> ParseParameterVariableDeclaration() throws SyntaxErrorException {
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
}

/*
* Parser 2
*
* ----Write-----
Class() : 45 lines
Constructor(): 12 lines
Member() : 15 lines
MethodDeclaration(): 18 lines
Statements(): 15 lines
Statement(): 5 lines
If() : 18 lines
Loop(): 12 lines
*
*
* */