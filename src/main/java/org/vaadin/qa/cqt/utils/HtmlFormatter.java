package org.vaadin.qa.cqt.utils;

import org.apache.commons.text.StringEscapeUtils;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Created by Artem Godin on 9/30/2020.
 */
@FunctionalInterface
public interface HtmlFormatter extends Function<String, String> {
    Pattern PACKAGE_PATTERN = Pattern.compile("(\\b([a-z0-9])[a-z0-9_]*[.]\\b)");

    static HtmlFormatter value() {
        return String::toString;
    }

    static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static String trimTo(String text, int length) {
        if (text.length() < length) {
            return text;
        } else {
            return text.substring(0, length) + "...";
        }
    }

    default String format(Object o) {
        return apply(Objects.toString(o));
    }

    default String format(String str) {
        return apply(str);
    }

    default HtmlFormatter urlEncode() {
        return andThen(HtmlFormatter::encodeValue);
    }

    default HtmlFormatter trim() {
        return andThen(String::trim);
    }

    default HtmlFormatter removeNewLines() {
        return andThen(str -> str.replace('\n', ' ').replace('\r', ' '));
    }

    default HtmlFormatter trimTo(int len) {
        return andThen(str -> trimTo(str, len));
    }

    default HtmlFormatter wrapWith(String str) {
        return wrapWith(str, str);
    }

    default HtmlFormatter wrapWith(String left, String right) {
        return andThen(s -> String.join("", left, s, right));
    }

    default HtmlFormatter replace(Pattern pattern, String replacement) {
        return andThen(s -> pattern.matcher(s).replaceAll(replacement));
    }

    default HtmlFormatter removePackages() {
        return replace(PACKAGE_PATTERN, "");
    }

    default HtmlFormatter shortenPackages() {
        return replace(PACKAGE_PATTERN, "$2");
    }

    default HtmlFormatter decorate(String tag) {
        return andThen(s -> s.isEmpty()?"":String.join("", "<", tag, ">", s, "</", tag, ">"));
    }

    default HtmlFormatter decorate(String tag, Map<String, String> attributes) {
        if (attributes.isEmpty()) {
            return decorate(tag);
        }
        String attrs = attributes.entrySet().stream()
                .map(entry -> String.join("", entry.getKey(), "=\"", escapeHtml4(entry.getValue()), "\""))
                .collect(Collectors.joining(" "));
        return andThen(s -> s.isEmpty()?"":String.join("", "<", tag, " ", attrs, ">", s, "</", tag, ">"));
    }

    default HtmlFormatter decorate(String tag, String attribute, String value) {
        return andThen(s -> s.isEmpty()?"":String.join("", "<", tag, " ", attribute, "=\"", escapeHtml4(value), "\">", s, "</", tag, ">"));
    }

    default HtmlFormatter styled(String className) {
        return decorate("span", "class", className);
    }

    default HtmlFormatter escapeHtml() {
        return andThen(StringEscapeUtils::escapeHtml4);
    }

    default HtmlFormatter escapeJava() {
        return andThen(StringEscapeUtils::escapeJava);
    }

    default <V> HtmlFormatter andThen(HtmlFormatter after) {
        Objects.requireNonNull(after);
        return s -> after.apply(apply(s));
    }

}
