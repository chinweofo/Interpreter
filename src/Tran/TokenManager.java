package Tran;
import java.util.List;
import java.util.Optional;

public class TokenManager {
    private List<Token> tokens;
    public TokenManager(List<Token> tokens) {
        this.tokens = tokens;
    }

    public boolean done() {
	    if (tokens.isEmpty()) {
            return true;
        }
        return false;
    }

    public Optional<Token> matchAndRemove(Token.TokenTypes t) {
	    if((!tokens.isEmpty()) && tokens.get(0).getType() == t) {
            return Optional.of(tokens.remove(0));
        }
        return Optional.empty();
    }

    public Optional<Token> peek(int i) {
        if(i >= 0 && i < tokens.size()) {
            return Optional.of(tokens.get(i));
        }
        return Optional.empty();
    }

    public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
	    if((!tokens.isEmpty() && tokens.get(0).getType() == first) && (!tokens.isEmpty() && tokens.get(1).getType() == second)) {
            return true;
        }
        return false;
    }

    public boolean nextIsEither(Token.TokenTypes first, Token.TokenTypes second) {
        if (!tokens.isEmpty() && (tokens.get(0).getType() == first || tokens.get(0).getType() == second)) {
            return true;
        } else {
            return false;
        }
    }
    //look at the first token and return the line
    public int getCurrentLine() {
        if(!tokens.isEmpty()) {
            return tokens.get(0).getLineNumber();
        }
        return '\0';
    }

    //look at the first token and return the column number
    public int getCurrentColumnNumber() {
        if(!tokens.isEmpty()) {
            return tokens.get(0).getColumnNumber();
        }
        return '\0';
    }
}

/*
* public boolean nextTwoTokensMatch(Token.TokenTypes first, Token.TokenTypes second) {
	    if((!tokens.isEmpty()) && tokens.get(0).getType() == first) {
            return !tokens.get(1).getType().equals(second);
        }
        return false;
    }
*
*
* */
