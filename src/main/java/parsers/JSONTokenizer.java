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

    private static class JSONTokenIterator implements AbstractBufferedTextTokenIterator<TextPosition> {
        private enum State {
            LF_VALUE,
            R_STRING,
            R_ESCAPED_CHARACTER,
            R_DECIMAL_INTEGER,
            R_SPECIAL_VALUE
        }

        private final CharacterIterator characterIterator;
        private State state;
        private char c;
        private final Deque<Token> tokens;
        private final StringBuilder valueBuilder;
        private String specialValue;
        private int specialI;
        private Token specialToken;
        private TextPosition textPosition;

        public JSONTokenIterator(CharacterIterator characterIterator) {
            this.characterIterator = characterIterator;
            state = State.LF_VALUE;
            valueBuilder = new StringBuilder();
            //valueBuilder.setLength(0);
            tokens = new ArrayDeque<>();
        }

        @Override
        public boolean hasNext() throws IOException {
            ensureNextToken();
            return !tokens.isEmpty();
        }

        @Override
        public Token next() throws IOException {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            return tokens.pop();
        }

        @Override
        public TextPosition position() {
            return null;
        }

        private void ensureNextToken() throws IOException {
            if (tokens.isEmpty() && characterIterator.hasNext()) {
                while (tokens.isEmpty() && characterIterator.hasNext()) {
                    c = characterIterator.next();

                    if (state == State.LF_VALUE) {
                        lfValue();
                    } else if (state == State.R_STRING) {
                        rString();
                    } else if (state == State.R_ESCAPED_CHARACTER) {
                        rEscapedCharacter();
                    } else if (state == State.R_DECIMAL_INTEGER) {
                        rDecimalInteger();
                    } else if (state == State.R_SPECIAL_VALUE) {
                        rSpecialValue();
                    }
                }

                if (tokens.isEmpty()) {
                    if (state != State.LF_VALUE) {
                        throw new ParsingException("Unexpected end of the input!");
                    }
                }
            }
        }

        private void lfValue() {
            if (c == '{') {
                tokens.push(LEFT_CURLY_BRACKET_TOKEN);
                state = State.LF_VALUE;
            } else if (c == '}') {
                tokens.push(RIGHT_CURLY_BRACKET_TOKEN);
                state = State.LF_VALUE;
            } else if (c == '[') {
                tokens.push(LEFT_SQUARE_BRACKET_TOKEN);
                state = State.LF_VALUE;
            } else if (c == ']') {
                tokens.push(RIGHT_SQUARE_BRACKET_TOKEN);
                state = State.LF_VALUE;
            } else if (c == ':') {
                tokens.push(COLON_TOKEN);
                state = State.LF_VALUE;
            } else if (c == ',') {
                tokens.push(COMMA_TOKEN);
                state = State.LF_VALUE;
            } else if (c == '"') {
                valueBuilder.setLength(0);
                state = State.R_STRING;
            } else if (c == 'n') {
                specialI = 1;
                specialValue = "null";
                specialToken = NULL_TOKEN;
                state = State.R_SPECIAL_VALUE;
            } else if (c == 't') {
                specialI = 1;
                specialValue = "true";
                specialToken = TRUE_TOKEN;
                state = State.R_SPECIAL_VALUE;
            } else if (c == 'f') {
                specialI = 1;
                specialValue = "false";
                specialToken = FALSE_TOKEN;
                state = State.R_SPECIAL_VALUE;
            } else if (c >= '0' && c <= '9') {
                valueBuilder.setLength(0);
                valueBuilder.append(c);
                state = State.R_DECIMAL_INTEGER;
            } else if (Character.isSpace(c)) {
                state = State.LF_VALUE;
            } else {
                throw new ParsingException(String.format("Unexpected character '%s'!", c));
            }
        }

        private void rString() {
            if (c == '\\') {
                state = State.R_ESCAPED_CHARACTER;
            } else if (c == '"') {
                tokens.push(new Token(STRING_TYPE, valueBuilder.toString()));
                state = State.LF_VALUE;
            } else {
                valueBuilder.append(c);
            }
        }

        private void rEscapedCharacter() {
            if (c == '"') {
                valueBuilder.append('"');
                state = State.R_STRING;
            } else {
                throw new ParsingException(String.format("Unexpected escaped character '%s'!", c));
            }
        }

        private void rDecimalInteger() {
            if (c >= '0' && c <= '9') {
                valueBuilder.append(c);
            } else {
                var value = valueBuilder.toString();
                lfValue();
                tokens.push(new Token(DECIMAL_TYPE, value));
            }
        }

        private void rSpecialValue() {
            if (c == specialValue.charAt(specialI++)) {
                if (specialI == specialValue.length()) {
                    tokens.push(specialToken);
                    state = State.LF_VALUE;
                }
            } else {
                throw new ParsingException(String.format("Unexpected character '%s'!", c));
            }
        }
    }
}
