package parsers;

import org.vinniks.parsla.grammar.Grammar;
import org.vinniks.parsla.parser.Parser;
import org.vinniks.parsla.parser.TextParser;
import org.vinniks.parsla.syntaxtree.SyntaxTreeNode;

import java.io.IOException;
import java.util.stream.Collectors;

public class JSONParser {
    private static final TextParser PARSER;

    static {
        var grammar =  Grammar.readStandard("""
            json: {>null};
            json: {>string, >};
            json: {>decimal, >};
            json: >boolean;
            json: object;
            json: array;
            boolean: {>true};
            boolean: {>false};
            >object: {left-curly-bracket} properties {right-curly-bracket};
            properties: ^;
            properties: property properties-tail;
            properties-tail: ^;
            properties-tail: {comma} property properties-tail;
            >property: name {colon} value;
            >name: {string, >};
            >value: json;
            >array: {left-square-bracket} elements {right-square-bracket};
            elements: ^;
            elements: json elements-tail;
            elements-tail: ^;
            elements-tail: {comma} json elements-tail;"""
        );


        PARSER = new TextParser(grammar, new JSONTokenizer());
    }

    public String parse(String json) throws IOException {
        PARSER.validate(json, "json");
//        return PARSER.parse(json, "json").toString();
        return null;
        //return syntaxTree.toString();
        //return buildJSON(syntaxTree.child());
    }

    private String buildJSON(SyntaxTreeNode node) {
        if (node.valueIs("OBJECT")) {
            return "{"
                + node
                    .children()
                    .stream()
                    .map(this::buildProperty)
                    .collect(Collectors.joining(", "))
                + "}";
        } else if (node.valueIs("ARRAY")) {
            return "["
                + node
                .children()
                .stream()
                .map(this::buildJSON)
                .collect(Collectors.joining(", "))
                + "]";
        } else if (node.valueIs("DECIMAL")) {
            return node.childValue();
        } else if (node.valueIs("STRING")) {
            return '"' + node.childValue() + '"';
        } else if (node.valueIs("BOOLEAN")) {
            return node.childValue().toLowerCase();
        } else {
            return "null";
        }
    }

    private String buildProperty(SyntaxTreeNode node) {
        return node.singular("NAME")
            + ": "
            + buildJSON(node.child("VALUE").child());
    }
}
