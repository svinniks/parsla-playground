import com.fasterxml.jackson.databind.ObjectMapper;
import org.vinniks.parsla.exception.GrammarException;
import org.vinniks.parsla.exception.ParsingException;
import org.vinniks.parsla.grammar.Grammar;
import org.vinniks.parsla.grammar.serialization.StandardGrammarReader;
import org.vinniks.parsla.parser.Parser;
import org.vinniks.parsla.tokenizer.tokenizers.PatternTokenizer;
import parsers.JSONParser;
import parsers.JSONTokenizer;

import java.io.IOException;
import java.io.StringReader;

import static org.vinniks.parsla.tokenizer.TokenFilter.ignoreTokens;

public class Test {
    public static void main(String[] args) throws GrammarException, IOException, ParsingException {
        //testArrayIterable();
        testJSONParser();
//        testLeftRecursion();
//        testPackageGrammar();
//        testWalker();
//        testMatches();
//        var l = new ArrayList<Option>();
//        l.add(null);
//        var g = Grammar.builder().options(null).build();
//        Grammar.builder().option(null);
    }



    public static void testJSONParser() throws ParsingException, IOException {
        var parser = new JSONParser();

        var json = """
            {
                "name": "Sergejs",
                "age": 123,
                "house": null,
                "unemployed": true,
                "employed": false,
                "wife": {
                    "name": "Natalja"
                },
                "children": [
                    {
                        "name": "Alisa",
                        "age": 10
                    }
                ]
            }""";


        var objectMapper = new ObjectMapper();

        var startTime = System.currentTimeMillis();
        var tokenizer = new JSONTokenizer();

        for (var i = 1; i <= 500000; i++) {
//            objectMapper.readTree(new StringReader(json));
            parser.parse(json);
//            System.out.println(parser.parse(json));
//            var it = parser.getTokenizer().getTokenIterator(new StringReader(json));
//            var it = tokenizer.getTokenIterator(new StringReader(json));
//
//            while (it.hasNext()) {
//                it.next();
//            }
//
//            it.close();
        }

        System.out.println(System.currentTimeMillis() - startTime);

    }



}
