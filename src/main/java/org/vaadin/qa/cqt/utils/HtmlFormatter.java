package org.vaadin.qa.cqt.utils;

import org.apache.commons.text.StringEscapeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * Html formatter helper.
 */
@FunctionalInterface
public interface HtmlFormatter extends Function<String, String> {

    /**
     * Pattern matching packages in class name.
     */
    Pattern PACKAGE_PATTERN = Pattern.compile("(\\b([a-z0-9])[a-z0-9_]*[.]\\b)");

    /**
     * Encode value string via {@link URLEncoder#encode(String, Charset)}.
     *
     * @param value the value
     * @return the string
     */
    static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Trim string to specified length.
     *
     * @param text   the text
     * @param length the length
     * @return the string
     */
    static String trimTo(String text, int length) {
        if (text.length() < length) {
            return text;
        } else {
            return text.substring(0, length) + "...";
        }
    }

    /**
     * Dummy formatter.
     *
     * @return the html formatter returning the same string
     */
    static HtmlFormatter value() {
        return String::toString;
    }

    /**
     * Chain formatters.
     *
     * @param <V>   the type parameter
     * @param after the after
     * @return the html formatter
     */
    default <V> HtmlFormatter andThen(HtmlFormatter after) {
        Objects.requireNonNull(after);
        return s -> after.apply(apply(s));
    }

    /**
     * Decorate string with html tag.
     *
     * @param tag the tag
     * @return the html formatter
     */
    default HtmlFormatter decorate(String tag) {
        return andThen(s -> s.isEmpty()
                            ? ""
                            : String.join("",
                                          "<",
                                          tag,
                                          ">",
                                          s,
                                          "</",
                                          tag,
                                          ">"
                            ));
    }

    /**
     * Decorate string with html tag with attributes.
     *
     * @param tag        the tag
     * @param attributes the attributes
     * @return the html formatter
     */
    default HtmlFormatter decorate(String tag, Map<String, String> attributes) {
        if (attributes.isEmpty()) {
            return decorate(tag);
        }
        String attrs = attributes.entrySet()
                .stream()
                .map(entry -> String.join("",
                                          entry.getKey(),
                                          "=\"",
                                          escapeHtml4(entry.getValue()),
                                          "\""
                ))
                .collect(Collectors.joining(" "));
        return andThen(s -> s.isEmpty()
                            ? ""
                            : String.join("",
                                          "<",
                                          tag,
                                          " ",
                                          attrs,
                                          ">",
                                          s,
                                          "</",
                                          tag,
                                          ">"
                            ));
    }

    /**
     * Decorate string with html tag and a single attribute.
     *
     * @param tag       the tag
     * @param attribute the attribute
     * @param value     the value
     * @return the html formatter
     */
    default HtmlFormatter decorate(String tag, String attribute, String value) {
        return andThen(s -> s.isEmpty() ? "" : String.join("",
                                                           "<",
                                                           tag,
                                                           " ",
                                                           attribute,
                                                           "=\"",
                                                           escapeHtml4(value),
                                                           "\">",
                                                           s,
                                                           "</",
                                                           tag,
                                                           ">"
        ));
    }

    /**
     * Escape html entities.
     *
     * @return the html formatter
     */
    default HtmlFormatter escapeHtml() {
        return andThen(StringEscapeUtils::escapeHtml4);
    }

    /**
     * Escape characters not allowed in Java strings.
     *
     * @return the html formatter
     */
    default HtmlFormatter escapeJava() {
        return andThen(StringEscapeUtils::escapeJava);
    }

    /**
     * Converts object to string via {@link Objects#toString(Object)}.
     *
     * @param o the o
     * @return the string
     */
    default String format(Object o) {
        return apply(Objects.toString(o));
    }

    /**
     * Applies formatter to string.
     *
     * @param str the str
     * @return the string
     */
    default String format(String str) {
        return apply(str);
    }

    /**
     * Remove new lines.
     *
     * @return the html formatter
     */
    default HtmlFormatter removeNewLines() {
        return andThen(str -> str.replace('\n', ' ').replace('\r', ' '));
    }

    /**
     * Remove packages from full class name.
     *
     * @return the html formatter
     */
    default HtmlFormatter removePackages() {
        return replace(PACKAGE_PATTERN, "");
    }

    /**
     * Replace pattern matches with string.
     *
     * @param pattern     the pattern
     * @param replacement the replacement
     * @return the html formatter
     */
    default HtmlFormatter replace(Pattern pattern, String replacement) {
        return andThen(s -> pattern.matcher(s).replaceAll(replacement));
    }

    /**
     * Shorten packages to first character.
     *
     * @return the html formatter
     */
    default HtmlFormatter shortenPackages() {
        return replace(PACKAGE_PATTERN, "$2");
    }

    /**
     * Decorate string with {@code <span class="...">}
     *
     * @param className the class name
     * @return the html formatter
     */
    default HtmlFormatter styled(String className) {
        return decorate("span", "class", className);
    }

    /**
     * Trim string.
     *
     * @return the html formatter
     */
    default HtmlFormatter trim() {
        return andThen(String::trim);
    }

    /**
     * Trim to string to specific length.
     *
     * @param len the len
     * @return the html formatter
     */
    default HtmlFormatter trimTo(int len) {
        return andThen(str -> trimTo(str, len));
    }

    /**
     * Encode string via {@link URLEncoder#encode(String, Charset)}.
     *
     * @return the html formatter
     */
    default HtmlFormatter urlEncode() {
        return andThen(HtmlFormatter::encodeValue);
    }

    /**
     * Wrap string between specified strings.
     *
     * @param str the str
     * @return the html formatter
     */
    default HtmlFormatter wrapWith(String str) {
        return wrapWith(str, str);
    }

    /**
     * Wrap string between specified strings.
     *
     * @param left  the left
     * @param right the right
     * @return the html formatter
     */
    default HtmlFormatter wrapWith(String left, String right) {
        return andThen(s -> String.join("", left, s, right));
    }

}
