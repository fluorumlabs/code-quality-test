package org.vaadin.qa.cqt;

import org.vaadin.qa.cqt.annotations.Level;
import org.vaadin.qa.cqt.utils.HtmlFormatter;

import java.util.*;
import java.util.stream.Collectors;

import static org.vaadin.qa.cqt.utils.HtmlFormatter.value;

/**
 * Created by Artem Godin on 9/24/2020.
 */
public class InspectionResult {
    private static final HtmlFormatter CATEGORY_FORMAT = value().escapeHtml().styled("category");
    private static final HtmlFormatter MESSAGE_FORMAT = value().escapeHtml().styled("message");
    private static final HtmlFormatter ID_FORMAT = value().escapeHtml().wrapWith("[","]").styled("id");

    private final Inspection inspection;
    private final List<Reference> references = new ArrayList<>();

    public InspectionResult(Inspection inspection) {
        this.inspection = inspection;
    }

    public void add(Reference reference) {
        references.add(reference);
    }

    public boolean hasReferences() {
        return !references.isEmpty();
    }

    public List<String> format() {
        if (references.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> results = new ArrayList<>();

        Map<String, List<Reference>> referenceGroup = new HashMap<>();
        for (Reference reference : references) {
            referenceGroup.computeIfAbsent(reference.getReference(), str -> new ArrayList<>()).add(reference);
        }

        List<String> groups = referenceGroup.keySet().stream()
                .sorted()
                .collect(Collectors.toList());

        for (String group : groups) {
            StringBuilder output = new StringBuilder();

            output.append(value().styled("report "+inspection.getLevel().name().toLowerCase(Locale.ENGLISH)).format(String.format("%s%s%s %s\n",
                    CATEGORY_FORMAT.format(inspection.getCategory()),
                    "<span class='separator'>: </span>",
                    MESSAGE_FORMAT.format(inspection.getMessage()),
                    ID_FORMAT.format(inspection.getId()))));
            List<Reference> referencesInGroup = referenceGroup.get(group);
            Reference first = referencesInGroup.iterator().next();
            String contextPath = first.formatPathToContext();
            output.append(String.format("\t\tClass:         %s\n", first.formatOwnerClass()));
            output.append(String.format("\t\tContext:       %s\n", first.formatScope()));
            if (!contextPath.isEmpty()) {
                output.append(String.format("\t\tContext path:  %s\n", contextPath));
            }
            output.append(String.format("\t\tField:         %s\n", first.formatField()));
            output.append(String.format("\t\tValue:         %s\n", first.formatValue()));
            if (referencesInGroup.size()>1) {
                output.append(String.format("\t\t               ... %d other\n", referencesInGroup.size()-1));
            }
            List<String> backrefs = referencesInGroup.stream().flatMap(reference -> reference.formatBackreferences().stream())
                    .sorted()
                    .distinct()
                    .collect(Collectors.toList());

            int counter = 0;
            for (String backref : backrefs) {
                if (counter == 0) {
                    output.append(String.format("\t\tReferenced by: %s\n", backref));
                } else {
                    output.append(String.format("\t\t               %s\n", backref));
                }
                counter++;
                if (counter>first.getScanner().getMaxReferences()) {
                    output.append(String.format("\t\t               ... %d more\n", backrefs.size()-counter));
                    break;
                }
            }

            results.add(output.toString());
        }
        return results;
    }



    public Level getLevel() {
        return inspection.getLevel();
    }

    public String getInspectionId() {
        return inspection.getId();
    }

    public String getCategory() {
        return inspection.getCategory();
    }

    public String getMessage() {
        return inspection.getMessage();
    }
}
