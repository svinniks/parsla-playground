package parsers;

import org.vinniks.parsla.exception.ParsingException;
import org.vinniks.parsla.tokenizer.Token;
import org.vinniks.parsla.tokenizer.TokenIterator;
import org.vinniks.parsla.tokenizer.text.AbstractBufferedTextTokenIterator;
import org.vinniks.parsla.tokenizer.text.AbstractBufferedTextTokenizer;
import org.vinniks.parsla.tokenizer.text.TextPosition;

import java.io.IOException;
import java.util.*;

public class JSONTokenizer extends AbstractBufferedTextTokenizer {
    private static final String LEFT_CURLY_BRACKET_TYPE = "left-curly-bracket";
    private static final String RIGHT_CURLY_BRACKET_TYPE = "right-curly-bracket";
    private static final String LEFT_SQUARE_BRACKET_TYPE = "left-square-bracket";
    private static final String RIGHT_SQUARE_BRACKET_TYPE = "right-square-bracket";
    private static final String STRING_TYPE = "string";
    private static final String DECIMAL_TYPE = "decimal";
    private static final String NULL_TYPE = "null";
    private static final String TRUE_TYPE = "true";
    private static final String FALSE_TYPE = "false";
    private static final String COLON_TYPE = "colon";
    private static final String COMMA_TYPE= "comma";

    private static final Token LEFT_CURLY_BRACKET_TOKEN = new Token(LEFT_CURLY_BRACKET_TYPE, null);
    private static final Token RIGHT_CURLY_BRACKET_TOKEN = new Token(RIGHT_CURLY_BRACKET_TYPE, null);
    private static final Token LEFT_SQUARE_BRACKET_TOKEN = new Token(LEFT_SQUARE_BRACKET_TYPE, null);
    private static final Token RIGHT_SQUARE_BRACKET_TOKEN = new Token(RIGHT_SQUARE_BRACKET_TYPE, null);
    private static final Token NULL_TOKEN = new Token(NULL_TYPE, null);
    private static final Token TRUE_TOKEN = new Token(TRUE_TYPE, null);
    private static final Token FALSE_TOKEN = new Token(FALSE_TYPE, null);
    private static final Token COLON_TOKEN = new Token(COLON_TYPE, null);
    private static final Token COMMA_TOKEN = new Token(COMMA_TYPE, null);

    @Override
    protected TokenIterator getTokenIterator(CharacterIterator characterIterator) {
        return new JSONTokenIterator(characterIterator);
    }

    private static class JSONTokenIterator extends AbstractBufferedTextTokenIterator {
        private enum State {
            LF_VALUE,
            R_STRING,
            R_ESCAPED_CHARACTER,
            R_DECIMAL_INTEGER,
            R_SPECIAL_VALUE
        }

        private State state;
        private final StringBuilder valueBuilder;
        private String specialValue;
        private int specialI;
        private Token specialToken;
        private TextPosition tokenPosition;

        public JSONTokenIterator(CharacterIterator characterIterator) {
            super(characterIterator);
            state = State.LF_VALUE;
            valueBuilder = new StringBuilder();
        }

        @Override
        protected void character(char c) {
            if (state == State.LF_VALUE) {
                lfValue(c);
            } else if (state == State.R_STRING) {
                rString(c);
            } else if (state == State.R_ESCAPED_CHARACTER) {
                rEscapedCharacter(c);
            } else if (state == State.R_DECIMAL_INTEGER) {
                rDecimalInteger(c);
            } else if (state == State.R_SPECIAL_VALUE) {
                rSpecialValue(c);
            }
        }

        @Override
        protected void end() {
            if (state != State.LF_VALUE) {
                throw new ParsingException("Unexpected end of the input!");
            }
        }

        private void lfValue(char c) {
            if (c == '{') {
                push(LEFT_CURLY_BRACKET_TOKEN, characterPosition());
                state = State.LF_VALUE;
            } else if (c == '}') {
                push(RIGHT_CURLY_BRACKET_TOKEN, characterPosition());
                state = State.LF_VALUE;
            } else if (c == '[') {
                push(LEFT_SQUARE_BRACKET_TOKEN, characterPosition());
                state = State.LF_VALUE;
            } else if (c == ']') {
                push(RIGHT_SQUARE_BRACKET_TOKEN, characterPosition());
                state = State.LF_VALUE;
            } else if (c == ':') {
                push(COLON_TOKEN, characterPosition());
                state = State.LF_VALUE;
            } else if (c == ',') {
                push(COMMA_TOKEN, characterPosition());
                state = State.LF_VALUE;
            } else if (c == '"') {
                valueBuilder.setLength(0);
                state = State.R_STRING;
                tokenPosition = characterPosition();
            } else if (c == 'n') {
                specialI = 1;
                specialValue = "null";
                specialToken = NULL_TOKEN;
                state = State.R_SPECIAL_VALUE;
                tokenPosition = characterPosition();
            } else if (c == 't') {
                specialI = 1;
                specialValue = "true";
                specialToken = TRUE_TOKEN;
                state = State.R_SPECIAL_VALUE;
                tokenPosition = characterPosition();
            } else if (c == 'f') {
                specialI = 1;
                specialValue = "false";
                specialToken = FALSE_TOKEN;
                state = State.R_SPECIAL_VALUE;
                tokenPosition = characterPosition();
            } else if (c >= '0' && c <= '9') {
                valueBuilder.setLength(0);
                valueBuilder.append(c);
                state = State.R_DECIMAL_INTEGER;
                tokenPosition = characterPosition();
            } else if (Character.isSpace(c)) {
                state = State.LF_VALUE;
            } else {
                throw new ParsingException(String.format("Unexpected character '%s'!", c));
            }
        }

        private void rString(char c) {
            if (c == '\\') {
                state = State.R_ESCAPED_CHARACTER;
            } else if (c == '"') {
                push(new Token(STRING_TYPE, valueBuilder.toString()), tokenPosition);
                state = State.LF_VALUE;
            } else {
                valueBuilder.append(c);
            }
        }

        private void rEscapedCharacter(char c) {
            if (c == '"') {
                valueBuilder.append('"');
                state = State.R_STRING;
            } else {
                throw new ParsingException(String.format("Unexpected escaped character '%s'!", c));
            }
        }

        private void rDecimalInteger(char c) {
            if (c >= '0' && c <= '9') {
                valueBuilder.append(c);
            } else {
                var value = valueBuilder.toString();
                var tokenPosition = this.tokenPosition;
                lfValue(c);
                push(new Token(DECIMAL_TYPE, value), tokenPosition);
            }
        }

        private void rSpecialValue(char c) {
            if (c == specialValue.charAt(specialI++)) {
                if (specialI == specialValue.length()) {
                    push(specialToken, tokenPosition);
                    state = State.LF_VALUE;
                }
            } else {
                throw new ParsingException(String.format("Unexpected character '%s'!", c));
            }
        }
    }
}
