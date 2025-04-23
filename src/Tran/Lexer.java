package Tran;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class Lexer {
    private final TextManager textManager;
    private HashMap<String, Token.TokenTypes> keywordHash;
    private HashMap<String, Token.TokenTypes> punctuationMap;
    private LinkedList<Token> ListOfTokens;
    private int lineNumber = 1;
    private int characterPosition = 0;
    private int currentIndentation = 0;  //level (in spaces)


    public Lexer(String input) {
        textManager = new TextManager(input);
        createKeywordHash();
        createPunctuationHashMap();
    }

    private void createKeywordHash() {
        keywordHash = new HashMap<>();
        keywordHash.put("implements", Token.TokenTypes.IMPLEMENTS);
        keywordHash.put("interface", Token.TokenTypes.INTERFACE);
        keywordHash.put("class", Token.TokenTypes.CLASS);
        keywordHash.put("loop", Token.TokenTypes.LOOP);
        keywordHash.put("if", Token.TokenTypes.IF);
        keywordHash.put("else", Token.TokenTypes.ELSE);
        keywordHash.put("private", Token.TokenTypes.PRIVATE);
        keywordHash.put("shared", Token.TokenTypes.SHARED);
        keywordHash.put("construct", Token.TokenTypes.CONSTRUCT);
        keywordHash.put("new", Token.TokenTypes.NEW);
    }

    private void createPunctuationHashMap() {
        punctuationMap = new HashMap<>();
        punctuationMap.put("=", Token.TokenTypes.ASSIGN);
        punctuationMap.put("(", Token.TokenTypes.LPAREN);
        punctuationMap.put(")", Token.TokenTypes.RPAREN);
        punctuationMap.put(":", Token.TokenTypes.COLON);
        punctuationMap.put(".", Token.TokenTypes.DOT);
        punctuationMap.put("+", Token.TokenTypes.PLUS);
        punctuationMap.put("-", Token.TokenTypes.MINUS);
        punctuationMap.put("*", Token.TokenTypes.TIMES);
        punctuationMap.put("/", Token.TokenTypes.DIVIDE);
        punctuationMap.put("%", Token.TokenTypes.MODULO);
        punctuationMap.put(",", Token.TokenTypes.COMMA);
        punctuationMap.put("==", Token.TokenTypes.EQUAL);
        punctuationMap.put("!=", Token.TokenTypes.NOTEQUAL);
        punctuationMap.put("<", Token.TokenTypes.LESSTHAN);
        punctuationMap.put("<=", Token.TokenTypes.LESSTHANEQUAL);
        punctuationMap.put(">", Token.TokenTypes.GREATERTHAN);
        punctuationMap.put(">=", Token.TokenTypes.GREATERTHANEQUAL);
    }

    public List<Token> Lex() throws Exception {
        List<Token> ListOfTokens = new LinkedList<>();
        currentIndentation = 0;
        while (!textManager.isAtEnd()) {
            char c = textManager.peekCharacter(0);
            if (c == '\n') {
                ListOfTokens.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition));
                textManager.getCharacter();
                lineNumber++;
                characterPosition = 0;
                handleIndentation(ListOfTokens);
            } else if (c == ' ') {
                textManager.getCharacter(); //consume the space or tab
                //handleIndentation(ListOfTokens);
            } else if (c == '\'') {
                //handle single-quoted characters
                textManager.getCharacter();
                if (textManager.getCharacter() != '\'') {
                    throw new SyntaxErrorException("Error: Unexpected character", lineNumber, characterPosition);
                }
            } else if (c == '\"') {
                //handle double-quoted strings
                Token quotedStringToken = readQuotedString();
                ListOfTokens.add(quotedStringToken);
            } else if (c == '{') {
                textManager.getCharacter();
                while (!textManager.isAtEnd()) {
                    char nextChar = textManager.peekCharacter(0);
                    if (nextChar == '}') {
                        textManager.getCharacter();
                        break;
                    }
                    textManager.getCharacter(); //keep consuming characters inside the comment
                }
                if (textManager.isAtEnd()) {
                    throw new SyntaxErrorException("Error: Unclosed comment", lineNumber, characterPosition);
                }
            } else {
                if (Character.isLetter(c)) {
                    Token wordToken = readWord();
                    ListOfTokens.add(wordToken);
                } else if (Character.isDigit(c)) {
                    Token numberToken = readNumber();
                    ListOfTokens.add(numberToken);
                } else{
                    //Optional <Token> wordToken = Optional.of(readPunctuation());
                    Token punctuationToken = readPunctuation();
                    ListOfTokens.add(punctuationToken);

                }
            }
        }
        while(currentIndentation > 0){
            currentIndentation--;
            ListOfTokens.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
        }
        return ListOfTokens;
    }

    private void handleIndentation(List<Token> ListOfTokens) throws Exception {
        int indentCount = 0;
        while (!textManager.isAtEnd()) {
            char c = textManager.peekCharacter(0);
            if (c == ' ') {
                indentCount += 1;
            } else if (c == '\t') {
                indentCount += 4;  //1 tab = 4 spaces
            } else if(c == '\n'){
                indentCount = 0;
                //NEWLINE TOKEN
                ListOfTokens.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition));
//                textManager.getCharacter();
                lineNumber++;
                characterPosition = 0;
            }else {
                break;
            }
            textManager.getCharacter();
        }

        if (indentCount % 4 != 0) {
            throw new SyntaxErrorException("Error: Indentation must be a multiple of 4 spaces", lineNumber, characterPosition);
        }

        int newIndentLevel = indentCount / 4;

        if (newIndentLevel > currentIndentation) {
            //indentation is more indented, so add INDENT token
            ListOfTokens.add(new Token(Token.TokenTypes.INDENT, lineNumber, characterPosition));
            currentIndentation++;
        } else if (newIndentLevel < currentIndentation) {
            //indentation is less indented, so add DEDENT token
            char x = textManager.peekCharacter(0);
            if(x == '\n'){
                ListOfTokens.add(new Token(Token.TokenTypes.NEWLINE, lineNumber, characterPosition));
                textManager.getCharacter();

            }
            ListOfTokens.add(new Token(Token.TokenTypes.DEDENT, lineNumber, characterPosition));
            currentIndentation--;

        }
    }

    private Token readWord() throws Exception {
        StringBuilder buildWord = new StringBuilder();
        int startPosition = characterPosition;

        while (!textManager.isAtEnd()) {
            char next = textManager.peekCharacter(0);
            if (Character.isLetterOrDigit(next)) {
                buildWord.append(textManager.getCharacter());
            } else {
                break;
            }
        }

        String finalWord = buildWord.toString();
        if (keywordHash.containsKey(finalWord)) {
            return new Token(keywordHash.get(finalWord), lineNumber, startPosition);
        } else {
            return new Token(Token.TokenTypes.WORD, lineNumber, startPosition, finalWord);
        }
    }

    private Token readNumber() throws Exception {
        StringBuilder buildNumber = new StringBuilder();
        int startPosition = characterPosition;
        while (!textManager.isAtEnd()) {
            char next = textManager.peekCharacter(0);
            if (Character.isDigit(next)) {
                buildNumber.append(textManager.getCharacter());
                next = textManager.peekCharacter(0);
            }
            else if(next == '.'){
                buildNumber.append(textManager.getCharacter());
                next = textManager.peekCharacter(0);

            }
            else {
                break;
            }
        }
        String finalNumber = buildNumber.toString();
        return new Token(Token.TokenTypes.NUMBER, lineNumber, startPosition, finalNumber);
    }

    //if the next character mushed with the first is part of the hashmap, then do it, if it's not,
    //then only do the first punctuation
    private Token readPunctuation() throws Exception {
        int startPosition = characterPosition;
        StringBuilder buildPunctuation = new StringBuilder();
        char c = textManager.getCharacter(); //would be getting o.foo() -> o.f oo() otherwise
        String punctuation = String.valueOf(c);
        //char endCheck = textManager.peekCharacter(1);

        //if the next character after c is the end of the file, then end it
        if(!textManager.isAtEnd()){
            char next = textManager.peekCharacter(0);
            String nextPunctuation = String.valueOf(next);
            String mushedPunctuation = punctuation + nextPunctuation;

            if (punctuationMap.containsKey(mushedPunctuation)) {
                next = textManager.getCharacter();
                return new Token(punctuationMap.get(mushedPunctuation), lineNumber, characterPosition);
            }
            /*if(punctuationMap.containsKey(nextPunctuation)){
                next = textManager.getCharacter();
                return new Token(punctuationMap.get(nextPunctuation), lineNumber, characterPosition);
            }*/
        }
        if(punctuationMap.containsKey(punctuation)){
            return new Token(punctuationMap.get(punctuation), lineNumber, characterPosition);
        }
        else{
            throw new SyntaxErrorException("Error: Unexpected punctuation '" + c + "'", lineNumber, characterPosition);
        }
    }

    private Token readQuotedString() throws Exception {
        StringBuilder buildQuotedString = new StringBuilder();
        int startPosition = characterPosition;
        //consume starting quote for the loop
        textManager.getCharacter();
        while (!textManager.isAtEnd()) {
            char c = textManager.peekCharacter(0);
            if (c == '\"') {
                textManager.getCharacter();
                break;
            } else if (c == '\\') {
                textManager.getCharacter();
                if (textManager.isAtEnd()) {
                    throw new SyntaxErrorException("Error: Unexpected character", lineNumber, characterPosition);
                }
                char nextChar = textManager.peekCharacter(0);
                buildQuotedString.append(nextChar);
                textManager.getCharacter();
            } else {
                buildQuotedString.append(c);
                textManager.getCharacter();
            }
        }
        String quotedString = buildQuotedString.toString();
        return new Token(Token.TokenTypes.QUOTEDSTRING, lineNumber, startPosition, quotedString);
    }
}

