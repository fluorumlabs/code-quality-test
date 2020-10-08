/*
 * Copyright (c) 2020 Artem Godin
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.vaadin.qa.cqt.data;

import org.vaadin.qa.cqt.annotations.Level;
import org.vaadin.qa.cqt.utils.HtmlFormatter;

import java.util.*;
import java.util.stream.Collectors;

import static org.vaadin.qa.cqt.utils.HtmlFormatter.encodeValue;
import static org.vaadin.qa.cqt.utils.HtmlFormatter.value;

/**
 * Inspection result definition.
 */
public class InspectionResult {

    private static final HtmlFormatter CATEGORY_FORMAT = value()
            .escapeHtml()
            .styled("category");

    private static final HtmlFormatter MESSAGE_FORMAT = value()
            .escapeHtml()
            .styled("message");

    private final Inspection inspection;

    private final List<Reference> references = new ArrayList<>();

    /**
     * Instantiate a new inspection result.
     *
     * @param inspection the inspection
     */
    public InspectionResult(Inspection inspection) {
        this.inspection = inspection;
    }

    /**
     * Add reference for which inspection predicate evaluates to {@code true}.
     *
     * @param reference the reference
     */
    public void add(Reference reference) {
        references.add(reference);
    }

    /**
     * Test if inspection result has any associated references.
     *
     * @return the boolean
     * @see InspectionResult#add(Reference)
     */
    public boolean hasReferences() {
        return !references.isEmpty();
    }

    /**
     * Format inspection result as a HTML
     *
     * @return the list of string containing complete inspection result
     */
    public List<String> toHtml() {
        if (references.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>();

        Map<String, List<Reference>> referenceGroup = new HashMap<>();
        for (Reference reference : references) {
            referenceGroup.computeIfAbsent(
                    reference.getId(),
                    str -> new ArrayList<>()
            ).add(reference);
        }

        List<String> groups = referenceGroup
                .keySet()
                .stream()
                .sorted()
                .collect(Collectors.toList());

        for (String group : groups) {
            StringBuilder   output            = new StringBuilder();
            List<Reference> referencesInGroup = referenceGroup.get(group);
            Reference       first             = referencesInGroup
                    .iterator()
                    .next();

            String descriptor = encodeValue("["
                                            + inspection.getId()
                                            + "] "
                                            + first.getId());

            String reportTitle = CATEGORY_FORMAT.format(inspection.getCategory())
                                 + "<span class='separator'>: </span>"
                                 + MESSAGE_FORMAT.format(inspection.getMessage())
                                 + " <span class='buttons'><a href='#' onclick='self.event.preventDefault(); comment = prompt(\"This report will be marked as suppressed for selected class/field in \\x2E\\uD835\\uDE8C\\uD835\\uDE9A\\uD835\\uDE9D\\uD835\\uDE92\\uD835\\uDE90\\uD835\\uDE97\\uD835\\uDE98\\uD835\\uDE9B\\uD835\\uDE8E file and will not appear in future scans.\\n\\nExplanation (required):\", \"False positive\"); if (comment !== null && comment.trim() !== \"\") fetch(\"suppress?\"+encodeURIComponent(\"# \"+comment+\"\\n\")+\""
                                 + descriptor
                                 + "\").then(() => self.location.reload());'>Suppress</a></span>\n";

            output
                    .append("<!-- Class: ")
                    .append(encodeValue(first.getOwnerClass().getName()))
                    .append(" --><!-- Descriptor: ")
                    .append(descriptor)
                    .append(" -->");
            output.append(value()
                                  .styled("report " + inspection
                                          .getLevel()
                                          .name()
                                          .toLowerCase(Locale.ENGLISH))
                                  .format(reportTitle));
            String contextPath = first.formatPathToScopeRoot();
            output.append(String.format(
                    "\t\tClass:         <a href='/%s/'>%s</a>\n",
                    encodeValue(first.getOwnerClass().getName()),
                    first.formatOwnerClass()
            ));
            output.append(String.format(
                    "\t\tContext:       %s\n",
                    first.formatScope()
            ));
            if (!contextPath.isEmpty()) {
                output.append(String.format(
                        "\t\tContext path:  %s\n",
                        contextPath
                ));
            }
            output.append(String.format(
                    "\t\tField:         %s\n",
                    first.formatField()
            ));
            output.append(String.format(
                    "\t\tValue:         %s\n",
                    first.formatValue()
            ));
            if (referencesInGroup.size() > 1) {
                output.append(String.format(
                        "\t\t               ... %d other\n",
                        referencesInGroup.size() - 1
                ));
            }
            List<String> backrefs = referencesInGroup
                    .stream()
                    .flatMap(reference -> reference
                            .formatBackreferences()
                            .stream())
                    .sorted()
                    .distinct()
                    .collect(Collectors.toList());

            int counter = 0;
            for (String backref : backrefs) {
                if (counter == 0) {
                    output.append(String.format(
                            "\t\tReferenced by: %s\n",
                            backref
                    ));
                } else {
                    output.append(String.format(
                            "\t\t               %s\n",
                            backref
                    ));
                }
                counter++;
                if (counter > first.getScanner().getMaxReferences()) {
                    output.append(String.format(
                            "\t\t               ... %d more\n",
                            backrefs.size() - counter
                    ));
                    break;
                }
            }

            results.add(output.toString());
        }
        return results;
    }

    /**
     * Get inspection category.
     *
     * @return the inspection category
     */
    public String getInspectionCategory() {
        return inspection.getCategory();
    }

    /**
     * Get inspection id.
     *
     * @return the inspection id
     */
    public String getInspectionId() {
        return inspection.getId();
    }

    /**
     * Get inspection severity level.
     *
     * @return the inspection severity level
     */
    public Level getInspectionLevel() {
        return inspection.getLevel();
    }

    /**
     * Get inspection message.
     *
     * @return the inspection message
     */
    public String getInspectionMessage() {
        return inspection.getMessage();
    }

}
