package Tran;
public class TextManager {
    private final String input;
    private int position = 0; //position of the current char being evaluated

    public TextManager(String input) {
        this.input = input;
    }

    public boolean isAtEnd() {
        return position >= input.length();
    }

    public char peekCharacter() {
        if(position < input.length()){
            return input.charAt(position);
        }
        return '\0';
    }

    public char peekCharacter(int dist) {
        int distPosition = position + dist;
        if (distPosition < input.length()) {
            return input.charAt(distPosition);
        }
        return '\0';
    }

    //is getCharacter incrementing properly?
    public char getCharacter() {
        if (position < input.length()) {
            return input.charAt(position++);
        }
        return '\0';
    }
}

